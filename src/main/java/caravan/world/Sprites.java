package caravan.world;

import caravan.util.SpriteAnimation;

/**
 * All bundled sprites.
 * The arrays bundle all animation sprites together.
 */
public final class Sprites {

	public static final SpriteAnimation CASTLE = new SpriteAnimation("sprites1");

	public static final SpriteAnimation VILLAGE = new SpriteAnimation("sprites2", "sprites3");

	public static final SpriteAnimation CARAVAN_RIGHT = new SpriteAnimation("sprites4", "sprites5");

	public static final SpriteAnimation CARAVAN_UP = new SpriteAnimation("sprites6", "sprites7");

	public static final SpriteAnimation CARAVAN_DOWN = new SpriteAnimation("sprites8", "sprites9");

	public static final SpriteAnimation BRIDGE_HORIZONTAL = new SpriteAnimation("sprites10");
	public static final SpriteAnimation BRIDGE_VERTICAL = new SpriteAnimation("sprites11");

	public static final SpriteAnimation ARMY_BLUE = new SpriteAnimation("sprites12");
	public static final SpriteAnimation ARMY_RED = new SpriteAnimation("sprites13");
	public static final SpriteAnimation ARMY_CYAN = new SpriteAnimation("sprites14");
	public static final SpriteAnimation ARMY_GREEN = new SpriteAnimation("sprites15");

	public static final SpriteAnimation BAND_SMALL = new SpriteAnimation("sprites16","sprites17");
	public static final SpriteAnimation BAND_MEDIUM = new SpriteAnimation("sprites18", "sprites19");
	public static final SpriteAnimation BAND_LARGE = new SpriteAnimation("sprites20", "sprites21");
}
