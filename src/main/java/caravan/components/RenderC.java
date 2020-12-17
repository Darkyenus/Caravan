package caravan.components;

import caravan.util.Sprite;
import com.darkyen.retinazer.Component;

/**
 * Adds a graphic representation to the entity in a form of a static or animated sprite.
 * The sprite is positioned so that the middle of the bottom edge of the sprite corresponds
 * to the entity's {@link PositionC}.
 */
public final class RenderC implements Component {

	public Sprite sprite;
	public float scaleX = 1f, scaleY = 1f;

	public void set(Sprite sprite, float scaleX, float scaleY) {
		this.sprite = sprite;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
	}

}
