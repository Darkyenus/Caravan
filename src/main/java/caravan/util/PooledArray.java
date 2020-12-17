package caravan.util;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Pool;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Arrays;

/**
 * A dynamically sized array, whose elements are all pooled.
 */
public final class PooledArray<T> {

	private final Class<T> elementType;
	private final Constructor<T> constructor;

	public T[] items;
	public int size;

	public PooledArray(Class<T> elementType) {
		this.elementType = elementType;
		//noinspection unchecked
		items = (T[]) Array.newInstance(elementType, 16);

		try {
			constructor = elementType.getConstructor();
			constructor.setAccessible(false);
		} catch (Exception e) {
			throw new IllegalArgumentException("Can't access constructor", e);
		}
	}

	public void ensureCapacity(int extraElements) {
		if (size + extraElements <= items.length) {
			return;
		}

		int newCapacity = MathUtils.nextPowerOfTwo(size + extraElements);
		assert newCapacity > items.length;

		items = Arrays.copyOf(items, newCapacity);
	}

	public T add() {
		ensureCapacity(1);
		T item = items[size];
		if (item == null) {
			try {
				items[size] = item = constructor.newInstance();
			} catch (Exception e) {
				throw new RuntimeException("Failed to instantiate new element", e);
			}
		} else if (item instanceof Pool.Poolable) {
			((Pool.Poolable) item).reset();
		}
		size++;
		return item;
	}

	public T get(int index) {
		assert index >= 0 && index < size;
		return items[index];
	}

	public int size() {
		return size;
	}

	public void clear() {
		size = 0;
	}
}
