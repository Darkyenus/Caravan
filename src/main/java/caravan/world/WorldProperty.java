package caravan.world;

/**
 * Various world properties and ways to generate them.
 */
public final class WorldProperty {

	/** Temperature of an area */
	public enum Temperature {
		/** Temperature range of sub 10C */
		COLD(0.3f),
		/** Temperature is around 10-25C */
		TEMPERATE(0.7f),
		/** Temperatures mostly above 25C */
		HOT(1);

		public final float value;

		Temperature(float value) {
			this.value = value;
		}
	}

	/** How much rain falls on an area */
	public enum Precipitation {
		/** Practically zero rainfall. This is a desert landscape, where nothing can grow.
		 * Examples: Sahara, Gobi, etc. */
		SUPER_ARID(0),
		/** Some rainfall allows limited farming, but nothing extensive.
		 * Examples: Central Spain */
		ARID(0.3f),
		/** "Normal" amount of rain, allows for heavy farming.
		 * Examples: Central and western europe */
		HUMID(0.7f),
		/** Very heavy rainfall.
		 * Examples: Bangladesh, Scotland, rainforests of the world */
		SUPER_HUMID(1);

		public final float value;

		public static final Precipitation[] VALUES = values();

		Precipitation(float value) {
			this.value = value;
		}
	}




}
