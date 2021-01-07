package caravan.world;

/**
 * Types of merchandise.
 */
public enum Merchandise {

	RAW_PLANT_FIBER("Raw plant fiber", true, Category.TEXTILES),
	RAW_ANIMAL_FIBER("Raw animal fiber", true, Category.TEXTILES),
	CLOTH("Cloth", true, Category.TEXTILES),
	CLOTH_LUXURY("Luxury cloth", true, Category.TEXTILES),
	CLOTHING("Clothing", true, Category.TEXTILES),
	CLOTHING_LUXURY("Luxury clothing", true, Category.TEXTILES),

	MEAT_FRESH("Fresh meat", false, Category.FOOD), // red, poultry or fish
	MEAT_PRESERVED("Preserved meat", true, Category.FOOD), // smoked or salted meat
	GRAIN("Grain", true, Category.FOOD),
	BAKED_GOODS("Baked goods", true, Category.FOOD),
	HONEY("Honey", true, Category.FOOD),
	SUGAR("Sugar", true, Category.FOOD),
	WATER_FRESH("Fresh water", true, Category.FOOD),
	FRUIT_FRESH("Fresh fruit", false, Category.FOOD),
	FRUIT_DRIED("Dried fruit", true, Category.FOOD),
	FRUIT_JAM("Fruit jam", true, Category.FOOD),
	VEGETABLES_FRESH("Fresh vegetables", false, Category.FOOD),
	VEGETABLES_PICKLED("Pickled vegetables", true, Category.FOOD),
	SALT("Salt", true, Category.FOOD),

	BAKED_GOODS_LUXURY("Luxury baked goods", true, Category.LUXURIES),
	MEAT_LUXURY("Luxury meat", true, Category.LUXURIES), // preserved luxury meats (dried meat, sausages, pates...)
	BEER("Beer", true, Category.LUXURIES),
	WINE("Wine", true, Category.LUXURIES),
	LIQUOR("Liquor", true, Category.LUXURIES),
	MEAD("Mead", true, Category.LUXURIES),
	SPICES("Spices", true, Category.LUXURIES),
	BOOK("Books", true, Category.LUXURIES),
	PERFUME("Perfume", true, Category.LUXURIES),
	JEWELRY("Jewelry", true, Category.LUXURIES),

	METAL_RARE_ORE("Rare metal ore", true, Category.TRADE_GOODS), // gold and silver ore
	METAL_RARE_INGOT("Rare metal ingots", true, Category.TRADE_GOODS), // gold and silver ingots
	METAL_ORE("Metal ore", true, Category.TRADE_GOODS), // Iron, copper, tin or lead ore
	METAL_INGOT("Metal ingots", true, Category.TRADE_GOODS), // Iron, copper, tin or lead ingot
	COAL("Coal", true, Category.TRADE_GOODS),
	JEWELS("Jewels", true, Category.TRADE_GOODS),
	ARMOR_AND_WEAPONS("Armor and weapons", true, Category.TRADE_GOODS),
	TOOLS("Tools", true, Category.TRADE_GOODS),
	WOOD_LOG("Wood logs", true, Category.TRADE_GOODS),
	WOOD_FUEL("Fuel wood", true, Category.TRADE_GOODS),
	WOOD_LUMBER("Lumber", true, Category.TRADE_GOODS),

	// Cut stuff (for now)

	//STONE("Masonry stone"),
	//LIMESTONE("Limestone"),
	//EGGS("Eggs"),
	//DAIRY_MILK("Milk"),
	//DAIRY_CHEESE("Cheese"),
	//FOOD_OIL_FAT("Cooking oil"),
	;

	public final String name;
	public final boolean tradeable;
	public final Category category;

	public static final Merchandise[] VALUES = values();

	Merchandise(String name, boolean tradeable, Category category) {
		this.name = name;
		this.tradeable = tradeable;
		this.category = category;
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
		TEXTILES("Textiles"),
		FOOD("Food"),
		LUXURIES("Luxuries"),
		TRADE_GOODS("Trade Goods"),
		;

		public final String name;

		public static final Category[] VALUES = values();

		Category(String name) {
			this.name = name;
		}
	}
}
