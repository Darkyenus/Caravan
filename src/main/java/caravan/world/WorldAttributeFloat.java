package caravan.world;

import caravan.util.ValueNoise;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/** A map which assigns a numerical attribute to the whole world. */
public final class WorldAttributeFloat {

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

	public WorldAttributeFloat(int width, int height, float defaultValue, FillFunction f) {
		this.width = width;
		this.height = height;
		this.defaultValue = defaultValue;
		this.values = new float[width * height];
		fill(f);
	}

	/** Get the value at specified position. */
	public float get(int x, int y) {
		if (x < 0 || x >= width || y < 0 || y >= height) {
			return defaultValue;
		}
		return values[x + y * width];
	}

	/** Apply given kernel centered to given coordinates, multiply the underlying values and return their sum. */
	public float getKernelSum(int x, int y, float @Nullable[] kernel, int kernelWidth, int kernelHeight) {
		x -= kernelWidth / 2;
		y -= kernelHeight / 2;
		int ki = 0;
		float sum = 0f;
		for (int ky = 0; ky < kernelHeight; ky++) {
			float lineSum = 0f;// Could give very slightly higher precision, maybe
			for (int kx = 0; kx < kernelWidth; kx++) {
				final float kv = kernel == null ? 1f : kernel[ki++];
				lineSum += get(x + kx, y + ky) * kv;
			}
			sum += lineSum;
		}
		return sum;
	}

	/** Apply given kernel centered to given coordinates, multiply the underlying values and return their max. */
	public float getKernelMax(int x, int y, float @Nullable[] kernel, int kernelWidth, int kernelHeight) {
		x -= kernelWidth / 2;
		y -= kernelHeight / 2;
		int ki = 0;
		float max = Float.NEGATIVE_INFINITY;
		for (int ky = 0; ky < kernelHeight; ky++) {
			for (int kx = 0; kx < kernelWidth; kx++) {
				final float kv = kernel == null ? 1f : kernel[ki++];
				max = Math.max(max, get(x + kx, y + ky) * kv);
			}
		}
		return max;
	}

	/** Apply given kernel centered to given coordinates, multiply the underlying values and return their min. */
	public float getKernelMin(int x, int y, float @Nullable[] kernel, int kernelWidth, int kernelHeight) {
		x -= kernelWidth / 2;
		y -= kernelHeight / 2;
		int ki = 0;
		float min = Float.POSITIVE_INFINITY;
		for (int ky = 0; ky < kernelHeight; ky++) {
			for (int kx = 0; kx < kernelWidth; kx++) {
				final float kv = kernel == null ? 1f : kernel[ki++];
				min = Math.min(min, get(x + kx, y + ky) * kv);
			}
		}
		return min;
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

	/** Add given value to the value at given coordinates. */
	public void add(int x, int y, float value) {
		if (x < 0 || x >= width || y < 0 || y >= height) {
			return;
		}
		values[x + y * width] += value;
	}

	/** Add a sampling of the noise on this map. */
	public void add(long seed, float octave, float magnitude) {
		final float[] values = this.values;
		final float invOctave = 1f / octave;
		int i = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				values[i++] += ValueNoise.sample(seed, x, y, invOctave, magnitude);
			}
		}
	}

	/** Add the given value to all values on the grid. */
	public void add(float offset) {
		final float[] values = this.values;
		final int length = values.length;
		for (int i = 0; i < length; i++) {
			values[i] += offset;
		}
	}

	/** Multiply all values on the grid with multiplier. */
	public void scale(float multiplier) {
		final float[] values = this.values;
		final int length = values.length;
		for (int i = 0; i < length; i++) {
			values[i] *= multiplier;
		}
	}

	/** Clamp all values to lie between min and max. */
	public void clamp(float min, float max) {
		final float[] values = this.values;
		final int length = values.length;
		for (int i = 0; i < length; i++) {
			values[i] = MathUtils.clamp(values[i], min, max);
		}
	}

	/** Take all values that are <= inset tiles away from the edge (tile at 0,0 is 1,1 from edge),
	 * normalize that distance to (0, 1] range, multiply with distance from other dimensions,
	 * pass through interpolation and multiply the grid value with that.
	 *
	 * Useful for creating a "fade" effect near the edges. */
	public void attenuateEdges(int inset, Interpolation interpolation) {
		final int width = this.width;
		final int height = this.height;
		final float[] values = this.values;
		int i = 0;
		for (int y = 0; y < height; y++) {
			final float yFactor = Math.min((float) Math.min(y, height - 1 - y) / (float) inset, 1f);
			for (int x = 0; x < width; x++) {
				final float xFactor = Math.min((float) Math.min(x, width - 1 - x) / (float) inset, 1f);
				final float factor = interpolation.apply(yFactor * xFactor);
				values[i++] *= factor;
			}
		}
	}

	/** @return the smallest value in the values array */
	public float min() {
		final float[] values = this.values;
		final int length = values.length;
		float min = values[0];
		for (int i = 1; i < length; i++) {
			min = Math.min(min, values[i]);
		}
		return min;
	}

	/** @return the largest value in the values array */
	public float max() {
		final float[] values = this.values;
		final int length = values.length;
		float max = values[0];
		for (int i = 1; i < length; i++) {
			max = Math.max(max, values[i]);
		}
		return max;
	}

	/** @return average value of the values in the array */
	public float average() {
		double sum = 0f;
		for (float value : this.values) {
			sum += value;
		}
		return (float) (sum / this.values.length);
	}

	/** @return median value of the values in the array */
	public float median() {
		final float[] sorted = Arrays.copyOf(values, values.length);
		Arrays.sort(sorted);
		final int length = sorted.length;
		if (length % 2 == 1) {
			return sorted[length / 2];
		} else {
			return (sorted[length / 2] + sorted[length / 2 + 1]) * 0.5f;
		}
	}

	/** Calculate slope into a new map. */
	public WorldAttributeFloat slope() {
		return new WorldAttributeFloat(width, height, 0f, (x, y, currentValue) -> {
			final float c = get(x, y);
			final float left = Math.abs(get(x - 1, y) - c);
			final float right = Math.abs(get(x + 1, y) - c);
			final float up = Math.abs(get(x, y - 1) - c);
			final float down = Math.abs(get(x, y + 1) - c);
			return (left + right + up + down) * 0.25f;
		});
	}

	@FunctionalInterface
	interface FillFunction {
		float value(int x, int y, float currentValue);
	}

	/** Set each cell value to whatever the f function returns. */
	public void fill(@NotNull FillFunction f) {
		final int width = this.width;
		final int height = this.height;
		final float[] values = this.values;
		int i = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				values[i] = f.value(x, y, values[i]);
				i++;
			}
		}
	}

	/** In the area around (x,y) decrease the values by (radius - euclidean)*scale distance from that point */
	public void dent(int xo, int yo, int radius, float scale) {
		final float[] values = this.values;
		final int width = this.width;
		final int height = this.height;

		for (int y = Math.max(yo - radius, 0); y <= Math.min(yo + radius, height - 1); y++) {
			for (int x = Math.max(xo - radius, 0); x <= Math.min(xo + radius, width - 1); x++) {
				float xd = x - xo;
				float yd = y - yo;
				float dent = radius - (float) Math.sqrt(xd * xd + yd * yd);
				if (dent <= 0) {
					continue;
				}

				values[width * y + x] -= dent * scale;
			}
		}
	}

	// https://colorbrewer2.org/#type=sequential&scheme=YlGnBu&n=9 reversed
	private static final int[] COLOR_RAMP = new int[] {
			0x081d58,
			0x253494,
			0x225ea8,
			0x1d91c0,
			0x41b6c4,
			0x7fcdbb,
			0xc7e9b4,
			0xedf8b1,
			0xffffd9
	};

	public void saveVisualization(@NotNull String name) {
		final float min = min();
		final float max = max();

		final Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGB888);
		pixmap.setBlending(Pixmap.Blending.None);

		final int width = this.width;
		final int height = this.height;
		final float[] values = this.values;
		int i = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				final float index = MathUtils.map(min, max, 0, COLOR_RAMP.length - 1, values[i++]);
				int indexI = MathUtils.clamp((int) index, 0, COLOR_RAMP.length - 2);
				float indexProgress = MathUtils.clamp(index - indexI, 0f, 1f);
				final int fromRGB = COLOR_RAMP[indexI];
				final int toRGB = COLOR_RAMP[indexI + 1];
				final int r = Math.round(MathUtils.lerp((fromRGB >> 16) & 0xFF, (toRGB >> 16) & 0xFF, indexProgress));
				final int g = Math.round(MathUtils.lerp((fromRGB >> 8) & 0xFF, (toRGB >> 8) & 0xFF, indexProgress));
				final int b = Math.round(MathUtils.lerp(fromRGB & 0xFF, toRGB & 0xFF, indexProgress));
				final int rgba = (r << 24) | (g << 16) | (b << 8) | 0xFF;
				pixmap.drawPixel(x, y, rgba);
			}
		}

		PixmapIO.writePNG(Gdx.files.local(name+".png"), pixmap);
	}
}
