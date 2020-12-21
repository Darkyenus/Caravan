package caravan.components;

import caravan.util.SpriteAnimation;
import com.badlogic.gdx.utils.Pool;
import com.darkyen.retinazer.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Adds a graphic representation to the entity in a form of a static or animated sprite.
 * The sprite is positioned so that the middle of the bottom edge of the sprite corresponds
 * to the entity's {@link PositionC}.
 */
public final class RenderC implements Component, Pool.Poolable {

	/** The sprite (animation) to render. */
	@Nullable
	public SpriteAnimation sprite;
	/** Time already spent showing the {@link #currentFrame}. Used for {@link caravan.services.RenderSystem} housekeeping. */
	public float timeOnThisFrame;
	/** The animation speed multiplier, applied to {@link SpriteAnimation#frameTime}. */
	public float animationSpeed;
	/** The current frame that is being shown. */
	public int currentFrame;

	/** The X and Y scale multiplier of the drawn sprite. Can be negative to flip the sprite. */
	public float scaleX, scaleY;

	public void set(@Nullable SpriteAnimation sprite) {
		if (this.sprite != sprite) {
			this.sprite = sprite;
			this.timeOnThisFrame = 0;
			this.currentFrame = 0;
		}
	}

	{// Called after the component is constructed
		reset();
	}

	@Override
	public void reset() {
		sprite = null;
		timeOnThisFrame = 0;
		animationSpeed = 1f;
		currentFrame = 0;
		scaleX = 1f;
		scaleY = 1f;
	}
}
