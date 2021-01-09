package caravan.services;

import com.darkyen.retinazer.util.Bag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;

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
		@Nullable
		private IdCarrier defaultValue = null;
		private short maxId = -1;

		private void register(@NotNull IdCarrier object, short id) {
			final IdCarrier old = registered.get(id);
			if (old != null) {
				throw new AssertionError("ID "+id+" can't be given to "+object+", because it is already used by "+old);
			}
			registered.set(id, object);
			if (defaultValue == null) {
				defaultValue = object;
			}
			maxId = (short) Math.max(maxId, id);
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
			final IdCarrier defaultValue = this.defaultValue;
			if (defaultValue == null) {
				throw new IllegalStateException("Can't return anything - nothing was registered yet");
			}
			return getOrDefault(id, defaultValue);
		}

		@NotNull
		@Override
		public Iterator<IdCarrier> iterator() {
			return new Iterator<IdCarrier>() {

				int nextId = findNextId(-1);

				short findNextId(int lastId) {
					int id = lastId + 1;
					while (id <= maxId && registered.get(id) == null) {
						id++;
					}
					return (short) id;
				}

				@Override
				public boolean hasNext() {
					return nextId <= maxId;
				}

				@Override
				public IdCarrier next() {
					final IdCarrier result = registered.get(nextId);
					if (result == null) {
						throw new NoSuchElementException();
					}
					nextId = findNextId(nextId);
					return result;
				}
			};
		}
	}

}
