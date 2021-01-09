package caravan.components;

import caravan.util.CaravanComponent;
import caravan.world.Inventory;
import caravan.world.Merchandise;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/** Entity represents a caravan. */
@CaravanComponent.Serialized(name = "Caravan", version = 1)
public final class CaravanC extends CaravanComponent {

	public int money;
	public boolean[] categories = new boolean[Merchandise.Category.VALUES.length];
	public final Inventory inventory = new Inventory();
	public float speed;

	{
		reset();
	}

	@Override
	public void reset() {
		money = 0;
		Arrays.fill(categories, false);
		inventory.clear();
		speed = 2f;
	}

	@Override
	public void save(@NotNull Output output) {
		output.writeInt(money);
		output.writeFloat(speed);
		output.writeInt(categories.length);
		output.writeBooleans(categories, 0, categories.length);
		inventory.save(output);
	}

	@Override
	public void load(@NotNull Input input, int version) {
		money = input.readInt();
		speed = input.readFloat();
		final boolean[] categories = input.readBooleans(input.readInt());
		System.arraycopy(categories, 0, this.categories, 0, Math.min(categories.length, this.categories.length));
		inventory.load(input);
	}
}
