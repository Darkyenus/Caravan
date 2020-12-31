package caravan.world;

/**
 * Various world properties and ways to generate them.
 */
public final class WorldProperty {

	/** Temperature of an area */
	public enum Temperature {
		/** Temperature range of sub 10C */
		COLD,
		/** Temperature is around 10-25C */
		TEMPERATE,
		/** Temperatures mostly above 25C */
		HOT
	}

	/** How much rain falls on an area */
	public enum Precipitation {
		/** Practically zero rainfall. This is a desert landscape, where nothing can grow.
		 * Examples: Sahara, Gobi, etc. */
		SUPER_ARID,
		/** Some rainfall allows limited farming, but nothing extensive.
		 * Examples: Central Spain */
		ARID,
		/** "Normal" amount of rain, allows for heavy farming.
		 * Examples: Central and western europe */
		HUMID,
		/** Very heavy rainfall.
		 * Examples: Bangladesh, Scotland, rainforests of the world */
		SUPER_HUMID
	}




}
