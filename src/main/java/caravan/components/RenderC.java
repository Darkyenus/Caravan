package caravan.components;

import caravan.util.CaravanComponent;
import caravan.util.SpriteAnimation;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adds a graphic representation to the entity in a form of a static or animated sprite.
 * The sprite is positioned so that the middle of the bottom edge of the sprite corresponds
 * to the entity's {@link PositionC}.
 */
@CaravanComponent.Serialized(name = "Render", version = 1)
public final class RenderC extends CaravanComponent {

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

	{
		reset();
	}

	public void set(@Nullable SpriteAnimation sprite) {
		if (this.sprite != sprite) {
			this.sprite = sprite;
			this.timeOnThisFrame = 0;
			this.currentFrame = 0;
		}
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

	@Override
	public void save(@NotNull Output output) {
		output.writeShort(sprite == null ? -1 : sprite.id);
		output.writeFloat(timeOnThisFrame);
		output.writeFloat(animationSpeed);
		output.writeInt(currentFrame);
		output.writeFloat(scaleX);
		output.writeFloat(scaleY);
	}

	@Override
	public void load(@NotNull Input input, int version) {
		final short id = input.readShort();
		if (id == -1) {
			sprite = null;
		} else {
			sprite = SpriteAnimation.REGISTRY.getOrDefault(id);
		}

		timeOnThisFrame = input.readFloat();
		animationSpeed = input.readFloat();
		currentFrame = input.readInt();
		scaleX = input.readFloat();
		scaleY = input.readFloat();
	}
}
