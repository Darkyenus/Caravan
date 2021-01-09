package caravan.services;

import com.darkyen.retinazer.EngineService;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Service that has some state that is worth saving.
 */
public interface StatefulService extends EngineService {

	/** Version of the output format. */
	default int stateVersion() {
		return 1;
	}

	/** Name of the service for identification. */
	default String serviceName() {
		return getClass().getName();
	}

	/** Save state to the output */
	void save(Output output);

	/** Load state from previously stored data with the same version. */
	void load(Input input);

	/** Load state from previously stored data, given some version.  */
	default void load(Input input, int version) {
		if (version == stateVersion()) {
			load(input);
		}
	}

}
