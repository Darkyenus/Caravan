package caravan.services;

import caravan.util.PathFinding;
import caravan.world.Tile;
import caravan.world.WorldAttribute;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.darkyen.retinazer.EngineService;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;

/**
 * Stores and renders tiles of the world.
 */
public final class WorldService implements EngineService, RenderingService, StatefulService {

	public int width, height;
	public WorldAttribute<Tile> tiles;

	public PathFinding pathFinding;

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
		reset(width, height, defaultTile);
	}

	public void reset(int width, int height, @NotNull Tile defaultTile) {
		this.width = width;
		this.height = height;
		this.tiles = new WorldAttribute<>(width, height, defaultTile);

		this.pathFinding = new PathFinding(width, height, defaultPathWorld);
	}

	@Override
	public void initialize() {
		// Ensure that tile graphics is loaded, so we don't have to worry about it during rendering
		for (Tile tile : Tile.REGISTRY) {
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

	@Override
	public int stateVersion() {
		return 1;
	}

	@Override
	public void save(@NotNull Output output) {
		output.writeInt(width);
		output.writeInt(height);
		output.writeShort(tiles.defaultValue.id);
		for (Tile value : tiles.values) {
			output.writeShort(value.id);
		}
	}

	@Override
	public void load(@NotNull Input input) {
		final int width = input.readInt();
		final int height = input.readInt();
		final Tile defaultTile = Tile.REGISTRY.getOrDefault(input.readShort());
		reset(width, height, defaultTile);
		final @NotNull Tile[] values = tiles.values;
		for (int i = 0; i < values.length; i++) {
			values[i] = Tile.REGISTRY.getOrDefault(input.readShort());
		}
	}
}
