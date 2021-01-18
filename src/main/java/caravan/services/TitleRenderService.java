package caravan.services;

import caravan.CaravanApplication;
import caravan.components.Components;
import caravan.components.PositionC;
import caravan.components.TitleC;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.utils.Align;
import com.darkyen.retinazer.Mapper;
import com.darkyen.retinazer.Wire;
import com.darkyen.retinazer.systems.EntityProcessorSystem;
import org.jetbrains.annotations.NotNull;

/**
 * Service that renders titles specified by {@link TitleC}.
 */
public final class TitleRenderService extends EntityProcessorSystem implements UIService {

	@Wire
	private Mapper<PositionC> position;
	@Wire
	private Mapper<TitleC> title;

	@Wire
	private CameraFocusSystem cameraFocusSystem;

	private BitmapFont titleFont;

	public TitleRenderService() {
		super(Components.DOMAIN.familyWith(PositionC.class, TitleC.class));
	}

	@Override
	public void initialize() {
		super.initialize();
		titleFont = CaravanApplication.uiSkin().getFont("title-world");
	}

	@Override
	public void update() {
		// Do not call super, it is called in draw()
	}

	private Batch renderBatch = null;
	private Widget titleRenderWidget = null;

	@Override
	public void createUI(@NotNull CaravanApplication application, @NotNull Stage stage) {
		final Widget titleRenderWidget = this.titleRenderWidget = new Widget() {
			@Override
			public void draw(Batch batch, float parentAlpha) {
				validate();

				renderBatch = batch;
				try {
					TitleRenderService.super.update();
				} finally {
					renderBatch = null;
				}
			}
		};
		titleRenderWidget.setFillParent(true);
		stage.getRoot().addActorAt(0, titleRenderWidget);
	}

	private final Vector3 process_projectTmp = new Vector3();

	@Override
	protected void process(int entity) {
		final PositionC position = this.position.get(entity);
		final TitleC title = this.title.get(entity);

		final Vector3 local = process_projectTmp.set(position.x, position.y + title.yOffset, 0f);
		local.prj(cameraFocusSystem.viewport.getCamera().combined);
		local.prj(titleRenderWidget.getStage().getViewport().getCamera().invProjectionView);

		final float wx = titleRenderWidget.getX();
		final float wy = titleRenderWidget.getY();
		final float ww = titleRenderWidget.getWidth();
		final float wh = titleRenderWidget.getHeight();

		if (local.x < wx || local.x > wx + ww || local.y < wy || local.y > wy + wh) {
			// Out of frustum
			return;
		}
		final float edge = 1f / 20f;
		final float alphaX = Math.min(Math.min((local.x - wx) * edge, (wx + ww - local.x) * edge), 1f);
		final float alphaY = Math.min(Math.min((local.y - wy) * edge, (wy + wh - local.y) * edge), 1f);


		final BitmapFontCache cache = titleFont.getCache();
		cache.setColor(title.color);
		cache.getColor().a *= alphaX * alphaY;
		cache.setText(title.title, local.x, local.y, 0, Align.center, false);
		cache.draw(renderBatch);
		cache.clear();
	}
}
