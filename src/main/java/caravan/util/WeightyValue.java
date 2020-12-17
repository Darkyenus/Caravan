package caravan.util;

/**
 * A float value that attempts to reach the target, but cannot move faster than specified amount.
 */
public final class WeightyValue {

	public float currentValue;
	public float targetValue;

	/** {@link #currentValue} can't go below this value. */
	public float minValue = Float.NEGATIVE_INFINITY;
	/** {@link #currentValue} can't go above this value. */
	public float maxValue = Float.POSITIVE_INFINITY;

	/** The max speed in units per second that the value can move by. Positive. */
	public float maxSpeed = Float.POSITIVE_INFINITY;
	/** The current signed speed in units per second that the value is currently moving by. */
	public float currentSpeed = 0;

	/** How fast, in units per second per second can the {@link #currentSpeed} change. Positive. */
	public float acceleration = Float.POSITIVE_INFINITY;

	public void update(float delta) {
		// Normalize change to compute change from 0 to +x
		float currentValue = 0;
		float targetValue = this.targetValue - currentValue;
		float currentSpeed = this.currentSpeed;
		if (targetValue == currentValue || delta <= 0) {
			return;
		}

		boolean flipped = false;
		if (targetValue < 0) {
			targetValue = -targetValue;
			currentSpeed = -currentSpeed;
			flipped = true;
		}

		if (Float.isInfinite(acceleration)) {
			// Simplified version without acceleration
			if (Float.isInfinite(maxSpeed)) {
				// Super simple version without any speed calculation
				currentValue = targetValue;
				currentSpeed = 0;
			} else {
				// Immediately max speed
				final float possibleChange = delta * maxSpeed; // positive
				if (possibleChange >= targetValue) {
					// Done
					currentValue = targetValue;
					currentSpeed = 0;
				} else {
					// Move towards
					currentValue += possibleChange;
					currentSpeed = maxSpeed;
				}
			}
		} else {
			final float targetMaxSpeed = Math.min(0.5f * ((float) Math.sqrt(4f * acceleration * targetValue + currentSpeed*currentSpeed) - currentSpeed), maxSpeed);
			if (currentSpeed < targetMaxSpeed) {
				// Accelerate
				final float speedChange = targetMaxSpeed - currentSpeed;
				final float timeToAccelerate = Math.min(speedChange / acceleration, delta);
				currentValue += 0.5f * acceleration * timeToAccelerate * timeToAccelerate + timeToAccelerate * currentSpeed;
				currentSpeed += acceleration * timeToAccelerate;
				delta -= timeToAccelerate;
			}

			if (currentSpeed > targetMaxSpeed) {
				// Decelerate
				final float speedChange = currentSpeed - targetMaxSpeed;
				final float timeToDecelerate = Math.min(speedChange / acceleration, delta);
				currentSpeed -= timeToDecelerate * acceleration;
				currentValue += 0.5f * acceleration * timeToDecelerate * timeToDecelerate + timeToDecelerate * currentSpeed;
				delta -= timeToDecelerate;
			}

			// Just move at current speed with remaining delta
			currentValue += currentSpeed * delta;
		}

		// Reapply to real values
		if (flipped) {
			currentValue = -currentValue;
			currentSpeed = -currentSpeed;
		}
		currentValue += this.currentValue;

		// Apply min/max
		if (currentValue < minValue) {
			currentValue = minValue;
			currentSpeed = 0;
		} else if (currentValue > maxValue) {
			currentValue = maxValue;
			currentSpeed = 0;
		}

		this.currentValue = currentValue;
		this.currentSpeed = currentSpeed;
	}


}
