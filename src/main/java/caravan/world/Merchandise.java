package caravan.world;

/**
 * Types of merchandise.
 */
public enum Merchandise {
	RAW_PLANT_FIBER("Raw plant fiber", true),
	RAW_ANIMAL_FIBER("Raw animal fiber", true),
	CLOTH("Cloth", true),
	CLOTH_LUXURY("Luxury cloth", true),
	CLOTHING("Clothing", true),
	CLOTHING_LUXURY("Luxury clothing", true),
	/** red, poultry or fish */
	MEAT_FRESH("Fresh meat", false),
	/** smoked or salted meat */
	MEAT_PRESERVED("Preserved meat", true),
	/** preserved luxury meats (dried meat, sausages, pates...) */
	MEAT_LUXURY("Luxury meat", true),
	GRAIN("Grain", true),
	BAKED_GOODS("Baked goods", true),
	BAKED_GOODS_LUXURY("Luxury baked goods", true),
	//EGGS("Eggs"),
	//DAIRY_MILK("Milk"),
	//DAIRY_CHEESE("Cheese"),
	HONEY("Honey", true),
	SUGAR("Sugar", true),
	//FOOD_OIL_FAT("Cooking oil"),
	BEER("Beer", true),
	WINE("Wine", true),
	LIQUOR("Liquor", true),
	MEAD("Mead", true),
	WATER_FRESH("Fresh water", true),
	FRUIT_FRESH("Fresh fruit", false),
	FRUIT_DRIED("Dried fruit", true),
	FRUIT_JAM("Fruit jam", true),
	VEGETABLES_FRESH("Fresh vegetables", false),
	VEGETABLES_PICKLED("Pickled vegetables", true),
	SPICES("Spices", true),
	SALT("Salt", true),
	BOOK("Books", true),
	PERFUME("Perfume", true),
	/** gold and silver ore */
	METAL_RARE_ORE("Rare metal ore", true),
	/** gold and silver ingots */
	METAL_RARE_INGOT("Rare metal ingots", true),
	/** Iron, copper, tin or lead ore */
	METAL_ORE("Metal ore", true),
	/** Iron, copper, tin or lead ingot */
	METAL_INGOT("Metal ingots", true),
	COAL("Coal", true),
	JEWELS("Jewels", true),
	JEWELRY("Jewelry", true),
	ARMOR_AND_WEAPONS("Armor and weapons", true),
	TOOLS("Tools", true),
	WOOD_LOG("Wood logs", true),
	WOOD_FUEL("Fuel wood", true),
	WOOD_LUMBER("Lumber", true),
	//STONE("Masonry stone"),
	//LIMESTONE("Limestone"),
	;

	public final String name;
	public final boolean tradeable;

	public static final Merchandise[] VALUES = values();

	Merchandise(String name, boolean tradeable) {
		this.name = name;
		this.tradeable = tradeable;

	}

	/** Stuff that is considered food for internal town consumption, including luxuries. */
	public static final Merchandise[] FOOD = new Merchandise[] {
			MEAT_FRESH,
			MEAT_PRESERVED,
			BAKED_GOODS,
			FRUIT_FRESH,
			FRUIT_DRIED,
			FRUIT_JAM,
			VEGETABLES_FRESH,
			VEGETABLES_PICKLED,
			SALT
	};

	/** required only when the city does not have its own source */
	public static final Merchandise[] FRESH_WATER = new Merchandise[] {
			WATER_FRESH
	};

	/** People also buy these things, always. */
	public static final Merchandise[] COMMON_GOODS = new Merchandise[] {
			CLOTHING,
			TOOLS,
	};

	/** Demand of these things grows only when the city is rich. */
	public static final Merchandise[] LUXURY_GOODS = new Merchandise[] {
			CLOTHING_LUXURY,
			BOOK,
			PERFUME,
			JEWELRY,
			MEAT_LUXURY,
			BAKED_GOODS_LUXURY,
			FRUIT_DRIED,
			FRUIT_JAM,
			VEGETABLES_PICKLED,
			BEER,
			WINE,
			LIQUOR,
			MEAD
	};

	/** Towns that grow require these materials. */
	public static final Merchandise[] BUILDING_MATERIALS = new Merchandise[] {
			TOOLS,
			WOOD_LOG,
			WOOD_LUMBER
	};

	/*
	Special goods:
	ARMOR_AND_WEAPONS - demand depends on the local politics
	 */

	/** Merchandise categories and subcategories. */
	public enum Category {
		TEXTILE(RAW_ANIMAL_FIBER, RAW_PLANT_FIBER, CLOTH, CLOTH_LUXURY, CLOTHING, CLOTHING_LUXURY),
		FOOD(MEAT_FRESH, MEAT_PRESERVED, MEAT_LUXURY, GRAIN, BAKED_GOODS, /*EGGS, DAIRY_MILK, DAIRY_CHEESE,*/ HONEY, SUGAR, /*FOOD_OIL_FAT,*/ WATER_FRESH, SPICES, SALT),
		FRUIT_AND_VEGETABLES(FRUIT_FRESH, FRUIT_DRIED, FRUIT_JAM, VEGETABLES_FRESH, VEGETABLES_PICKLED),
		ALCOHOL(BEER, WINE, LIQUOR, MEAD),
		MINING(METAL_RARE_ORE, METAL_RARE_INGOT, METAL_ORE, METAL_INGOT, COAL, JEWELS),
		WOOD(WOOD_LOG, WOOD_FUEL, WOOD_LUMBER),
		//STONE(Merchandise.STONE, LIMESTONE),
		OTHER(ARMOR_AND_WEAPONS, TOOLS, BOOK, JEWELRY, PERFUME),
		;

		public final Merchandise[] merch;

		Category(Merchandise...merch) {
			this.merch = merch;
		}

		public static final Category[] VALUES = values();
	}
}
