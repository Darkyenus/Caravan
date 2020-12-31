package caravan.components;

import caravan.world.Inventory;
import com.badlogic.gdx.utils.Pool;
import com.darkyen.retinazer.Component;

/**
 * Entity represents a caravan.
 */
public final class CaravanC implements Component, Pool.Poolable {

	public int money = 0;
	public final Inventory inventory = new Inventory();

	@Override
	public void reset() {
		money = 0;
		inventory.clear();
	}
}
