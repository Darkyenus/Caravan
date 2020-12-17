package caravan.input;

/**
 * Called by {@link BoundInputFunction} when something happens.
 */
@FunctionalInterface
public interface Trigger {
	/**
	 * @param times how many times this was triggered in given window
	 * @param pressed true if pressed OR toggle event enabled, false otherwise
	 * @return true if the event was accepted, false otherwise. Returning false on PRESS event will cause RELEASE event to not be triggered.
	 */
	boolean triggered(int times, boolean pressed);
}
