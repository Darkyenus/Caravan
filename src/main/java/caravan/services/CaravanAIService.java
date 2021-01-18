package caravan.services;

import caravan.TradingScreen;
import caravan.components.CaravanAIC;
import caravan.components.CaravanC;
import caravan.components.Components;
import caravan.components.MoveC;
import caravan.components.PositionC;
import caravan.components.TownC;
import caravan.util.PriceMemory;
import caravan.world.Merchandise;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.darkyen.retinazer.Mapper;
import com.darkyen.retinazer.Wire;
import com.darkyen.retinazer.systems.EntityProcessorSystem;

/**
 * System that controls NPC caravans.
 */
public final class CaravanAIService extends EntityProcessorSystem {

	@Wire
	private Mapper<PositionC> position;
	@Wire
	private Mapper<MoveC> move;
	@Wire
	private Mapper<CaravanC> caravan;
	@Wire
	private Mapper<CaravanAIC> caravanAi;
	@Wire
	private Mapper<TownC> town;

	@Wire
	private TownSystem townSystem;
	@Wire
	private TimeService timeService;
	@Wire
	private WorldService worldService;

	public CaravanAIService() {
		super(Components.DOMAIN.familyWith(PositionC.class, MoveC.class, CaravanC.class, CaravanAIC.class));
	}

	@Override
	public void update() {
		if (timeService.simulating) {
			super.update();
		}
	}

	@Override
	protected void process(int entity) {
		final MoveC move = this.move.get(entity);
		if (move.waypoints.size > 0) {
			// Still has somewhere to go
			return;
		}

		// This caravan has arrived.
		final PositionC position = this.position.get(entity);
		final CaravanC caravan = this.caravan.get(entity);
		final CaravanAIC caravanAi = this.caravanAi.get(entity);

		final int nearbyTown = townSystem.getNearbyTown(position);
		final int nextTown;
		if (nearbyTown != -1) {
			final TownC town = this.town.get(nearbyTown);
			town.rumors.addRandomPriceRumors(caravan.priceMemory, town.prices, nearbyTown, timeService.day);
			final PriceMemory priceMemory = caravan.priceMemory;

			// Sell everything that makes profit
			for (Merchandise m : Merchandise.VALUES) {
				final short buyPrice = caravan.inventoryPriceBuyMemory[m.ordinal()];
				while (caravan.inventory.get(m) > 0 && town.prices.sellPrice(m) > buyPrice) {
					TradingScreen.performSell(town, caravan, m, true);
				}
			}

			// Pick stuff to buy here and a destination
			final int memoryCapacity = priceMemory.capacity();
			final int noOlderThanDay = timeService.day - 7;

			int bestProfitMemorySlot = -1;
			Merchandise bestProfitMerchandise = null;
			int bestProfit = 5;

			for (int memorySlot = 0; memorySlot < memoryCapacity; memorySlot++) {
				if (!priceMemory.isMemorySlotValid(memorySlot, noOlderThanDay, nearbyTown)) {
					continue;
				}

				for (Merchandise m : Merchandise.VALUES) {
					if (!m.tradeable) {
						continue;
					}

					final int localBuyPrice = town.prices.buyPrice(m);
					final int remoteSellPrice = priceMemory.sellPrice(memorySlot, m);
					final int couldBuy = Math.min(caravan.money / localBuyPrice, 10);
					final int totalProfit = couldBuy * remoteSellPrice;
					if (totalProfit > bestProfit) {
						bestProfit = totalProfit;
						bestProfitMemorySlot = memorySlot;
						bestProfitMerchandise = m;
					}
				}
			}

			if (bestProfitMemorySlot != -1) {
				// Buy stuff and go there.
				int bought = 0;
				while (bought < 10 && TradingScreen.performBuy(town, caravan, bestProfitMerchandise, true)) {
					// Buying everything, but not too much.
					bought++;
				}
				nextTown = priceMemory.townEntity(bestProfitMemorySlot);
				caravanAi.currentActivity = CaravanAIC.Activity.TRADING_SINGLE_GOOD;
				caravanAi.tradedMerchandise = bestProfitMerchandise;
			} else {
				// Pick a random town and go there
				final int[] closestNeighbors = town.closestNeighbors;
				final int offset = MathUtils.random.nextInt(closestNeighbors.length);
				int bestKnownOption = -1;
				int bestExplorationOption = -1;
				for (int i = 0; i < closestNeighbors.length; i++) {
					final int neighbor = closestNeighbors[(i + offset) % closestNeighbors.length];
					if (neighbor == caravanAi.previousTown) {
						continue;
					}
					final int slot = priceMemory.validSlotForTown(neighbor, timeService.day - 10);
					if (slot == -1) {
						bestExplorationOption = neighbor;
					} else {
						bestKnownOption = neighbor;
					}
				}

				if (bestKnownOption != -1 && bestExplorationOption != -1) {
					// Pick randomly
					if (MathUtils.randomBoolean()) {
						caravanAi.currentActivity = CaravanAIC.Activity.EXPLORING;
						nextTown = bestExplorationOption;
					} else {
						caravanAi.currentActivity = CaravanAIC.Activity.LOOKING_FOR_A_GOOD_DEAL;
						nextTown = bestKnownOption;
					}
				} else if (bestExplorationOption != -1) {
					caravanAi.currentActivity = CaravanAIC.Activity.EXPLORING;
					nextTown = bestExplorationOption;
				} else if (bestKnownOption != -1) {
					caravanAi.currentActivity = CaravanAIC.Activity.LOOKING_FOR_A_GOOD_DEAL;
					nextTown = bestKnownOption;
				} else {
					Gdx.app.log("CaravanAIService", "Town has no neighbors");
					nextTown = townSystem.getNearestTown(position, Float.POSITIVE_INFINITY, nearbyTown);
					caravanAi.currentActivity = CaravanAIC.Activity.LOOKING_FOR_A_GOOD_DEAL;
				}
			}

			priceMemory.remember(timeService.day, nearbyTown, town);
		} else {
			// This is probably our spawn point, move towards the nearest town
			nextTown = townSystem.getNearestTown(position, Float.POSITIVE_INFINITY, -1);
		}

		if (nextTown == -1) {
			// I have got nowhere to go! Hopefully this will pass.
			Gdx.app.log("CaravanAIService", "Caravan has nowhere to go");
			return;
		}

		caravanAi.previousTown = nearbyTown;
		caravanAi.targetTown = nextTown;

		final PositionC nextTownPosition = this.position.get(nextTown);
		worldService.addMovePathTo(position, move, caravan.speed, MathUtils.floor(nextTownPosition.x), MathUtils.floor(nextTownPosition.y));
	}
}
