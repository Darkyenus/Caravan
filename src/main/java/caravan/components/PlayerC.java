package caravan.components;

import caravan.util.CaravanComponent;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;

/** Entities with this component are player controllable. */
@CaravanComponent.Serialized(name = "Player", version = 2)
public final class PlayerC extends CaravanComponent {

    public boolean selected;
    public boolean openTradeOnArrival;

    public @NotNull String notes = "";

    public void set(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void reset() {
        selected = false;
        openTradeOnArrival = false;
        notes = "";
    }

    @Override
    public void save(@NotNull Output output) {
        output.writeBoolean(selected);
        output.writeBoolean(openTradeOnArrival);
        output.writeString(notes);
    }

    @Override
    public void load(@NotNull Input input, int version) {
        selected = input.readBoolean();
        openTradeOnArrival = input.readBoolean();
        if (version >= 2) {
            notes = input.readString();
        }
    }
}
