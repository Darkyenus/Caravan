package caravan.world;

import org.jetbrains.annotations.NotNull;

import java.util.BitSet;

/** A map which assigns a boolean attribute to the whole world. */
public final class WorldAttributeBool {

	public final int width, height;
	public final @NotNull BitSet values;

	/**
	 * @param width of the world
	 * @param height of the world
	 */
	public WorldAttributeBool(int width, int height) {
		this.width = width;
		this.height = height;
		this.values = new BitSet(width * height);
	}

	/** Get the value at specified position. */
	public boolean get(int x, int y) {
		if (x < 0 || x >= width || y < 0 || y >= height) {
			return false;
		}
		return values.get(x + y * width);
	}

	/** Set the value at specified position.
	 * @return old value at that position or defaultValue if the set failed because the coordinates are out of bounds */
	public boolean set(int x, int y, boolean value) {
		if (x < 0 || x >= width || y < 0 || y >= height) {
			return false;
		}
		final int index = x + y * width;
		final boolean result = values.get(index);
		values.set(index, value);
		return result;
	}
}
