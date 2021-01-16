package caravan.world;

import caravan.components.Components;
import caravan.components.PositionC;
import caravan.components.TownC;
import caravan.services.EntitySpawnService;
import caravan.services.Id;
import caravan.services.TownSystem;
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

		final WorldAttributeFloat temperature = generateTemperature(altitude, random);
		final WorldAttributeFloat precipitation = generatePrecipitation(temperature, random);
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
				if (h <= 0f) {
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

		// Generate ore locations
		final WorldAttributeFloat rareMetalOccurrence = new WorldAttributeFloat(world.width, world.height, 0f);
		final WorldAttributeFloat metalOccurrence = new WorldAttributeFloat(world.width, world.height, 0f);
		final WorldAttributeFloat coalOccurrence = new WorldAttributeFloat(world.width, world.height, 0f);
		final WorldAttributeFloat jewelOccurrence = new WorldAttributeFloat(world.width, world.height, 0f);
		final WorldAttributeFloat stoneOccurrence = new WorldAttributeFloat(world.width, world.height, 0f);
		final WorldAttributeFloat limestoneOccurrence = new WorldAttributeFloat(world.width, world.height, 0f);
		generateMineral(rareMetalOccurrence, 0.35f, 30f, random);
		generateMineral(metalOccurrence, 0.55f, 40f, random);
		generateMineral(coalOccurrence, 0.4f, 50f, random);
		generateMineral(jewelOccurrence, 0.25f, 20f, random);
		generateMineral(stoneOccurrence, 0.7f, 50f, random);
		generateMineral(limestoneOccurrence, 0.5f, 40f, random);

		// Generate cities
		final Mapper<TownC> townMapper = engine.getMapper(TownC.class);
		final Mapper<PositionC> positionMapper = engine.getMapper(PositionC.class);
		final IntArray townEntities = new IntArray();

		final WorldAttributeFloat townPlacementScore = new WorldAttributeFloat(width, height, 0f);
		final WorldAttributeFloat townClosenessPenalty = new WorldAttributeFloat(width, height, 0f);
		townClosenessPenalty.add(random.nextLong(), 30, 0.1f);// Some interesting randomness

		final int townCount = 24;
		for (int townIndex = 0; townIndex < townCount; townIndex++) {
			if (townIndex % 4 == 0) {
				fillOutTownPlacementScore(townPlacementScore, townClosenessPenalty, townEntities, townMapper, altitude, temperature, precipitation,
						forestMap, pastureMap, fishMap, rareMetalOccurrence, metalOccurrence, coalOccurrence, jewelOccurrence, stoneOccurrence, limestoneOccurrence);
			}

			final int townCellIndex = maxIndex(townPlacementScore.values);
			final int townX = townCellIndex % width;
			final int townY = townCellIndex / width;
			tiles.set(townX, townY, Tiles.Town);

			townPlacementScore.dent(townX, townY, 35, 3f);
			townClosenessPenalty.dent(townX, townY, 35, 3f);

			// Place the town and set it up
			final String townName = generateTownName();
			final int townEntity = engine.getService(EntitySpawnService.class).spawnTown(townX, townY, townName);
			townEntities.add(townEntity);

			final TownC town = townMapper.get(townEntity);
			town.name = townName;
			town.population = 10 + random.nextInt(90);
			town.money = town.population * 10 + random.nextInt(50);
			town.prices.initialize((short) 10, (short) 10);
			extractEnvironment(town.environment, townX, townY, precipitation, altitude, forestMap, pastureMap, fishMap, temperature,
					rareMetalOccurrence, metalOccurrence, coalOccurrence, jewelOccurrence, stoneOccurrence, limestoneOccurrence);

			if (townIndex % 4 == 3) {
				simulateSuperInitialWorldPrices(townMapper, townEntities, 50);
			}
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

	private static void fillOutTownPlacementScore(WorldAttributeFloat townPlacementScore, WorldAttributeFloat townClosenessPenalty, IntArray townEntities, Mapper<TownC> townMapper, WorldAttributeFloat altitude, WorldAttributeFloat temperature, WorldAttributeFloat precipitation, WorldAttributeFloat forestMap, WorldAttributeFloat pastureMap, WorldAttributeFloat fishMap, WorldAttributeFloat rareMetalOccurrence, WorldAttributeFloat metalOccurrence, WorldAttributeFloat coalOccurrence, WorldAttributeFloat jewelOccurrence, WorldAttributeFloat stoneOccurrence, WorldAttributeFloat limestoneOccurrence) {
		townPlacementScore.fill(0);

		final TownC dummyTown = new TownC();
		for (int i = 0; i < townEntities.size; i++) {
			dummyTown.prices.add(townMapper.get(townEntities.get(i)).prices);
		}

		townPlacementScore.fillParallel((x, y, v) -> {
			final float alt = altitude.get(x, y);
			if (alt <= 0f) {
				// No towns under the sea, hopefully
				return -100f;
			}

			extractEnvironment(dummyTown.environment, x, y, precipitation, altitude, forestMap, pastureMap, fishMap, temperature,
					rareMetalOccurrence, metalOccurrence, coalOccurrence, jewelOccurrence, stoneOccurrence, limestoneOccurrence);

			float profit = 0;
			final Id.Registry<Production> registry = Production.REGISTRY;
			for (int i = 0; i < registry.count(); i++) {
				final Production production = registry.getDense(i);
				profit += Math.max(TownSystem.productionProfit(dummyTown, production), 0);
			}

			return profit;
		});
		townPlacementScore.normalize(0f, 1f);
		townPlacementScore.add(townClosenessPenalty);
	}

	private static void extractEnvironment(@NotNull Environment environment, int townX, int townY,
	                                       @NotNull WorldAttributeFloat precipitation,
	                                       @NotNull WorldAttributeFloat altitude,
	                                       @NotNull WorldAttributeFloat forestMap,
	                                       @NotNull WorldAttributeFloat pastureMap,
	                                       @NotNull WorldAttributeFloat fishMap,
	                                       @NotNull WorldAttributeFloat temperature,
	                                       @NotNull WorldAttributeFloat rareMetalOccurrence,
	                                       @NotNull WorldAttributeFloat metalOccurrence,
	                                       @NotNull WorldAttributeFloat coalOccurrence,
	                                       @NotNull WorldAttributeFloat jewelOccurrence,
	                                       @NotNull WorldAttributeFloat stoneOccurrence,
	                                       @NotNull WorldAttributeFloat limestoneOccurrence) {
		environment.hasFreshWater = precipitation.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, MINERAL_REACH_KERNEL_SIZE, MINERAL_REACH_KERNEL_SIZE) >= 0.4f;
		environment.hasSaltWater = altitude.getKernelMin(townX, townY, null, 5, 5) <= 0f;
		environment.woodAbundance = forestMap.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, MINERAL_REACH_KERNEL_SIZE, MINERAL_REACH_KERNEL_SIZE);
		environment.fieldSpace = pastureMap.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, MINERAL_REACH_KERNEL_SIZE, MINERAL_REACH_KERNEL_SIZE);
		environment.fishAbundance = fishMap.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, MINERAL_REACH_KERNEL_SIZE, MINERAL_REACH_KERNEL_SIZE);
		environment.temperature = temperature.get(townX, townY);
		environment.precipitation = precipitation.get(townX, townY);
		environment.rareMetalOccurrence = rareMetalOccurrence.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, MINERAL_REACH_KERNEL_SIZE, MINERAL_REACH_KERNEL_SIZE);
		environment.metalOccurrence = metalOccurrence.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, MINERAL_REACH_KERNEL_SIZE, MINERAL_REACH_KERNEL_SIZE);
		environment.coalOccurrence = coalOccurrence.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, MINERAL_REACH_KERNEL_SIZE, MINERAL_REACH_KERNEL_SIZE);
		environment.jewelOccurrence = jewelOccurrence.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, MINERAL_REACH_KERNEL_SIZE, MINERAL_REACH_KERNEL_SIZE);
		environment.stoneOccurrence = stoneOccurrence.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, MINERAL_REACH_KERNEL_SIZE, MINERAL_REACH_KERNEL_SIZE);
		environment.limestoneOccurrence = limestoneOccurrence.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, MINERAL_REACH_KERNEL_SIZE, MINERAL_REACH_KERNEL_SIZE);
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
		float scale = 5f / (Math.abs(offX) + Math.abs(offY));
		if (!Float.isFinite(scale)) {
			scale = 0;
		}
		offX *= scale;
		offY *= scale;
		float posX = starterTownPosition.x + offX;
		float posY = starterTownPosition.y + offY;

		engine.getService(EntitySpawnService.class).spawnPlayerCaravan(posX, posY, Merchandise.Category.VALUES);
		engine.flush();
	}

	public static void simulateSuperInitialWorldPrices(@NotNull Mapper<TownC> town, @NotNull IntArray townEntities, int iterations) {
		for (int i = 0; i < iterations; i++) {
			// Update internal economy and production
			for (int ti = 0; ti < townEntities.size; ti++) {
				simulateInternalEconomy(town.get(townEntities.get(ti)));
			}

			// Simulate trade
			townEntities.shuffle();
			for (int ti0 = 0; ti0 < townEntities.size; ti0++) {
				final TownC localTown = town.get(townEntities.get(ti0));

				for (int ti1 = 0; ti1 < townEntities.size; ti1++) {
					final TownC otherTown = town.get(townEntities.get(ti1));

					arbitrageTowns(localTown, otherTown, 5f);
					arbitrageTowns(otherTown, localTown, 5f);
				}
			}

			// Apply communism to reset initial price instability
			int totalMoney = 0;
			for (int ti = 0; ti < townEntities.size; ti++) {
				totalMoney += town.get(townEntities.get(ti)).money;
			}
			final int townMoney = (totalMoney + townEntities.size - 1) / townEntities.size;
			for (int ti = 0; ti < townEntities.size; ti++) {
				final TownC localTown = town.get(townEntities.get(ti));
				localTown.money = townMoney;
				localTown.wealth = 0;
			}
		}
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

			if (i < iterations / 2) {
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

	/**
	 * Generate heightmap, where 0 = sea level and 1 = 1km, with no hard cap,
	 * but average max height being around 4km
	 */
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
		return h;
	}

	/** Temperature in degrees Celsius */
	private static WorldAttributeFloat generateTemperature(WorldAttributeFloat altitude, RandomXS128 random) {
		final WorldAttributeFloat temperature = new WorldAttributeFloat(altitude.width, altitude.height, 0f);
		temperature.add(random.nextLong(), 80f, 1f);
		temperature.add(random.nextLong(), 40f, 0.5f);
		temperature.add(random.nextLong(), 20f, 0.25f);
		temperature.normalize(-3f, 37f);

		// Account for height height - most sources give drop of 6C per 1km of height
		for (int y = 0; y < altitude.height; y++) {
			for (int x = 0; x < altitude.width; x++) {
				temperature.set(x, y, temperature.get(x, y) + altitude.get(x, y) * /*-6f*/ -3f);
			}
		}
		// Another potential contributors: continentality, winds, slope etc.
		// https://www.yourarticlelibrary.com/geography/climate-geography/factors-influencing-temperature-with-diagram-geography/77664
		//temperature.saveVisualization("temperature");
		return temperature;
	}

	/** Generate rainfall, 0 being no rainfall, 1 being raining almost always. */
	private static WorldAttributeFloat generatePrecipitation(WorldAttributeFloat temperature, RandomXS128 random) {
		final WorldAttributeFloat precipitation = new WorldAttributeFloat(temperature.width, temperature.height, 1f);
		precipitation.add(random.nextLong(), 160f, 2f);
		precipitation.add(random.nextLong(), 80f, 1f);
		precipitation.add(random.nextLong(), 30f, 0.5f);
		precipitation.normalize(0f, 1f);
		return precipitation;
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
		forest.interpolate(Interpolation.pow5);
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
		pasture.interpolate(Interpolation.pow5);
		return pasture;
	}

	/**
	 * Generate a mineral occurrence map.
	 *
	 * @param map output
	 * @param rarity (0, 1) how much of the world should have this
	 * @param fieldSize the size of the mineral fields
	 */
	private static void generateMineral(WorldAttributeFloat map, float rarity, float fieldSize, RandomXS128 random) {
		map.add(random.nextLong(), fieldSize, 1f);
		map.add(rarity * 2f - 1f);
		map.clamp(0f, 0.5f);
		map.scale(2f);
		map.interpolate(Interpolation.smooth);
	}

	private static void dumpTownData(int index, Mapper<TownC> town, Mapper<PositionC> position, IntArray townEntities) {
		townEntities.sort();

		try (CSVWriter w = new CSVWriter(new OutputStreamWriter(new FileOutputStream("towns" + index + ".csv")))) {
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
				w.item("supply_" + value.name());
				w.item("demand_" + value.name());
				w.item("price_" + value.name());
			}
			for (Production production : Production.REGISTRY) {
				w.item(production.name);
			}
			w.row();

			for (int ti = 0; ti < townEntities.size; ti++) {
				final int townEntity = townEntities.get(ti);
				final TownC t = town.get(townEntity);
				final PositionC p = position.get(townEntity);

				w.item("" + townEntity);
				w.item("" + p.x);
				w.item("" + p.y);
				w.item("" + t.closestNeighbors.length);

				w.item("" + t.tradeBuyCounter);
				w.item("" + t.tradeSellCounter);

				w.item("" + t.population);
				w.item("" + t.money);
				w.item("" + t.wealth);
				w.item("" + t.environment.hasFreshWater);
				w.item("" + t.environment.hasSaltWater);
				w.item("" + t.environment.woodAbundance);
				w.item("" + t.environment.fieldSpace);
				w.item("" + t.environment.fishAbundance);
				w.item("" + t.environment.temperature);
				w.item("" + t.environment.precipitation);
				w.item("" + t.environment.rareMetalOccurrence);
				w.item("" + t.environment.metalOccurrence);
				w.item("" + t.environment.coalOccurrence);
				w.item("" + t.environment.jewelOccurrence);
				w.item("" + t.environment.stoneOccurrence);
				w.item("" + t.environment.limestoneOccurrence);
				for (Merchandise m : Merchandise.VALUES) {
					w.item("" + t.prices.supply(m));
					w.item("" + t.prices.demand(m));
					w.item("" + t.prices.basePrice(m));
				}
				for (Production production : Production.REGISTRY) {
					w.item("" + t.production.get(production, 0));
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
