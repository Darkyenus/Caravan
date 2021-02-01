package caravan.services;

import caravan.components.Components;
import caravan.components.PositionC;
import caravan.components.RenderC;
import caravan.components.TownC;
import caravan.util.Inventory;
import caravan.world.Merchandise;
import caravan.world.Production;
import caravan.world.Sprites;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.darkyen.retinazer.Mapper;
import com.darkyen.retinazer.Wire;
import com.darkyen.retinazer.systems.EntityProcessorSystem;
import org.jetbrains.annotations.NotNull;

import static caravan.util.Util.max;
import static caravan.util.Util.maxIndex;
import static caravan.util.Util.rRound;

/**
 * Simulates town economics, internal supply and demand.
 * Also handles querying towns for other systems.
 */
public final class TownSystem extends EntityProcessorSystem {

	@Wire
	private TimeService timeService;
	@Wire
	private Mapper<TownC> town;
	@Wire
	private Mapper<PositionC> position;
	@Wire
	private Mapper<RenderC> render;

	private static final int MIN_POPULATION = 10;
	private static final int CASTLE_POPULATION = 80;
	private static final int MAX_POPULATION = 100;

	public TownSystem() {
		super(Components.DOMAIN.familyWith(TownC.class, PositionC.class));
	}

	@Override
	public void update() {
		for (int i = 0; i < timeService.dayAdvances; i++) {
			super.update();
		}
	}

	@Override
	protected void process(int entity) {
		final TownC town = this.town.get(entity);
		if (simulateInternalEconomy(town) != 0) {
			final RenderC render = this.render.getOrNull(entity);
			if (render != null) {
				render.sprite = town.population >= CASTLE_POPULATION ? Sprites.CASTLE : Sprites.VILLAGE;
			}
		}
	}

	public int getNearestTown(@NotNull PositionC position, float maxDistance, int excludingTownEntity) {
		int town = -1;
		float townDistance = maxDistance;

		final IntArray townIndices = getEntities().getIndices();
		for (int i = 0; i < townIndices.size; i++) {
			final int townEntity = townIndices.get(i);
			if (townEntity == excludingTownEntity) {
				continue;
			}

			final PositionC townPos = this.position.get(townEntity);
			final float distance = PositionC.manhattanDistance(townPos, position);
			if (distance < townDistance) {
				town = townEntity;
				townDistance = distance;
			}
		}

		return town;
	}

	/** Get a town entity that is accessible from the given position or -1 if there is no such town. */
	public int getNearbyTown(@NotNull PositionC position) {
		return getNearestTown(position, 1.5f, -1);
	}

	public static int simulateInternalEconomy(@NotNull TownC town) {
		int popGrowth = updateProduction(town);
		simulateInternalProduction(town);
		simulateInternalConsumption(town, popGrowth);
		town.prices.update();
		return popGrowth;
	}

	private static void simulateInternalProduction(@NotNull TownC town) {
		final Inventory produced = new Inventory();
		final Inventory consumed = new Inventory();
		final Inventory consumedOne = new Inventory();

		for (ObjectIntMap.Entry<Production> entry : town.production) {
			final Production production = entry.key;
			final float scale = entry.value / 10f;

			final float created = production.produce(town.environment, consumedOne);
			produced.add(production.output, created * scale);
			consumed.add(consumedOne, scale);
			consumedOne.reset();
		}

		for (Merchandise m : Merchandise.VALUES) {
			final int p = produced.get(m);
			for (int i = 0; i < p; i++) {
				town.prices.sellUnit(m);
			}
			final int c = consumed.get(m);
			for (int i = 0; i < c; i++) {
				town.prices.buyUnit(m);
			}
		}
	}

	private static float fulfillNeed(@NotNull TownC town, @NotNull Merchandise[] merch, float totalAmount, float budget, float variableConsumption) {
		if (merch.length <= 0) {
			return 0;
		}

		if (merch.length == 1) {
			// Special case
			final int amount = rRound(totalAmount);
			float spentValue = 0;
			final Merchandise m = merch[0];
			for (int a = 0; a < amount; a++) {
				final float price = town.prices.basePrice(m);
				if (spentValue + price > budget) {
					break;
				}
				spentValue += price;
				town.prices.buyUnit(m);
			}
			return spentValue;
		}

		final float variableAmountTotal = totalAmount * variableConsumption;
		final float guaranteedAmountPerItem = (totalAmount - variableAmountTotal) / merch.length;

		float[] unitsToBuy = new float[merch.length];
		for (int i = 0; i < merch.length; i++) {
			unitsToBuy[i] = -town.prices.basePrice(merch[i]);
		}
		{// Softmax-like
			final float offset = max(unitsToBuy);
			float sum = 0;
			for (int i = 0; i < merch.length; i++) {
				sum += (unitsToBuy[i] = (float) Math.exp(unitsToBuy[i] - offset));
			}
			float iSum = variableAmountTotal / sum;
			for (int i = 0; i < merch.length; i++) {
				unitsToBuy[i] = unitsToBuy[i] * iSum + guaranteedAmountPerItem;
			}
		}

		float totalPriceEstimate = 0f;
		for (int i = 0; i < merch.length; i++) {
			totalPriceEstimate += unitsToBuy[i] * town.prices.basePrice(merch[i]);
		}

		if (totalPriceEstimate > budget) {
			float scaleDown = budget / totalPriceEstimate;
			for (int i = 0; i < merch.length; i++) {
				unitsToBuy[i] *= scaleDown;
			}
		}

		float spentValue = 0;
		for (int i = 0; i < merch.length; i++) {
			final int amount = rRound(unitsToBuy[i]);

			for (int a = 0; a < amount; a++) {
				final Merchandise m = merch[i];
				spentValue += town.prices.basePrice(m);
				town.prices.buyUnit(m);
			}
		}

		return spentValue;
	}

	private static void simulateInternalConsumption(@NotNull TownC town, int popGrowth) {
		float valueOfBoughtStuff = 0;

		// Basic food
		valueOfBoughtStuff += fulfillNeed(town, Merchandise.BASIC_FOOD, town.population * 0.15f, Float.POSITIVE_INFINITY,1f);
		valueOfBoughtStuff += fulfillNeed(town, Merchandise.EXTRA_FOOD, town.population * 0.06f, Float.POSITIVE_INFINITY, 0.7f);
		if (!town.environment.hasFreshWater) {
			valueOfBoughtStuff += fulfillNeed(town, Merchandise.FRESH_WATER, town.population * 0.2f, Float.POSITIVE_INFINITY, 0f);
		}

		// Basic goods
		valueOfBoughtStuff += fulfillNeed(town, Merchandise.COMMON_GOODS, town.population * 0.02f, Float.POSITIVE_INFINITY, 0.4f);

		if (popGrowth > 0) {
			// Building materials
			valueOfBoughtStuff += fulfillNeed(town, Merchandise.BUILDING_MATERIALS, popGrowth * 5f, town.money - valueOfBoughtStuff + 300f, 0.5f);
		}

		// Luxury goods, as budget allows
		float budget = (town.money - valueOfBoughtStuff) * 0.5f;
		town.wealth = MathUtils.clamp(town.wealth + (float) Math.tanh(budget * 0.1) * 0.1f, -1, 1);

		fulfillNeed(town, Merchandise.LUXURY_GOODS, town.population * 0.2f, budget, 0.5f);
	}

	/** Pick which production leads to most money. */
	private static int updateProduction(@NotNull TownC town) {
		final RandomXS128 random = new RandomXS128();

		final int productionCount = Production.REGISTRY.count();
		float[] profitByProduction = new float[productionCount];
		for (int i = 0; i < productionCount; i++) {
			profitByProduction[i] = productionProfit(town, Production.REGISTRY.getDense(i));
		}
		final float maxProfitableProduction = max(profitByProduction);
		final float veryLowProfitThreshold = maxProfitableProduction / 10f;
		final float lowProfitThreshold = maxProfitableProduction / 2f;

		int employed = 0;
		final Array<Production> productionToRemove = new Array<>(town.production.size / 2);
		// Count employed and reduce where unprofitable, and some randomly
		for (ObjectIntMap.Entry<Production> entry : town.production) {
			final Production production = entry.key;
			int workers = entry.value;
			final float profit = productionProfit(town, production);
			if (profit <= veryLowProfitThreshold) {
				workers = workers / 2;
			} else if (profit <= lowProfitThreshold) {
				workers -= random.nextInt(Math.min(3, workers));
			} else {
				int dropout = Math.max(random.nextInt(Math.min(6, workers)) - 3, 0);
				workers -= dropout;
			}

			town.production.put(production, workers);
			employed += workers;
			if (workers == 0) {
				productionToRemove.add(production);
			}
		}
		for (Production production : productionToRemove) {
			town.production.remove(production, 0);
		}

		int unemployed = town.population - employed;
		int populationGrowth = 0;

		if (unemployed > 0 && town.wealth <= -1f && town.population > MIN_POPULATION) {
			// Decrease population
			town.population--;
			unemployed--;
			populationGrowth--;
		} else if (town.wealth >= 1f && town.population < MAX_POPULATION) {
			unemployed++;
			town.population++;
			populationGrowth++;
		}

		// Pick new profitable industries
		while (unemployed > 0) {
			final int mostProfitable = maxIndex(profitByProduction);
			final float mostProfitableProfit = profitByProduction[mostProfitable];
			if (mostProfitableProfit <= 0) {
				// Do not do that, it is better to not work at all.
				break;
			}
			profitByProduction[mostProfitable] = -1f;// So that it is not picked again

			final int nextMostProfitable = maxIndex(profitByProduction);
			final float nextMostProfitableProfit = profitByProduction[nextMostProfitable];

			float portionToGiveToMostProfitable = MathUtils.clamp(mostProfitableProfit / (nextMostProfitableProfit + mostProfitableProfit), 0, 1);
			final int giveToMostProfitable = MathUtils.clamp(rRound(unemployed * portionToGiveToMostProfitable), 1, unemployed);
			unemployed -= giveToMostProfitable;
			town.production.getAndIncrement(Production.REGISTRY.getDense(mostProfitable), 0, giveToMostProfitable);
		}

		return populationGrowth;
	}

	public static float productionProfit(@NotNull TownC town, @NotNull Production production) {
		final Inventory inv = new Inventory();
		final float result = production.produce(town.environment, inv);

		float gained = result * town.prices.buyPrice(production.output);
		float lost = 0f;
		for (Merchandise m : Merchandise.VALUES) {
			final int amount = inv.get(m);
			if (amount != 0) {
				lost += amount * town.prices.sellPrice(m);
			}
		}

		return gained - lost;
	}
}
