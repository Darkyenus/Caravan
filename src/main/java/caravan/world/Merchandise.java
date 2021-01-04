package caravan.world;

/**
 * Types of merchandise.
 */
public enum Merchandise {
	RAW_PLANT_FIBER("Raw plant fiber"),
	RAW_ANIMAL_FIBER("Raw animal fiber"),
	CLOTH("Cloth"),
	CLOTH_LUXURY("Luxury cloth"),
	CLOTHING("Clothing"),
	CLOTHING_LUXURY("Luxury clothing"),
	/** red, poultry or fish */
	MEAT_FRESH("Fresh meat"),
	/** smoked or salted meat */
	MEAT_PRESERVED("Preserved meat"),
	/** preserved luxury meats (dried meat, sausages, pates...) */
	MEAT_LUXURY("Luxury meat"),
	GRAIN("Grain"),
	BAKED_GOODS("Baked goods"),
	BAKED_GOODS_LUXURY("Luxury baked goods"),
	//EGGS("Eggs"),
	//DAIRY_MILK("Milk"),
	//DAIRY_CHEESE("Cheese"),
	HONEY("Honey"),
	SUGAR("Sugar"),
	//FOOD_OIL_FAT("Cooking oil"),
	BEER("Beer"),
	WINE("Wine"),
	LIQUOR("Liquor"),
	MEAD("Mead"),
	WATER_FRESH("Fresh water"),
	FRUIT_FRESH("Fresh fruit"),
	FRUIT_DRIED("Dried fruit"),
	FRUIT_JAM("Fruit jam"),
	VEGETABLES_FRESH("Fresh vegetables"),
	VEGETABLES_PICKLED("Pickled vegetables"),
	SPICES("Spices"),
	SALT("Salt"),
	BOOK("Books"),
	PERFUME("Perfume"),
	/** gold and silver ore */
	METAL_RARE_ORE("Rare metal ore"),
	/** gold and silver ingots */
	METAL_RARE_INGOT("Rare metal ingots"),
	/** Iron, copper, tin or lead ore */
	METAL_ORE("Metal ore"),
	/** Iron, copper, tin or lead ingot */
	METAL_INGOT("Metal ingots"),
	COAL("Coal"),
	JEWELS("Jewels"),
	JEWELRY("Jewelry"),
	ARMOR_AND_WEAPONS("Armor and weapons"),
	TOOLS("Tools"),
	WOOD_LOG("Wood logs"),
	WOOD_FUEL("Fuel wood"),
	WOOD_LUMBER("Lumber"),
	//STONE("Masonry stone"),
	//LIMESTONE("Limestone"),
	;

	public final String name;
	public final float price;

	public static final Merchandise[] VALUES = values();

	Merchandise(String name) {
		this.name = name;
		this.price = this.name().contains("LUXURY") ? 50 : 10;
	}

	/** Stuff that is considered food for internal town consumption, including luxuries. */
	public static final Merchandise[] FOOD = new Merchandise[] {
			MEAT_FRESH,
			MEAT_PRESERVED,
			MEAT_LUXURY,
			BAKED_GOODS,
			BAKED_GOODS_LUXURY,
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

	/** Demand of these foods grows when the town is wealthy. */
	public static final Merchandise[] FOOD_LUXURY = new Merchandise[] {
			MEAT_LUXURY,
			BAKED_GOODS_LUXURY,
			FRUIT_FRESH,
			FRUIT_DRIED,
			FRUIT_JAM,
			VEGETABLES_FRESH,
			VEGETABLES_PICKLED,
			SALT,
			SPICES,
			HONEY,
			SUGAR,
			BEER,
			WINE,
			LIQUOR,
			MEAD
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
