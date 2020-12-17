package caravan.components;

import com.darkyen.retinazer.Component;

/**
 * Entities with this component are player controllable.
 */
public final class PlayerC implements Component {

    public boolean selected;

    public void set(boolean selected) {
        this.selected = selected;
    }
}
