package caravan.world;

import caravan.util.PerlinNoise;
import com.darkyen.retinazer.Engine;
import org.jetbrains.annotations.NotNull;

/**
 * Generates game worlds.
 */
public final class WorldGenerator {

	public static void generateWorld(@NotNull Engine engine, long seed) {
		final PerlinNoise n = new PerlinNoise(seed);
		final TileWorld world = engine.getService(TileWorld.class);
		for (int y = 0; y < world.height; y++) {
			for (int x = 0; x < world.width; x++) {
				final float height = n.sampleVeryDense(x, y);
				if(height < 0.3f){
					world.setTile(x, y, Tiles.Water);
				}else if(height < 0.55f){
					world.setTile(x, y, Tiles.Grass);
				}else{
					world.setTile(x, y, Tiles.Forest);
				}
			}
		}
	}

}
