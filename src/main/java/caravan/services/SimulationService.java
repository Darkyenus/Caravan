package caravan.services;

import com.darkyen.retinazer.EngineService;

/**
 * Service that provides other services with simulation info.
 */
public final class SimulationService implements EngineService {

	/** The update delta, always current for the frame. */
	public float delta;
	/** Whether or not the simulation is running. */
	public boolean simulating;

}
