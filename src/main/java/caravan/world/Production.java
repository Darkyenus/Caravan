package caravan.world;

import caravan.components.TownC;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import org.jetbrains.annotations.NotNull;

/** Definition of a production type.
 * Each production assumes 10 people working, if there is less,
 * the inputs and outputs are reduced accordingly. */
public abstract class Production {

	public static final Array<Production> PRODUCTION = new Array<>(true, 100, Production.class);

	/** Name of the production process/industry */
	public final String name;
	public final Merchandise output;

	public final int productionIndex;

	private Production(String name, Merchandise output) {
		this.name = name;
		this.output = output;
		this.productionIndex = PRODUCTION.size;
		PRODUCTION.add(this);
	}

	/** Evaluate what is needed to produce something.
	 * @param environment in this environment
	 * @param resources put what is required into this inventory */
	public abstract float produce(@NotNull TownC environment, @NotNull Inventory resources);

	@Override
	public String toString() {
		return name;
	}

	/*
	Basic economics:

	10 people eat 1 unit of food per day
	10 people produce 1 unit of food per day when the conditions are 25% favorable
					  0.1 food/day on 0% conditions
					  3 food/day on 100% conditions
	 */

	static {
		final boolean[] ALTERNATIVES = new boolean[]{ false, true };

		new Production("Wool and Hide Production", Merchandise.RAW_ANIMAL_FIBER) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				return map(0.05f, 0.5f, 2f, environment.fieldSpace);
			}
		};
		new Production("Plant Fiber Farming", Merchandise.RAW_PLANT_FIBER) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				float land = environment.fieldSpace;
				float water = (float) environment.precipitation.ordinal() / (WorldProperty.Precipitation.VALUES.length - 1);
				return map(0.1f, 1f, 2.5f, mix(land, water));
			}
		};
		new Production("Weaving (plant fiber)", Merchandise.CLOTH) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.RAW_PLANT_FIBER, 5);
				return 4.9f;
			}
		};
		new Production("Weaving (animal fiber)", Merchandise.CLOTH) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.RAW_ANIMAL_FIBER, 5);
				return 4.8f;
			}
		};
		new Production("Luxury Weaving", Merchandise.CLOTH_LUXURY) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.RAW_ANIMAL_FIBER, 3);
				resources.add(Merchandise.RAW_PLANT_FIBER, 3);
				return 4.5f;
			}
		};
		new Production("Clothing Tailoring", Merchandise.CLOTHING) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.CLOTH, 7);
				return 6f;
			}
		};
		new Production("Luxury Clothing Tailoring", Merchandise.CLOTHING_LUXURY) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.CLOTH, 3);
				resources.add(Merchandise.CLOTH_LUXURY, 4);
				return 5.5f;
			}
		};

		new Production("Livestock Farming", Merchandise.MEAT_FRESH) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				// https://www.wolframalpha.com/input/?i=fit+polynomial+%281%2C3%29+%280.25%2C1%29+%280%2C0.1%29
				final float p = environment.fieldSpace;
				return -0.933333f*p*p + 3.83333f*p + 0.1f;
			}
		};
		new Production("Fishing", Merchandise.MEAT_FRESH) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				float units = 0;
				if (environment.hasSaltWater) {
					units += 1.5f;
				}
				if (environment.hasFreshWater) {
					units += 0.7f;
				}
				return units;
			}
		};
		new Production("Meat salting", Merchandise.MEAT_PRESERVED) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.MEAT_FRESH, 40);
				resources.add(Merchandise.SALT, 10);
				return 40;
			}
		};
		new Production("Meat smoking", Merchandise.MEAT_PRESERVED) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.MEAT_FRESH, 40);
				resources.add(Merchandise.WOOD_FUEL, 30);
				return 40;
			}
		};
		new Production("Sausage making", Merchandise.MEAT_LUXURY) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.MEAT_PRESERVED, 40);
				resources.add(Merchandise.SALT, 7);
				resources.add(Merchandise.SPICES, 4);
				return 40;
			}
		};

		new Production("Grain Farming", Merchandise.GRAIN) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				float land = environment.fieldSpace;
				float water = environment.precipitation.value;
				float temperature = environment.temperature.value;
				return map(0.1f, 2f, 6f, mix(land, water, (float) Math.sqrt(temperature)));
			}
		};

		for (boolean coal : ALTERNATIVES) {
			final String fuelName = coal ? "coal" : "wood";
			final Merchandise fuel = coal ? Merchandise.COAL : Merchandise.WOOD_FUEL;

			new Production("Baking (" + fuelName + ")", Merchandise.BAKED_GOODS) {
				@Override
				public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
					resources.add(Merchandise.GRAIN, 50);
					resources.add(Merchandise.WATER_FRESH, 20);
					resources.add(fuel, 10);
					resources.add(Merchandise.SALT, 3);
					return 50;
				}
			};

			for (boolean sugar : ALTERNATIVES) {
				final String sweetenerName = sugar ? "sugar" : "honey";
				final Merchandise sweetener = sugar ? Merchandise.SUGAR : Merchandise.HONEY;

				new Production("Luxury Baking ("+fuelName+", "+sweetenerName+")", Merchandise.BAKED_GOODS_LUXURY) {
					@Override
					public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
						resources.add(Merchandise.GRAIN, 50);
						resources.add(Merchandise.WATER_FRESH, 20);
						resources.add(fuel, 10);
						resources.add(Merchandise.SALT, 3);
						resources.add(Merchandise.SPICES, 3);
						resources.add(sweetener, 6);
						return 50;
					}
				};
			}
		}

		new Production("Beekeeping", Merchandise.HONEY) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				return mix(environment.fieldSpace, environment.woodAbundance) * 5;
			}
		};
		new Production("Sugar cane farming", Merchandise.SUGAR) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				if (environment.temperature != WorldProperty.Temperature.HOT || environment.precipitation.value < WorldProperty.Precipitation.HUMID.value) {
					return 0;
				}
				return map(0, 1, 5, mix(environment.fieldSpace, environment.precipitation.value));
			}
		};

		new Production("Beer brewing", Merchandise.BEER) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.GRAIN, 15);
				resources.add(Merchandise.WATER_FRESH, 20);
				return 20;
			}
		};
		new Production("Wine making", Merchandise.WINE) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.FRUIT_FRESH, 15);
				resources.add(Merchandise.WATER_FRESH, 10);
				return 10;
			}
		};
		for (Merchandise origin : new Merchandise[]{Merchandise.VEGETABLES_FRESH, Merchandise.GRAIN, Merchandise.FRUIT_FRESH}) {
			String originName;
			switch (origin) {
				case VEGETABLES_FRESH:
					originName = "vegetable";
					break;
				case GRAIN:
					originName = "grain";
					break;
				default:
				case FRUIT_FRESH:
					originName = "fruit";
					break;
			}
			new Production("Liquor making ("+originName+")", Merchandise.LIQUOR) {
				@Override
				public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
					resources.add(origin, 15);
					resources.add(Merchandise.WATER_FRESH, 20);
					return 10;
				}
			};
		}
		new Production("Mead making", Merchandise.MEAD) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.HONEY, 15);
				resources.add(Merchandise.WATER_FRESH, 15);
				return 15;
			}
		};
		new Production("Water gathering", Merchandise.WATER_FRESH) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				if (environment.hasFreshWater) {
					return 50;
				}
				float desalination = environment.hasSaltWater ? 5 : 0;
				float rainCatching = environment.precipitation.value * 10;
				return Math.max(desalination, rainCatching);
			}
		};

		new Production("Fruit growing", Merchandise.FRUIT_FRESH) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				float land = environment.fieldSpace;
				float water = environment.precipitation.value;
				float temperature = environment.temperature.value;
				return map(0.1f, 1f, 4f, mix(land, water, temperature));
			}
		};
		new Production("Vegetable growing", Merchandise.VEGETABLES_FRESH) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				float land = environment.fieldSpace;
				float water = environment.precipitation.value;
				float temperature = environment.temperature.value;
				return map(0.1f, 2f, 4f, mix(land, water, (float) Math.sqrt(temperature)));
			}
		};
		new Production("Fruit drying", Merchandise.FRUIT_DRIED) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				if (environment.temperature.value < WorldProperty.Temperature.HOT.value) {
					return 0;
				}
				if (environment.precipitation.value >= WorldProperty.Precipitation.SUPER_HUMID.value) {
					return 0;
				}
				resources.add(Merchandise.FRUIT_FRESH, 50);
				return 30;
			}
		};
		new Production("Fruit jam production (sugar)", Merchandise.FRUIT_JAM) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.FRUIT_FRESH, 50);
				resources.add(Merchandise.SUGAR, 20);
				return 50;
			}
		};
		new Production("Fruit jam production (honey)", Merchandise.FRUIT_JAM) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.FRUIT_FRESH, 50);
				resources.add(Merchandise.HONEY, 20);
				return 50;
			}
		};

		new Production("Vegetable pickling (vinegar)", Merchandise.VEGETABLES_PICKLED) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.VEGETABLES_FRESH, 50);
				resources.add(Merchandise.WINE, 10);
				return 50;
			}
		};
		new Production("Vegetable pickling (brine)", Merchandise.VEGETABLES_PICKLED) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.VEGETABLES_FRESH, 50);
				resources.add(Merchandise.SALT, 10);
				return 50;
			}
		};

		new Production("Spice farming", Merchandise.SPICES) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				float temperature = 0;
				switch (environment.temperature) {
					case COLD:
						return 0;
					case TEMPERATE:
						temperature = 0.4f;
						break;
					case HOT:
						temperature = 1f;
						break;
				}
				float precipitation = 0;
				switch (environment.precipitation) {
					case SUPER_ARID:
						precipitation = 0;
						break;
					case ARID:
						precipitation = 0.1f;
						break;
					case HUMID:
						precipitation = 0.8f;
						break;
					case SUPER_HUMID:
						precipitation = 0.7f;
						break;
				}
				return map(0, 0.2f, 2f, mix(temperature, precipitation));
			}
		};

		new Production("Salt extraction", Merchandise.SALT) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				if (!environment.hasSaltWater || environment.precipitation.value > WorldProperty.Precipitation.ARID.value) {
					return 0;
				}
				switch (environment.temperature) {
					default:
					case COLD:
						return 10;
					case TEMPERATE:
						return 20;
					case HOT:
						return 40;
				}
			}
		};

		new Production("Book writing", Merchandise.BOOK) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.RAW_ANIMAL_FIBER, 1);
				return 0.1f;
			}
		};

		new Production("Perfume making", Merchandise.PERFUME) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.FRUIT_FRESH, 1);
				resources.add(Merchandise.WOOD_FUEL, 2);
				resources.add(Merchandise.SPICES, 3);
				resources.add(Merchandise.LIQUOR, 2);
				return 2;
			}
		};

		new Production("Rare metal mining", Merchandise.METAL_RARE_ORE) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				return map(0, 1, 8, environment.rareMetalOccurrence);
			}
		};
		new Production("Rare metal smelting (wood)", Merchandise.METAL_RARE_INGOT) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.METAL_RARE_ORE, 20);
				resources.add(Merchandise.WOOD_FUEL, 40);
				return 10;
			}
		};
		new Production("Rare metal smelting (coal)", Merchandise.METAL_RARE_INGOT) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.METAL_RARE_ORE, 20);
				resources.add(Merchandise.COAL, 40);
				return 10;
			}
		};

		new Production("Metal mining", Merchandise.METAL_ORE) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				return map(0, 2, 9, environment.metalOccurrence);
			}
		};
		new Production("Metal smelting (wood)", Merchandise.METAL_INGOT) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.METAL_ORE, 20);
				resources.add(Merchandise.WOOD_FUEL, 40);
				return 15;
			}
		};
		new Production("Metal smelting (coal)", Merchandise.METAL_INGOT) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.METAL_ORE, 20);
				resources.add(Merchandise.COAL, 40);
				return 15;
			}
		};

		new Production("Coal mining", Merchandise.COAL) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				return map(0, 3, 10, environment.coalOccurrence);
			}
		};

		new Production("Jewel mining", Merchandise.JEWELS) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				return map(0, 1, 2, environment.jewelOccurrence);
			}
		};

		for (boolean coal : ALTERNATIVES) {
			final String fuelName = coal ? "coal" : "wood";
			final Merchandise fuel = coal ? Merchandise.COAL : Merchandise.WOOD_FUEL;

			new Production("Jewelry crafting ("+fuelName+")", Merchandise.JEWELRY) {
				@Override
				public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
					resources.add(Merchandise.METAL_RARE_INGOT, 8);
					resources.add(Merchandise.JEWELS, 5);
					resources.add(fuel, 3);
					return 5;
				}
			};

			new Production("Armor and weapon smithing ("+fuelName+")", Merchandise.ARMOR_AND_WEAPONS) {
				@Override
				public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
					resources.add(Merchandise.METAL_INGOT, 10);
					resources.add(fuel, 15);
					return 9;
				}
			};

			new Production("Tool smithing ("+fuelName+")", Merchandise.ARMOR_AND_WEAPONS) {
				@Override
				public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
					resources.add(Merchandise.METAL_INGOT, 10);
					resources.add(Merchandise.WOOD_LUMBER, 10);
					resources.add(fuel, 15);
					return 15;
				}
			};
		}

		new Production("Forestry", Merchandise.WOOD_LOG) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				return map(0.1f, 5, 20, environment.woodAbundance);
			}
		};

		new Production("Wood processing to lumber", Merchandise.WOOD_LUMBER) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.WOOD_LOG, 10);
				return 7;
			}
		};

		new Production("Wood processing to fuel (logs)", Merchandise.WOOD_FUEL) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.WOOD_LOG, 20);
				return 20;
			}
		};

		new Production("Wood processing to fuel (lumber)", Merchandise.WOOD_FUEL) {
			@Override
			public float produce(@NotNull TownC environment, @NotNull Inventory resources) {
				resources.add(Merchandise.WOOD_LUMBER, 30);
				return 30;
			}
		};
	}

	/** Given production values for 0% condition, 25% condition, 100% condition and the current condition, return the result production. */
	private static float map(float production0, float productionQ, float production1, float condition) {
		condition = MathUtils.clamp(condition, 0, 1);
		if (condition < 0.25f) {
			return MathUtils.lerp(production0, productionQ, condition * 4f);
		} else {
			return MathUtils.lerp(production0, production1, (condition - 0.25f) * 1.33333333333333333f);
		}
	}

	/** Mix two necessary conditions to produce combined conditions */
	private static float mix(float condition0, float condition1) {
		condition0 = MathUtils.clamp(condition0, 0, 1);
		condition1 = MathUtils.clamp(condition1, 0, 1);
		return (float) Math.sqrt(condition0 * condition1);
	}

	private static float mix(float condition0, float condition1, float condition2) {
		condition0 = MathUtils.clamp(condition0, 0, 1);
		condition1 = MathUtils.clamp(condition1, 0, 1);
		condition2 = MathUtils.clamp(condition2, 0, 1);
		return (float) Math.sqrt(condition0 * condition1 * condition2);
	}
}
