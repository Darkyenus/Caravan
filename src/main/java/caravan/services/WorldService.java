package caravan.services;

import caravan.util.PathFinding;
import caravan.world.Tile;
import caravan.world.WorldAttribute;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.darkyen.retinazer.EngineService;
import org.jetbrains.annotations.NotNull;

/**
 * Stores and renders tiles of the world.
 */
public final class WorldService implements EngineService, RenderingService {

	public final int width, height;
	public final WorldAttribute<Tile> tiles;

	public final PathFinding pathFinding;

	public final PathFinding.PathWorld defaultPathWorld = new PathFinding.PathWorld() {
		@Override
		public boolean isAccessible(int x, int y) {
			return x >= 0 && x < width && y >= 0 && y < height;
			//return tiles.get(x, y).movementSpeedMultiplier > 0f;
		}

		@Override
		public float movementSpeedMultiplier(int x, int y) {
			return tiles.get(x, y).movementSpeedMultiplier;
		}
	};

	public WorldService(int width, int height, @NotNull Tile defaultTile) {
		this.width = width;
		this.height = height;
		this.tiles = new WorldAttribute<>(width, height, defaultTile);

		this.pathFinding = new PathFinding(width, height, defaultPathWorld);
	}

	@Override
	public void initialize() {
		// Ensure that tile graphics is loaded, so we don't have to worry about it during rendering
		for (Tile tile : Tile.TILES) {
			tile.ensureLoaded();
		}
	}

	@Override
	public void render(@NotNull Batch batch, @NotNull Rectangle frustum) {
		batch.begin();
		final float OVERLAP = 3;
		final int x0 = MathUtils.floor(frustum.x - OVERLAP);
		final int y0 = MathUtils.floor(frustum.y - OVERLAP);
		final int x1 = MathUtils.ceil(frustum.x + frustum.width + OVERLAP);
		final int y1 = MathUtils.ceil(frustum.y + frustum.height + OVERLAP);
		Tile.drawTiles(this.tiles, batch, x0, y0, x1, y1);
		batch.end();
	}
}
