package caravan.components;

import com.darkyen.retinazer.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Entity represents a town.
 */
public final class TownC implements Component {

	@NotNull
	public String name = "<no name>";

	/** Amount of people that currently live in a city. */
	public int population;

	/** Current amount of money a town has */
	public int money;

	//region Town economic properties
	/** Whether the town has own source of water for its own consumption. */
	public boolean hasFreshWater;

	/** Whether this town has a source of salt water, for salt generation. */
	public boolean hasSaltWater;

	/** [0, 1] how many trees are around this town / how easy it is to generate wood */
	public float woodAbundance;

	/** [0, 1] how much pasture space is there around this town */
	public float pastureAbundance;

	/** [0, 1] how much fish for fishing is there around this town */
	public float fishAbundance;

	/** Average temperature in Celsius at the town's location. */
	public float temperature;

	/** [0, 1] How much rain falls down here. */
	public float precipitation;

	/** [0, 1] how easy is it to mine each material here, 0 = impossible, 1 = very easy */
	public float goldOccurrence;
	public float silverOccurrence;
	public float ironOccurrence;
	public float copperOccurrence;
	public float tinOccurrence;
	public float leadOccurrence;
	public float coalOccurrence;
	public float jewelOccurrence;
	public float stoneOccurrence;
	public float limestoneOccurrence;
	//endregion
}
