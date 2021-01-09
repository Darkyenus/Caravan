package caravan.services;

import com.badlogic.gdx.utils.Array;
import com.darkyen.retinazer.util.Bag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Iterator;

/**
 * Instances of this class have an ID and are findable through that ID.
 */
public abstract class Id<This extends Id<This>> {

	public final short id;

	@SuppressWarnings("unchecked")
	protected Id(int id, @NotNull Registry<This> registry) {
		final short sId = (short) id;
		if (id != sId) throw new AssertionError("Tile ID is too large!");
		this.id = sId;
		registry.register((This) this, sId);
	}

	public static final class Registry<IdCarrier extends Id<IdCarrier>> implements Iterable<IdCarrier> {

		private final Bag<IdCarrier> registered = new Bag<>();
		private final Array<IdCarrier> registeredDense = new Array<>(true, 64);
		private boolean registeredDenseSorted = true;

		private void register(@NotNull IdCarrier object, short id) {
			final IdCarrier old = registered.get(id);
			if (old != null) {
				throw new AssertionError("ID "+id+" can't be given to "+object+", because it is already used by "+old);
			}
			registered.set(id, object);
			registeredDense.add(object);
			registeredDenseSorted = false;
		}

		@Nullable
		public IdCarrier get(short id) {
			if (id < 0) {
				return null;
			}
			return registered.get(id);
		}

		@NotNull
		public IdCarrier getOrDefault(short id, @NotNull IdCarrier defaultValue) {
			final IdCarrier result = get(id);
			if (result != null) {
				return result;
			}
			return defaultValue;
		}

		@NotNull
		public IdCarrier getOrDefault(short id) {
			final Array<IdCarrier> array = array();
			if (array.size == 0) {
				throw new IllegalStateException("Can't return anything - nothing was registered yet");
			}
			return getOrDefault(id, array.get(0));
		}

		public int count() {
			return registeredDense.size;
		}

		/** Sort the registered items by id and return the one at given {@code index}.
		 * Note that the index has no relation to the actual ID. */
		@NotNull
		public IdCarrier getDense(int index) {
			return array().get(index);
		}

		private Array<IdCarrier> array() {
			final Array<IdCarrier> dense = this.registeredDense;
			if (!registeredDenseSorted) {
				registeredDenseSorted = true;
				dense.sort(ID_COMPARATOR);
			}
			return dense;
		}

		@NotNull
		@Override
		public Iterator<IdCarrier> iterator() {
			return array().iterator();
		}

		private static final Comparator<Id<?>> ID_COMPARATOR = Comparator.comparingInt(o -> o.id);
	}

}
