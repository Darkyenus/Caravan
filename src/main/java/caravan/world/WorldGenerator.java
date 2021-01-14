package caravan.world;

import caravan.components.Components;
import caravan.components.PositionC;
import caravan.components.TownC;
import caravan.services.EntitySpawnService;
import caravan.services.WorldService;
import caravan.util.CSVWriter;
import caravan.util.PooledArray;
import caravan.util.PriceList;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.IntArray;
import com.darkyen.retinazer.Engine;
import com.darkyen.retinazer.Mapper;
import org.jetbrains.annotations.NotNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import static caravan.services.TownSystem.simulateInternalEconomy;
import static caravan.util.Util.kernelSide;
import static caravan.util.Util.manhattanKernel;
import static caravan.util.Util.maxIndex;
import static caravan.util.Util.rRound;

/**
 * Generates game worlds.
 */
public final class WorldGenerator {

	public static void generateWorld(@NotNull Engine engine, long seed, final int width, final int height) {
		final WorldService world = engine.getService(WorldService.class);

		final WorldAttribute<Tile> tiles = world.tiles;
		final RandomXS128 random = new RandomXS128(seed);

		final WorldAttributeFloat altitude = generateHeight(width, height, random);
		final WorldAttributeFloat slope = altitude.slope();

		final WorldAttributeFloat windX = new WorldAttributeFloat(width, height, 0f);
		final WorldAttributeFloat windY = new WorldAttributeFloat(width, height, 0f);
		generateWind(windX, windY, altitude, random);

		final WorldAttributeFloat temperature = generateTemperature(altitude, random);
		final WorldAttributeFloat precipitation = generatePrecipitation(temperature, altitude, windX, windY, random);
		final WorldAttributeFloat forestMap = generateForests(altitude, temperature, precipitation, random);
		final WorldAttributeFloat pastureMap = generatePastures(temperature, precipitation, random);

		// Generate rivers
		//TODO

		final WorldAttributeFloat fishMap = new WorldAttributeFloat(width, height, 0f, (x, y, v) -> {
			if (altitude.get(x, y) <= 0f) {
				return 1f;
			}
			// TODO(jp): Rivers
			return 0f;
		});

		// Place tiles
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				final float h = altitude.get(x, y);
				if (h <= 0f){
					tiles.set(x, y, Tiles.Water);
					continue;
				}

				if (slope.get(x, y) > 0.03f || h > 5f) {
					tiles.set(x, y, Tiles.Rock);
					continue;
				}

				final float frs = forestMap.get(x, y);
				if (frs > 0.5f) {
					tiles.set(x, y, Tiles.Forest);
					continue;
				}

				final float pst = pastureMap.get(x, y);
				if (pst > 0.5f) {
					tiles.set(x, y, Tiles.Grass);
					continue;
				}

				tiles.set(x, y, Tiles.Desert);
			}
		}

		// Generate cities
		final WorldAttributeFloat townPlacementScore = new WorldAttributeFloat(width, height, 0f);
		townPlacementScore.fill((x, y, v) -> {
			final float alt = altitude.get(x, y);
			if (alt <= 0f) {
				// No towns under the sea
				return Float.NEGATIVE_INFINITY;
			}

			final float wood = forestMap.getKernelMax(x, y, MINERAL_REACH_KERNEL, MINERAL_REACH_KERNEL_SIZE, MINERAL_REACH_KERNEL_SIZE);
			final float pasture = pastureMap.getKernelMax(x, y, MINERAL_REACH_KERNEL, MINERAL_REACH_KERNEL_SIZE, MINERAL_REACH_KERNEL_SIZE);
			final float fish = fishMap.getKernelMax(x, y, MINERAL_REACH_KERNEL, MINERAL_REACH_KERNEL_SIZE, MINERAL_REACH_KERNEL_SIZE);

			final boolean saltWaterSource = altitude.getKernelMin(x, y, null, 5, 5) <= 0f;
			// TODO(jp): Incorporate river computation and use harsher precipitation cutoff (don't forget to change town computation as well)
			final boolean freshWaterSource = precipitation.getKernelMax(x, y, MINERAL_REACH_KERNEL, MINERAL_REACH_KERNEL_SIZE, MINERAL_REACH_KERNEL_SIZE) >= 0.4f;

			float score = 0;
			score += freshWaterSource ? 0.6f : 0f;
			score += saltWaterSource ? 0.2f : 0f;
			score += wood * 0.5f;
			score += pasture * 0.5f;
			score += fish * 0.5f;
			return score;
		});
		townPlacementScore.add(random.nextLong(), 30, 2f);// Some interesting randomness

		final Mapper<TownC> townMapper = engine.getMapper(TownC.class);
		final Mapper<PositionC> positionMapper = engine.getMapper(PositionC.class);
		final IntArray townEntities = new IntArray();

		final int townCount = 24;
		float[] rareMetal = new float[townCount];
		float[] metal = new float[townCount];
		float[] coal = new float[townCount];
		float[] jewel = new float[townCount];
		float[] stone = new float[townCount];
		float[] limestone = new float[townCount];
		generateMineral(rareMetal, 0.05f, random);
		generateMineral(metal, 0.1f, random);
		generateMineral(coal, 0.2f, random);
		generateMineral(jewel, 0.06f, random);
		generateMineral(stone, 0.3f, random);
		generateMineral(limestone, 0.3f, random);

		for (int townIndex = 0; townIndex < townCount; townIndex++) {
			final int townCellIndex = maxIndex(townPlacementScore.values);
			final int townX = townCellIndex % width;
			final int townY = townCellIndex / width;
			tiles.set(townX, townY, Tiles.Town);

			// Decrement score around this place, to have towns more far away from each other
			townPlacementScore.dent(townX, townY, 35, 3f);

			// Place the town and set it up
			final String townName = generateTownName();
			final int townEntity = engine.getService(EntitySpawnService.class).spawnTown(townX, townY, townName);
			townEntities.add(townEntity);

			final TownC town = townMapper.get(townEntity);
			town.name = townName;

			town.population = 10 + random.nextInt(90);
			town.money = town.population * 10 + random.nextInt(50);
			town.hasFreshWater = precipitation.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, MINERAL_REACH_KERNEL_SIZE, MINERAL_REACH_KERNEL_SIZE) >= 0.4f;
			town.hasSaltWater = altitude.getKernelMin(townX, townY, null, 5, 5) <= 0f;

			town.woodAbundance = forestMap.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, MINERAL_REACH_KERNEL_SIZE, MINERAL_REACH_KERNEL_SIZE);
			town.fieldSpace = pastureMap.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, MINERAL_REACH_KERNEL_SIZE, MINERAL_REACH_KERNEL_SIZE);
			town.fishAbundance = fishMap.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, MINERAL_REACH_KERNEL_SIZE, MINERAL_REACH_KERNEL_SIZE);

			town.temperature = temperature.get(townX, townY);
			town.precipitation = precipitation.get(townX, townY);

			town.rareMetalOccurrence = rareMetal[townIndex];
			town.metalOccurrence = metal[townIndex];
			town.coalOccurrence = coal[townIndex];
			town.jewelOccurrence = jewel[townIndex];
			town.stoneOccurrence = stone[townIndex];
			town.limestoneOccurrence = limestone[townIndex];

			town.prices.initialize((short) 10, (short) 10);
		}

		// Fill in town neighbor distances
		class TownDistance implements Comparable<TownDistance> {
			int townEntity;
			float distance;

			@Override
			public int compareTo(@NotNull TownDistance o) {
				return Float.compare(distance, o.distance);
			}
		}
		final PooledArray<TownDistance> distances = new PooledArray<>(TownDistance::new);

		for (int i = 0; i < townEntities.size; i++) {
			final int townEntity = townEntities.get(i);
			final PositionC townPos = positionMapper.get(townEntity);
			distances.clear();

			for (int o = 0; o < townEntities.size; o++) {
				if (o == i) {
					continue;
				}
				final int otherTownEntity = townEntities.get(o);
				final PositionC otherTownPos = positionMapper.get(otherTownEntity);

				final TownDistance td = distances.add();
				td.townEntity = otherTownEntity;
				td.distance = PositionC.manhattanDistance(townPos, otherTownPos);
			}

			distances.sort();
			final float maxTradingDistance = distances.get(0).distance * 2.5f;
			int closeTownCount = 1;
			while (closeTownCount < distances.size && distances.get(closeTownCount).distance <= maxTradingDistance) {
				closeTownCount++;
			}
			closeTownCount = Math.max(closeTownCount, 3);

			final int[] closeTowns = new int[closeTownCount];
			for (int t = 0; t < closeTownCount; t++) {
				closeTowns[t] = distances.get(t).townEntity;
			}
			townMapper.get(townEntity).closestNeighbors = closeTowns;
		}


		// Generate some roads and bridges
		//TODO

		engine.flush();
	}

	public static void generatePlayerCaravan(@NotNull Engine engine) {
		final Mapper<PositionC> position = engine.getMapper(PositionC.class);
		final IntArray townEntities = engine.getEntities(Components.DOMAIN.familyWith(TownC.class, PositionC.class)).getIndices();
		final int starterTown = townEntities.random();
		final PositionC starterTownPosition = position.get(starterTown);
		int closestTown = -1;
		float closestTownDistance = Float.POSITIVE_INFINITY;
		for (int i = 0; i < townEntities.size; i++) {
			final int t = townEntities.get(i);
			if (t == starterTown) {
				continue;
			}
			final float dist = PositionC.manhattanDistance(starterTownPosition, position.get(t));
			if (dist < closestTownDistance) {
				closestTownDistance = dist;
				closestTown = t;
			}
		}

		final PositionC otherTownPosition = position.get(closestTown);
		float offX = otherTownPosition.x - starterTownPosition.x;
		float offY = otherTownPosition.y - starterTownPosition.y;
		final float scale = 5f / (Math.abs(offX) + Math.abs(offY));
		offX *= scale;
		offY *= scale;
		float posX = starterTownPosition.x + offX;
		float posY = starterTownPosition.y + offY;

		engine.getService(EntitySpawnService.class).spawnPlayerCaravan(posX, posY, Merchandise.Category.VALUES);
		engine.flush();
	}

	public static void simulateInitialWorldPrices(@NotNull Engine engine, int iterations, boolean dumpResults) {
		final Mapper<TownC> town = engine.getMapper(TownC.class);
		final Mapper<PositionC> position = engine.getMapper(PositionC.class);
		final IntArray townEntities = engine.getEntities(Components.DOMAIN.familyWith(TownC.class, PositionC.class)).getIndices();

		for (int i = 0; i < iterations; i++) {
			// Update internal economy and production
			for (int ti = 0; ti < townEntities.size; ti++) {
				simulateInternalEconomy(town.get(townEntities.get(ti)));
			}

			// Simulate trade
			townEntities.shuffle();
			for (int ti = 0; ti < townEntities.size; ti++) {
				final int townEntity = townEntities.get(ti);
				final TownC localTown = town.get(townEntity);
				final PositionC localTownPosition = position.get(townEntity);

				for (int neighborTownEntity : localTown.closestNeighbors) {
					final TownC otherTown = town.get(neighborTownEntity);
					final PositionC otherTownPosition = position.get(neighborTownEntity);
					final float distance = PositionC.manhattanDistance(localTownPosition, otherTownPosition);

					arbitrageTowns(localTown, otherTown, distance);
					arbitrageTowns(otherTown, localTown, distance);
				}
			}

			if (i < iterations/2) {
				// Apply communism to reset initial price instability
				int totalMoney = 0;
				for (int ti = 0; ti < townEntities.size; ti++) {
					final TownC localTown = town.get(townEntities.get(ti));
					totalMoney += localTown.money;
				}
				final int townMoney = (totalMoney + townEntities.size - 1) / townEntities.size;
				for (int ti = 0; ti < townEntities.size; ti++) {
					final TownC localTown = town.get(townEntities.get(ti));
					localTown.money = townMoney;
					localTown.wealth = 0;
				}
			}

			if (dumpResults && (i % 10) == 0) {
				dumpTownData(i, town, position, townEntities);
			}
		}

		if (dumpResults) {
			dumpTownData(iterations, town, position, townEntities);
		}
	}

	/** Single direction arbitrage, goods moving from localTown to otherTown */
	private static void arbitrageTowns(TownC localTown, TownC otherTown, float distance) {
		final PriceList localPrices = localTown.prices;
		final PriceList otherPrices = otherTown.prices;

		// Moving stuff from local to other
		for (Merchandise m : Merchandise.VALUES) {
			if (!m.tradeable) {
				continue;
			}

			while (true) {
				final int buy = localPrices.buyPrice(m);
				final int sell = Math.min(otherPrices.sellPrice(m), otherTown.money);
				float caravanProfit = sell - buy - distance * 0.05f;
				if (caravanProfit <= 0) {
					break;
				}

				localTown.money += buy;
				localPrices.buyUnit(m);
				otherTown.money -= sell;
				otherPrices.sellUnit(m);

				final int profit = ((sell - buy) + 1) / 2;
				localTown.money += profit;
				otherTown.money += profit;

				localTown.tradeBuyCounter++;
				otherTown.tradeSellCounter++;
			}
		}
	}

	private static final float[] MINERAL_REACH_KERNEL = manhattanKernel(0.3f, 1);
	private static final int MINERAL_REACH_KERNEL_SIZE = kernelSide(MINERAL_REACH_KERNEL);

	/** Generate heightmap, where 0 = sea level and 1 = 1km, with no hard cap,
	 * but average max height being around 4km */
	private static WorldAttributeFloat generateHeight(int width, int height, RandomXS128 random) {
		final WorldAttributeFloat h = new WorldAttributeFloat(width, height, 1f);
		// We want a continent, so start off with a huge mountain in the center
		h.attenuateEdges(Math.min(width, height) / 2, Interpolation.fade);
		h.scale(20f);
		h.add(random.nextLong(), 80f, 10f);
		h.add(random.nextLong(), 70f, 9f);
		h.add(random.nextLong(), 50f, 5f);
		h.add(5f);
		h.attenuateEdges(60, Interpolation.pow2Out);
		h.add(-5f);
		h.add(random.nextLong(), 25f, 4f);
		h.add(random.nextLong(), 12f, 3f);
		h.clamp(0, Float.POSITIVE_INFINITY);
		// Max attainable height is 51, average max is around 25-29
		// So let's set 51 at 8km, which makes the average max at 4-4.5 km
		h.scale(8f / 51f);
		h.fill((x, y, currentValue) -> {
			float a = currentValue / 8f;
			a *= a;
			return a * 8f;
		});

		//h.saveVisualization("height");
		//System.out.println("Max height: "+h.max()+" km  Median: "+h.median() + "   Average:  "+h.average());
		return h;
	}

	private static void generateWind(WorldAttributeFloat windX, WorldAttributeFloat windY, WorldAttributeFloat height, RandomXS128 random) {
		// Wind generally moves westward (positive X) and can be broken up by mountains
		// Note that this concerns high-altitude winds (2km to 40km) not near-ground winds
		// http://www.eniscuola.net/en/argomento/air-in-motion/winds/high-altitude-winds/

		// Generate wind by generating some amount of "wind particles" and propagating them through the map.
		// As each particle travels through the land,

		// Initialize wind at the west edge, with more wind near the middle, as area near equator gets less wind (?)
		// For simplicity, let's use nondescript wind units, with 1 being "very windy"
		for (int y = 0; y < windX.height; y++) {
			float closenessToMiddle = 1f - Math.abs((windX.height - y * 2f) / windX.height);
			for (int i = 0; i < 10; i++) {
				simulateWindParticle(-1, y, (0.5f + closenessToMiddle * 0.5f) * 0.8f + 0.2f * random.nextFloat(), random.nextFloat() * 0.2f - 0.1f, windX, windY, height, random, 0.5f + random.nextFloat() * 0.1f);
			}
		}
	}

	private static void simulateWindParticle(int originX, int originY, float originalVelocityX, float originalVelocityY,
	                                         WorldAttributeFloat windX, WorldAttributeFloat windY,
	                                         WorldAttributeFloat height, RandomXS128 random, float power) {

		//TODO
	}

	/** Temperature in degrees Celsius */
	private static WorldAttributeFloat generateTemperature(WorldAttributeFloat altitude, RandomXS128 random) {
		final WorldAttributeFloat temperature = new WorldAttributeFloat(altitude.width, altitude.height, 0f);
		// Temperature is primarily determined by latitude, with south part of the map being very hot,
		// while north being freezing, just because.
		final float northTemperature = -10f + random.nextFloat() * 15f;
		final float southTemperature = 23f + random.nextFloat() * 12f;
		for (int y = 0; y < altitude.height; y++) {
			final float temp = MathUtils.map(0, altitude.height - 1, northTemperature, southTemperature, y);
			for (int x = 0; x < altitude.width; x++) {
				temperature.set(x, y, temp);
			}
		}
		// Another contributor is height - most sources give drop of 6C per 1km of height
		for (int y = 0; y < altitude.height; y++) {
			for (int x = 0; x < altitude.width; x++) {
				temperature.set(x, y, temperature.get(x, y) + altitude.get(x, y) * -6f);
			}
		}
		// Another potential contributors: continentality, winds, slope etc.
		// https://www.yourarticlelibrary.com/geography/climate-geography/factors-influencing-temperature-with-diagram-geography/77664
		//temperature.saveVisualization("temperature");
		return temperature;
	}

	/** Generate rainfall, 0 being no rainfall, 1 being raining almost always. */
	private static WorldAttributeFloat generatePrecipitation(WorldAttributeFloat temperature, WorldAttributeFloat height, WorldAttributeFloat windX, WorldAttributeFloat windY, RandomXS128 random) {
		final WorldAttributeFloat precipitation = new WorldAttributeFloat(temperature.width, temperature.height, 1f);
		precipitation.add(random.nextLong(), 80f, 1f);
		precipitation.scale(0.5f);
		return precipitation;
		// TODO(jp): More complex algorithm that uses given inputs
	}

	/** Generate forest, 0 being no forest, 1 being forest, split point is at 0.5 */
	private static WorldAttributeFloat generateForests(WorldAttributeFloat altitude, WorldAttributeFloat temperature, WorldAttributeFloat precipitation, RandomXS128 random) {
		final WorldAttributeFloat forest = new WorldAttributeFloat(temperature.width, temperature.height, 0f);
		forest.fill((x, y, old) -> {
			final float temp = temperature.get(x, y);
			final float rain = precipitation.get(x, y);
			// The idea behind this:
			// Forests like rain and medium temperature - the ideal temperature is between 1.5C and 35C, with peak in the middle and slow falloff
			// This is not ideal or super realistic, but eh.
			float score = MathUtils.clamp((float) Math.sqrt(rain) * (float) Math.sqrt(MathUtils.clamp(1f - (temp - 18.25f) / 16.75f, 0f, 1f)), 0f, 1f);
			score *= score;

			final float alt = altitude.get(x, y);
			score *= MathUtils.clamp(alt / 0.01f, 0f, 1f);// Prevent forests near large bodies of water

			float treeLine = MathUtils.clamp(MathUtils.map(-12, 45, 0f, 4, temp), 0f, 4);
			if (alt > treeLine) {
				score = 0;
			}

			return MathUtils.clamp(score, 0, 1);
		});
		forest.add(random.nextLong(), 40f, 0.8f);
		forest.add(random.nextLong(), 10f, 0.6f);
		forest.add(random.nextLong(), 5f, 0.4f);
		forest.add(-0.1f);
		forest.clamp(0f, 1f);
		return forest;
	}

	/** Generate pasture viability, 0 being no pasture, 1 being good pasture */
	private static WorldAttributeFloat generatePastures(WorldAttributeFloat temperature, WorldAttributeFloat precipitation, RandomXS128 random) {
		final WorldAttributeFloat pasture = new WorldAttributeFloat(temperature.width, temperature.height, 0f);
		pasture.fill((x, y, old) -> {
			final float temp = temperature.get(x, y);
			final float rain = precipitation.get(x, y);
			// The idea behind this:
			// Forests like rain and medium temperature - the ideal temperature is between 1.5C and 35C, with peak in the middle and slow falloff
			// This is not ideal or super realistic, but eh.
			final float r = MathUtils.clamp((float) Math.sqrt(rain) * (float) Math.sqrt(MathUtils.clamp(1f - (temp - 18.25f) / 16.75f, 0f, 1f)), 0f, 1f);
			return r * r;
		});
		pasture.add(0.4f);
		pasture.add(random.nextLong(), 20f, 0.4f);
		pasture.add(random.nextLong(), 2f, 0.15f);
		pasture.clamp(0f, 1f);
		return pasture;
	}

	/**
	 * Generate a mineral occurrence map.
	 * @param map output
	 * @param rarity (0, 1) how much of the world should have this
	 * @param fieldSize the size of the mineral fields
	 */
	private static void generateMineral(WorldAttributeFloat map, float rarity, float fieldSize, RandomXS128 random) {
		map.add(random.nextLong(), fieldSize, 1f);
		map.add(rarity * 2f - 1f);
		map.clamp(0f, 1f);
		map.fill((x, y, v) -> (float) Math.sqrt(v)); // To make nicer falloff
	}

	private static void generateMineral(float[] amounts, float rarity, RandomXS128 random) {
		final int good = rRound(amounts.length * rarity);
		final int medium = rRound(amounts.length * rarity * 2);
		final int bad = rRound(amounts.length * rarity * 3);

		for (int i = 0; i < good; i++) {
			amounts[random.nextInt(amounts.length)] = 0.8f + random.nextFloat() * 0.2f;
		}
		for (int i = 0; i < medium; i++) {
			amounts[random.nextInt(amounts.length)] = 0.4f + random.nextFloat() * 0.4f;
		}
		for (int i = 0; i < bad; i++) {
			amounts[random.nextInt(amounts.length)] = random.nextFloat() * 0.4f;
		}

		for (int i = 0; i < amounts.length; i++) {
			amounts[i] = MathUtils.clamp(amounts[i], 0, 1);
		}
	}

	private static void dumpTownData(int index, Mapper<TownC> town, Mapper<PositionC> position, IntArray townEntities) {
		townEntities.sort();

		try (CSVWriter w = new CSVWriter(new OutputStreamWriter(new FileOutputStream("towns"+index+".csv")))) {
			w.item("id");
			w.item("x");
			w.item("y");
			w.item("neighborCount");

			w.item("buyCount");
			w.item("sellCount");

			w.item("population");
			w.item("money");
			w.item("wealth");
			w.item("hasFreshWater");
			w.item("hasSaltWater");
			w.item("woodAbundance");
			w.item("fieldSpace");
			w.item("fishAbundance");
			w.item("temperature");
			w.item("precipitation");
			w.item("rareMetalOccurrence");
			w.item("metalOccurrence");
			w.item("coalOccurrence");
			w.item("jewelOccurrence");
			w.item("stoneOccurrence");
			w.item("limestoneOccurrence");
			for (Merchandise value : Merchandise.VALUES) {
				w.item("supply_"+value.name());
				w.item("demand_"+value.name());
				w.item("price_"+value.name());
			}
			for (Production production : Production.REGISTRY) {
				w.item(production.name);
			}
			w.row();

			for (int ti = 0; ti < townEntities.size; ti++) {
				final int townEntity = townEntities.get(ti);
				final TownC t = town.get(townEntity);
				final PositionC p = position.get(townEntity);

				w.item(""+townEntity);
				w.item(""+p.x);
				w.item(""+p.y);
				w.item(""+t.closestNeighbors.length);

				w.item(""+t.tradeBuyCounter);
				w.item(""+t.tradeSellCounter);

				w.item(""+t.population);
				w.item(""+t.money);
				w.item(""+t.wealth);
				w.item(""+t.hasFreshWater);
				w.item(""+t.hasSaltWater);
				w.item(""+t.woodAbundance);
				w.item(""+t.fieldSpace);
				w.item(""+t.fishAbundance);
				w.item(""+t.temperature);
				w.item(""+t.precipitation);
				w.item(""+t.rareMetalOccurrence);
				w.item(""+t.metalOccurrence);
				w.item(""+t.coalOccurrence);
				w.item(""+t.jewelOccurrence);
				w.item(""+t.stoneOccurrence);
				w.item(""+t.limestoneOccurrence);
				for (Merchandise m : Merchandise.VALUES) {
					w.item(""+t.prices.supply(m));
					w.item(""+t.prices.demand(m));
					w.item(""+t.prices.basePrice(m));
				}
				for (Production production : Production.REGISTRY) {
					w.item(""+t.production.get(production, 0));
				}

				w.row();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static final String VOWELS = "euioay";
	private static final String CONSONANTS = "qwrtzpsdfghjklxcvbnmrtzpsdfghjklcvbnm";

	private static char random(@NotNull String from) {
		return from.charAt(MathUtils.random(from.length() - 1));
	}

	public static @NotNull String generateTownName() {
		final StringBuilder sb = new StringBuilder();
		boolean vowel = MathUtils.random(9) == 0;
		sb.append(Character.toUpperCase(random(vowel ? VOWELS : CONSONANTS)));

		final int length = 3 + MathUtils.random(4);
		for (int i = 1; i < length; i++) {
			vowel = !vowel;
			sb.append(random(vowel ? VOWELS : CONSONANTS));
			if (i + 1 != length && MathUtils.random(40) == 0) {
				sb.append('-');
			}
		}

		return sb.toString();
	}
}
