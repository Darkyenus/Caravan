package caravan.util;

import com.badlogic.gdx.utils.SerializationException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Serializer for enums that supports enum versions.
 * Create one instance per enum type.
 */
public final class EnumSerializer<E extends Enum<E>> {

	private final int version;
	private final @Nullable E @NotNull [] @NotNull[] versionValues;
	private final Writer writer;
	private final Reader[] readers;

	/**
	 * @param version current version, start at 0 and increment every time enum changes
	 * @param versionValues array of arrays, array at index N corresponds to the value of enums
	 *  {@code values()} when that was the current version. Entries for all possible versions must be present,
	 *  from 0 up to and including current version. Supply the values explicitly, not through a {@code values()} call.
	 *  Deleted enum constants in old versions may be null.
	 */
	@SafeVarargs
	public EnumSerializer(int version, @Nullable E @NotNull [] @NotNull ... versionValues) {
		assert version >= 0 && version + 1 == versionValues.length;
		this.version = version;
		this.versionValues = versionValues;
		final @Nullable E[] reportedEnumValues = versionValues[version];
		final int currentValueCount = reportedEnumValues.length;
		writer = new Writer(currentValueCount);
		readers = new Reader[version + 1];

		final E firstValue = versionValues[version][0];
		assert firstValue != null;
		@SuppressWarnings("unchecked")
		final E[] currentEnumValues = ((Class<E>) firstValue.getClass()).getEnumConstants();
		assert currentEnumValues.length == reportedEnumValues.length;
		for (int i = 0; i < currentValueCount; i++) {
			assert currentEnumValues[i] == reportedEnumValues[i];
		}
	}

	public @NotNull E @NotNull [] currentValues() {
		//noinspection NullableProblems
		return versionValues[version];
	}

	/** Write a version header and return an object, through which data can be serialized. */
	public @NotNull Writer write(@NotNull Output output) {
		output.writeVarInt(version, true);
		return writer;
	}

	/** Read a version header and return an object, through which previously serialized data can be read. */
	public @NotNull Reader read(@NotNull Input input) {
		final int version = input.readVarInt(true);
		if (version < 0 || version > this.version) {
			throw new SerializationException("Unknown enum version: "+version);
		}

		Reader reader = readers[version];
		if (reader == null) {
			reader = readers[version] = new Reader(versionValues[this.version].length, versionValues[version]);
		}

		return reader;
	}

	public static final class Writer {

		private final int enumConstantCount;

		private Writer(int enumConstantCount) {
			this.enumConstantCount = enumConstantCount;
		}

		/** Calls {@link #write(Output, short[], int, int)} for the whole array. */
		public void write(@NotNull Output output, short @NotNull[] data) {
			write(output, data, 0, data.length);
		}

		/**
		 * Treats the input {@code data} array ({@code length} values starting at {@code offset})
		 * as a map (dictionary) keyed with enum ordinals.
		 */
		public void write(@NotNull Output output, short @NotNull[] data, int offset, int length) {
			assert length == enumConstantCount;
			output.writeShorts(data, offset, enumConstantCount);
		}

		public void write(@NotNull Output output, boolean @NotNull[] data) {
			write(output, data, 0, data.length);
		}

		public void write(@NotNull Output output, boolean @NotNull[] data, int offset, int length) {
			assert length == enumConstantCount;
			int inIndex = offset;
			int remainingBits = enumConstantCount;
			while (remainingBits > 0) {
				byte bb = 0;
				final int bits = Math.min(remainingBits, 8);
				for (int bitIndex = 0; bitIndex < bits; bitIndex++) {
					if (data[inIndex++]) {
						bb |= 1 << bitIndex;
					}
				}
				remainingBits -= bits;
				output.writeByte(bb);
			}
		}
	}

	public static final class Reader {

		private final int enumConstantCount;
		private final @Nullable Enum<?> @NotNull [] values;

		private Reader(int enumConstantCount, @Nullable Enum<?> @NotNull [] values) {
			this.enumConstantCount = enumConstantCount;
			this.values = values;
		}

		/**
		 * Treats the input {@code data} array ({@code length} values starting at {@code offset})
		 * as a map (dictionary) keyed with enum ordinals.
		 */
		public void read(@NotNull Input input, short @NotNull[] out, int offset, int length) {
			assert length == enumConstantCount;

			for (@Nullable Enum<?> e : values) {
				final short data = input.readShort();
				if (e == null) {
					// There is no longer any place for this data, throw it away.
					continue;
				}
				out[offset + e.ordinal()] = data;
			}
		}

		/** Calls {@link #read(Input, short[], int, int)} for the whole array. */
		public void read(@NotNull Input input, short @NotNull[] out) {
			read(input, out, 0, out.length);
		}

		public void read(@NotNull Input input, boolean @NotNull[] out) {
			read(input, out, 0, out.length);
		}

		public void read(@NotNull Input input, boolean @NotNull[] out, int offset, int length) {
			assert length == enumConstantCount;

			int outIndex = offset;
			int remainingBits = enumConstantCount;
			while (remainingBits > 0) {
				final byte bb = input.readByte();
				final int bits = Math.min(remainingBits, 8);
				for (int bitIndex = 0; bitIndex < bits; bitIndex++) {
					out[outIndex++] = (bb & (1 << bitIndex)) != 0;
				}
				remainingBits -= bits;
			}
		}
	}
}
