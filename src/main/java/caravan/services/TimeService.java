package caravan.services;

import com.darkyen.retinazer.EngineService;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;

/**
 * Service that provides other services with simulation info.
 */
public final class TimeService implements EngineService, StatefulService {

	/** The update delta, always current for the frame, not necessarily zero when not simulating.
	 * Set externally. */
	public transient float delta;
	/** Whether or not the simulation is running.
	 * Set externally. */
	public transient boolean simulating;
	/** How many day advances did happen on this time step.
	 * Usually 0, sometimes 1 when the day advances, but could be more in some extreme cases.
	 * Set internally.*/
	public transient int dayAdvances;

	/** The duration of a game world day in real world seconds. */
	private static final float DAY_DURATION = 30f;

	/** What day is it in the game world.
	 * Set internally. */
	public int day;
	/** [0,1) the time of day in the game world.
	 * Set internally. */
	public float timeOfDay;

	@Override
	public void update() {
		dayAdvances = 0;
		timeOfDay += delta / DAY_DURATION;
		while (timeOfDay >= 1f) {
			dayAdvances++;
			day++;
			timeOfDay -= 1f;
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
