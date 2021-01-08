package caravan.services;

import caravan.components.CameraFocusC;
import caravan.components.CaravanC;
import caravan.components.MoveC;
import caravan.components.PlayerC;
import caravan.components.PositionC;
import caravan.components.RenderC;
import caravan.components.TitleC;
import caravan.components.TownC;
import caravan.world.Merchandise;
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
	@Wire private Mapper<TitleC> title;

	/** Create player's caravan at given position. */
	public int spawnPlayerCaravan(float x, float y, Merchandise.Category...categories) {
		final int entity = engine.createEntity();
		position.create(entity).set(x, y);
		move.create(entity);
		player.create(entity).set(true);
		cameraFocus.create(entity).set(10f);
		render.create(entity).set(CARAVAN_RIGHT);
		final CaravanC c = caravan.create(entity);
		for (Merchandise.Category cat : categories) {
			c.categories[cat.ordinal()] = true;
		}
		c.money = 100;
		c.speed = 5f;
		return entity;
	}

	public int spawnTown(int x, int y, @NotNull String name) {
		final int entity = engine.createEntity();
		position.create(entity).set(x + 0.5f, y + 0.5f);
		render.create(entity).set(Sprites.VILLAGE);
		town.create(entity);
		final TitleC title = this.title.create(entity);
		title.title = name;
		title.yOffset = 1f;
		return entity;
	}
}
