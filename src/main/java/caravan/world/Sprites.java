package caravan.world;

import caravan.util.SpriteAnimation;

/**
 * All bundled sprites.
 * The arrays bundle all animation sprites together.
 */
public final class Sprites {

	public static final SpriteAnimation CASTLE = new SpriteAnimation(0, "sprites1");

	public static final SpriteAnimation VILLAGE = new SpriteAnimation(1, "sprites2", "sprites3");

	public static final SpriteAnimation CARAVAN_RIGHT = new SpriteAnimation(2, "sprites4", "sprites5");

	public static final SpriteAnimation CARAVAN_UP = new SpriteAnimation(3, "sprites6", "sprites7");

	public static final SpriteAnimation CARAVAN_DOWN = new SpriteAnimation(4, "sprites8", "sprites9");

	public static final SpriteAnimation BRIDGE_HORIZONTAL = new SpriteAnimation(5, "sprites10");
	public static final SpriteAnimation BRIDGE_VERTICAL = new SpriteAnimation(6, "sprites11");

	public static final SpriteAnimation ARMY_BLUE = new SpriteAnimation(7, "sprites12");
	public static final SpriteAnimation ARMY_RED = new SpriteAnimation(8, "sprites13");
	public static final SpriteAnimation ARMY_CYAN = new SpriteAnimation(9, "sprites14");
	public static final SpriteAnimation ARMY_GREEN = new SpriteAnimation(10, "sprites15");

	public static final SpriteAnimation BAND_SMALL = new SpriteAnimation(11, "sprites16","sprites17");
	public static final SpriteAnimation BAND_MEDIUM = new SpriteAnimation(12, "sprites18", "sprites19");
	public static final SpriteAnimation BAND_LARGE = new SpriteAnimation(13, "sprites20", "sprites21");

	/** Does nothing, but calling this makes sure that all variables are loaded and registered. */
	public static void loadClass() {}
}
