package caravan.world;

/**
 * Tile definitions.
 */
public final class Tiles {

	public static final Tile Debug = new Tile(Tile.MAX_HEIGHT, "tile91");

	public static final Tile Water = new Tile(1, "tile25", "tile62", "tile64", "tile65", "tile63", "tile66", "tile67", "tile68", "tile69", "tile70", "tile71", "tile72", "tile73", "tile74", "tile76", "tile77", "tile75", "tile78");
	public static final Tile Dirt = new Tile(0, "tile6");
	public static final Tile Grass = new Tile(2, "tile13", "tile1", "tile5", "tile7", "tile11", "tile0", "tile2", "tile10", "tile12", "tile9", "tile8", "tile4", "tile3", "tile58", "tile60", "tile61", "tile59", "tile57");


	public static final Tile FloorWood = new Tile(Tile.MAX_HEIGHT, "tile17");
	public static final Tile FloorWoodDoor = new Tile(Tile.MAX_HEIGHT, "tile26");
	
}
