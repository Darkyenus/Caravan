package caravan.components;

import caravan.world.PriceList;
import caravan.world.Production;
import caravan.world.WorldProperty;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.Pool;
import com.darkyen.retinazer.Component;
import org.jetbrains.annotations.NotNull;

/** Entity represents a town. */
public final class TownC implements Component, Pool.Poolable {

	@NotNull
	public String name = "<no name>";

	/** Amount of people that currently live in a city. */
	public int population;

	/** Current amount of money a town has */
	public int money;

	/** The town prices and inventory */
	public final PriceList prices = new PriceList();

	/** How many people are doing what. */
	public final ObjectIntMap<Production> production = new ObjectIntMap<>();

	//region Town economic properties
	/** Whether the town has own source of water for its own consumption. */
	public boolean hasFreshWater;

	/** Whether this town has a source of salt water, for salt generation. */
	public boolean hasSaltWater;

	/** [0, 1] how many trees are around this town / how easy it is to generate wood */
	public float woodAbundance;

	/** [0, 1] how much pasture space is there around this town */
	public float fieldSpace;

	/** [0, 1] how much fish for fishing is there around this town */
	public float fishAbundance;

	/** Average temperature at the town's location. */
	@NotNull
	public WorldProperty.Temperature temperature = WorldProperty.Temperature.TEMPERATE;

	/** How much rain falls down here. */
	@NotNull
	public WorldProperty.Precipitation precipitation = WorldProperty.Precipitation.HUMID;

	/** [0, 1] how easy is it to mine each material here, 0 = impossible, 1 = very easy */
	public float rareMetalOccurrence;
	public float metalOccurrence;
	public float coalOccurrence;
	public float jewelOccurrence;
	public float stoneOccurrence;
	public float limestoneOccurrence;
	//endregion

	//region Technical
	private static final int[] NO_NEIGHBORS = new int[0];

	public int[] closestNeighbors = NO_NEIGHBORS;

	/** Increment this value whenever the content changes, used to detect changes in other systems. */
	public short modificationCounter;
	//endregion

	@Override
	public void reset() {
		name = "<no name>";
		population = 0;
		money = 0;
		prices.clear();
		production.clear();

		hasFreshWater = false;
		hasSaltWater = false;
		woodAbundance = 0;
		fieldSpace = 0;
		fishAbundance = 0;
		temperature = WorldProperty.Temperature.TEMPERATE;
		precipitation = WorldProperty.Precipitation.HUMID;

		rareMetalOccurrence = 0;
		metalOccurrence = 0;
		coalOccurrence = 0;
		jewelOccurrence = 0;
		stoneOccurrence = 0;
		limestoneOccurrence = 0;

		closestNeighbors = NO_NEIGHBORS;
	}
}
