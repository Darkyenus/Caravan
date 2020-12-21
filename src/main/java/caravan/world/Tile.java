package caravan.world;

import caravan.CaravanApplication;
import caravan.util.RenderUtil;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static caravan.util.RenderUtil.drawTile;

/**
 * Tiles are singletons, that cover whole map, as its first layer.
 */
public final class Tile {

	/**
	 * Use this as your height, when you don't want to overlap onto anything and don't want anything overlapping onto you
	 */
	public static final byte MAX_HEIGHT = 101;

	/** All created tiles. DO NOT MODIFY. */
	public static final Array<Tile> TILES = new Array<>(true, 16, Tile.class);

	/**
	 * Determines how "high" this tile is.
	 * This is used for overlap calculations - higher tiles may draw their overlaps onto lower tiles.
	 * Same height tiles won't draw overlaps on each other, but will overlap "together", which may create ugly seams.
	 * Therefore, distinct tiles that might appear next to each other should be either overlap-less or have different heights.
	 */
	public final byte height;

	/** Static tile ID. */
	public final short tileID = (short) TILES.size;
	{
		TILES.add(this);
	}

	private @Nullable TextureRegion[] base = null;
	private @Nullable TextureAtlas.AtlasRegion[] overlaps = null;

	private String[] baseImageNames;
	private String[] overlapImageNames;

	public Tile(int height, @NotNull String @NotNull[] bases, @NotNull String @NotNull[] overlaps) {
		assert bases.length >= 1;
		assert overlaps.length % OVERLAP_IMAGE_COUNT == 0;
		this.height = (byte) height;
		this.baseImageNames = bases;
		this.overlapImageNames = overlaps;
	}

	public void ensureLoaded() {
		final String[] baseImageNames = this.baseImageNames;
		final String[] overlapImageNames = this.overlapImageNames;
		if (baseImageNames == null) {
			return;
		}

		this.baseImageNames = null;
		this.overlapImageNames = null;

		this.base = new TextureRegion[baseImageNames.length];
		this.overlaps = new TextureAtlas.AtlasRegion[overlapImageNames.length];

		final TextureAtlas atlas = CaravanApplication.textureAtlas();

		for (int i = 0; i < baseImageNames.length; i++) {
			if ((this.base[i] = atlas.findRegion(baseImageNames[i])) == null) {
				Gdx.app.error("Tile", "Tile named "+baseImageNames[i]+" not found");
			}
		}
		for (int i = 0; i < overlapImageNames.length; i++) {
			if ((this.overlaps[i] = atlas.findRegion(overlapImageNames[i])) == null) {
				Gdx.app.error("Tile", "Overlap named "+baseImageNames[i]+" not found");
			}
		}
	}

	public @Nullable TextureRegion getBaseTexture() {
		ensureLoaded();
		for (TextureRegion region : base) {
			if (region != null) return region;
		}
		return null;
	}

	//region Overlap mask parts
	private static final byte TOP = (byte) 0b1000_0000;
	private static final byte LEFT = 0b0100_0000;
	private static final byte RIGHT = 0b0010_0000;
	private static final byte BOTTOM = 0b0001_0000;

	private static final byte EDGE_MASK = TOP | LEFT | RIGHT | BOTTOM;

	private static final byte TOP_LEFT = 0b0000_1000;
	private static final byte TOP_RIGHT = 0b0000_0100;
	private static final byte BOTTOM_LEFT = 0b0000_0010;
	private static final byte BOTTOM_RIGHT = 0b0000_0001;
	//endregion

	private static final Tile[] draw_collisionTiles = new Tile[8];

	public static void drawTiles(@NotNull final TileWorld t, @NotNull final Batch b, final int startX, final int startY, final int endX, final int endY) {
		//Draw tiles & overlaps
		final Tile[] tiles = draw_collisionTiles;
		for (int y = endY; y >= startY; y--) {
			for (int x = startX; x <= endX; x++) {
				final Tile tile = t.getTile(x, y);

				final int tileHash = ((0x8c26b2de ^ x) * (0xbc671103 ^ y)) & 0x7FFFFFFF /* Force into being positive */;

				final TextureRegion base = tile.base[tileHash % tile.base.length];
				if (base != null) {
					RenderUtil.drawTile(b, base, x, y);
				}

				tiles[0] = t.getTile(x+1,y-1); //BOTTOM_RIGHT
				tiles[1] = t.getTile(x-1,y-1); //BOTTOM_LEFT
				tiles[2] = t.getTile(x+1,y+1); //TOP_RIGHT
				tiles[3] = t.getTile(x-1,y+1); //TOP_LEFT
				tiles[4] = t.getTile(x,y-1); //BOTTOM
				tiles[5] = t.getTile(x+1,y); //RIGHT
				tiles[6] = t.getTile(x-1,y); //LEFT
				tiles[7] = t.getTile(x,y+1); //TOP

				byte lastHeight = tile.height;
				while (true) {
					// Looking for lowest tile that is higher than us/last overlap
					byte nowHeight = Byte.MAX_VALUE;
					Tile nowHeightTile = null;
					for (Tile neighborTile : tiles) {
						if (neighborTile.height > lastHeight && neighborTile.height < nowHeight && neighborTile.overlaps != null) {
							nowHeight = neighborTile.height;
							nowHeightTile = neighborTile;
						}
					}
					if (nowHeightTile == null) break;

					// Draw overlap
					byte map = 0;
					for (int i = 0; i < 8; i++) {
						if(tiles[i].height == nowHeight){
							map |= 1 << i;
						}
					}

					drawTileOverlaps(b, nowHeightTile.overlaps, x, y, map);

					// reset
					lastHeight = nowHeight;
				}
			}
		}
	}

	//region Overlap image indices
	//Edge has all of its material on that edge
	private static final int E_T = 0; //Edge top
	private static final int E_L = 1; //Edge left
	private static final int E_R = 2; //Edge right
	private static final int E_B = 3; //Edge bottom

	//Inside has its material in that corner and in two neighboring
	private static final int I_TL = 4; //Inside top left
	private static final int I_TR = 5; //Inside top right
	private static final int I_BL = 6; //Inside bottom left
	private static final int I_BR = 7; //Inside bottom right

	//Corner has its material only in that corner
	private static final int C_TL = 8; //Corner top left
	private static final int C_TR = 9; //Corner top right
	private static final int C_BL = 10; //Corner bottom left
	private static final int C_BR = 11; //Corner bottom right

	//U-shape has all of its material on that edge and on two neighboring edges
	private static final int U_T = 12; //U-shape top
	private static final int U_L = 13; //U-shape left
	private static final int U_R = 14; //U-shape right
	private static final int U_B = 15; //U-shape bottom

	private static final int O = 16;//O-shape, all edges and corners combined
	private static final int OVERLAP_IMAGE_COUNT = 17;
	//endregion

	/**
	 * Note: in overlapMask, when edge bit is set, both neighboring corner bits will be set as well
	 * <p>
	 * Won't get called if overlapMask is 0.
	 *
	 * @param batch to which render
	 * @param x of lower left corner
	 * @param y of lower left corner
	 * @param overlapMask marks which parts of tile are overlapped into
	 */
	private static void drawTileOverlaps(@NotNull Batch batch, @Nullable final TextureAtlas.AtlasRegion @NotNull[] overlaps, int x, int y, byte overlapMask) {
		byte cornerMask = 0;
		switch (overlapMask & EDGE_MASK) {
			case 0:
				// Only corners
				cornerMask = TOP_LEFT | TOP_RIGHT | BOTTOM_LEFT | BOTTOM_RIGHT;
				break;
			//Edges
			case TOP:
				drawTile(batch, overlaps[E_T], x, y);
				cornerMask = BOTTOM_LEFT | BOTTOM_RIGHT;
				break;
			case LEFT:
				drawTile(batch, overlaps[E_L], x, y);
				cornerMask = TOP_RIGHT | BOTTOM_RIGHT;
				break;
			case RIGHT:
				drawTile(batch, overlaps[E_R], x, y);
				cornerMask = BOTTOM_LEFT | TOP_LEFT;
				break;
			case BOTTOM:
				drawTile(batch, overlaps[E_B], x, y);
				cornerMask = TOP_LEFT | TOP_RIGHT;
				break;
			//Double edges
			case TOP | BOTTOM:
				drawTile(batch, overlaps[E_T], x, y);
				drawTile(batch, overlaps[E_B], x, y);
				break;
			case LEFT | RIGHT:
				drawTile(batch, overlaps[E_L], x, y);
				drawTile(batch, overlaps[E_R], x, y);
				break;
			//Insides
			case TOP | LEFT:
				drawTile(batch, overlaps[I_TL], x, y);
				cornerMask = BOTTOM_RIGHT;
				break;
			case TOP | RIGHT:
				drawTile(batch, overlaps[I_TR], x, y);
				cornerMask = BOTTOM_LEFT;
				break;
			case BOTTOM | LEFT:
				drawTile(batch, overlaps[I_BL], x, y);
				cornerMask = TOP_RIGHT;
				break;
			case BOTTOM | RIGHT:
				drawTile(batch, overlaps[I_BR], x, y);
				cornerMask = TOP_LEFT;
				break;
			//U-shapes
			case LEFT | TOP | RIGHT:
				drawTile(batch, overlaps[U_T], x, y);
				return;
			case TOP | RIGHT | BOTTOM:
				drawTile(batch, overlaps[U_R], x, y);
				return;
			case RIGHT | BOTTOM | LEFT:
				drawTile(batch, overlaps[U_B], x, y);
				return;
			case BOTTOM | LEFT | TOP:
				drawTile(batch, overlaps[U_L], x, y);
				return;
			//O-shape
			case TOP | LEFT | BOTTOM | RIGHT:
				drawTile(batch, overlaps[O], x, y);
				return;
		}
		//Render corners
		final byte corners = (byte) (overlapMask & cornerMask);
		if (corners == 0) return;
		if ((corners & TOP_LEFT) != 0) {
			drawTile(batch, overlaps[C_TL], x, y);
		}
		if ((corners & TOP_RIGHT) != 0) {
			drawTile(batch, overlaps[C_TR], x, y);
		}
		if ((corners & BOTTOM_LEFT) != 0) {
			drawTile(batch, overlaps[C_BL], x, y);
		}
		if ((corners & BOTTOM_RIGHT) != 0) {
			drawTile(batch, overlaps[C_BR], x, y);
		}
	}
}
