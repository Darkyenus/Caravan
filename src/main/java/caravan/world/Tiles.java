package caravan.world;

import org.jetbrains.annotations.NotNull;

/** Tile definitions. */
public final class Tiles {

	public static final Tile Grass = overlapTile(0, 1, "grass", 1f);
	public static final Tile Desert = overlapTile(1, 2, "desert", 0.8f);
	public static final Tile Water = overlapTile(2, 3, "water", 0.1f);
	public static final Tile Rock = overlapTile(3, 4, "rock", 0.4f);
	public static final Tile Forest = overlapTile(4, 5, "forest", 0.7f);
	public static final Tile Town = overlapTile(5, 6, "town", 0.99f);

	@NotNull
	private static Tile overlapTile(int id, int height, @NotNull String baseName, float movSpeed) {
		return new Tile(id, height, new String[] {
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
				baseName + 17, baseName + 15, baseName + 16, baseName + 18,
				baseName + 14 }, movSpeed);
	}

	/** Does nothing, but calling this makes sure that all variables are loaded and registered. */
	public static void loadClass() {}
}
