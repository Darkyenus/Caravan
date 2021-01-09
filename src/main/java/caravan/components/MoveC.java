package caravan.components;

import caravan.util.CaravanComponent;
import com.badlogic.gdx.utils.FloatArray;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;

/** A component for entities with {@link PositionC} that can move around. */
@CaravanComponent.Serialized(name = "Move", version = 1)
public final class MoveC extends CaravanComponent {

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

	@Override
	public void save(@NotNull Output output) {
		output.writeInt(waypoints.size);
		output.writeFloats(waypoints.items, 0, waypoints.size);
	}

	@Override
	public void load(@NotNull Input input, int version) {
		final int waypointCount = input.readInt();
		waypoints.clear();
		final float[] waypointItems = waypoints.ensureCapacity(waypointCount);
		waypoints.size = waypointCount;
		for (int i = 0; i < waypointCount; i++) {
			waypointItems[i] = input.readFloat();
		}
	}
}
