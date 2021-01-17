package caravan.services;

import caravan.components.CaravanAIC;
import caravan.components.CaravanC;
import caravan.components.Components;
import caravan.components.MoveC;
import caravan.components.PositionC;
import caravan.components.TownC;
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
		if (timeService.simulating && timeService.delta > 0) {
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
		int nextTown = -1;
		if (nearbyTown != -1) {
			final TownC town = this.town.get(nearbyTown);

			// Sell everything that makes profit
			//TODO

			// Pick stuff to buy here and a destination
			//TODO
		} else {
			// This is probably our spawn point, move towards the nearest town
			nextTown = townSystem.getNearestTown(position, Float.POSITIVE_INFINITY);
		}

		if (nextTown == -1) {
			// I have got nowhere to go! Hopefully this will pass.
			return;
		}

		final PositionC nextTownPosition = this.position.get(nextTown);
		worldService.addMovePathTo(position, move, caravan.speed, MathUtils.floor(nextTownPosition.x), MathUtils.floor(nextTownPosition.y));
	}
}
