package caravan.services;

import caravan.CaravanApplication;
import caravan.components.Components;
import caravan.components.PositionC;
import caravan.components.TitleC;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.darkyen.retinazer.Mapper;
import com.darkyen.retinazer.Wire;
import com.darkyen.retinazer.systems.EntityProcessorSystem;
import org.jetbrains.annotations.NotNull;

/**
 * Service that renders titles specified by {@link TitleC}.
 */
public final class TitleRenderService extends EntityProcessorSystem implements RenderingService {

	@Wire
	private Mapper<PositionC> position;
	@Wire
	private Mapper<TitleC> title;

	private BitmapFont titleFont;

	public TitleRenderService() {
		super(Components.DOMAIN.familyWith(PositionC.class, TitleC.class));
	}

	@Override
	public void initialize() {
		super.initialize();
		titleFont = CaravanApplication.uiSkin().getFont("font-ui-small");
	}

	@Override
	public void update() {
		// Do not call super, it is called in render()
	}

	private Batch renderBatch = null;
	private Rectangle renderFrustum = null;

	@Override
	public void render(@NotNull Batch batch, @NotNull Rectangle frustum) {
		renderBatch = batch;
		renderFrustum = frustum;
		batch.begin();
		try {
			super.update();
		} finally {
			batch.end();
			renderBatch = null;
			renderFrustum = null;
		}
	}

	@Override
	protected void process(int entity) {
		final PositionC position = this.position.get(entity);
		if (!PositionC.inFrustum(position, renderFrustum, 20f)) {
			return;
		}

		final TitleC title = this.title.get(entity);

		final BitmapFontCache cache = titleFont.getCache();
		cache.setColor(title.color);
		final float centerX = position.x;
		final float centerY = position.y + title.yOffset;
		cache.setText(title.title, centerX, centerY, 0, Align.center, false);

		final Batch batch = this.renderBatch;
		final float scale = title.lineHeight / cache.getFont().getLineHeight();
		final Array<TextureRegion> regions = cache.getFont().getRegions();
		final int pageCount = regions.size;
		for (int page = 0; page < pageCount; page++) {
			final int vertexCount = cache.getVertexCount(page);
			final float[] vertices = cache.getVertices(page);
			for (int ii = 0; ii < vertexCount; ii += 5) {
				vertices[ii] = (vertices[ii] - centerX) * scale + centerX;
				vertices[ii + 1] = (vertices[ii + 1] - centerY) * scale + centerY;
			}

			batch.draw(regions.get(page).getTexture(), vertices, 0, vertexCount);
		}
		cache.clear();
	}
}
