package caravan.components;

import caravan.util.CaravanComponent;
import caravan.util.EnumSerializer;
import caravan.util.Inventory;
import caravan.util.PriceMemory;
import caravan.world.Merchandise;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/** Entity represents a caravan. */
@CaravanComponent.Serialized(name = "Caravan", version = 1)
public final class CaravanC extends CaravanComponent {

	public int money;
	public boolean[] categories = new boolean[Merchandise.Category.COUNT];
	public final Inventory inventory = new Inventory();
	public final PriceMemory priceMemory = new PriceMemory(7);
	/** The last price, for which some merchandise was bought. 0 for unknown. */
	public final short[] inventoryPriceBuyMemory = new short[Merchandise.COUNT];
	/** The last price, for which some merchandise was sold. 0 for unknown. */
	public final short[] inventoryPriceSellMemory = new short[Merchandise.COUNT];
	public float speed;

	{
		reset();
	}

	@Override
	public void reset() {
		money = 0;
		Arrays.fill(categories, false);
		inventory.reset();
		priceMemory.reset();
		Arrays.fill(inventoryPriceBuyMemory, (short) 0);
		Arrays.fill(inventoryPriceSellMemory, (short) 0);
		speed = 2f;
	}

	@Override
	public void save(@NotNull Output output) {
		output.writeInt(money);
		output.writeFloat(speed);

		{
			final EnumSerializer.Writer writer = Merchandise.Category.SERIALIZER.write(output);
			writer.write(output, categories);
		}

		inventory.save(output);
		priceMemory.save(output);
		{
			final EnumSerializer.Writer writer = Merchandise.SERIALIZER.write(output);
			writer.write(output, inventoryPriceBuyMemory);
			writer.write(output, inventoryPriceSellMemory);
		}
	}

	@Override
	public void load(@NotNull Input input, int version) {
		money = input.readInt();
		speed = input.readFloat();

		{
			final EnumSerializer.Reader reader = Merchandise.Category.SERIALIZER.read(input);
			reader.read(input, categories);
		}

		inventory.load(input);
		priceMemory.load(input);

		{
			final EnumSerializer.Reader reader = Merchandise.SERIALIZER.read(input);
			reader.read(input, inventoryPriceBuyMemory);
			reader.read(input, inventoryPriceSellMemory);
		}
	}
}
