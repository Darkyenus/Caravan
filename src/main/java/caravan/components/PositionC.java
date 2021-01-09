package caravan.components;

import caravan.util.CaravanComponent;
import com.badlogic.gdx.math.Rectangle;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;

/** A position component. */
@CaravanComponent.Serialized(name = "Position", version = 1)
public final class PositionC extends CaravanComponent {

    public float x, y;

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void reset() {
        x = y = 0;
    }

    public static float manhattanDistance(@NotNull PositionC a, @NotNull PositionC b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    public static boolean inFrustum(@NotNull PositionC pos, @NotNull Rectangle frustum, float size) {
        return pos.x + size > frustum.x && pos.x - size < frustum.x + frustum.width && pos.y + size > frustum.y && pos.y - size < frustum.y + frustum.height;
    }

    @Override
    public void save(@NotNull Output output) {
        output.writeFloat(x);
        output.writeFloat(y);
    }

    @Override
    public void load(@NotNull Input input, int version) {
        x = input.readFloat();
        y = input.readFloat();
    }
}
