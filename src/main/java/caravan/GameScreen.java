package caravan;

import caravan.components.Components;
import caravan.components.PositionC;
import caravan.components.TownC;
import caravan.input.GameInput;
import caravan.services.CameraFocusSystem;
import caravan.services.EntitySpawnService;
import caravan.services.MoveSystem;
import caravan.services.PlayerControlSystem;
import caravan.services.RenderSystem;
import caravan.services.RenderingService;
import caravan.services.SimulationService;
import caravan.services.TradingSystem;
import caravan.services.UIService;
import caravan.services.WorldService;
import caravan.world.Tiles;
import caravan.world.WorldGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.IntArray;
import com.darkyen.retinazer.Engine;
import com.darkyen.retinazer.Mapper;
import org.jetbrains.annotations.NotNull;

/**
 * The screen with the actual game. Deals with setup of the engine, systems, loading (TBD), etc.
 * The gameplay is implemented inside the systems.
 */
public final class GameScreen extends CaravanApplication.UIScreen {

	public final Engine engine;

	private final SimulationService simulationService;
	private final CameraFocusSystem cameraFocusSystem;

	private final RenderingService[] renderingServices;
	private final Rectangle frustum = new Rectangle();

	public GameScreen() {
		final GameInput gameInput = new GameInput();
		addProcessor(gameInput);

		final EntitySpawnService entitySpawn;

		final int worldWidth = 300;
		final int worldHeight = 300;
		engine = new Engine(Components.DOMAIN,
				simulationService = new SimulationService(),
				entitySpawn = new EntitySpawnService(),
				new PlayerControlSystem(gameInput),
				new TradingSystem(),
				new MoveSystem(),
				cameraFocusSystem = new CameraFocusSystem(worldWidth, worldHeight, 5f, gameInput),
				new WorldService(worldWidth, worldHeight, Tiles.Water),
				new RenderSystem()
				);
		renderingServices = engine.getServices(RenderingService.class).toArray(new RenderingService[0]);

		WorldGenerator.generateWorld(engine, System.nanoTime());
		engine.flush();

		final Mapper<PositionC> position = engine.getMapper(PositionC.class);
		final IntArray townEntities = engine.getEntities(Components.DOMAIN.familyWith(TownC.class, PositionC.class)).getIndices();
		final int starterTown = townEntities.random();
		final PositionC starterTownPosition = position.get(starterTown);
		int closestTown = -1;
		float closestTownDistance = Float.POSITIVE_INFINITY;
		for (int i = 0; i < townEntities.size; i++) {
			final int t = townEntities.get(i);
			if (t == starterTown) {
				continue;
			}
			final float dist = PositionC.manhattanDistance(starterTownPosition, position.get(t));
			if (dist < closestTownDistance) {
				closestTownDistance = dist;
				closestTown = t;
			}
		}

		final PositionC otherTownPosition = position.get(closestTown);
		float offX = otherTownPosition.x - starterTownPosition.x;
		float offY = otherTownPosition.y - starterTownPosition.y;
		final float scale = 5f / (Math.abs(offX) + Math.abs(offY));
		offX *= scale;
		offY *= scale;
		float posX = starterTownPosition.x + offX;
		float posY = starterTownPosition.y + offY;

		entitySpawn.spawnPlayerCaravan(posX, posY);
	}

	@Override
	protected void initializeUI(@NotNull CaravanApplication application, @NotNull Stage stage) {
		for (UIService service : engine.getServices(UIService.class)) {
			service.createUI(application, stage);
		}
	}

	@Override
	public void update(@NotNull CaravanApplication application, float delta) {
		simulationService.simulating = true;// Set this to false when paused
		simulationService.delta = delta;

		engine.update();
		super.update(application, delta);
	}

	@Override
	public void render(@NotNull CaravanApplication application) {
		for (RenderingService service : renderingServices) {
			service.render(CaravanApplication.batch(), frustum);
		}
		super.render(application);
	}

	@Override
	public void resize(@NotNull CaravanApplication application, int width, int height) {
		cameraFocusSystem.screenWidth = width;
		cameraFocusSystem.screenHeight = height;
	}
}
