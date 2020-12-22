package caravan.world;

import java.util.Arrays;

/** A map which assigns a numerical attribute to the whole world. */
public class WorldAttributeFloat {

	public final int width, height;
	public final float[] values;
	public final float defaultValue;

	public WorldAttributeFloat(int width, int height, float defaultValue) {
		this.width = width;
		this.height = height;
		this.defaultValue = defaultValue;
		this.values = new float[width * height];
		Arrays.fill(this.values, defaultValue);
	}

	/** Get the value at specified position. */
	public float get(int x, int y) {
		if (x < 0 || x >= width || y < 0 || y >= height) {
			return defaultValue;
		}
		return values[x + y * width];
	}

	/** Set the value at specified position.
	 * @return old value at that position or defaultValue if the set failed because the coordinates are out of bounds */
	public float set(int x, int y, float value) {
		if (x < 0 || x >= width || y < 0 || y >= height) {
			return defaultValue;
		}
		final int index = x + y * width;
		final float result = values[index];
		values[index] = value;
		return result;
	}
}
