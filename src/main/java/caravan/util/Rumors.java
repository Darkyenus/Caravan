package caravan.util;

import caravan.components.TownC;
import caravan.world.Merchandise;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Pool;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * List of saucy rumors.
 */
public final class Rumors implements Pool.Poolable {

	public int maxSize;
	public final PooledArray<Rumor> rumors = new PooledArray<>(Rumor::new);

	public Rumors(int maxSize) {
		this.maxSize = maxSize;
	}

	public void addPriceRumor(boolean buyPrice, int townEntity, @NotNull Merchandise merch, int price, int today) {
		final RumorType type = buyPrice ? RumorType.BUY_PRICE : RumorType.SELL_PRICE;

		// First check whether we can update an existing rumor
		for (int i = 0; i < rumors.size; i++) {
			final Rumor rumor = rumors.get(i);
			if (rumor.type == type && rumor.aboutTownEntity == townEntity && rumor.aboutMerchandise == merch) {
				rumor.aboutPrice = Util.toShortClampUnsigned(price);
				rumor.day = today;
				update(today);
				return;
			}
		}

		// Nope, create a new rumor
		rumors.add().set(type, today, townEntity, merch, price);
		update(today);
	}

	public void addRandomPriceRumors(@NotNull PriceMemory caravanMemory, @NotNull TownC town, int thisTownEntity, int today) {
		final int offset = MathUtils.random.nextInt(Merchandise.COUNT);
		int count = MathUtils.random(5, 10);
		final PriceList townPrices = town.prices;

		Merchandise bestBuyPriceMerch = null;
		int bestBuyPriceDifference = 0;
		int bestBuyPrice = 4;
		int bestBuyPriceTown = -1;

		Merchandise bestSellPriceMerch = null;
		int bestSellPriceDifference = 0;
		int bestSellPrice = 4;
		int bestSellPriceTown = -1;

		for (int i = 0; i < count; i++) {
			final Merchandise m = Merchandise.VALUES[(offset + i) % Merchandise.COUNT];
			if (!m.tradeable) {
				count++;
				continue;
			}

			final int localBuyPrice = townPrices.buyPrice(m);
			final int localSellPrice = town.realSellPrice(m);

			for (int ms = 0; ms < caravanMemory.capacity(); ms++) {
				if (!caravanMemory.isMemorySlotValid(ms, today - 14, thisTownEntity)) {
					continue;
				}
				final int remoteBuyPrice = caravanMemory.buyPrice(ms, m);
				final int remoteSellPrice = caravanMemory.sellPrice(ms, m);

				final int remoteToHereBenefit = localSellPrice - remoteBuyPrice;
				final int hereToRemoteBenefit = remoteSellPrice - localBuyPrice;

				if (remoteToHereBenefit > bestBuyPriceDifference) {
					bestBuyPriceMerch = m;
					bestBuyPriceDifference = remoteToHereBenefit;
					bestBuyPrice = remoteBuyPrice;
					bestBuyPriceTown = caravanMemory.townEntity(ms);
				}

				if (hereToRemoteBenefit > bestSellPriceDifference) {
					bestSellPriceMerch = m;
					bestSellPriceDifference = hereToRemoteBenefit;
					bestSellPrice = remoteSellPrice;
					bestSellPriceTown = caravanMemory.townEntity(ms);
				}
			}
		}

		if (bestBuyPriceMerch != null) {
			addPriceRumor(true, bestBuyPriceTown, bestBuyPriceMerch, bestBuyPrice, today);
		}
		if (bestSellPriceMerch != null) {
			addPriceRumor(false, bestSellPriceTown, bestSellPriceMerch, bestSellPrice, today);
		}
	}

	/** Sort, cleanup and remove rumors that are too old. */
	public void update(int today) {
		rumors.sort();
		rumors.size = Math.min(rumors.size, maxSize);
		while (rumors.size > 0 && (rumors.get(rumors.size - 1).day < today - 30 || (rumors.get(rumors.size - 1).day < today - 20 && MathUtils.randomBoolean()))) {
			rumors.size--;
		}
	}

	public void save(@NotNull Output output) {
		output.writeVarInt(rumors.size, true);
		if (rumors.size <= 0) {
			return;
		}

		final EnumSerializer.Writer<RumorType> rumorWriter = RumorType.SERIALIZER.write(output);
		final EnumSerializer.Writer<Merchandise> merchWriter = Merchandise.SERIALIZER.write(output);
		for (int i = 0; i < rumors.size; i++) {
			final Rumor rumor = rumors.get(i);
			rumorWriter.writeValue(output, rumor.type);
			output.writeInt(rumor.day);
			output.writeInt(rumor.aboutTownEntity);
			merchWriter.writeValueOrNull(output, rumor.aboutMerchandise);
			output.writeShort(rumor.aboutPrice);
		}
	}

	public void load(@NotNull Input input) {
		rumors.clear();
		final int size = input.readVarInt(true);
		if (size == 0) {
			return;
		}
		rumors.ensureCapacity(size);

		final EnumSerializer.Reader<RumorType> rumorReader = RumorType.SERIALIZER.read(input);
		final EnumSerializer.Reader<Merchandise> merchReader = Merchandise.SERIALIZER.read(input);
		for (int i = 0; i < size; i++) {
			final Rumor rumor = rumors.add();
			rumor.type = rumorReader.readValue(input, RumorType.THING_EXISTS);
			rumor.day = input.readInt();
			rumor.aboutTownEntity = input.readInt();
			rumor.aboutMerchandise = merchReader.readValueOrNull(input, null);
			rumor.aboutPrice = input.readShort();
		}
	}

	@Override
	public void reset() {
		rumors.clear();
	}

	public enum RumorType {
		/** Dummy rumor type used for serialization errors and stupid rumors. */
		THING_EXISTS,
		BUY_PRICE,
		SELL_PRICE;

		public static final EnumSerializer<RumorType> SERIALIZER = new EnumSerializer<>(0, new RumorType[] {
				THING_EXISTS, BUY_PRICE, SELL_PRICE
		});
	}

	public static final class Rumor implements Comparable<Rumor> {
		public @NotNull RumorType type = RumorType.THING_EXISTS;
		public int day;

		public int aboutTownEntity;
		public @Nullable Merchandise aboutMerchandise;
		public short aboutPrice;

		public void set(@NotNull RumorType type, int day, int aboutTownEntity, @Nullable Merchandise aboutMerchandise, int aboutPrice) {
			this.type = type;
			this.day = day;
			this.aboutTownEntity = aboutTownEntity;
			this.aboutMerchandise = aboutMerchandise;
			this.aboutPrice = Util.toShortClampUnsigned(aboutPrice);
		}

		@Override
		public int compareTo(@NotNull Rumors.Rumor o) {
			// Newest first
			return Integer.compare(o.day, this.day);
		}
	}

}
