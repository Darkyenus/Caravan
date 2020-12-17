package caravan.world;

import caravan.services.RenderingService;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.darkyen.retinazer.EngineService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Stores and renders tiles of the world.
 */
public final class TileWorld implements EngineService, RenderingService {

	public final int width, height;
	private final Tile[] tiles;
	private final Tile defaultTile;

	public TileWorld(int width, int height, @NotNull Tile defaultTile) {
		this.width = width;
		this.height = height;
		this.tiles = new Tile[width * height];
		this.defaultTile = defaultTile;
		Arrays.fill(this.tiles, defaultTile);
	}

	@Override
	public void initialize() {
		// Ensure that tile graphics is loaded, so we don't have to worry about it during rendering
		for (Tile tile : Tile.TILES) {
			tile.ensureLoaded();
		}
	}

	/** Get the tile at specified position. */
	@NotNull
	public Tile getTile(int x, int y) {
		if (x < 0 || x >= width || y < 0 || y >= height) {
			return defaultTile;
		}
		return tiles[x + y * width];
	}

	/** Set the tile at specified position.
	 * @return old tile at that position or null if the set failed because the coordinates are out of bounds */
	@Nullable
	public Tile setTile(int x, int y, @NotNull Tile tile) {
		if (x < 0 || x >= width || y < 0 || y >= height) {
			return null;
		}
		final int index = x + y * width;
		final Tile result = tiles[index];
		tiles[index] = tile;
		return result;
	}

	@Override
	public void render(@NotNull Batch batch, @NotNull Rectangle frustum) {
		batch.begin();
		final float OVERLAP = 3;
		final int x0 = MathUtils.floor(frustum.x - OVERLAP);
		final int y0 = MathUtils.floor(frustum.y - OVERLAP);
		final int x1 = MathUtils.ceil(frustum.x + frustum.width + OVERLAP);
		final int y1 = MathUtils.ceil(frustum.y + frustum.height + OVERLAP);
		Tile.drawTiles(this, batch, x0, y0, x1, y1);
		batch.end();
	}
}
