package caravan.world;

/**
 * Types of merchandise.
 */
public enum Merchandise {
	RAW_WOOL("Raw wool"),
	RAW_COTTON("Raw cotton"),
	FLAX("Flax"),
	ANIMAL_SKINS("Animal skins"),
	LINEN("Linen cloth"),
	COTTON("Cotton cloth"),
	SILK("Silk cloth"),
	WOOL("Wool cloth"),
	CLOTHING_CHEAP("Cheap clothing"),
	CLOTHING_REGULAR("Regular clothing"),
	CLOTHING_LUXURY("Luxury clothing"),
	/** red, poultry or fish */
	MEAT_FRESH("Fresh meat"),
	/** smoked or salted meat */
	MEAT_PRESERVED("Preserved meat"),
	/** preserved luxury meats (dried meat, sausages, pates...) */
	MEAT_LUXURY("Luxury meat"),
	GRAIN("Grain"),
	BAKED_GOODS("Baked goods"),
	EGGS("Eggs"),
	DAIRY_MILK("Milk"),
	DAIRY_CHEESE("Cheese"),
	HONEY("Honey"),
	SUGAR("Sugar"),
	FOOD_OIL_FAT("Cooking oil"),
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
	STONE("Masonry stone"),
	LIMESTONE("Limestone"),
	;

	public final String name;

	public static final Merchandise[] VALUES = values();

	Merchandise(String name) {
		this.name = name;
	}

	/** Merchandise categories and subcategories. */
	public enum Category {
		TEXTILE(RAW_WOOL, RAW_COTTON, FLAX, ANIMAL_SKINS, LINEN, COTTON, SILK, WOOL),
		TEXTILE_RAW(RAW_WOOL, RAW_COTTON, FLAX, ANIMAL_SKINS),
		TEXTILE_CHEAP(LINEN, COTTON),
		TEXTILE_LUXURY(SILK, WOOL),
		CLOTHING(CLOTHING_CHEAP, CLOTHING_REGULAR, CLOTHING_LUXURY),
		FOOD(MEAT_FRESH, MEAT_PRESERVED, MEAT_LUXURY, GRAIN, BAKED_GOODS, EGGS, DAIRY_MILK, DAIRY_CHEESE, HONEY, SUGAR, FOOD_OIL_FAT, BEER, WINE, LIQUOR, MEAD, WATER_FRESH, FRUIT_FRESH, FRUIT_DRIED,
				FRUIT_JAM, VEGETABLES_FRESH, VEGETABLES_PICKLED, SPICES, SALT),
		FOOD_ALCOHOLIC_DRINK(BEER, WINE, LIQUOR, MEAD),
		FOOD_FRUIT(FRUIT_FRESH, FRUIT_DRIED, FRUIT_JAM),
		FOOD_VEGETABLE(VEGETABLES_FRESH, VEGETABLES_PICKLED),
		MINING(METAL_RARE_ORE, METAL_RARE_INGOT, METAL_ORE, METAL_INGOT, COAL, JEWELS),
		WOOD(WOOD_LOG, WOOD_FUEL, WOOD_LUMBER),
		STONE(Merchandise.STONE, LIMESTONE),
		OTHER(ARMOR_AND_WEAPONS, TOOLS),
		OTHER_LUXURY(BOOK, JEWELRY, PERFUME)
		;

		public final Merchandise[] merch;

		Category(Merchandise...merch) {
			this.merch = merch;
		}

		public static final Category[] VALUES = values();
	}
}
