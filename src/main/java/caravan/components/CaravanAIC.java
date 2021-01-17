package caravan.components;

import caravan.util.CaravanComponent;
import caravan.util.EnumSerializer;
import caravan.world.Merchandise;
import com.darkyen.retinazer.Mapper;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Caravan Artificial Intelligence.
 */
@CaravanComponent.Serialized(name = "CaravanAI", version = 1)
public final class CaravanAIC extends CaravanComponent {

	public int previousTown = -1;
	public int targetTown = -1;
	public @Nullable Merchandise tradedMerchandise = null;

	public @NotNull Activity currentActivity = Activity.NONE;

	private static @NotNull String getTownName(@NotNull Mapper<TownC> townMapper, int town) {
		final TownC townC = town < 0 ? null : townMapper.getOrNull(town);
		if (townC == null) {
			return "somewhere";
		}
		return townC.name;
	}

	public @NotNull String getActivityDescription(@NotNull Mapper<TownC> townMapper) {
		final String prevTownName = getTownName(townMapper, previousTown);
		final String nextTownName = getTownName(townMapper, targetTown);
		switch (currentActivity) {
			case NONE:
				return "We were just established and are trying to figure out what to do next.";
			case TRADING_SINGLE_GOOD:
				final String merchName = tradedMerchandise == null ? "something" : tradedMerchandise.name;
				return "We are trading "+merchName+" from "+prevTownName+" to "+nextTownName+".";
			case LOOKING_FOR_A_GOOD_DEAL:
				return "We are travelling from "+prevTownName+" to "+nextTownName+" in hopes of finding a good deal.";
			case EXPLORING:
				return "We are travelling from "+prevTownName+" to "+nextTownName+" to explore the local prices.";
		}
		return "We are very confused.";
	}

	@Override
	public void reset() {
		previousTown = -1;
		targetTown = -1;
		tradedMerchandise = null;
		currentActivity = Activity.NONE;
	}

	@Override
	public void save(@NotNull Output output) {
		output.writeInt(previousTown);
		output.writeInt(targetTown);
		Merchandise.SERIALIZER.write(output).writeValueOrNull(output, tradedMerchandise);
		Activity.SERIALIZER.write(output).writeValue(output, currentActivity);
	}

	@Override
	public void load(@NotNull Input input, int version) {
		previousTown = input.readInt();
		targetTown = input.readInt();
		tradedMerchandise = Merchandise.SERIALIZER.read(input).readValueOrNull(input, null);
		currentActivity = Activity.SERIALIZER.read(input).readValue(input, Activity.NONE);
	}

	public enum Activity {
		NONE,
		TRADING_SINGLE_GOOD,
		LOOKING_FOR_A_GOOD_DEAL,
		EXPLORING;

		public static final EnumSerializer<Activity> SERIALIZER = new EnumSerializer<>(0, new Activity[] {
				NONE, TRADING_SINGLE_GOOD, LOOKING_FOR_A_GOOD_DEAL, EXPLORING
		});
	}
}
