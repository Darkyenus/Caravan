package caravan.world;

import com.badlogic.gdx.utils.Pool;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;

/**
 * Specification of an environment at a certain tile.
 */
public final class Environment implements Pool.Poolable {

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

	/** Average temperature at the town's location in degrees celsius. */
	public float temperature;

	/** How much rain falls down here in [0, 1] range. */
	public float precipitation;

	/** [0, 1] how easy is it to mine each material here, 0 = impossible, 1 = very easy */
	public float rareMetalOccurrence;
	public float metalOccurrence;
	public float coalOccurrence;
	public float jewelOccurrence;
	public float stoneOccurrence;
	public float limestoneOccurrence;

	@Override
	public void reset() {
		hasFreshWater = false;
		hasSaltWater = false;
		woodAbundance = 0;
		fieldSpace = 0;
		fishAbundance = 0;
		temperature = 0;
		precipitation = 0;

		rareMetalOccurrence = 0;
		metalOccurrence = 0;
		coalOccurrence = 0;
		jewelOccurrence = 0;
		stoneOccurrence = 0;
		limestoneOccurrence = 0;
	}

	public void save(@NotNull Output output) {
		output.writeBoolean(hasFreshWater);
		output.writeBoolean(hasSaltWater);
		output.writeFloat(woodAbundance);
		output.writeFloat(fieldSpace);
		output.writeFloat(fishAbundance);
		output.writeFloat(temperature);
		output.writeFloat(precipitation);
		output.writeFloat(rareMetalOccurrence);
		output.writeFloat(metalOccurrence);
		output.writeFloat(coalOccurrence);
		output.writeFloat(jewelOccurrence);
		output.writeFloat(stoneOccurrence);
		output.writeFloat(limestoneOccurrence);
	}

	public void load(@NotNull Input input) {
		hasFreshWater = input.readBoolean();
		hasSaltWater = input.readBoolean();
		woodAbundance = input.readFloat();
		fieldSpace = input.readFloat();
		fishAbundance = input.readFloat();
		temperature = input.readFloat();
		precipitation = input.readFloat();
		rareMetalOccurrence = input.readFloat();
		metalOccurrence = input.readFloat();
		coalOccurrence = input.readFloat();
		jewelOccurrence = input.readFloat();
		stoneOccurrence = input.readFloat();
		limestoneOccurrence = input.readFloat();
	}
}
