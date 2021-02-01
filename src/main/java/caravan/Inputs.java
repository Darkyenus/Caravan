package caravan;

import caravan.input.Binding;
import caravan.input.InputFunction;
import com.badlogic.gdx.Input;

import static com.badlogic.gdx.Input.Keys.*;

/**
 * Collection of used input functions.
 */
public final class Inputs {

	public static final InputFunction UP    = InputFunction.function("Walk Up", Binding.keyboard(W), Binding.keyboard(Input.Keys.UP));
	public static final InputFunction DOWN  = InputFunction.function("Walk Down", Binding.keyboard(S), Binding.keyboard(Input.Keys.DOWN));
	public static final InputFunction LEFT  = InputFunction.function("Walk Left", Binding.keyboard(A), Binding.keyboard(Input.Keys.LEFT));
	public static final InputFunction RIGHT = InputFunction.function("Walk Right", Binding.keyboard(D), Binding.keyboard(Input.Keys.RIGHT));

	public static final InputFunction MOVE = InputFunction.function("Move to Cursor", Binding.mouseButton(Input.Buttons.LEFT));

	public static final InputFunction PAUSE = InputFunction.function("Pause", Binding.keyboard(Input.Keys.SPACE));

	public static final InputFunction SCROLL = InputFunction.function("Scroll", Binding.mouseButton(Input.Buttons.RIGHT));
	public static final InputFunction ZOOM_IN = InputFunction.function("Zoom In", Binding.scrollWheelY(false));
	public static final InputFunction ZOOM_OUT = InputFunction.function("Zoom Out", Binding.scrollWheelY(true));

	public static final InputFunction NOTES = InputFunction.function("Notes", Binding.keyboard(N));

	public static final InputFunction[] ALL_INPUTS = new InputFunction[] {
			UP,
			DOWN,
			LEFT,
			RIGHT,
			MOVE,
			PAUSE,
			SCROLL,
			ZOOM_IN,
			ZOOM_OUT,
			NOTES
	};
}
