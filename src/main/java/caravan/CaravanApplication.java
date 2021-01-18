package caravan;

import caravan.debug.ApplicationDebugOverlay;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.SkinLoader;
import com.badlogic.gdx.assets.loaders.resolvers.LocalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.SnapshotArray;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * Base application which manages screens and basic assets.
 */
public final class CaravanApplication implements ApplicationListener, InputProcessor {

	private final SnapshotArray<Screen> activeScreens = new SnapshotArray<>(true, 4, Screen.class);

	private int lastResizeW = -1, lastResizeH = -1;
	private boolean paused = false;
	private boolean disposed = false;

	private FileHandle saveDir;
	private final AssetManager assetManager = new AssetManager(new LocalFileHandleResolver());
	{
		assetManager.setLoader(Skin.class, new SkinLoader(assetManager.getFileHandleResolver()) {
			@Override
			protected Skin newSkin(TextureAtlas atlas) {
				final Skin skin = super.newSkin(atlas);
				skin.setScale(2f);
				return skin;
			}
		});
	}
	private static Batch batch;
	private static Skin skin;
	private static TextureAtlas atlas;
	private final ScreenViewport uiViewport = new ScreenViewport();

	public static long frameId = 0;

	/** @return the shared batch */
	public static @NotNull Batch batch() {
		return batch;
	}

	/** @return the shared UI skin */
	public static @NotNull Skin uiSkin() {
		return skin;
	}

	/** @return the shared game texture atlas */
	public static @NotNull TextureAtlas textureAtlas() {
		return atlas;
	}

	/** @return all active screens */
	public @NotNull Array<Screen> screens() {
		return activeScreens;
	}

	/** @return save directory */
	public @NotNull FileHandle saveDir() {
		return saveDir;
	}

	/** Remove all screens. */
	private void clearScreens(boolean all) {
		try {
			final Screen[] screens = activeScreens.begin();
			final int screenCount = activeScreens.size;
			for (int i = 0; i < screenCount; i++) {
				final Screen screen = screens[i];
				if (!all && screen.sticky) {
					continue;
				}
				screen.application = null;
				screen.dispose();
				activeScreens.removeValue(screen, true);
			}
		} finally {
			activeScreens.end();
		}
	}

	/** Replace all screens with the given screen. */
	public final void setScreen(@NotNull Screen screen) {
		clearScreens(false);
		addScreen(screen);
	}

	/** Add given screen on top of the screen stack. */
	public final boolean addScreen(@NotNull Screen screen) {
		if (disposed) {
			return false;
		}
		if (screen.application != null) {
			return false;
		}
		screen.application = this;
		if (!screen.created) {
			screen.create(this);
			screen.created = true;
		}
		if (screen.application == this && lastResizeW != -1 && lastResizeH != -1) {
			screen.resize(this, lastResizeW, lastResizeH);
		}
		if (screen.application == this && paused) {
			screen.pause(this);
		}

		if (screen.application != this) {
			// It removed and disposed itself!
			return true;
		}

		activeScreens.add(screen);
		activeScreens.sort(RENDER_SCREEN_COMPARATOR);
		return true;
	}

	/** Remove the given screen from the screen stack. */
	public final boolean removeScreen(@NotNull Screen screen, boolean dispose) {
		if (screen.application == this) {
			screen.application = null;
			activeScreens.removeValue(screen, true);
			if (dispose) {
				screen.dispose();
				screen.created = false;
			}
			return true;
		}
		return false;
	}

	@Override
	public void create() {
		saveDir = Gdx.files.local("save");
		saveDir.mkdirs();

		batch = new SpriteBatch();
		assetManager.load("UISkin.json", Skin.class);
		assetManager.load("World.atlas", TextureAtlas.class);

		Options.load(saveDir);
		assetManager.finishLoading();

		skin = assetManager.get("UISkin.json");
		atlas = assetManager.get("World.atlas");

		Gdx.input.setInputProcessor(this);
		addScreen(new ApplicationDebugOverlay());


		// TODO(jp): resume the screen which was active on exit
		addScreen(new MainMenuScreen());
	}

	@Override
	public void resize(int width, int height) {
		if (disposed) {
			return;
		}
		lastResizeW = width;
		lastResizeH = height;
		uiViewport.update(width, height, true);
		try {
			final Screen[] screens = activeScreens.begin();
			final int screenCount = activeScreens.size;
			for (int i = 0; i < screenCount; i++) {
				screens[i].resize(this, width, height);
			}
		} finally {
			activeScreens.end();
		}
	}

	@Override
	public void render() {
		if (disposed) {
			return;
		}
		final float deltaTime = Math.min(Gdx.graphics.getDeltaTime(), 1f / 10f);
		try {
			final Screen[] screens = activeScreens.begin();
			final int screenCount = activeScreens.size;
			for (int i = 0; i < screenCount; i++) {
				screens[i].update(this, deltaTime);
			}

			int firstScreen = screenCount - 1;
			while (firstScreen > 0 && !screens[firstScreen].opaque) {
				firstScreen--;
			}

			final Color clearColor = firstScreen < screenCount ? screens[firstScreen].backgroundColor : Color.BLACK;
			Gdx.gl.glClearColor(clearColor.r, clearColor.g, clearColor.b, clearColor.a);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			for (int i = firstScreen; i < screenCount; i++) {
				screens[i].render(this);
			}
		} finally {
			activeScreens.end();
			frameId++;
		}
	}

	@Override
	public void pause() {
		if (disposed || paused) {
			return;
		}
		paused = true;
		try {
			final Screen[] screens = activeScreens.begin();
			final int screenCount = activeScreens.size;
			for (int i = 0; i < screenCount; i++) {
				screens[i].pause(this);
			}
		} finally {
			activeScreens.end();
		}
	}

	@Override
	public void resume() {
		if (disposed || !paused) {
			return;
		}
		paused = false;
		try {
			final Screen[] screens = activeScreens.begin();
			final int screenCount = activeScreens.size;
			for (int i = 0; i < screenCount; i++) {
				screens[i].resume(this);
			}
		} finally {
			activeScreens.end();
		}
	}

	@Override
	public void dispose() {
		if (disposed) {
			return;
		}
		disposed = true;
		clearScreens(true);
		batch.dispose();
		batch = null;
		assetManager.dispose();
		skin = null;
		atlas = null;
	}

	//region Input
	public boolean keyDown (int keycode) {
		final Screen[] screens = activeScreens.begin();
		try {
			for (int i = activeScreens.size - 1; i >= 0; i--) {
				if (screens[i].keyDown(keycode)) return true;
				if (screens[i].opaque) break;
			}
		} finally {
			activeScreens.end();
		}
		return false;
	}

	public boolean keyUp (int keycode) {
		final Screen[] screens = activeScreens.begin();
		try {
			for (int i = activeScreens.size - 1; i >= 0; i--) {
				if (screens[i].keyUp(keycode)) return true;
				if (screens[i].opaque) break;
			}
		} finally {
			activeScreens.end();
		}
		return false;
	}

	public boolean keyTyped (char character) {
		final Screen[] screens = activeScreens.begin();
		try {
			for (int i = activeScreens.size - 1; i >= 0; i--) {
				if (screens[i].keyTyped(character)) return true;
				if (screens[i].opaque) break;
			}
		} finally {
			activeScreens.end();
		}
		return false;
	}

	public boolean touchDown (int screenX, int screenY, int pointer, int button) {
		final Screen[] screens = activeScreens.begin();
		try {
			for (int i = activeScreens.size - 1; i >= 0; i--) {
				if (screens[i].touchDown(screenX, screenY, pointer, button)) return true;
				if (screens[i].opaque) break;
			}
		} finally {
			activeScreens.end();
		}
		return false;
	}

	public boolean touchUp (int screenX, int screenY, int pointer, int button) {
		final Screen[] screens = activeScreens.begin();
		try {
			for (int i = activeScreens.size - 1; i >= 0; i--) {
				if (screens[i].touchUp(screenX, screenY, pointer, button)) return true;
				if (screens[i].opaque) break;
			}
		} finally {
			activeScreens.end();
		}
		return false;
	}

	public boolean touchDragged (int screenX, int screenY, int pointer) {
		final Screen[] screens = activeScreens.begin();
		try {
			for (int i = activeScreens.size - 1; i >= 0; i--) {
				if (screens[i].touchDragged(screenX, screenY, pointer)) return true;
				if (screens[i].opaque) break;
			}
		} finally {
			activeScreens.end();
		}
		return false;
	}

	public boolean mouseMoved (int screenX, int screenY) {
		final Screen[] screens = activeScreens.begin();
		try {
			for (int i = activeScreens.size - 1; i >= 0; i--) {
				if (screens[i].mouseMoved(screenX, screenY)) return true;
				if (screens[i].opaque) break;
			}
		} finally {
			activeScreens.end();
		}
		return false;
	}

	public boolean scrolled (float amountX, float amountY) {
		final Screen[] screens = activeScreens.begin();
		try {
			for (int i = activeScreens.size - 1; i >= 0; i--) {
				if (screens[i].scrolled(amountX, amountY)) return true;
				if (screens[i].opaque) break;
			}
		} finally {
			activeScreens.end();
		}
		return false;
	}
	//endregion

	private static final Comparator<Screen> RENDER_SCREEN_COMPARATOR = Comparator.comparingInt(s -> s.z);

	public static abstract class Screen extends InputMultiplexer {

		private CaravanApplication application;
		private final int z;
		private final boolean sticky;
		private final boolean opaque;
		private boolean created = false;

		/** The color to be used as a background when this screen is the first one to be rendered on a frame. */
		public final Color backgroundColor = new Color(Color.BLACK);

		/**
		 * @param z sorting priority. Lower Z is drawn earlier, but receives inputs later.
		 * @param sticky if true, the screen will not be removed when setting a new screen
		 * @param opaque screen completely covers screens below, so they don't get rendered
		 */
		protected Screen(int z, boolean sticky, boolean opaque) {
			this.z = z;
			this.sticky = sticky;
			this.opaque = opaque;
		}

		protected Screen() {
			this(0, false, false);
		}

		/**
		 * Remove this screen from the application and dispose it.
		 *
		 * @return true if removed and disposed, false if not added
		 */
		public final boolean removeScreen(boolean dispose) {
			return application != null && application.removeScreen(this, dispose);
		}

		public void create(@NotNull CaravanApplication application) {
		}

		public void resize(@NotNull CaravanApplication application, int width, int height) {
		}

		public void update(@NotNull CaravanApplication application, float delta) {
		}

		public void render(@NotNull CaravanApplication application) {
		}

		public void pause(@NotNull CaravanApplication application) {
		}

		public void resume(@NotNull CaravanApplication application) {
		}

		public void dispose() {
		}
	}

	/** Convenience {@link Screen} base for UI based screens. */
	public static abstract class UIScreen extends Screen {

		protected Stage stage;

		/** @see Screen#Screen(int, boolean, boolean) parent constructor */
		public UIScreen(int z, boolean sticky, boolean opaque) {
			super(z, sticky, opaque);
		}

		public UIScreen() {
		}

		@Override
		public void create(@NotNull CaravanApplication application) {
			stage = new Stage(application.uiViewport, batch) {
				final Vector2 v = new Vector2();

				@Override
				public boolean scrolled(float amountX, float amountY) {
					final Vector2 v = screenToStageCoordinates(this.v.set(Gdx.input.getX(), Gdx.input.getY()));
					setScrollFocus(hit(v.x, v.y, true));
					final boolean result = super.scrolled(amountX, amountY);
					setScrollFocus(null);
					return result;
				}
			};
			addProcessor(0, stage);
			initializeUI(application, stage);
		}

		/** Override either this or {@link #initializeUI(CaravanApplication, Table)} to create your UI. */
		protected void initializeUI(@NotNull CaravanApplication application, @NotNull Stage stage) {
			final Table rootTable = new Table(uiSkin());
			rootTable.setFillParent(true);
			stage.getRoot().addActor(rootTable);
			initializeUI(application, rootTable);
		}

		/** Override either this or {@link #initializeUI(CaravanApplication, Stage)} to create your UI. */
		protected void initializeUI(@NotNull CaravanApplication application, @NotNull Table root) {
		}

		@Override
		public void update(@NotNull CaravanApplication application, float delta) {
			stage.act(delta);
			final Actor keyboardFocus = stage.getKeyboardFocus();
			if (keyboardFocus != null && !keyboardFocus.ascendantsVisible()) {
				stage.setKeyboardFocus(null);
			}
			final Actor scrollFocus = stage.getScrollFocus();
			if (scrollFocus != null && !scrollFocus.ascendantsVisible()) {
				stage.setScrollFocus(null);
			}
		}

		@Override
		public void render(@NotNull CaravanApplication application) {
			stage.getViewport().apply();
			stage.draw();
		}
	}
}
