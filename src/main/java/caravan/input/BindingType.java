package caravan.input;

import com.badlogic.gdx.Input;

/**
 * A type of binding used in {@link Binding}
 */
public enum BindingType {
	/**
	 * Value is ID of key
	 */
	KEYBOARD {
		@Override
		String toMenuString(int value) {
			String result = Input.Keys.toString(value);
			if (result != null) return result;
			else return "Unrecognized key (" + value + ")";
		}
	},
	/**
	 * Value is ID of mouse button
	 */
	MOUSE_BUTTON {
		@Override
		String toMenuString(int value) {
			switch (value) {
				case Input.Buttons.LEFT:
					return "Left Mouse Button";
				case Input.Buttons.MIDDLE:
					return "Middle Mouse Button";
				case Input.Buttons.RIGHT:
					return "Right Mouse Button";
				case Input.Buttons.BACK:
					return "Back Mouse Button";
				case Input.Buttons.FORWARD:
					return "Forward Mouse Button";
				default:
					return value + " Mouse Button";
			}
		}
	},
	/** Sign of value is direction of mouse wheel */
	MOUSE_WHEEL_X {
		@Override
		String toMenuString(int value) {
			return value > 0 ? "Scroll Wheel Right" : "Scroll Wheel Left";
		}
	},
	/** Sign of value is direction of mouse wheel */
	MOUSE_WHEEL_Y {
		@Override
		String toMenuString(int value) {
			return value > 0 ? "Scroll Wheel Up" : "Scroll Wheel Down";
		}
	};
	//Note: Mouse is not rebindable

	abstract String toMenuString(int value);
}
