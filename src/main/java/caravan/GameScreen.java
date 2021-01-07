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
import caravan.services.UIService;
import caravan.services.WorldService;
import caravan.world.Tiles;
import caravan.world.WorldGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.darkyen.retinazer.Engine;
import org.jetbrains.annotations.NotNull;

/**
 * The screen with the actual game. Deals with setup of the engine, systems, loading (TBD), etc.
 * The gameplay is implemented inside the systems.
 */
public final class GameScreen extends CaravanApplication.UIScreen {

	public Engine engine;

	private SimulationService simulationService;
	private CameraFocusSystem cameraFocusSystem;

	private RenderingService[] renderingServices;
	private final Rectangle frustum = new Rectangle();

	@Override
	public void create(@NotNull CaravanApplication application) {
		final GameInput gameInput = new GameInput();
		addProcessor(gameInput);

		final int worldWidth = 300;
		final int worldHeight = 300;
		engine = new Engine(Components.DOMAIN,
				simulationService = new SimulationService(),
				new EntitySpawnService(),
				new PlayerControlSystem(application, gameInput),
				new MoveSystem(),
				cameraFocusSystem = new CameraFocusSystem(worldWidth, worldHeight, 5f, gameInput),
				new WorldService(worldWidth, worldHeight, Tiles.Water),
				new RenderSystem()
		);
		renderingServices = engine.getServices(RenderingService.class).toArray(new RenderingService[0]);

		WorldGenerator.generateWorld(engine, System.nanoTime());

		// Spawn player caravan
		WorldGenerator.generatePlayerCaravan(engine);

		// Simulate the game world a bit to initialize
		WorldGenerator.simulateInitialWorldPrices(engine, 200, false);

		super.create(application);
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
