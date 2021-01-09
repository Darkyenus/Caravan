package caravan.world;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/** Prices and market caps of goods of a particular trading post. */
public final class PriceList {

	/** How many units have been sold here */
	private final short[] supply = new short[Merchandise.VALUES.length];
	/** How many units have been bought here */
	private final short[] demand = new short[Merchandise.VALUES.length];

	/** Price a caravan has to pay for a single unit of merchandise. */
	public int buyPrice(@NotNull Merchandise m) {
		return MathUtils.ceil(basePrice(m) * 1.05f);
	}

	/** Price a caravan will get for selling a single unit of merchandise. */
	public int sellPrice(@NotNull Merchandise m) {
		return MathUtils.floor(basePrice(m) * 0.95f);
	}

	/** Price locals pay for the merchandise. */
	public float basePrice(@NotNull Merchandise m) {
		final int ordinal = m.ordinal();
		return (float) (Math.pow(1.02, demand[ordinal] - supply[ordinal]) * 10);
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

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (Merchandise m : Merchandise.VALUES) {
			sb.append(m.name).append("\tp:").append(basePrice(m)).append("\ts:").append(supply[m.ordinal()]).append("\td:").append(demand[m.ordinal()]).append('\n');
		}
		return sb.toString();
	}

	public void save(@NotNull Output output) {
		assert supply.length == demand.length;
		output.writeInt(supply.length);
		output.writeShorts(supply, 0, supply.length);
		output.writeShorts(demand, 0, supply.length);
	}

	public void load(@NotNull Input input) {
		final int length = input.readInt();
		final short[] supply = input.readShorts(length);
		final short[] demand = input.readShorts(length);
		System.arraycopy(supply, 0, this.supply, 0, Math.min(supply.length, this.supply.length));
		System.arraycopy(demand, 0, this.demand, 0, Math.min(demand.length, this.demand.length));
	}
}
