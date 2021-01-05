package caravan.services;

import caravan.components.Components;
import caravan.components.TownC;
import caravan.world.Inventory;
import caravan.world.Merchandise;
import caravan.world.Production;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.darkyen.retinazer.Mapper;
import com.darkyen.retinazer.Wire;
import com.darkyen.retinazer.systems.EntityProcessorSystem;
import org.jetbrains.annotations.NotNull;

import static caravan.util.Util.max;
import static caravan.util.Util.maxIndex;
import static caravan.util.Util.minIndex;
import static caravan.util.Util.rRound;

/**
 * Simulates town economics, internal supply and demand.
 */
public final class TownSystem extends EntityProcessorSystem {

	private static final float DAY_DURATION = 60f;

	private float dayCountdown;

	@Wire
	private SimulationService simulationService;
	@Wire
	private Mapper<TownC> town;

	public TownSystem() {
		super(Components.DOMAIN.familyWith(TownC.class));
	}

	@Override
	public void update() {
		dayCountdown -= simulationService.delta;
		while (dayCountdown <= 0) {
			super.update();
			dayCountdown += DAY_DURATION;
		}
	}

	@Override
	protected void process(int entity) {
		simulateInternalEconomy(town.get(entity));
	}

	public static void simulateInternalEconomy(@NotNull TownC town) {
		updateProduction(town);
		simulateInternalProduction(town);
		simulateInternalConsumption(town);
		town.prices.update();
	}

	private static void simulateInternalProduction(@NotNull TownC town) {
		final Inventory produced = new Inventory();
		final Inventory consumed = new Inventory();
		final Inventory consumedOne = new Inventory();

		for (ObjectIntMap.Entry<Production> entry : town.production) {
			final Production production = entry.key;
			final float scale = entry.value / 10f;

			final float created = production.produce(town, consumedOne);
			produced.add(production.output, created * scale);
			consumed.add(consumedOne, scale);
			consumedOne.clear();
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

	/** Simulate buying whatever is cheap from the merch amount times. */
	private static float fulfillBasicNeed(@NotNull TownC town, @NotNull Merchandise[] merch, int amount) {
		float[] prices = new float[merch.length];
		for (int i = 0; i < merch.length; i++) {
			prices[i] = town.prices.basePrice(merch[i]);
		}
		float value = 0;
		while (amount > 0) {
			int toBuyIndex = minIndex(prices);
			value += town.prices.basePrice(merch[toBuyIndex]);
			town.prices.buyUnit(merch[toBuyIndex]);
			// Steadily increase price to simulate lowered demand of the same good type in large quantities.
			// i.e. people don't want to eat only one thing and would rather pay up for something else
			prices[toBuyIndex] = Math.max(town.prices.basePrice(merch[toBuyIndex]), prices[toBuyIndex] * 1.2f);
			amount--;
		}
		return value;
	}

	/** Simulate buying amount*every entry of merch. */
	private static float fulfillGeneralNeed(@NotNull TownC town, @NotNull Merchandise[] merch, float amount) {
		float value = 0;
		for (Merchandise m : merch) {
			int roundedAmount = rRound(amount);
			for (int i = 0; i < roundedAmount; i++) {
				value += town.prices.basePrice(m);
				town.prices.buyUnit(m);
			}
		}
		return value;
	}

	private static float fulfillLuxuryNeed(@NotNull TownC town, @NotNull Merchandise[] merch, int amount, float budget) {
		if (amount < 1 || budget <= 0) {
			return 0;
		}

		float value = 0;
		final Array<@NotNull Merchandise> shuffle = new Array<>(merch);
		boolean boughtSomething;
		outer:do {
			boughtSomething = false;
			shuffle.shuffle();
			for (Merchandise m : shuffle) {
				final float p = town.prices.basePrice(m);
				value += p;
				budget -= p;
				town.prices.buyUnit(m);
				boughtSomething = true;
				if (--amount <= 0 || budget < 0) {
					break outer;
				}
			}
		} while (boughtSomething);
		return value;
	}

	private static void simulateInternalConsumption(@NotNull TownC town) {
		float valueOfBoughtStuff = 0;

		// Basic food
		valueOfBoughtStuff += fulfillBasicNeed(town, Merchandise.FOOD, rRound(town.population / 10f));
		if (!town.hasFreshWater) {
			valueOfBoughtStuff += fulfillBasicNeed(town, Merchandise.FRESH_WATER, rRound(town.population / 8f));
		}

		// Basic goods
		valueOfBoughtStuff += fulfillGeneralNeed(town, Merchandise.COMMON_GOODS, town.population / 50f);

		// Luxury goods, as budget allows
		float budget = (town.money - valueOfBoughtStuff) * 0.5f;
		town.wealth = MathUtils.clamp(town.wealth + (float) Math.tanh(budget * 0.1) * 0.1f, -1, 1);

		fulfillLuxuryNeed(town, Merchandise.LUXURY_GOODS, rRound(town.population / 5f), budget);
	}

	/** Pick which production leads to most money. */
	private static void updateProduction(@NotNull TownC town) {
		final RandomXS128 random = new RandomXS128();

		float[] profitByProduction = new float[Production.PRODUCTION.size];
		for (int i = 0; i < Production.PRODUCTION.size; i++) {
			profitByProduction[i] = productionProfit(town, Production.PRODUCTION.get(i));
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
			town.production.getAndIncrement(Production.PRODUCTION.items[mostProfitable], 0, giveToMostProfitable);
		}
	}

	private static float productionProfit(@NotNull TownC town, @NotNull Production production) {
		final Inventory inv = new Inventory();
		final float result = production.produce(town, inv);

		float gained = result * town.prices.sellPrice(production.output);
		float lost = 0f;
		for (Merchandise m : Merchandise.VALUES) {
			lost += inv.get(m) * town.prices.buyPrice(m);
		}

		return gained - lost;
	}
}
