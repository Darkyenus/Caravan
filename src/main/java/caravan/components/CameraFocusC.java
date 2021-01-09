package caravan.components;


import caravan.util.CaravanComponent;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;

/**
 * Game camera will focus on these entities, if they have {@link PositionC}.
 */
@CaravanComponent.Serialized(name = "CameraFocus", version = 1)
public final class CameraFocusC extends CaravanComponent {

    public float radiusVisibleAround;

    public void set(float areaVisibleAround) {
        this.radiusVisibleAround = areaVisibleAround;
    }

    @Override
    public void reset() {
        radiusVisibleAround = 0f;
    }

    @Override
    public void save(@NotNull Output output) {
        output.writeFloat(radiusVisibleAround);
    }

    @Override
    public void load(@NotNull Input input, int version) {
        radiusVisibleAround = input.readFloat();
    }
}
