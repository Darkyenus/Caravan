package caravan.world;

import com.badlogic.gdx.math.MathUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/** Inventory of items.
 * The maximum amount of items of a single type is {@link Short#MAX_VALUE}, should be enough. */
public final class Inventory {

	private final short[] amount = new short[Merchandise.VALUES.length];

	public int get(@NotNull Merchandise m) {
		return amount[m.ordinal()];
	}

	public void set(@NotNull Merchandise m, int amount) {
		this.amount[m.ordinal()] = (short) (amount < 0 ? 0 : (amount > Short.MAX_VALUE ? Short.MAX_VALUE : amount));
		assert amount >= 0;
	}

	public void add(@NotNull Merchandise m, float amount) {
		this.add(m, (int) (amount + MathUtils.random()));
	}

	public void add(@NotNull Merchandise m, int amount) {
		final int ordinal = m.ordinal();
		int a = this.amount[ordinal] + amount;
		this.amount[ordinal] = (short) (a < 0 ? 0 : (a > Short.MAX_VALUE ? Short.MAX_VALUE : a));
		assert a >= 0;
	}

	public void add(@NotNull Inventory inventory, float scale) {
		final short[] amount = this.amount;
		final short[] otherAmount = inventory.amount;
		for (int i = 0; i < amount.length; i++) {
			// Random rounding
			amount[i] += (short) (otherAmount[i] * scale + MathUtils.random());
		}
	}

	public boolean remove(@NotNull Merchandise m, int amount) {
		assert amount >= 0;
		final int ordinal = m.ordinal();
		int a = this.amount[ordinal];
		if (a < amount) {
			return false;
		}
		this.amount[ordinal] = (short) (a - amount);
		return true;
	}

	public void set(@NotNull Inventory inventory) {
		System.arraycopy(inventory.amount, 0, this.amount, 0, this.amount.length);
	}

	public void clear() {
		Arrays.fill(this.amount, (short) 0);
	}
}
