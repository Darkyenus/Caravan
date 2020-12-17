package caravan;

import caravan.input.Binding;
import caravan.input.InputFunction;
import com.badlogic.gdx.Input;

import static com.badlogic.gdx.Input.Keys.A;
import static com.badlogic.gdx.Input.Keys.D;
import static com.badlogic.gdx.Input.Keys.S;
import static com.badlogic.gdx.Input.Keys.W;

/**
 * Collection of used input functions.
 */
public final class Inputs {

	public static final InputFunction UP    = InputFunction.function("Walk Up", Binding.keyboard(W), Binding.keyboard(Input.Keys.UP));
	public static final InputFunction DOWN  = InputFunction.function("Walk Down", Binding.keyboard(S), Binding.keyboard(Input.Keys.DOWN));
	public static final InputFunction LEFT  = InputFunction.function("Walk Left", Binding.keyboard(A), Binding.keyboard(Input.Keys.LEFT));
	public static final InputFunction RIGHT = InputFunction.function("Walk Right", Binding.keyboard(D), Binding.keyboard(Input.Keys.RIGHT));

}
