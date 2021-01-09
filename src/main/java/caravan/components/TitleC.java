package caravan.components;

import caravan.util.CaravanComponent;
import com.badlogic.gdx.graphics.Color;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;

/** Positioned entity that has a title rendered over it. */
@CaravanComponent.Serialized(name = "Title", version = 1)
public final class TitleC extends CaravanComponent {

	/** Title to render */
	@NotNull
	public String title = "";
	/** Color of the title */
	@NotNull
	public final Color color = new Color(Color.WHITE);

	public float yOffset;
	public float lineHeight;

	{
		reset();
	}

	@Override
	public void reset() {
		title = "";
		color.set(Color.WHITE);
		yOffset = 0f;
		lineHeight = 1f;
	}

	@Override
	public void save(@NotNull Output output) {
		output.writeString(title);
		output.writeInt(Color.argb8888(color));
		output.writeFloat(yOffset);
		output.writeFloat(lineHeight);
	}

	@Override
	public void load(@NotNull Input input, int version) {
		title = input.readString();
		Color.argb8888ToColor(color, input.readInt());
		yOffset = input.readFloat();
		lineHeight = input.readFloat();
	}
}
