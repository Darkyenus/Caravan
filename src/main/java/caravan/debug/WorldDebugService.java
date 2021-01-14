package caravan.debug;

import caravan.CaravanApplication;
import caravan.components.Components;
import caravan.components.PositionC;
import caravan.services.RenderingService;
import caravan.services.UIService;
import caravan.services.WorldService;
import caravan.world.Tile;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.darkyen.retinazer.EntitySystem;
import org.jetbrains.annotations.NotNull;

/** World debug rendering. */
public class WorldDebugService extends EntitySystem implements RenderingService, UIService {

	private ItemSelector<Tile> tileSelector;
	private EntityEditorWindow entityEditorWindow;
	private EconomyOverviewWindow economyOverviewWindow;

	private ShapeRenderer shapeRenderer;

	private final Vector2 worldSpaceCursor = new Vector2();

	public WorldDebugService() {
		super(Components.DOMAIN.familyWith(PositionC.class));
	}

	@Override
	public void initialize() {
		tileSelector = new ItemSelector<>(
				CaravanApplication.uiSkin(), "Tiles",
				tile -> engine.getService(WorldService.class).tiles.set((int) Math.floor(worldSpaceCursor.x), (int) Math.floor(worldSpaceCursor.y), tile),
				(tile, batch, x, y, size) -> {
					final TextureRegion tex = tile.getBaseTexture();
					if (tex != null) {
						batch.draw(tex, x, y, size, size);
					}
				},
				Tile.REGISTRY.iterator());

		entityEditorWindow = new EntityEditorWindow(CaravanApplication.uiSkin(), engine, worldSpaceCursor);
		economyOverviewWindow = new EconomyOverviewWindow(engine, worldSpaceCursor);

		shapeRenderer = new ShapeRenderer();
	}

	@Override
	public void createUI(@NotNull CaravanApplication application, @NotNull Stage stage) {
		stage.addActor(tileSelector);
		tileSelector.setVisible(false);
		tileSelector.setPosition(10f, 10f);

		stage.addActor(entityEditorWindow);
		entityEditorWindow.setVisible(false);
		entityEditorWindow.setPosition(220f, 10f);

		stage.addActor(economyOverviewWindow);
		economyOverviewWindow.setVisible(false);
		economyOverviewWindow.setPosition(430f, 10f);
	}

	@Override
	public void update() {
		if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) {
			tileSelector.setVisible(!tileSelector.isVisible());
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
			entityEditorWindow.setVisible(!entityEditorWindow.isVisible());
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) {
			economyOverviewWindow.setVisible(!economyOverviewWindow.isVisible());
		}
	}

	@Override
	public void render(@NotNull Batch batch, @NotNull Rectangle frustum) {
		frustum.getCenter(worldSpaceCursor);

		final boolean tiles = tileSelector.isVisible();
		final boolean entities = entityEditorWindow.isVisible();

		if (!tiles && !entities) {
			return;
		}

		// Draw tile lines
		batch.flush();

		final ShapeRenderer shapeRenderer = this.shapeRenderer;
		shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(Color.BLACK);

		// Tile lines
		if (tiles) {
			for (int x = MathUtils.ceil(frustum.x); x < frustum.x + frustum.width; x++) {
				shapeRenderer.line(x, frustum.y, x, frustum.y + frustum.height);
			}
			for (int y = MathUtils.ceil(frustum.y); y < frustum.y + frustum.height; y++) {
				shapeRenderer.line(frustum.x, y, frustum.x + frustum.width, y);
			}
		}

		// Mid-screen cursor
		final float o = 0.3f;
		shapeRenderer.line(worldSpaceCursor.x - o, worldSpaceCursor.y, worldSpaceCursor.x + o, worldSpaceCursor.y);
		shapeRenderer.line(worldSpaceCursor.x, worldSpaceCursor.y - o, worldSpaceCursor.x, worldSpaceCursor.y + o);

		shapeRenderer.end();
	}
}
