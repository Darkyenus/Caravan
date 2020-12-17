package caravan.services;

import caravan.components.CameraFocusC;
import caravan.components.MoveC;
import caravan.components.PlayerC;
import caravan.components.PositionC;
import caravan.components.RenderC;
import caravan.util.Sprite;
import com.darkyen.retinazer.Engine;
import com.darkyen.retinazer.EngineService;
import com.darkyen.retinazer.Mapper;
import com.darkyen.retinazer.Wire;

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

	/** Create player's caravan at given position. */
	public int spawnPlayerCaravan(float x, float y) {
		final int entity = engine.createEntity();
		position.create(entity).set(x, y);
		move.create(entity).set(x, y, 0f);
		player.create(entity).set(true);
		cameraFocus.create(entity).set(4f);
		render.create(entity).set(Dummy, 1f, 1f);
		return entity;
	}

	// TODO(jp): Sort these out
	public static final Sprite Dummy = new Sprite("tile86");
	public static final Sprite[] Flowers = {new Sprite("tile79"), new Sprite("tile80"), new Sprite("tile81"), new Sprite("tile82")};
	public static final Sprite[] Sprites = {new Sprite("tile87"), new Sprite("tile88")};
	public static final Sprite[] Shirt = {new Sprite("tile89"), new Sprite("tile90")};

}
