package caravan.components;

import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Pool;
import com.darkyen.retinazer.Component;

/**
 * A component for entities with {@link PositionC} that can move around.
 */
public final class MoveC implements Component, Pool.Poolable {

	/**
	 * Contains packed target info, in format:
	 * [x0, y0, speed0, x1, y1, speed1, ..., xN, yN, speedN]
	 * {@link caravan.services.MoveSystem} looks at the first 3 elements and moves the entity
	 * towards x0, y0, with speed0. After the entity reaches that, the waypoint 0 is removed
	 * and x1 becomes x0 and so on.
	 */
	public final FloatArray waypoints = new FloatArray(true, 30);

	public void addWaypoint(float x, float y, float speed) {
		waypoints.add(x, y, speed);
	}

	@Override
	public void reset() {
		waypoints.clear();
	}
}
