package caravan.components;

import com.badlogic.gdx.math.Rectangle;
import com.darkyen.retinazer.Component;
import org.jetbrains.annotations.NotNull;

/**
 * A position component.
 */
public final class PositionC implements Component {

    public float x, y;

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public static float manhattanDistance(@NotNull PositionC a, @NotNull PositionC b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    public static boolean inFrustum(@NotNull PositionC pos, @NotNull Rectangle frustum, float size) {
        return pos.x + size > frustum.x && pos.x - size < frustum.x + frustum.width && pos.y + size > frustum.y && pos.y - size < frustum.y + frustum.height;
    }
}
