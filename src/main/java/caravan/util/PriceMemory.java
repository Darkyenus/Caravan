package caravan.util;

import caravan.components.TownC;
import caravan.world.Merchandise;
import com.badlogic.gdx.utils.Pool;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * What does a caravan remember.
 *
 * Stores sets of (townEntityId, day of the memory, price by merchandise id []).
 */
public final class PriceMemory implements Pool.Poolable {

	/** For each memory contains the town entity ID or -1 if there is no memory. */
	private int[] townEntityIds;
	/** Day (as recorded by {@link caravan.services.TimeService}) in which this memory was made. */
	private int[] memoryDay;
	/** Packed prices, the size is {@code memoryCount * merchandise count},
	 * prices for n-th memory starts at {@code n * merchandise count} */
	private short[] buyPrices, sellPrices;

	/**
	 * @param memoryCapacity how many towns does the caravan remember
	 */
	public PriceMemory(int memoryCapacity) {
		townEntityIds = new int[memoryCapacity];
		memoryDay = new int[memoryCapacity];
		buyPrices = new short[memoryCapacity * Merchandise.COUNT];
		sellPrices = new short[memoryCapacity * Merchandise.COUNT];
		reset();
	}

	@Override
	public void reset() {
		Arrays.fill(townEntityIds, -1);
	}

	private void swap(int index0, int index1) {
		Util.swap(townEntityIds, index0, index1);
		Util.swap(memoryDay, index0, index1);
		index0 *= Merchandise.COUNT;
		index1 *= Merchandise.COUNT;
		final short[] buyPrices = this.buyPrices;
		final short[] sellPrices = this.sellPrices;
		for (int i = 0; i < Merchandise.COUNT; i++) {
			Util.swap(buyPrices, index0 + i, index1 + i);
			Util.swap(sellPrices, index0 + i, index1 + i);
		}
	}

	private boolean resize_shouldSwap(int index0, int index1) {
		final int[] townEntityIds = this.townEntityIds;
		final boolean valid0 = townEntityIds[index0] == -1;
		final boolean valid1 = townEntityIds[index1] == -1;
		if (!valid0) {
			return valid1;
		}

		final int[] memoryDay = this.memoryDay;
		return memoryDay[index0] < memoryDay[index1];
	}

	public void resize(int newMemoryCount) {
		final int currentMemoryCount = townEntityIds.length;
		if (newMemoryCount == currentMemoryCount) {
			return;
		}

		if (newMemoryCount < currentMemoryCount) {
			// Sort the memories so that the ones that get dumped are the oldest ones
			// Simple old insertion sort will do
			for (int i = 1; i < currentMemoryCount; i++) {
				for (int j = i; j > 0 && resize_shouldSwap(j - 1, j); j--) {
					swap(j - 1, j);
				}
			}
		}

		townEntityIds = Arrays.copyOf(townEntityIds, newMemoryCount);
		if (newMemoryCount > currentMemoryCount) {
			Arrays.fill(townEntityIds, currentMemoryCount, newMemoryCount, -1);
		}
		memoryDay = Arrays.copyOf(memoryDay, newMemoryCount);
		buyPrices = Arrays.copyOf(buyPrices, newMemoryCount * Merchandise.COUNT);
		sellPrices = Arrays.copyOf(sellPrices, newMemoryCount * Merchandise.COUNT);
	}

	private int indexOfTown(int townEntity, boolean toWrite) {
		int index = Util.indexOf(townEntityIds, townEntity);
		if (index != -1 || !toWrite) {
			return index;
		}
		// Find index to overwrite
		final int freeIndex = Util.indexOf(townEntityIds, -1);
		if (freeIndex >= 0) {
			return freeIndex;
		}
		// There are no free memory slots, kick out the oldest memory
		return Util.minIndex(memoryDay);
	}

	public void remember(int today, int townEntity, @NotNull TownC town) {
		final int index = indexOfTown(townEntity, true);
		townEntityIds[index] = townEntity;
		memoryDay[index] = today;

		final int priceOffset = index * Merchandise.COUNT;
		final Merchandise[] merchandise = Merchandise.VALUES;
		final int maxSellPrice = town.money;
		final PriceList prices = town.prices;
		final short[] buyPrices = this.buyPrices;
		final short[] sellPrices = this.sellPrices;

		for (int i = 0; i < Merchandise.COUNT; i++) {
			final Merchandise m = merchandise[i];
			final int priceIndex = priceOffset + i;
			buyPrices[priceIndex] = Util.toShortClampUnsigned(prices.buyPrice(m));
			sellPrices[priceIndex] = Util.toShortClampUnsigned(Math.min(maxSellPrice, prices.sellPrice(m)));
		}
	}

	/** Total capacity of this memory.
	 * Valid memory slots are [0, capacity). */
	public int capacity() {
		return this.townEntityIds.length;
	}

	/**
	 * Check whether the given memory slot is valid for reading.
	 * @param slot slot id
	 * @param noOlderThanDay consider the memory invalid if it is older than from this day
	 * @param notThisTownEntity consider the memory invalid if the town is this one (for convenience)
	 */
	public boolean isMemorySlotValid(int slot, int noOlderThanDay, int notThisTownEntity) {
		final int townEntityId = townEntityIds[slot];
		return townEntityId != -1 && townEntityId != notThisTownEntity && memoryDay[slot] >= noOlderThanDay;
	}

	/**
	 * Return the slot ID for the memory of townEntity that is not older than specified date.
	 * @return id or -1 if no such memory slot
	 */
	public int validSlotForTown(int townEntity, int noOlderThanDay) {
		final int i = Util.indexOf(townEntityIds, townEntity);
		if (i < 0 || memoryDay[i] < noOlderThanDay) {
			return -1;
		}
		return i;
	}

	/** Return the remembered buy price at the given memory slot for the given merchandise. */
	public int buyPrice(int slot, @NotNull Merchandise m) {
		return buyPrices[slot * townEntityIds.length + m.ordinal()];
	}

	/** Return the remembered sell price at the given memory slot for the given merchandise. */
	public int sellPrice(int slot, @NotNull Merchandise m) {
		return sellPrices[slot * townEntityIds.length + m.ordinal()];
	}

	/** Return the town entity ID for the memory at the given slot. */
	public int townEntity(int slot) {
		return townEntityIds[slot];
	}

	public void save(@NotNull Output output) {
		final int[] townEntityIds = this.townEntityIds;
		final int[] memoryDay = this.memoryDay;
		final int memoryCapacity = townEntityIds.length;

		output.writeInt(memoryCapacity);
		output.writeInts(townEntityIds, 0, memoryCapacity);
		output.writeInts(memoryDay, 0, memoryCapacity);

		final short[] buyPrices = this.buyPrices;
		final short[] sellPrices = this.sellPrices;
		final EnumSerializer.Writer writer = Merchandise.SERIALIZER.write(output);
		for (int i = 0; i < memoryCapacity; i++) {
			writer.write(output, buyPrices, i * Merchandise.COUNT, Merchandise.COUNT);
			writer.write(output, sellPrices, i * Merchandise.COUNT, Merchandise.COUNT);
		}
	}

	public void load(@NotNull Input input) {
		final int memoryCapacity = input.readInt();
		resize(memoryCapacity);

		final int[] townEntityIds = this.townEntityIds;
		for (int i = 0; i < memoryCapacity; i++) {
			townEntityIds[i] = input.readInt();
		}

		final int[] memoryDay = this.memoryDay;
		for (int i = 0; i < memoryCapacity; i++) {
			memoryDay[i] = input.readInt();
		}

		final short[] buyPrices = this.buyPrices;
		final short[] sellPrices = this.sellPrices;
		final EnumSerializer.Reader reader = Merchandise.SERIALIZER.read(input);
		for (int i = 0; i < memoryCapacity; i++) {
			reader.read(input, buyPrices, i * Merchandise.COUNT, Merchandise.COUNT);
			reader.read(input, sellPrices, i * Merchandise.COUNT, Merchandise.COUNT);
		}
	}
}
