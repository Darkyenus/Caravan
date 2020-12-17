package caravan.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Input processor that handles input through {@link BoundInputFunction}.
 */
public final class GameInput implements InputProcessor {

	private static final String TAG = "GameInput";
	private static final long DISCARD_SCROLL_ACCUMULATORS_AFTER_MS = 1000;

	private final Array<BoundInputFunction> functions = new Array<>(BoundInputFunction.class);

	private final IntMap<BoundInputFunction> keyBound             = new IntMap<>();
	private final IntMap<BoundInputFunction> buttonBound          = new IntMap<>();
	private       MouseTrigger               mouseTrigger         = null;
	private       BoundInputFunction         positiveXScrollBound = null;
	private       BoundInputFunction         negativeXScrollBound = null;
	private       BoundInputFunction         positiveYScrollBound = null;
	private       BoundInputFunction         negativeYScrollBound = null;

	private int   lastX               = Gdx.input.getX();
	private int   lastY               = Gdx.input.getY();
	private long  lastScrollTimestamp = 0;
	private float scrollXAccumulator  = 0;
	private float scrollYAccumulator  = 0;

	private boolean rebuildNeeded = false;

	/** Same as {@link #use(InputFunction, Trigger)} with no trigger. */
	@NotNull
	public BoundInputFunction use(@NotNull InputFunction function) {
		return use(function, null);
	}

	/**
	 * Create a binding for the given function.
	 * @param function to be used
	 * @param trigger to be notified when the input is triggered
	 * @return object that can be queried for the current state of the input
	 */
	@NotNull
	public BoundInputFunction use(@NotNull InputFunction function, @Nullable Trigger trigger) {
		final BoundInputFunction bound = new BoundInputFunction(function, trigger);
		functions.add(bound);
		rebuildNeeded = true;
		return bound;
	}

	/** Set mouse listener. */
	public void mouse(@NotNull MouseTrigger mouse) {
		this.mouseTrigger = mouse;
	}

	/**
	 * Rebuild internal lookups because the binding has changed.
	 */
	public void invalidate() {
		rebuildNeeded = true;
	}

	private void ensureBuilt() {
		if (!rebuildNeeded) {
			return;
		}
		rebuildNeeded = false;

		// Assign all used functions to the access cache
		keyBound.clear();
		buttonBound.clear();
		positiveXScrollBound = null;
		negativeXScrollBound = null;
		positiveYScrollBound = null;
		negativeYScrollBound = null;
		for (BoundInputFunction function : functions) {
			for (Binding binding : function.function.realBinding) {
				final int value = binding.value;
				switch (binding.type) {
					case KEYBOARD:
						if (keyBound.containsKey(value)) {
							Gdx.app.log(TAG, "Duplicate binding for "+ binding.toMenuString()+": "+keyBound.get(value)+" and "+function);
						} else {
							keyBound.put(value, function);
						}
						break;
					case MOUSE_BUTTON:
						if (buttonBound.containsKey(value)) {
							Gdx.app.log(TAG, "Duplicate binding for "+ binding.toMenuString() +": "+keyBound.get(value)+" and "+function);
						} else {
							buttonBound.put(value, function);
						}
						break;
					case MOUSE_WHEEL_X:
						if (value > 1) {
							if (positiveXScrollBound == null) {
								positiveXScrollBound = function;
							} else {
								Gdx.app.log(TAG, "Duplicate binding for "+ binding.toMenuString() +": "+positiveXScrollBound+" and "+function);
							}
						} else {
							if (negativeXScrollBound == null) {
								negativeXScrollBound = function;
							} else {
								Gdx.app.log(TAG, "Duplicate binding for "+ binding.toMenuString() +": "+negativeXScrollBound+" and "+function);
							}
						}
						break;
					case MOUSE_WHEEL_Y:
						if (value > 1) {
							if (positiveYScrollBound == null) {
								positiveYScrollBound = function;
							} else {
								Gdx.app.log(TAG, "Duplicate binding for "+ binding.toMenuString() +": "+positiveYScrollBound+" and "+function);
							}
						} else {
							if (negativeYScrollBound == null) {
								negativeYScrollBound = function;
							} else {
								Gdx.app.log(TAG, "Duplicate binding for "+ binding.toMenuString() +": "+negativeYScrollBound+" and "+function);
							}
						}
						break;
				}
			}
		}
	}


	@Override
	public boolean keyDown(int keycode) {
		ensureBuilt();
		final BoundInputFunction function = keyBound.get(keycode);
		return function != null && function.keyPressed();
	}

	@Override
	public boolean keyUp(int keycode) {
		ensureBuilt();
		final BoundInputFunction function = keyBound.get(keycode);
		return function != null && function.keyReleased();
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}


	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		ensureBuilt();
		final BoundInputFunction function = buttonBound.get(button);
		return function != null && function.keyPressed();
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		ensureBuilt();
		final BoundInputFunction function = buttonBound.get(button);
		return function != null && function.keyReleased();
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		ensureBuilt();
		return mouseMoved(screenX, screenY, true);
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		ensureBuilt();
		return mouseMoved(screenX, screenY, false);
	}

	private boolean mouseMoved(int x, int y, boolean drag) {
		ensureBuilt();
		boolean result = false;
		if (mouseTrigger != null) {
			result = mouseTrigger.mouseMoved(lastX, lastY, x, y, drag);
		}
		lastX = x;
		lastY = y;
		return result;
	}

	@Override
	public boolean scrolled(float amountX, float amountY) {
		ensureBuilt();
		final long now = System.currentTimeMillis();
		if (lastScrollTimestamp + DISCARD_SCROLL_ACCUMULATORS_AFTER_MS < now) {
			scrollXAccumulator = 0;
			scrollYAccumulator = 0;
		}
		lastScrollTimestamp = now;

		scrollXAccumulator += amountX;
		scrollYAccumulator += amountY;

		final int scrollX = (int) this.scrollXAccumulator;
		final int scrollY = (int) this.scrollYAccumulator;
		scrollXAccumulator -= scrollX;
		scrollYAccumulator -= scrollY;

		boolean handled = false;
		if (scrollX != 0) {
			BoundInputFunction function = scrollX > 0 ? positiveXScrollBound : negativeXScrollBound;
			if (function == null) return false;
			if (function.wheelTurned(Math.abs(scrollX))) {
				handled = true;
			}
		}

		if (scrollY != 0) {
			BoundInputFunction function = scrollY > 0 ? positiveYScrollBound : negativeYScrollBound;
			if (function == null) return false;
			if (function.wheelTurned(Math.abs(scrollY))) {
				handled = true;
			}
		}

		return handled;
	}
}
