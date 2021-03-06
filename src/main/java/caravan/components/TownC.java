package caravan.components;

import caravan.util.CaravanComponent;
import caravan.util.PriceList;
import caravan.util.Rumors;
import caravan.world.Environment;
import caravan.world.Merchandise;
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

	/** The town rumors. */
	public final Rumors rumors = new Rumors(30);

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

	/** The real sell price of a merchandise may be lower than what the market dictates,
	 * because the town may not have enough money or don't care about this merchandise. */
	public int realSellPrice(@NotNull Merchandise m) {
		int price = prices.sellPrice(m);

		if (wealth <= 0.3f && Merchandise.WEALTHY_DEMAND_ONLY.contains(m)) {
			float factor = (1f + wealth) / 1.3f;
			price = Math.round(price * (factor * factor * factor));
		}

		return Math.min(price, money);
	}

	@Override
	public void reset() {
		name = "<no name>";
		population = 0;
		money = 0;
		wealth = 0;
		prices.clear();
		rumors.reset();
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
		rumors.save(output);

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
		rumors.load(input);

		closestNeighbors = input.readInts(input.readInt());

		tradeSellCounter = input.readInt();
		tradeBuyCounter = input.readInt();
	}
}
