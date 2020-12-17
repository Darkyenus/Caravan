package caravan.input;

/**
 * An immutable specification of an action.
 */
public final class Binding {

	static final Binding[] EMPTY_ARRAY = new Binding[0];

	public final BindingType type;
	public final int         value;

	public Binding(BindingType type, int value) {
		this.type = type;
		this.value = value;
	}

	public String toMenuString() {
		return type.toMenuString(value);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Binding binding = (Binding) o;
		return value == binding.value && type == binding.type;
	}

	@Override
	public int hashCode() {
		int result = type.hashCode();
		result = 31 * result + value;
		return result;
	}

	public static Binding keyboard(int key) {
		return new Binding(BindingType.KEYBOARD, key);
	}

	public static Binding mouseButton(int button) {
		return new Binding(BindingType.MOUSE_BUTTON, button);
	}

	public static Binding scrollWheelX(boolean positive) {
		return new Binding(BindingType.MOUSE_WHEEL_X, positive ? 1 : -1);
	}

	public static Binding scrollWheelY(boolean positive) {
		return new Binding(BindingType.MOUSE_WHEEL_Y, positive ? 1 : -1);
	}

}
