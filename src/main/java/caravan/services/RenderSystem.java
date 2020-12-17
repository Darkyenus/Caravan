package caravan.services;

import caravan.components.Components;
import caravan.components.PositionC;
import caravan.components.RenderC;
import caravan.util.PooledArray;
import caravan.util.RenderUtil;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.darkyen.retinazer.Mapper;
import com.darkyen.retinazer.Wire;
import com.darkyen.retinazer.systems.EntityProcessorSystem;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Renders the {@link caravan.components.RenderC} components.
 */
public final class RenderSystem extends EntityProcessorSystem implements RenderingService {

	@Wire
	private Mapper<RenderC> render;
	@Wire
	private Mapper<PositionC> position;

	public RenderSystem() {
		super(Components.DOMAIN.familyWith(RenderC.class, PositionC.class));
	}

	private static final float RENDER_FRUSTUM_OVERLAP = 5f;
	private final Rectangle renderFrustum = new Rectangle();
	private final PooledArray<EntityRenderable> renderables = new PooledArray<>(EntityRenderable.class);

	@Override
	public void update() {
		// Do not call super.update() because that would iterate over entities, which we want to only do at render time
	}

	@Override
	public void render(@NotNull Batch batch, @NotNull Rectangle frustum) {
		renderFrustum.set(frustum.x - RENDER_FRUSTUM_OVERLAP, frustum.y - RENDER_FRUSTUM_OVERLAP, frustum.width + RENDER_FRUSTUM_OVERLAP * 2f, frustum.height + RENDER_FRUSTUM_OVERLAP * 2f);

		super.update();

		final PooledArray<EntityRenderable> renderables = this.renderables;
		final int renderableCount = renderables.size;
		final EntityRenderable[] renderableItems = renderables.items;
		Arrays.sort(renderableItems, 0, renderableCount, RENDERABLE_COMPARATOR);

		batch.begin();
		final Mapper<RenderC> renderMapper = this.render;
		for (int i = 0; i < renderableCount; i++) {
			final EntityRenderable renderable = renderableItems[i];
			final RenderC render = renderMapper.get(renderable.entity);
			RenderUtil.drawCentered(batch, render.sprite.getRegion(), renderable.x, renderable.y, render.sprite.size, render.scaleX, render.scaleY);
		}
		batch.end();
		renderables.clear();
	}

	@Override
	protected void process(int entity) {
		final PositionC position = this.position.get(entity);
		if (!renderFrustum.contains(position.x, position.y)) return; // Not visible, don't render

		final EntityRenderable renderable = renderables.add();
		renderable.entity = entity;
		renderable.x = position.x;
		renderable.y = position.y;
	}


	private static final Comparator<EntityRenderable> RENDERABLE_COMPARATOR = (o1, o2) -> Float.compare(o2.y, o1.y);

	public static final class EntityRenderable {
		public EntityRenderable() {}

		public int entity;
		public float x, y;
	}
}
