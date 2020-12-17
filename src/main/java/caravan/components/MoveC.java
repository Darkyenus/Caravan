package caravan.components;

import com.darkyen.retinazer.Component;

/**
 * A component for entities with {@link PositionC} that can move around.
 */
public final class MoveC implements Component {

	public float targetX, targetY;
	public float speed;

	public void set(float targetX, float targetY, float speed) {
		this.targetX = targetX;
		this.targetY = targetY;
		this.speed = speed;
	}

}
