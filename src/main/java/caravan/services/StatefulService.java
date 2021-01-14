package caravan.services;

import com.darkyen.retinazer.EngineService;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;

/**
 * Service that has some state that is worth saving.
 */
public interface StatefulService extends EngineService {

	/** Version of the output format. */
	default int stateVersion() {
		return 1;
	}

	/** Name of the service for identification. */
	@NotNull
	default String serviceName() {
		return getClass().getName();
	}

	/** Save state to the output */
	void save(@NotNull Output output);

	/** Load state from previously stored data with the same version. */
	void load(@NotNull Input input);

	/** Load state from previously stored data, given some version.  */
	default void load(@NotNull Input input, int version) {
		if (version == stateVersion()) {
			load(input);
		}
	}

}
