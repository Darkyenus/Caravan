package caravan.util;

import com.badlogic.gdx.math.MathUtils;

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

	/** Random round a positive value. */
	public static int rRound(float v) {
		return (int) (v + MathUtils.random());
	}
}
