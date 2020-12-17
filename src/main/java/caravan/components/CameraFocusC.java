package caravan.components;


import com.darkyen.retinazer.Component;

/**
 * Game camera will focus on these entities, if they have {@link PositionC}.
 */
public final class CameraFocusC implements Component {

    public float radiusVisibleAround;

    public void set(float areaVisibleAround) {
        this.radiusVisibleAround = areaVisibleAround;
    }
}
