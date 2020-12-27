package caravan.util;

import org.jetbrains.annotations.NotNull;

/**
 * Garbage-less 2D integer vector packed into long. Supports 31-bit precision on each axis.
 */
public final class Vec2 {

	private static final long COORD_MASK = 0x7FFF_FFFFL;
	private static final long PACKED_MASK = 0x7FFF_FFFF_7FFF_FFFFL;

	public static long make(int x, int y) {
		return (((long) x & COORD_MASK) << 32) | ((long) y & COORD_MASK);
	}

	public static int x(long vec) {
		return (int) ((vec << 1) >> 33);
	}

	public static int y(long vec) {
		return (int) ((vec << 33) >> 33);
	}

	public static long plus(long vec0, long vec1) {
		return (vec0 + vec1) & PACKED_MASK;
	}

	public static long unaryMinus(long vec) {
		return ((vec ^ PACKED_MASK) + 0x0000_0001_0000_0001L) & PACKED_MASK;
	}

	public static long minus(long vec0, long vec1) {
		return (vec0 + (vec1 ^ PACKED_MASK) + 0x0000_0001_0000_0001L) & PACKED_MASK;
	}

	public static long times(long vec, int factor) {
		return make(x(vec) * factor, y(vec) * factor);
	}

	public static long div(long vec, int factor) {
		return make(x(vec) / factor, y(vec) / factor);
	}

	/** Euclidean length, squared */
	public static int len2(long vec) {
		final int x = x(vec);
		final int y = y(vec);
		return x*x + y*y;
	}

	/** Euclidean length */
	public static float len(long vec) {
		return (float) Math.sqrt(len2(vec));
	}

	/** Manhattan length */
	public static int manhattanLen(long vec) {
		return Math.abs(x(vec)) + Math.abs(y(vec));
	}

	@NotNull
	public static String toString(long vec) {
		return "["+x(vec)+", "+y(vec)+"]";
	}

	/** Unique zero vector, different from standard zero vector, but behaving the same. */
	public static final long NULL = ~PACKED_MASK;

	public static final long UP = make(0, 1);
	public static final long DOWN = make(0, -1);
	public static final long LEFT = make(-1, 0);
	public static final long RIGHT = make(1, 0);

	public static final long[] DIRECTIONS = {
			UP,
			DOWN,
			LEFT,
			RIGHT
	};

	public static final long ZERO = 0L;

}
