package caravan.world;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.Arrays;

/** A map which assigns an object attribute to the whole world.
 * @param <T> a type of the attribute to store */
public final class WorldAttribute<T> {

	public final int width, height;
	public final @NotNull T @NotNull[] values;
	public final @NotNull T defaultValue;

	/**
	 * @param width of the world
	 * @param height of the world
	 * @param defaultValue that is used to fill the world initially and returned on out-of bounds access.
	 *                      MUST BE BASE CLASS OF T, because {@link #values} array will be of that type.
	 */
	public WorldAttribute(int width, int height, @NotNull T defaultValue) {
		this.width = width;
		this.height = height;
		this.defaultValue = defaultValue;
		//noinspection unchecked
		this.values = (T[]) Array.newInstance(defaultValue.getClass(), width * height);
		Arrays.fill(this.values, defaultValue);
	}

	/** Get the value at specified position. */
	public @NotNull T get(int x, int y) {
		if (x < 0 || x >= width || y < 0 || y >= height) {
			return defaultValue;
		}
		return values[x + y * width];
	}

	/** Set the value at specified position.
	 * @return old value at that position or defaultValue if the set failed because the coordinates are out of bounds */
	public @NotNull T set(int x, int y, @NotNull T value) {
		if (x < 0 || x >= width || y < 0 || y >= height) {
			return defaultValue;
		}
		final int index = x + y * width;
		final T result = values[index];
		values[index] = value;
		return result;
	}
}
