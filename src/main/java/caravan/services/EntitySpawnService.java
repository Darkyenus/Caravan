package caravan.services;

import caravan.components.CameraFocusC;
import caravan.components.CaravanC;
import caravan.components.MoveC;
import caravan.components.PlayerC;
import caravan.components.PositionC;
import caravan.components.RenderC;
import caravan.components.TownC;
import caravan.world.Sprites;
import com.darkyen.retinazer.Engine;
import com.darkyen.retinazer.EngineService;
import com.darkyen.retinazer.Mapper;
import com.darkyen.retinazer.Wire;
import org.jetbrains.annotations.NotNull;

import static caravan.world.Sprites.CARAVAN_RIGHT;

/**
 * A service responsible for spawning predefined entities on behalf of other services.
 */
public final class EntitySpawnService implements EngineService {

	@Wire private Engine engine;
	@Wire private Mapper<PositionC> position;
	@Wire private Mapper<MoveC> move;
	@Wire private Mapper<PlayerC> player;
	@Wire private Mapper<CameraFocusC> cameraFocus;
	@Wire private Mapper<RenderC> render;
	@Wire private Mapper<TownC> town;
	@Wire private Mapper<CaravanC> caravan;

	/** Create player's caravan at given position. */
	public int spawnPlayerCaravan(float x, float y) {
		final int entity = engine.createEntity();
		position.create(entity).set(x, y);
		move.create(entity);
		player.create(entity).set(true);
		cameraFocus.create(entity).set(10f);
		render.create(entity).set(CARAVAN_RIGHT);
		caravan.create(entity).money = 100;
		return entity;
	}

	@NotNull
	public int spawnTown(int x, int y) {
		final int entity = engine.createEntity();
		position.create(entity).set(x + 0.5f, y);
		render.create(entity).set(Sprites.VILLAGE);
		town.create(entity);
		return entity;
	}
}
