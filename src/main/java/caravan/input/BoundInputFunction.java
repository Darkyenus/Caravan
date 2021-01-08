package caravan.input;

import caravan.CaravanApplication;
import com.badlogic.gdx.Gdx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An {@link InputFunction}, wired into appropriate {@link GameInput}.
 * Stores current state of the input (pressed, etc.)
 */
public final class BoundInputFunction {

	@NotNull
	public final InputFunction function;

	/** Called when triggered. */
	private final Trigger trigger;
	/** Whether or not is the function currently pressed. */
	private boolean pressed = false;
	/** Frame ID at which pressed was last changed. */
	private long pressedFrameId = 0;
	/** The time when this was last triggered */
	private long    lastPressed = 0;
	/** How many times in row was this triggered. */
	private int     times = 1;

	public BoundInputFunction(@NotNull InputFunction function, @Nullable Trigger trigger) {
		this.function = function;
		this.trigger = trigger;
	}

	public boolean isPressed() {
		return pressed;
	}

	public boolean isJustPressed() {
		return pressed && pressedFrameId == CaravanApplication.frameId;
	}

	public boolean isJustReleased() {
		return !pressed && pressedFrameId == CaravanApplication.frameId;
	}

	//region Internal logic
	private boolean trigger(boolean pressed) {
		if (this.pressed != pressed) {
			this.pressedFrameId = CaravanApplication.frameId;
		}
		this.pressed = pressed;
		return trigger != null && trigger.triggered(times, pressed);
	}

	protected boolean keyPressed() {
		if (!function.toggle) {
			if (pressed) {
				//This is weird, ignore
				return true;
			}

			if (function.repeatTimeout != 0) {
				long now = System.currentTimeMillis();
				if (lastPressed + function.repeatTimeout > now) {
					times++;
				} else {
					times = 1;
				}
				lastPressed = now;
			}

			return trigger(true);
		} else {
			times = 1;
			if (pressed) {
				return trigger(false);
			} else {
				return trigger(true);
			}
		}
	}

	protected boolean keyReleased() {
		if (!function.toggle) {
			//noinspection SimplifiableIfStatement
			if (!pressed) return false;
			return trigger(false);
		} else return false;
	}

	protected boolean wheelTurned(int amount) {
		boolean result = false;
		if (function.repeatTimeout == 0) {
			//Can collapse the repeats
			if (function.toggle) {
				final boolean originalState = pressed;
				final boolean finalState = ((pressed ? 1 : 0) + amount) % 2 == 1;
				if (originalState != finalState) {
					trigger(finalState);
				}
				result = true;
			} else {
				times = amount;
				result = trigger(true);
				result = result || trigger(false);
			}
		} else {
			for (int i = 0; i < amount; i++) {
				result = result || keyPressed();
				result = result || keyReleased();
			}
		}
		return result;
	}

	//endregion


	@Override
	public String toString() {
		return function.toString();
	}
}
