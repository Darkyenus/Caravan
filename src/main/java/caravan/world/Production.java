package caravan.world;

import caravan.components.TownC;
import org.jetbrains.annotations.NotNull;

/** Definition of a production type. */
public abstract class Production {

	public final Merchandise output;

	private Production(Merchandise output) {
		this.output = output;
	}

	/** Attempt to produce something */
	public abstract int produce(@NotNull TownC environment, @NotNull Inventory resources);

}
