package caravan.util;

import org.jetbrains.annotations.NotNull;

/**
 * A list of {@link Sprite}s that form an animation,
 * with info about the animation frame length.
 */
public final class SpriteAnimation {

	/** Frames of the animation. */
	public final Sprite[] frames;
	/** Time it takes to show a single frame. */
	public final float frameTime;



	public SpriteAnimation(float frameTime, @NotNull Sprite...frames) {
		this.frames = frames;
		this.frameTime = frameTime;

		assert frameTime * 144f >= 1f : "Frame time is too low!"; // RenderSystem algorithm for animation advancement is not efficient for small values.
	}

	public SpriteAnimation(float frameTime, @NotNull String...frames) {
		this(frameTime, new Sprite[frames.length]);
		for (int i = 0; i < frames.length; i++) {
			this.frames[i] = new Sprite(frames[i]);
		}
	}

	/** Utility constructor for default animation frame time. */
	public SpriteAnimation(@NotNull String...frames) {
		this(1f / 3f, frames);
	}

	/** Single frame utility constructor. */
	public SpriteAnimation(@NotNull String frame) {
		this(Float.POSITIVE_INFINITY, new Sprite(frame));
	}
}
