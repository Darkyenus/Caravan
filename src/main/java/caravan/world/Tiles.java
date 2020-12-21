package caravan.world;

import org.jetbrains.annotations.NotNull;

/**
 * Tile definitions.
 */
public final class Tiles {

	public static final Tile Water = overlapTile(1, "water");
	public static final Tile Forest = overlapTile(10, "forest");
	public static final Tile Grass = overlapTile(2, "grass");

	@NotNull
	private static Tile overlapTile(int height, @NotNull String baseName) {
		return new Tile(height, new String[] {
				baseName + 1,
				baseName + 19,
				baseName + 20,
				baseName + 21,
				baseName + 22,
				baseName + 23
		}, new String[] {
				baseName + 8, baseName + 6, baseName + 5, baseName + 3,
				baseName + 10, baseName + 11, baseName + 13, baseName + 12,
				baseName + 9, baseName + 7, baseName + 4, baseName + 2,
				baseName + 17, baseName + 16, baseName + 16, baseName + 18,
				baseName + 14 });
	}
	
}
