package caravan.util;

import caravan.CaravanApplication;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;

/**
 * Lazily loaded image of specific world size.
 */
public final class Sprite {
    private final String regionName;
    private AtlasRegion atlasRegion;

    /** Size of the sprite in world units, in particular its width. */
    public float size = 1f;

    public float originX = 0.5f;
    public float originY = 0.5f;

    public Sprite(String regionName) {
        this.regionName = regionName;
    }

    public AtlasRegion getRegion() {
        AtlasRegion res = atlasRegion;
        if (res == null) {
            res = atlasRegion = CaravanApplication.textureAtlas().findRegion(regionName);
            assert res != null : "Region \"" + regionName + "\" does not exist!";
        }
        return res;
    }
}
