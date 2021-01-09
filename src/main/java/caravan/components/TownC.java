package caravan.components;

import caravan.util.CaravanComponent;
import caravan.world.PriceList;
import caravan.world.Production;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;

/** Entity represents a town. */
@CaravanComponent.Serialized(name = "Town", version = 1)
public final class TownC extends CaravanComponent {

	@NotNull
	public String name = "<no name>";

	/** Amount of people that currently live in a city. */
	public int population;

	/** Current amount of money a town has */
	public int money;

	/** [-1, 1] how wealthy this town thinks it is */
	public float wealth;

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
	//endregion

	//region Technical
	private static final int[] NO_NEIGHBORS = new int[0];

	public int[] closestNeighbors = NO_NEIGHBORS;

	/** How many trades did occur with this city, selling goods to them. */
	public int tradeSellCounter;
	/** How many trades did occur with this city, buying goods from them. */
	public int tradeBuyCounter;
	//endregion

	{
		reset();
	}

	@Override
	public void reset() {
		name = "<no name>";
		population = 0;
		money = 0;
		wealth = 0;
		prices.clear();
		production.clear();

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

		closestNeighbors = NO_NEIGHBORS;

		tradeSellCounter = 0;
		tradeBuyCounter = 0;
	}

	@Override
	public void save(@NotNull Output output) {
		output.writeString(name);
		output.writeInt(population);
		output.writeInt(money);
		output.writeFloat(wealth);
		prices.save(output);

		output.writeInt(production.size);
		for (ObjectIntMap.Entry<Production> entry : production) {
			output.writeShort(entry.key.id);
			output.writeShort(entry.value);
		}

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

		output.writeInt(closestNeighbors.length);
		output.writeInts(closestNeighbors, 0, closestNeighbors.length);

		output.writeInt(tradeSellCounter);
		output.writeInt(tradeBuyCounter);
	}

	@Override
	public void load(@NotNull Input input, int version) {
		name = input.readString();
		population = input.readInt();
		money = input.readInt();
		wealth = input.readFloat();
		prices.load(input);

		production.clear();
		final int productionCount = input.readInt();
		production.ensureCapacity(productionCount);
		for (int i = 0; i < productionCount; i++) {
			final short productionId = input.readShort();
			final short value = input.readShort();

			final Production production = Production.REGISTRY.get(productionId);
			if (production != null) {
				this.production.put(production, value);
			}
		}

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

		closestNeighbors = input.readInts(input.readInt());

		tradeSellCounter = input.readInt();
		tradeBuyCounter = input.readInt();
	}
}
