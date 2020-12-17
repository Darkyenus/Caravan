package caravan.components;

import com.darkyen.retinazer.Component;

/**
 * A position component.
 */
public final class PositionC implements Component {

    public float x, y;

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

}
