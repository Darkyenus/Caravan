package caravan.util;

import caravan.world.Merchandise;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/** Prices and market caps of goods of a particular trading post. */
public final class PriceList {

	/** How many units have been sold here */
	private final short[] supply = new short[Merchandise.COUNT];
	/** How many units have been bought here */
	private final short[] demand = new short[Merchandise.COUNT];

	/** Price a caravan has to pay for a single unit of merchandise. */
	public int buyPrice(@NotNull Merchandise m) {
		return MathUtils.ceil(basePrice(m) * (1f + baseVariability(m)));
	}

	/** Price a caravan will get for selling a single unit of merchandise. */
	public int sellPrice(@NotNull Merchandise m) {
		return MathUtils.floor(basePrice(m) * (1f - baseVariability(m)));
	}

	/** Price locals pay for the merchandise. */
	public float basePrice(@NotNull Merchandise m) {
		final int ordinal = m.ordinal();
		return (float) (Math.pow(1.02, demand[ordinal] - supply[ordinal]) * 10);
	}

	/** The more goods are traded, the smaller the buy/sell gap is. Returns values (0, 0.5]. */
	private float baseVariability(@NotNull Merchandise m) {
		final int ordinal = m.ordinal();
		final int base = (int) demand[ordinal] + (int) supply[ordinal];
		return (0.5f / (base * 0.2f + 1));
	}

	/** Update prices after a single unit of merchandise was bought by a caravan. */
	public void buyUnit(@NotNull Merchandise m) {
		this.demand[m.ordinal()]++;
	}

	/** Update prices after a single unit of merchandise was sold to the town by a caravan. */
	public void sellUnit(@NotNull Merchandise m) {
		this.supply[m.ordinal()]++;
	}

	/** Called every game day or so to update the internal counters. */
	public void update() {
		final short[] demand = this.demand;
		final short[] supply = this.supply;
		final int length = demand.length;
		for (int i = 0; i < length; i++) {
			int fulfilledDemand = Math.min(supply[i], demand[i]) / 4;
			supply[i] -= fulfilledDemand;
			demand[i] -= fulfilledDemand;
		}
	}

	public short supply(@NotNull Merchandise m) { return this.supply[m.ordinal()]; }
	public short demand(@NotNull Merchandise m) { return this.demand[m.ordinal()]; }

	public void clear() {
		Arrays.fill(this.demand, (short) 0);
		Arrays.fill(this.supply, (short) 0);
	}

	public void initialize(short defaultSupply, short defaultDemand) {
		Arrays.fill(this.demand, defaultSupply);
		Arrays.fill(this.supply, defaultDemand);
	}

	/** Add everything from the other price list into this one. Used during world generation. */
	public void add(PriceList prices) {
		final short[] supply = this.supply;
		final short[] demand = this.demand;
		final short[] otherSupply = prices.supply;
		final short[] otherDemand = prices.demand;
		for (int i = 0; i < Merchandise.COUNT; i++) {
			supply[i] += otherSupply[i];
			demand[i] += otherDemand[i];
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (Merchandise m : Merchandise.VALUES) {
			sb.append(m.name).append("\tp:").append(basePrice(m)).append("\ts:").append(supply[m.ordinal()]).append("\td:").append(demand[m.ordinal()]).append('\n');
		}
		return sb.toString();
	}

	public void save(@NotNull Output output) {
		final EnumSerializer.Writer writer = Merchandise.SERIALIZER.write(output);
		writer.write(output, this.supply);
		writer.write(output, this.demand);
	}

	public void load(@NotNull Input input) {
		final EnumSerializer.Reader reader = Merchandise.SERIALIZER.read(input);
		reader.read(input, this.supply);
		reader.read(input, this.demand);
	}
}
