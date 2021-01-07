package caravan.components;

import caravan.world.Inventory;
import caravan.world.Merchandise;
import com.badlogic.gdx.utils.Pool;
import com.darkyen.retinazer.Component;

import java.util.Arrays;

/**
 * Entity represents a caravan.
 */
public final class CaravanC implements Component, Pool.Poolable {

	public int money = 0;
	public boolean[] categories = new boolean[Merchandise.Category.VALUES.length];
	public final Inventory inventory = new Inventory();

	@Override
	public void reset() {
		money = 0;
		Arrays.fill(categories, false);
		inventory.clear();
	}
}
