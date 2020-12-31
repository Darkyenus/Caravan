package caravan.world;

import com.badlogic.gdx.math.MathUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/** Prices and market caps of goods of a particular trading post. */
public final class PriceList {

	/** Current merchandise price */
	private float[] price = new float[Merchandise.VALUES.length];
	/** How many units have been sold here */
	private short[] supply = new short[Merchandise.VALUES.length];
	/** How many units have been bought here */
	private short[] demand = new short[Merchandise.VALUES.length];

	/** Price a caravan has to pay for a single unit of merchandise. */
	public int buyPrice(@NotNull Merchandise m) {
		return MathUtils.ceil(price[m.ordinal()]);
	}

	/** Price a caravan will get for selling a single unit of merchandise. */
	public int sellPrice(@NotNull Merchandise m) {
		return MathUtils.floor(price[m.ordinal()]);
	}

	/** Update prices after a single unit of merchandise was bought by a caravan. */
	public void buyUnit(@NotNull Merchandise m) {
		short demand = ++this.demand[m.ordinal()];
		price[m.ordinal()] += priceCurve(demand) * price[m.ordinal()];
	}

	/** Update prices after a single unit of merchandise was sold to the town by a caravan. */
	public void sellUnit(@NotNull Merchandise m) {
		short supply = ++this.supply[m.ordinal()];
		price[m.ordinal()] -= priceCurve(supply) * price[m.ordinal()];
	}

	/** Called every game day or so to update the internal counters. */
	public void update() {
		final int length = this.price.length;
		final short[] demand = this.demand;
		final short[] supply = this.supply;
		for (int i = 0; i < length; i++) {
			int fulfilledDemand = Math.min(supply[i], demand[i]) / 2;
			supply[i] -= fulfilledDemand;
			demand[i] -= fulfilledDemand;
		}
	}

	public void clear() {
		Arrays.fill(this.price, 0);
		Arrays.fill(this.demand, (short) 0);
		Arrays.fill(this.supply, (short) 0);
	}

	private static double priceCurve(short volume) {
		return Math.pow(1.0 / (volume + 1.0), 0.9);
	}
}
