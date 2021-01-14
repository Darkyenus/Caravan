package caravan.util;

import com.badlogic.gdx.math.MathUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Utility methods for finding min or max value in an array.
 */
public final class Util {

	public static float max(float...values) {
		float result = values[0];
		for (int i = 1, valuesLength = values.length; i < valuesLength; i++) {
			result = Math.max(result, values[i]);
		}
		return result;
	}

	public static float min(float...values) {
		float result = values[0];
		for (int i = 1, valuesLength = values.length; i < valuesLength; i++) {
			result = Math.min(result, values[i]);
		}
		return result;
	}

	public static int maxIndex(float[] values) {
		float max = values[0];
		int maxIndex = 0;
		for (int i = 1; i < values.length; i++) {
			if (values[i] > max) {
				max = values[i];
				maxIndex = i;
			}
		}
		return maxIndex;
	}

	public static int minIndex(float[] values) {
		float min = values[0];
		int minIndex = 0;
		for (int i = 1; i < values.length; i++) {
			if (values[i] < min) {
				min = values[i];
				minIndex = i;
			}
		}
		return minIndex;
	}

	public static int minIndex(int[] values) {
		int min = values[0];
		int minIndex = 0;
		for (int i = 1; i < values.length; i++) {
			if (values[i] < min) {
				min = values[i];
				minIndex = i;
			}
		}
		return minIndex;
	}

	public static int indexOf(int[] values, int value) {
		for (int i = 0; i < values.length; i++) {
			if (values[i] == value) {
				return i;
			}
		}
		return -1;
	}

	public static short toShortClamp(int value) {
		return (short) MathUtils.clamp(value, Short.MIN_VALUE, Short.MAX_VALUE);
	}

	public static short toShortClampUnsigned(int value) {
		return (short) MathUtils.clamp(value, 0, Short.MAX_VALUE);
	}

	/** Random round a positive value. */
	public static int rRound(float v) {
		return (int) (v + MathUtils.random());
	}

	public static void swap(int @NotNull[] array, int index0, int index1) {
		final int v0 = array[index0];
		array[index0] = array[index1];
		array[index1] = v0;
	}

	public static void swap(short @NotNull[] array, int index0, int index1) {
		final short v0 = array[index0];
		array[index0] = array[index1];
		array[index1] = v0;
	}

	public static float[] manhattanKernel(float falloff, int offset) {
		final int steps = MathUtils.ceilPositive(1f / falloff) + offset;
		final int size = steps * 2 + 1;
		final float[] kernel = new float[size * size];
		int k = 0;
		for (int y = 0; y < size; y++) {
			final int yOff = Math.abs(steps - y);
			for (int x = 0; x < size; x++) {
				final int xOff = Math.abs(steps - x);
				kernel[k++] = Math.max(0f , 1f - falloff * (Math.max(xOff + yOff - offset, 0f)));
			}
		}
		return kernel;
	}

	public static int kernelSide(float[] kernel) {
		return Math.round((float) Math.sqrt(kernel.length));
	}
}
