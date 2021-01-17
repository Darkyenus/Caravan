package caravan.util;

import caravan.CaravanApplication;
import caravan.components.PositionC;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.utils.IntArray;
import com.darkyen.retinazer.Component;
import com.darkyen.retinazer.EntitySetView;
import com.darkyen.retinazer.Mapper;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Consumer;

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

	public static double max(double...values) {
		double result = values[0];
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

	/**
	 * Return an array that contains all values of the original array and also the new value.
	 * If the new value already exists, does not add it.
	 */
	public static int[] intArraySetAdd(int[] values, int newValue) {
		if (indexOf(values, newValue) != -1) {
			return values;
		}
		final int[] newValues = Arrays.copyOf(values, values.length + 1);
		newValues[values.length] = newValue;
		return newValues;
	}

	public static boolean isSanePositive(short value) {
		return value >= 0 && value < 30000;
	}

	public static boolean isSanePositive(float value) {
		return Float.isFinite(value) && value < 1e10f && value >= 0f;
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

	public static @NotNull ScrollPane newScrollPane(@NotNull Actor inside) {
		return newScrollPane(inside, "default");
	}

	public static @NotNull ScrollPane newScrollPane(@NotNull Actor inside, @NotNull String style) {
		final ScrollPane pane = new ScrollPane(inside, CaravanApplication.uiSkin(), style);
		pane.setFadeScrollBars(false);
		pane.setFlickScroll(false);
		pane.setScrollingDisabled(true, false);
		pane.setSmoothScrolling(true);
		return pane;
	}

	public static <C extends Component> void forEach(@NotNull EntitySetView entities, @NotNull Mapper<C> mapper, @NotNull Consumer<C> action) {
		final IntArray indices = entities.getIndices();
		for (int i = 0; i < indices.size; i++) {
			action.accept(mapper.get(indices.get(i)));
		}
	}

	public static int findClosest(@NotNull EntitySetView entitySet, @NotNull Mapper<PositionC> positionMapper, @NotNull Vector2 target) {
		int nearest = -1;
		float nearestDist2 = Float.MAX_VALUE;

		final IntArray entitiesArray = entitySet.getIndices();
		final int[] entities = entitiesArray.items;
		final int entitiesSize = entitiesArray.size;
		for (int i = 0; i < entitiesSize; i++) {
			final int entity = entities[i];

			final PositionC position = positionMapper.getOrNull(entity);
			if (position == null) continue;

			final float entityDst2 = PositionC.manhattanDistance(position, target.x, target.y);
			if (entityDst2 < nearestDist2) {
				nearest = entity;
				nearestDist2 = entityDst2;
			}
		}

		return nearest;
	}

	public static final EventListener ALL_HANDLING_INPUT_LISTENER = event -> event instanceof InputEvent;
}
