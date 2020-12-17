package caravan.input;

/**
 * Called by {@link GameInput} when mouse moves.
 */
@FunctionalInterface
public interface MouseTrigger {
	/**
	 * @param drag true if any button is pressed during the move
	 * @return true if the event was accepted, false otherwise. Has effect when bubbling.
	 */
	boolean mouseMoved(int fromX, int fromY, int toX, int toY, boolean drag);
}
