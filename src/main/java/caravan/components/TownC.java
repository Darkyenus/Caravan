package caravan.components;

import caravan.util.CaravanComponent;
import caravan.util.PriceList;
import caravan.world.Environment;
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

	/** Natural environment of the town. */
	public final Environment environment = new Environment();

	//region Technical
	private static final int[] NO_NEIGHBORS = new int[0];

	public int @NotNull [] closestNeighbors = NO_NEIGHBORS;

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

		environment.reset();

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

		environment.save(output);

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

		environment.load(input);

		closestNeighbors = input.readInts(input.readInt());

		tradeSellCounter = input.readInt();
		tradeBuyCounter = input.readInt();
	}
}
