package caravan.debug;

import caravan.CaravanApplication;
import caravan.GameScreen;
import caravan.services.CameraFocusSystem;
import caravan.world.Tile;
import caravan.services.WorldService;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.darkyen.retinazer.Engine;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * A debug overlay for editing and inspecting the world.
 */
public final class WorldEditOverlay extends CaravanApplication.UIScreen {

	private ItemSelector<Tile> tileSelector;
	private EntityEditorWindow entityEditor;

	public WorldEditOverlay() {
		super(9, false, false);
	}

	@Override
	public void create(@NotNull CaravanApplication application) {
		Engine engine_ = null;
		for (CaravanApplication.Screen screen : application.screens()) {
			if (screen instanceof GameScreen) {
				engine_ = ((GameScreen) screen).engine;
			}
		}
		if (engine_ == null) {
			Gdx.app.error("WorldEditOverlay", "Can't enable when there is no GameScreen in application");
			removeScreen();
			return;
		}
		final Engine engine = engine_;

		tileSelector = new ItemSelector<>(
				CaravanApplication.uiSkin(), "Tiles",
				tile -> {
					final Rectangle lookAt = engine.getService(CameraFocusSystem.class).lastFrustum;
					engine.getService(WorldService.class).tiles.set((int) Math.floor(lookAt.x + lookAt.width * 0.5f), (int) Math.floor(lookAt.y + lookAt.height * 0.5f), tile);
				},
				(tile, batch, x, y, size) -> {
					final TextureRegion tex = tile.getBaseTexture();
					if (tex != null) {
						batch.draw(tex, x, y, size, size);
					}
				},
				new Iterator<Tile>() {

					int i = 0;

					@Override
					public boolean hasNext() {
						return i < Tile.TILES.size;
					}

					@Override
					public Tile next() {
						return Tile.TILES.get(i++);
					}
				});

		entityEditor = new EntityEditorWindow(CaravanApplication.uiSkin());
		entityEditor.setWorld(engine);
		super.create(application);
	}

	@Override
	protected void initializeUI(@NotNull CaravanApplication application, @NotNull Stage stage) {
		stage.addActor(tileSelector);
		tileSelector.setVisible(false);
		tileSelector.setPosition(500f, 500f);

		stage.addActor(entityEditor);
		entityEditor.setVisible(false);
		entityEditor.setPosition(300f, 300f);
	}

	@Override
	public boolean keyDown(int keycode) {
		if (keycode == Input.Keys.F4) {
			tileSelector.setVisible(!tileSelector.isVisible());
			return true;
		} else if (keycode == Input.Keys.F5) {
			entityEditor.setVisible(!entityEditor.isVisible());
			return true;
		}

		return super.keyDown(keycode);
	}
}
