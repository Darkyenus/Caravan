package caravan.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Pool;
import com.darkyen.retinazer.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Positioned entity that has a title rendered over it.
 */
public class TitleC implements Component, Pool.Poolable {

	/** Title to render */
	@NotNull
	public String title = "";
	/** Color of the title */
	@NotNull
	public Color color = Color.WHITE;

	public float yOffset = 0f;
	public float lineHeight = 1f;

	@Override
	public void reset() {
		title = "";
		color = Color.WHITE;
		yOffset = 0f;
		lineHeight = 1f;
	}
}
