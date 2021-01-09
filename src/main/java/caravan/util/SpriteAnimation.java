package caravan.util;

import caravan.services.Id;
import org.jetbrains.annotations.NotNull;

/**
 * A list of {@link Sprite}s that form an animation,
 * with info about the animation frame length.
 */
public final class SpriteAnimation extends Id<SpriteAnimation> {

	public static final Registry<SpriteAnimation> REGISTRY = new Registry<>();

	/** Frames of the animation. */
	public final Sprite[] frames;
	/** Time it takes to show a single frame. */
	public final float frameTime;

	public SpriteAnimation(int id, float frameTime, @NotNull Sprite...frames) {
		super(id, REGISTRY);
		this.frames = frames;
		this.frameTime = frameTime;

		assert frames.length >= 1;
		assert frameTime * 144f >= 1f : "Frame time is too low!"; // RenderSystem algorithm for animation advancement is not efficient for small values.
	}

	public SpriteAnimation(int id, float frameTime, @NotNull String...frames) {
		this(id, frameTime, new Sprite[frames.length]);
		for (int i = 0; i < frames.length; i++) {
			this.frames[i] = new Sprite(frames[i]);
		}
	}

	/** Utility constructor for default animation frame time. */
	public SpriteAnimation(int id, @NotNull String...frames) {
		this(id, frames.length <= 1 ? Float.POSITIVE_INFINITY : 1f / 3f, frames);
	}
}
