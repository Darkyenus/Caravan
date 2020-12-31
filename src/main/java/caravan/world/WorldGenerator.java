package caravan.world;

import caravan.components.TownC;
import caravan.services.EntitySpawnService;
import caravan.services.WorldService;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.darkyen.retinazer.Engine;
import org.jetbrains.annotations.NotNull;

/**
 * Generates game worlds.
 */
public final class WorldGenerator {

	public static void generateWorld(@NotNull Engine engine, long seed) {
		final WorldService world = engine.getService(WorldService.class);
		final RandomXS128 random = new RandomXS128(seed);

		final WorldAttributeFloat altitude = generateHeight(world.width, world.height, random);

		final WorldAttributeFloat windX = new WorldAttributeFloat(world.width, world.height, 0f);
		final WorldAttributeFloat windY = new WorldAttributeFloat(world.width, world.height, 0f);
		generateWind(windX, windY, altitude, random);

		final WorldAttributeFloat temperature = generateTemperature(world, random, altitude);
		final WorldAttributeFloat precipitation = generatePrecipitation(temperature, altitude, windX, windY, random);
		final WorldAttributeFloat forest = generateForests(altitude, temperature, precipitation, random);
		final WorldAttributeFloat pastures = generatePastures(temperature, precipitation, random);

		final WorldAttribute<WorldProperty.Temperature> temperatureClass = new WorldAttribute<>(world.width, world.height, WorldProperty.Temperature.TEMPERATE);
		temperatureClass.fill((x, y, currentValue) -> {
			float temp = temperature.get(x, y);
			if (temp < 10f) {
				return WorldProperty.Temperature.COLD;
			} else if (temp < 25f) {
				return WorldProperty.Temperature.TEMPERATE;
			} else {
				return WorldProperty.Temperature.HOT;
			}
		});

		final WorldAttribute<WorldProperty.Precipitation> precipitationClass = new WorldAttribute<>(world.width, world.height, WorldProperty.Precipitation.HUMID);
		precipitationClass.fill((x, y, currentValue) -> {
			float prec = precipitation.get(x, y);
			if (prec < 0.1f) {
				return WorldProperty.Precipitation.SUPER_ARID;
			} else if (prec < 0.4f) {
				return WorldProperty.Precipitation.ARID;
			} else if (prec < 0.85f) {
				return WorldProperty.Precipitation.HUMID;
			} else {
				return WorldProperty.Precipitation.SUPER_HUMID;
			}
		});

		// Generate rivers
		//TODO

		final WorldAttributeFloat fish = new WorldAttributeFloat(world.width, world.height, 0f, (x, y, v) -> {
			if (altitude.get(x, y) <= 0f) {
				return 1f;
			}
			// TODO(jp): Rivers
			return 0f;
		});

		// Place tiles
		for (int y = 0; y < world.height; y++) {
			for (int x = 0; x < world.width; x++) {
				final float h = altitude.get(x, y);
				if (h <= 0f){
					world.tiles.set(x, y, Tiles.Water);
					continue;
				}

				final float frs = forest.get(x, y);
				if (frs > 0.5f) {
					world.tiles.set(x, y, Tiles.Forest);
					continue;
				}

				final float pst = pastures.get(x, y);
				if (pst > 0.5f) {
					world.tiles.set(x, y, Tiles.Grass);
				}

				// TODO(jp): More terrain types (desert, rocky area...)
				world.tiles.set(x, y, Tiles.Grass);
			}
		}

		// Generate ore locations
		final WorldAttributeFloat goldOccurrence = new WorldAttributeFloat(world.width, world.height, 0f);
		final WorldAttributeFloat silverOccurrence = new WorldAttributeFloat(world.width, world.height, 0f);
		final WorldAttributeFloat ironOccurrence = new WorldAttributeFloat(world.width, world.height, 0f);
		final WorldAttributeFloat copperOccurrence = new WorldAttributeFloat(world.width, world.height, 0f);
		final WorldAttributeFloat tinOccurrence = new WorldAttributeFloat(world.width, world.height, 0f);
		final WorldAttributeFloat leadOccurrence = new WorldAttributeFloat(world.width, world.height, 0f);
		final WorldAttributeFloat coalOccurrence = new WorldAttributeFloat(world.width, world.height, 0f);
		final WorldAttributeFloat jewelOccurrence = new WorldAttributeFloat(world.width, world.height, 0f);
		final WorldAttributeFloat stoneOccurrence = new WorldAttributeFloat(world.width, world.height, 0f);
		final WorldAttributeFloat limestoneOccurrence = new WorldAttributeFloat(world.width, world.height, 0f);
		generateMineral(goldOccurrence, 0.01f, 30f, random);
		generateMineral(silverOccurrence, 0.015f, 30f, random);
		generateMineral(ironOccurrence, 0.03f, 40f, random);
		generateMineral(copperOccurrence, 0.035f, 40f, random);
		generateMineral(tinOccurrence, 0.04f, 40f, random);
		generateMineral(leadOccurrence, 0.04f, 40f, random);
		generateMineral(coalOccurrence, 0.07f, 50f, random);
		generateMineral(jewelOccurrence, 0.03f, 20f, random);
		generateMineral(stoneOccurrence, 0.2f, 50f, random);
		generateMineral(limestoneOccurrence, 0.1f, 40f, random);

		// Generate cities
		final WorldAttributeFloat townPlacementScore = new WorldAttributeFloat(world.width, world.height, 0f);
		townPlacementScore.fill((x, y, v) -> {
			final float alt = altitude.get(x, y);
			if (alt <= 0f) {
				// No towns under the sea
				return Float.NEGATIVE_INFINITY;
			}

			final float gold = goldOccurrence.getKernelMax(x, y, MINERAL_REACH_KERNEL, 5, 5);
			final float silver = silverOccurrence.getKernelMax(x, y, MINERAL_REACH_KERNEL, 5, 5);
			final float iron = ironOccurrence.getKernelMax(x, y, MINERAL_REACH_KERNEL, 5, 5);
			final float copper = copperOccurrence.getKernelMax(x, y, MINERAL_REACH_KERNEL, 5, 5);
			final float tin = tinOccurrence.getKernelMax(x, y, MINERAL_REACH_KERNEL, 5, 5);
			final float lead = leadOccurrence.getKernelMax(x, y, MINERAL_REACH_KERNEL, 5, 5);
			final float coal = coalOccurrence.getKernelMax(x, y, MINERAL_REACH_KERNEL, 5, 5);
			final float jewel = jewelOccurrence.getKernelMax(x, y, MINERAL_REACH_KERNEL, 5, 5);
			final float stone = stoneOccurrence.getKernelMax(x, y, MINERAL_REACH_KERNEL, 5, 5);
			final float limestone = limestoneOccurrence.getKernelMax(x, y, MINERAL_REACH_KERNEL, 5, 5);
			final float mineral = max(gold, silver, iron, copper, tin, lead, coal, jewel, stone, limestone);

			final float wood = forest.getKernelMax(x, y, MINERAL_REACH_KERNEL, 5, 5);
			final float pasture = pastures.getKernelMax(x, y, MINERAL_REACH_KERNEL, 5, 5);

			final boolean saltWaterSource = altitude.getKernelMin(x, y, null, 5, 5) <= 0f;
			// TODO(jp): Incorporate river computation and use harsher precipitation cutoff (don't forget to change town computation as well)
			final boolean freshWaterSource = precipitation.getKernelMax(x, y, MINERAL_REACH_KERNEL, 5, 5) >= 0.4f;

			float score = 0;
			score += mineral;
			score += freshWaterSource ? 1f : 0f;
			score += saltWaterSource ? 0.3f : 0f;
			score += wood * 0.3f;
			score += pasture * 0.3f;
			return score;
		});

		for (int townIndex = 0; townIndex < 16; townIndex++) {
			final int townCellIndex = maxIndex(townPlacementScore.values);
			final int townX = townCellIndex % world.width;
			final int townY = townCellIndex / world.width;

			// Decrement score around this place, to have towns more far away from each other
			townPlacementScore.dent(townX, townY, 20, 3f);

			// Place the town and set it up
			final TownC town = engine.getService(EntitySpawnService.class).spawnTown(townX, townY);
			town.population = 10 + random.nextInt(90);
			town.money = town.population + random.nextInt(10);
			town.hasFreshWater = precipitation.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, 5, 5) >= 0.4f;
			town.hasSaltWater = altitude.getKernelMin(townX, townY, null, 5, 5) <= 0f;

			town.woodAbundance = forest.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, 5, 5);
			town.pastureAbundance = pastures.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, 5, 5);
			town.fishAbundance = fish.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, 5, 5);

			town.temperature = temperatureClass.get(townX, townY);
			town.precipitation = precipitationClass.get(townX, townY);

			town.goldOccurrence = goldOccurrence.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, 5, 5);
			town.silverOccurrence = silverOccurrence.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, 5, 5);
			town.ironOccurrence = ironOccurrence.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, 5, 5);
			town.copperOccurrence = copperOccurrence.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, 5, 5);
			town.tinOccurrence = tinOccurrence.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, 5, 5);
			town.leadOccurrence = leadOccurrence.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, 5, 5);
			town.coalOccurrence = coalOccurrence.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, 5, 5);
			town.jewelOccurrence = jewelOccurrence.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, 5, 5);
			town.stoneOccurrence = stoneOccurrence.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, 5, 5);
			town.limestoneOccurrence = limestoneOccurrence.getKernelMax(townX, townY, MINERAL_REACH_KERNEL, 5, 5);
		}

		// Generate some roads and bridges
		//TODO
	}

	private static final float[] MINERAL_REACH_KERNEL = new float[] {
			0.1f, 0.2f, 0.3f, 0.2f, 0.1f,
			0.2f, 0.5f, 0.7f, 0.5f, 0.2f,
			0.3f, 0.7f, 1.0f, 0.7f, 0.3f,
			0.2f, 0.5f, 0.7f, 0.5f, 0.2f,
			0.1f, 0.2f, 0.3f, 0.2f, 0.1f,
	};

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
		h.add(random.nextLong(), 25f, 4f);
		h.add(random.nextLong(), 12f, 3f);
		h.add(5f);
		h.attenuateEdges(30, Interpolation.pow2Out);
		h.add(-5f);
		h.clamp(0, Float.POSITIVE_INFINITY);
		// Max attainable height is 51, average max is around 25-29
		// So let's set 51 at 8km, which makes the average max at 4-4.5 km
		h.scale(8f / 51f);

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
	private static WorldAttributeFloat generateTemperature(WorldService world, RandomXS128 random, WorldAttributeFloat altitude) {
		final WorldAttributeFloat temperature = new WorldAttributeFloat(world.width, world.height, 0f);
		// Temperature is primarily determined by latitude, with south part of the map being very hot,
		// while north being freezing, just because.
		final float northTemperature = -10f + random.nextFloat() * 15f;
		final float southTemperature = 23f + random.nextFloat() * 12f;
		for (int y = 0; y < world.height; y++) {
			final float temp = MathUtils.map(0, world.height - 1, northTemperature, southTemperature, y);
			for (int x = 0; x < world.width; x++) {
				temperature.set(x, y, temp);
			}
		}
		// Another contributor is height - most sources give drop of 6C per 1km of height
		for (int y = 0; y < world.height; y++) {
			for (int x = 0; x < world.width; x++) {
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
			float score = MathUtils.clamp((float) Math.sqrt(rain) + (float) Math.sqrt(MathUtils.clamp(1f - (temp - 18.25f) / 16.75f, 0f, 1f)), 0f, 1f);
			final float alt = altitude.get(x, y);
			score *= MathUtils.clamp(alt / 0.2f, 0f, 1f);// Prevent forests near large bodies of water
			return score;
		});
		forest.add(random.nextLong(), 40f, 0.5f);
		forest.add(random.nextLong(), 10f, 0.2f);
		forest.add(-0.1f);
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
			return MathUtils.clamp((float) Math.sqrt(rain) + (float) Math.sqrt(MathUtils.clamp(1f - (temp - 18.25f)/16.75f, 0f, 1f)), 0f, 1f);
		});
		pasture.add(0.4f);
		pasture.add(random.nextLong(), 20f, 0.4f);
		pasture.add(random.nextLong(), 2f, 0.15f);
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

	private static float max(float value0, float...values) {
		float result = value0;
		for (float value : values) {
			result = Math.max(result, value);
		}
		return result;
	}

	private static float min(float value0, float...values) {
		float result = value0;
		for (float value : values) {
			result = Math.min(result, value);
		}
		return result;
	}

	private static int maxIndex(float[] values) {
		float max = values[0];
		int maxIndex = 0;
		for (int i = 1; i < values.length; i++) {
			if (values[i] > max) {
				max = values[i];
				maxIndex = i;
			}
		}
		return maxIndex;
	}
}
