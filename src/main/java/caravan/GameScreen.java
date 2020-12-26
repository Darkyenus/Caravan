package caravan;

import caravan.components.Components;
import caravan.input.GameInput;
import caravan.services.CameraFocusSystem;
import caravan.services.EntitySpawnService;
import caravan.services.MoveSystem;
import caravan.services.PlayerControlSystem;
import caravan.services.RenderSystem;
import caravan.services.RenderingService;
import caravan.services.SimulationService;
import caravan.services.WorldService;
import caravan.world.Tiles;
import caravan.world.WorldGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.darkyen.retinazer.Engine;
import org.jetbrains.annotations.NotNull;

/**
 * The screen with the actual game. Deals with setup of the engine, systems, loading (TBD), etc.
 * The gameplay is implemented inside the systems.
 */
public final class GameScreen extends CaravanApplication.Screen {

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
				new MoveSystem(),
				cameraFocusSystem = new CameraFocusSystem(worldWidth, worldHeight, 5f, gameInput),
				new WorldService(worldWidth, worldHeight, Tiles.Water),
				new RenderSystem()
				);
		renderingServices = engine.getServices(RenderingService.class).toArray(new RenderingService[0]);

		WorldGenerator.generateWorld(engine, System.nanoTime());

		entitySpawn.spawnPlayerCaravan(worldWidth * 0.5f, worldHeight * 0.5f);
	}

	@Override
	public void update(@NotNull CaravanApplication application, float delta) {
		simulationService.simulating = true;// Set this to false when paused
		simulationService.delta = delta;

		engine.update();
	}

	@Override
	public void render(@NotNull CaravanApplication application) {
		for (RenderingService service : renderingServices) {
			service.render(CaravanApplication.batch(), frustum);
		}
	}

	@Override
	public void resize(@NotNull CaravanApplication application, int width, int height) {
		cameraFocusSystem.screenWidth = width;
		cameraFocusSystem.screenHeight = height;
	}
}
