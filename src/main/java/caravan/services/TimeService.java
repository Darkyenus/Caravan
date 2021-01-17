package caravan.services;

import caravan.CaravanApplication;
import caravan.Inputs;
import caravan.input.GameInput;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.darkyen.retinazer.EngineService;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static caravan.CaravanApplication.uiSkin;

/**
 * Service that provides other services with simulation info.
 * Do not change the fields externally
 */
public final class TimeService implements EngineService, StatefulService, UIService {

	/** The update delta, always current for the frame, not affected by pause or time scale.
	 * Set externally. */
	public float rawDelta;
	/** The time-scale to use when simulating. Strictly positive. */
	private float timeScale = 1f;
	/** The game delta. 0 when not simulating and affected by the timeScale. */
	public float gameDelta;

	/** Whether or not the simulation is running or is paused. */
	public boolean simulating;
	/** How many day advances did happen on this time step.
	 * Usually 0, sometimes 1 when the day advances, but could be more in some extreme cases. */
	public int dayAdvances;

	/** The duration of a game world day in real world seconds. */
	private static final float DAY_DURATION = 30f;

	/** What day is it in the game world. */
	public int day;
	/** [0,1) the time of day in the game world. */
	public float timeOfDay;

	private Actor pausedOverlay;
	private @Nullable Boolean pauseRequested = null;
	private float requestedTimeScale = timeScale;

	public TimeService(@NotNull GameInput gameInput) {
		gameInput.use(Inputs.PAUSE, (times, pressed) -> {
			if (pressed) {
				pauseRequested = simulating;
				return true;
			}
			return false;
		});
	}

	@Override
	public void createUI(@NotNull CaravanApplication application, @NotNull Stage stage) {
		final Label pausedLabel = new Label("Paused", uiSkin(), "hud");
		final Container<Label> container = new Container<>(pausedLabel);
		container.setFillParent(true);
		container.align(Align.bottom).padBottom(60f);
		container.setTouchable(Touchable.disabled);
		container.setVisible(false);
		stage.addActor(container);
		pausedOverlay = container;
	}

	/** Request that the game pauses on the next frame. */
	public void requestPause() {
		pauseRequested = true;
	}

	/** Request that the paused game resumes on the next frame. */
	public void requestResume() {
		pauseRequested = false;
	}

	/** Request specific time scale. Change takes place on the next frame.
	 * @param timeScale must be strictly positive */
	public void requestTimeScale(float timeScale) {
		requestedTimeScale = timeScale > 0 ? timeScale : 0.01f;
	}

	@Override
	public void update() {
		if (pauseRequested != null) {
			simulating = !pauseRequested;
			pauseRequested = null;
		}
		timeScale = requestedTimeScale;
		gameDelta = simulating ? rawDelta * timeScale : 0f;

		pausedOverlay.setVisible(!simulating);

		if (simulating) {
			dayAdvances = 0;
			timeOfDay += gameDelta / DAY_DURATION;
			while (timeOfDay >= 1f) {
				dayAdvances++;
				day++;
				timeOfDay -= 1f;
			}
		}
	}

	@Override
	public int stateVersion() {
		return 1;
	}

	@Override
	public @NotNull String serviceName() {
		return "Time";
	}

	@Override
	public void save(@NotNull Output output) {
		output.writeInt(day);
		output.writeFloat(timeOfDay);
	}

	@Override
	public void load(@NotNull Input input) {
		day = input.readInt();
		timeOfDay = input.readFloat();
	}
}
