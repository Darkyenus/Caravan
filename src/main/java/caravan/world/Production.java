package caravan.world;

import caravan.services.Id;
import caravan.util.Inventory;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

/** Definition of a production type.
 * Each production assumes 10 people working, if there is less,
 * the inputs and outputs are reduced accordingly. */
public abstract class Production extends Id<Production> {

	public static final Registry<Production> REGISTRY = new Registry<>();

	/** Name of the production process/industry */
	public final String name;
	public final Merchandise output;

	private Production(int id, String name, Merchandise output) {
		super(id, REGISTRY);
		this.name = name;
		this.output = output;
	}

	/** Evaluate what is needed to produce something.
	 * @param environment in this environment
	 * @param resources put what is required into this inventory */
	public abstract float produce(@NotNull Environment environment, @NotNull Inventory resources);

	@Override
	public String toString() {
		return name;
	}

	private static final Merchandise[] PRODUCTION_MATERIAL_FUEL = new Merchandise[] {
			Merchandise.COAL,
			Merchandise.WOOD_FUEL,
	};
	private static final Merchandise[] PRODUCTION_MATERIAL_SWEETENER = new Merchandise[] {
			Merchandise.SUGAR,
			Merchandise.HONEY,
	};
	private static final Merchandise[] PRODUCTION_MATERIAL_LIQUOR_BASE = new Merchandise[] {
			Merchandise.VEGETABLES_FRESH,
			Merchandise.GRAIN,
			Merchandise.FRUIT_FRESH
	};
	private static final Merchandise[] PRODUCTION_MATERIAL_PICKLING_BASE = new Merchandise[] {
			Merchandise.WINE,
			Merchandise.SALT,
	};

	static {
		short id = 0;
		new Production(id++, "Wool and Hide Production", Merchandise.RAW_ANIMAL_FIBER) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				return map(0.05f, 0.5f, 2f, environment.fieldSpace);
			}
		};
		new Production(id++, "Plant Fiber Farming", Merchandise.RAW_PLANT_FIBER) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				float land = environment.fieldSpace;
				float water = environment.precipitation;
				return map(0.1f, 1f, 2.5f, mix(land, water));
			}
		};
		new Production(id++, "Weaving (plant fiber)", Merchandise.CLOTH) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				resources.add(Merchandise.RAW_PLANT_FIBER, 5);
				return 4.9f;
			}
		};
		new Production(id++, "Weaving (animal fiber)", Merchandise.CLOTH) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				resources.add(Merchandise.RAW_ANIMAL_FIBER, 5);
				return 4.8f;
			}
		};
		new Production(id++, "Luxury Weaving", Merchandise.CLOTH_LUXURY) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				resources.add(Merchandise.RAW_ANIMAL_FIBER, 3);
				resources.add(Merchandise.RAW_PLANT_FIBER, 3);
				return 4.5f;
			}
		};
		new Production(id++, "Clothing Tailoring", Merchandise.CLOTHING) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				resources.add(Merchandise.CLOTH, 7);
				return 6f;
			}
		};
		new Production(id++, "Luxury Clothing Tailoring", Merchandise.CLOTHING_LUXURY) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				resources.add(Merchandise.CLOTH, 3);
				resources.add(Merchandise.CLOTH_LUXURY, 4);
				return 5.5f;
			}
		};
		new Production(id++, "Livestock Farming", Merchandise.MEAT_FRESH) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				return Production.map(0.2f, 2f, 6f, environment.fieldSpace);
			}
		};
		new Production(id++, "Fishing", Merchandise.MEAT_FRESH) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				float units = 0;
				if (environment.hasSaltWater) {
					units += 3f;
				}
				if (environment.hasFreshWater) {
					units += 1.5f;
				}
				return units;
			}
		};
		new Production(id++, "Meat salting", Merchandise.MEAT_PRESERVED) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				resources.add(Merchandise.MEAT_FRESH, 40);
				resources.add(Merchandise.SALT, 10);
				return 40;
			}
		};
		new Production(id++, "Meat smoking", Merchandise.MEAT_PRESERVED) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				resources.add(Merchandise.MEAT_FRESH, 40);
				resources.add(Merchandise.WOOD_FUEL, 25);
				return 40;
			}
		};
		new Production(id++, "Sausage making", Merchandise.MEAT_LUXURY) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				resources.add(Merchandise.MEAT_PRESERVED, 30);
				resources.add(Merchandise.SALT, 7);
				resources.add(Merchandise.SPICES, 4);
				return 30;
			}
		};
		new Production(id++, "Grain Farming", Merchandise.GRAIN) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				float land = environment.fieldSpace;
				float water = environment.precipitation;
				float temperature = fuzzyInRange(10f, 35f, environment.temperature);
				return map(0.1f, 4f, 11f, mix(land, water, (float) Math.sqrt(temperature)));
			}
		};
		new Production(id++, "Beekeeping", Merchandise.HONEY) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				return mix(environment.fieldSpace, environment.woodAbundance) * 5;
			}
		};
		new Production(id++, "Sugar cane farming", Merchandise.SUGAR) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				return map(0, 1, 5, mix(environment.fieldSpace, fuzzyInRange(25, 35, environment.temperature), fuzzyInRange(0.7f, 1f, environment.precipitation)));
			}
		};
		new Production(id++, "Beer brewing", Merchandise.BEER) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				resources.add(Merchandise.GRAIN, 15);
				resources.add(Merchandise.WATER_FRESH, 20);
				return 20;
			}
		};
		new Production(id++, "Wine making", Merchandise.WINE) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				resources.add(Merchandise.FRUIT_FRESH, 15);
				resources.add(Merchandise.WATER_FRESH, 10);
				return 10;
			}
		};
		new Production(id++, "Mead making", Merchandise.MEAD) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				resources.add(Merchandise.HONEY, 15);
				resources.add(Merchandise.WATER_FRESH, 15);
				return 15;
			}
		};
		new Production(id++, "Water gathering", Merchandise.WATER_FRESH) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				if (environment.hasFreshWater) {
					return 50;
				}
				float desalination = environment.hasSaltWater ? 5 : 0;
				float rainCatching = environment.precipitation * 10;
				return Math.max(desalination, rainCatching);
			}
		};
		new Production(id++, "Fruit growing", Merchandise.FRUIT_FRESH) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				float land = environment.fieldSpace;
				float water = environment.precipitation;
				float temperature = fuzzyInRangeWithPeak(18f, 30f,35f, environment.temperature);
				return map(0.1f, 1f, 4f, mix(land, water, temperature));
			}
		};
		new Production(id++, "Vegetable growing", Merchandise.VEGETABLES_FRESH) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				float land = environment.fieldSpace;
				float water = environment.precipitation;
				float temperature = fuzzyInRangeWithPeak(10f, 25f, 32f, environment.temperature);
				return map(0.1f, 2f, 4f, mix(land, water, (float) Math.sqrt(temperature)));
			}
		};
		new Production(id++, "Fruit drying", Merchandise.FRUIT_DRIED) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				if (environment.temperature < 25f) {
					return 0;
				}
				if (environment.precipitation >= 0.5f) {
					return 0;
				}
				resources.add(Merchandise.FRUIT_FRESH, 50);
				return 30;
			}
		};
		new Production(id++, "Spice farming", Merchandise.SPICES) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				final float temperature = fuzzyInRange(25f, 35f, environment.temperature);
				final float precipitation = fuzzyInRange(0.1f, 0.5f, environment.precipitation);
				return map(0, 0.2f, 2f, mix(temperature, precipitation));
			}
		};
		new Production(id++, "Salt extraction", Merchandise.SALT) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				if (!environment.hasSaltWater || environment.precipitation > 0.3f) {
					return 0;
				}
				return fuzzyInRangeWithPeak(15f, 35f, 40f, environment.temperature) * 40f;
			}
		};
		new Production(id++, "Book writing", Merchandise.BOOK) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				resources.add(Merchandise.RAW_ANIMAL_FIBER, 1);
				return 1f;
			}
		};
		new Production(id++, "Perfume making", Merchandise.PERFUME) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				resources.add(Merchandise.FRUIT_FRESH, 1);
				resources.add(Merchandise.WOOD_FUEL, 2);
				resources.add(Merchandise.SPICES, 3);
				resources.add(Merchandise.LIQUOR, 2);
				return 2;
			}
		};
		new Production(id++, "Rare metal mining", Merchandise.METAL_RARE_ORE) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				return map(0, 2, 8, environment.rareMetalOccurrence);
			}
		};
		new Production(id++, "Metal mining", Merchandise.METAL_ORE) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				return map(0, 2, 9, environment.metalOccurrence);
			}
		};
		new Production(id++, "Coal mining", Merchandise.COAL) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				return map(0, 10, 26, environment.coalOccurrence);
			}
		};
		new Production(id++, "Jewel mining", Merchandise.JEWELS) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				return map(0, 2, 4, environment.jewelOccurrence);
			}
		};
		new Production(id++, "Forestry", Merchandise.WOOD_LOG) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				return map(0.1f, 5, 25, environment.woodAbundance);
			}
		};
		new Production(id++,"Wood processing to lumber", Merchandise.WOOD_LUMBER) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				resources.add(Merchandise.WOOD_LOG, 10);
				return 7;
			}
		};
		new Production(id++,"Wood processing to fuel (logs)", Merchandise.WOOD_FUEL) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				resources.add(Merchandise.WOOD_LOG, 20);
				return 20;
			}
		};
		new Production(id++,"Wood processing to fuel (lumber)", Merchandise.WOOD_FUEL) {
			@Override
			public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
				resources.add(Merchandise.WOOD_LUMBER, 30);
				return 30;
			}
		};

		id = alternatives(id, PRODUCTION_MATERIAL_FUEL, (idBox, fuel) -> {
			new Production(idBox[0]++, "Baking (" + fuel.materialName + ")", Merchandise.BAKED_GOODS) {
				@Override
				public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
					resources.add(Merchandise.GRAIN, 30);
					resources.add(Merchandise.WATER_FRESH, 20);
					resources.add(fuel, 10);
					resources.add(Merchandise.SALT, 1);
					return 50;
				}
			};

			new Production(idBox[0]++, "Rare metal smelting ("+fuel.materialName+")", Merchandise.METAL_RARE_INGOT) {
				@Override
				public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
					resources.add(Merchandise.METAL_RARE_ORE, 20);
					resources.add(fuel, 20);
					return 10;
				}
			};
			new Production(idBox[0]++, "Metal smelting ("+fuel.materialName+")", Merchandise.METAL_INGOT) {
				@Override
				public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
					resources.add(Merchandise.METAL_ORE, 20);
					resources.add(fuel, 40);
					return 15;
				}
			};
			new Production(idBox[0]++, "Jewelry crafting ("+ fuel.materialName +")", Merchandise.JEWELRY) {
				@Override
				public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
					resources.add(Merchandise.METAL_RARE_INGOT, 7);
					resources.add(Merchandise.METAL_INGOT, 1);
					resources.add(Merchandise.JEWELS, 5);
					resources.add(fuel, 3);
					return 5;
				}
			};

			new Production(idBox[0]++, "Armor and weapon smithing ("+ fuel.materialName +")", Merchandise.ARMOR_AND_WEAPONS) {
				@Override
				public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
					resources.add(Merchandise.METAL_INGOT, 10);
					resources.add(fuel, 7);
					return 9;
				}
			};

			new Production(idBox[0]++, "Tool smithing ("+ fuel.materialName +")", Merchandise.TOOLS) {
				@Override
				public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
					resources.add(Merchandise.METAL_INGOT, 10);
					resources.add(Merchandise.WOOD_LUMBER, 10);
					resources.add(fuel, 7);
					return 15;
				}
			};
		});

		id = alternatives(id, PRODUCTION_MATERIAL_LIQUOR_BASE, (idBox, origin) -> {
			new Production(idBox[0]++, "Liquor making ("+ origin.materialName +")", Merchandise.LIQUOR) {
				@Override
				public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
					resources.add(origin, 15);
					resources.add(Merchandise.WATER_FRESH, 20);
					return 10;
				}
			};
		});

		id = alternatives(id, PRODUCTION_MATERIAL_SWEETENER, (idBox, sweetener) -> {
			new Production(idBox[0]++, "Fruit jam production ("+sweetener.materialName+")", Merchandise.FRUIT_JAM) {
				@Override
				public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
					resources.add(Merchandise.FRUIT_FRESH, 50);
					resources.add(sweetener, 20);
					return 50;
				}
			};

			new Production(idBox[0]++, "Luxury Baking ("+ sweetener.materialName +")", Merchandise.BAKED_GOODS_LUXURY) {
				@Override
				public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
					resources.add(Merchandise.GRAIN, 50);
					resources.add(Merchandise.WATER_FRESH, 20);
					resources.add(Merchandise.WOOD_FUEL, 10);
					resources.add(Merchandise.SALT, 3);
					resources.add(Merchandise.SPICES, 3);
					resources.add(sweetener, 6);
					return 50;
				}
			};
		});

		id = alternatives(id, PRODUCTION_MATERIAL_PICKLING_BASE, (idBox, picklingBase) -> {
			new Production(idBox[0]++, "Vegetable pickling ("+picklingBase.materialName+")", Merchandise.VEGETABLES_PICKLED) {
				@Override
				public float produce(@NotNull Environment environment, @NotNull Inventory resources) {
					resources.add(Merchandise.VEGETABLES_FRESH, 50);
					resources.add(picklingBase, 10);
					return 50;
				}
			};
		});
	}

	private static int alternativeLevel = 0;

	private static short alternatives(short nextId, Merchandise[] alternatives, BiConsumer<short[], Merchandise> create) {
		assert alternatives.length <= 0x7;
		assert alternativeLevel >= 0 && alternativeLevel <= 1;
		alternativeLevel++;
		try {
			final short[] idBox = { nextId };
			create.accept(idBox, alternatives[0]);
			final short resultNextId = idBox[0];
			for (int i = 1; i < alternatives.length; i++) {
				idBox[0] = (short) (nextId | ((i & 0x7) << (16 - alternativeLevel * 3)));
				create.accept(idBox, alternatives[i]);
			}
			return resultNextId;
		} finally {
			alternativeLevel--;
		}
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

	private static float fuzzyInRange(float range0, float range1, float value) {
		if (value >= range0 && value <= range1) {
			return 1f;
		}
		final float fuzz = (range1 - range0) * 0.1f;
		if (value < range0 && value > range0 - fuzz) {
			return (value - (range0 - fuzz)) / fuzz;
		}
		if (value > range1 && value < range1 + fuzz) {
			return 1f - (value - range1) / fuzz;
		}
		return 0f;
	}

	private static float fuzzyInRangeWithPeak(float range0, float peak, float range1, float value) {
		if (value >= range0 && value <= range1) {
			if (value <= peak) {
				return (value - range0) / (peak - range0) * 0.5f + 0.5f;
			} else {
				return 1f - (value - peak) / (range1 - peak) * 0.5f;
			}
		}
		final float fuzz = (range1 - range0) * 0.1f;
		if (value < range0 && value > range0 - fuzz) {
			return (value - (range0 - fuzz)) / fuzz * 0.5f;
		}
		if (value > range1 && value < range1 + fuzz) {
			return 1f - (value - range1) / fuzz * 0.5f;
		}
		return 0f;
	}
}
