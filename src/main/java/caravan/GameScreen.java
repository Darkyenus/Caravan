package caravan;

import caravan.components.Components;
import caravan.debug.WorldDebugService;
import caravan.input.GameInput;
import caravan.services.CameraFocusSystem;
import caravan.services.CaravanAIService;
import caravan.services.EntitySpawnService;
import caravan.services.MoveSystem;
import caravan.services.PlayerControlSystem;
import caravan.services.RenderSystem;
import caravan.services.RenderingService;
import caravan.services.TimeService;
import caravan.services.StatefulService;
import caravan.services.TitleRenderService;
import caravan.services.TownSystem;
import caravan.services.UIService;
import caravan.services.WorldService;
import caravan.util.CaravanComponent;
import caravan.world.Sprites;
import caravan.world.Tiles;
import caravan.world.WorldGenerator;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.IntArray;
import com.darkyen.retinazer.Engine;
import com.darkyen.retinazer.Mapper;
import com.darkyen.retinazer.util.Mask;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.util.List;

/**
 * The screen with the actual game. Deals with setup of the engine, systems, loading, etc.
 * The gameplay is implemented inside the systems.
 */
public final class GameScreen extends CaravanApplication.UIScreen {

	public Engine engine;

	private TimeService timeService;
	private CameraFocusSystem cameraFocusSystem;

	private RenderingService[] renderingServices;
	private final Rectangle frustum = new Rectangle();

	private FileHandle saveFile;

	@Override
	public void create(@NotNull CaravanApplication application) {
		final GameInput gameInput = new GameInput();
		addProcessor(gameInput);

		final int worldWidth = 300;
		final int worldHeight = 300;
		engine = new Engine(Components.DOMAIN,
				timeService = new TimeService(gameInput),
				new EntitySpawnService(),
				new PlayerControlSystem(application, gameInput),
				new MoveSystem(),
				new TownSystem(),
				new CaravanAIService(),
				cameraFocusSystem = new CameraFocusSystem(5f, gameInput),
				new WorldService(worldWidth, worldHeight, Tiles.Water),
				new RenderSystem(),
				new TitleRenderService(),
				new WorldDebugService()
		);
		renderingServices = engine.getServices(RenderingService.class).toArray(new RenderingService[0]);

		saveFile = application.saveDir().child("caravan_save.bin");

		if (!load(saveFile)) {
			Gdx.app.log("GameScreen", "Generating a new world");
			WorldGenerator.generateWorld(engine, System.nanoTime(), worldWidth, worldHeight, 24);
			// Spawn player caravan
			WorldGenerator.generatePlayerCaravan(engine);
			// Spawn NPC caravans
			WorldGenerator.generateNPCCaravans(engine, 32);
			// Simulate the game world a bit to initialize
			WorldGenerator.simulateInitialWorldPrices(engine, 200, false);
		} else {
			Gdx.app.log("GameScreen", "Loaded successfully");
		}
		timeService.requestPause();

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
		timeService.rawDelta = delta;

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

	@Override
	public void dispose() {
		if (save(saveFile)) {
			Gdx.app.log("GameScreen", "Saved successfully");
		}
	}

	private static final byte SAVE_FILE_VERSION = 1;

	private static @Nullable StatefulService findService(@NotNull List<StatefulService> services, @NotNull String name) {
		for (StatefulService service : services) {
			if (service.serviceName().equals(name)) {
				return service;
			}
		}
		return null;
	}

	private static void fillPartialInput(@NotNull Input partialInput, @NotNull Input sourceInput, int byteCount) {
		partialInput.reset();
		if (partialInput.getBuffer().length < byteCount) {
			partialInput.setBuffer(new byte[byteCount]);
		}
		sourceInput.readBytes(partialInput.getBuffer(), 0, byteCount);
		partialInput.setLimit(byteCount);
	}

	private boolean load(@NotNull FileHandle file) {
		Tiles.loadClass();
		Sprites.loadClass();

		final BufferedInputStream fileInput;
		try {
			fileInput = file.read(4096);
		} catch (Exception e) {
			Gdx.app.debug("GameScreen", "Failed to load save file "+file, e);
			return false;
		}

		final Input partialInput = new Input(1024);

		try (Input input = new Input(fileInput)) {
			final byte version = input.readByte();
			if (version != SAVE_FILE_VERSION) {
				throw new Exception("Save file version unsupported: "+version);
			}
			final List<StatefulService> services = engine.getServices(StatefulService.class);
			final int serviceCount = input.readInt();
			for (int serviceI = 0; serviceI < serviceCount; serviceI++) {
				final String serviceName = input.readString();
				final int serviceVersion = input.readInt();
				final int serviceSize = input.readInt();

				final StatefulService service = findService(services, serviceName);
				if (service == null) {
					input.skip(serviceSize);
					continue;
				}

				fillPartialInput(partialInput, input, serviceSize);

				try {
					service.load(partialInput, serviceVersion);

					if (partialInput.position() != partialInput.limit()) {
						Gdx.app.error("GameScreen", "Loading of service "+serviceName+" may be broken, "+(partialInput.limit() - partialInput.position())+" bytes left in the buffer");
					}
				} catch (Exception e) {
					Gdx.app.error("GameScreen", "Failed to load service "+serviceName, e);
				}
			}

			final @NotNull Mapper<?>[] availableMappers = engine.getMappers();
			final int savedMappers = input.readInt();
			final @Nullable Mapper<?>[] mappers = new Mapper<?>[savedMappers];
			final int[] mapperVersions = new int[savedMappers];
			for (int m = 0; m < savedMappers; m++) {
				final String mapperName = input.readString();
				final int mapperVersion = input.readInt();

				if (mapperVersion <= 0) {
					continue;
				}

				Mapper<?> foundMapper = null;
				for (Mapper<?> mapper : availableMappers) {
					final CaravanComponent.Serialized annotation = mapper.type.getAnnotation(CaravanComponent.Serialized.class);
					if (annotation.name().equals(mapperName)) {
						if (mapperVersion <= annotation.version()) {
							foundMapper = mapper;
						} else {
							Gdx.app.log("GameScreen", "Can't load component "+mapperName+" - version too high ("+mapperVersion+", max supported is "+annotation.version()+")");
						}
						break;
					}
				}

				if (foundMapper == null) {
					Gdx.app.error("GameScreen", "Loading may not be complete, component "+m+" - "+mapperName+":"+mapperVersion+" is missing");
					continue;
				}

				mappers[m] = foundMapper;
				mapperVersions[m] = mapperVersion;
			}

			final int componentWordCount = (mappers.length + 63) / 64;
			final Mask mask = new Mask();
			final IntArray componentSizes = new IntArray(mappers.length);
			final int entityCount = input.readInt();

			for (int i = 0; i < entityCount; i++) {
				final int entity = input.readInt();
				for (int w = 0; w < componentWordCount; w++) {
					mask.setWord(w, input.readLong());
				}
				final int entityComponentCount = mask.cardinality();
				componentSizes.clear();
				for (int c = 0; c < entityComponentCount; c++) {
					componentSizes.add(input.readInt());
				}

				if (!engine.createEntity(entity)) {
					Gdx.app.error("GameScreen", "Loading of entity "+entity+" may not succeed, the entity could not be created");
				}

				for (int c = mask.nextSetBit(0), ci = 0; c != -1; c = mask.nextSetBit(c + 1), ci++) {
					final Mapper<?> mapper = mappers[c];
					final int componentSize = componentSizes.get(ci);
					if (mapper == null) {
						Gdx.app.error("GameScreen", "Loading of entity "+entity+" may not be complete, component "+c+" is missing");
						input.skip(componentSize);
						continue;
					}


					fillPartialInput(partialInput, input, componentSize);

					final CaravanComponent component = (CaravanComponent) mapper.create(entity);
					try {
						component.load(partialInput, mapperVersions[c]);
						if (partialInput.position() != partialInput.limit()) {
							Gdx.app.error("GameScreen", "Loading of entity "+entity+" may be broken, component "+component+" left "+(partialInput.limit() - partialInput.position())+" bytes in the buffer");
						}
					} catch (Exception e) {
						Gdx.app.error("GameScreen", "Loading of entity "+entity+" may not be complete, component "+component+" failed loading", e);
					}
				}
			}
		} catch (Exception e) {
			Gdx.app.error("GameScreen", "Failed to load save file "+file, e);
			return false;
		}
		return true;
	}

	private boolean save(@NotNull FileHandle file) {
		final FileHandle safeSaveAlias = file.sibling(file.name() + "~");
		final Output partialOutput = new Output(1024, -1);

		try (Output output = new Output(safeSaveAlias.write(false, 4096))) {
			output.writeByte(SAVE_FILE_VERSION);
			final List<StatefulService> services = engine.getServices(StatefulService.class);
			output.writeInt(services.size());
			for (StatefulService service : services) {
				output.writeString(service.serviceName());
				output.writeInt(service.stateVersion());

				partialOutput.reset();
				service.save(partialOutput);
				output.writeInt(partialOutput.position());
				output.write(partialOutput.getBuffer(), 0, partialOutput.position());
			}

			final @NotNull Mapper<?>[] mappers = engine.getMappers();
			output.writeInt(mappers.length);
			for (Mapper<?> mapper : mappers) {
				final CaravanComponent.Serialized serialized = mapper.type.getAnnotation(CaravanComponent.Serialized.class);
				assert serialized != null;
				output.writeString(serialized.name());
				output.writeInt(serialized.version());
			}

			final IntArray entities = engine.getEntities().getIndices();
			output.writeInt(entities.size);
			final int componentWordCount = (mappers.length + 63) / 64;

			final Mask mask = new Mask();
			final IntArray componentLengths = new IntArray(mappers.length);
			for (int i = 0; i < entities.size; i++) {
				final int entity = entities.get(i);

				partialOutput.reset();
				int lastPosition = 0;
				for (int m = 0; m < mappers.length; m++) {
					final CaravanComponent component = (CaravanComponent) mappers[m].getOrNull(entity);
					if (component == null) {
						continue;
					}
					mask.set(m);

					component.save(partialOutput);
					final int position = partialOutput.position();
					componentLengths.add(position - lastPosition);
					lastPosition = position;
				}

				output.writeInt(entity);
				for (int w = 0; w < componentWordCount; w++) {
					output.writeLong(mask.getWord(w));
				}
				for (int c = 0; c < componentLengths.size; c++) {
					output.writeInt(componentLengths.items[c]);
				}
				output.write(partialOutput.getBuffer(), 0, partialOutput.position());

				mask.clear();
				componentLengths.clear();
			}

		} catch (Exception e) {
			Gdx.app.error("GameScreen", "Failed to save to alias file "+safeSaveAlias, e);
			return false;
		}

		try {
			safeSaveAlias.moveTo(file);
		} catch (Exception e) {
			Gdx.app.error("GameScreen", "Failed to overwrite save file "+file+" with "+safeSaveAlias, e);
			return false;
		}
		return true;
	}
}
