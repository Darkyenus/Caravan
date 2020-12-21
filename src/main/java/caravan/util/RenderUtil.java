package caravan.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Render utilities.
 */
public final class RenderUtil {

	private static final float[] draw_vertices = new float[4 * 5];
	static {
		Arrays.fill(draw_vertices, Color.WHITE.toFloatBits()); // So we don't have to set the color
	}

	public static void drawTile(@NotNull Batch b, @NotNull TextureRegion base, int x, int y) {
		final float u = base.getU();
		final float v = base.getV2();
		final float u2 = base.getU2();
		final float v2 = base.getV();

		final float[] vertices = draw_vertices;
		vertices[0] = x;
		vertices[1] = y;
		vertices[3] = u;
		vertices[4] = v;

		vertices[5] = x;
		vertices[6] = y + 1;
		vertices[8] = u;
		vertices[9] = v2;

		vertices[10] = x + 1;
		vertices[11] = y + 1;
		vertices[13] = u2;
		vertices[14] = v2;

		vertices[15] = x + 1;
		vertices[16] = y;
		vertices[18] = u2;
		vertices[19] = v;

		b.draw(base.getTexture(), vertices, 0, vertices.length);
	}

	public static void drawTile(@NotNull Batch b, @Nullable TextureAtlas.AtlasRegion region, int x, int y) {
		if (region == null) {
			return;
		}
		final float[] vertices = draw_vertices;
		vertices[5] = vertices[0] = x + region.offsetX / region.originalWidth;
		vertices[16] = vertices[1] = y + region.offsetY / region.originalHeight;
		vertices[11] = vertices[6] = y + (region.offsetY + region.packedHeight) / region.originalHeight;
		vertices[15] = vertices[10] = x + (region.offsetX + region.packedWidth) / region.originalWidth;

		vertices[8] = vertices[3] = region.getU();
		vertices[19] = vertices[4] = region.getV2();
		vertices[14] = vertices[9] = region.getV();
		vertices[18] = vertices[13] = region.getU2();
		b.draw(region.getTexture(), vertices, 0, vertices.length);
	}

	public static void drawCentered(@NotNull Batch batch, @NotNull TextureAtlas.AtlasRegion region, float x, float y, float width, float scaleX, float scaleY) {
		// Ideal centered drawing coordinates
		float dw = width * scaleX;
		float dh = width * region.originalHeight / region.originalWidth * scaleY;
		float dx = x - dw * 0.5f;
		float dy = y /*+ dh*/; //TODO???

		// Account for texture atlas shift
		final float ox0 = region.offsetX / region.originalWidth;
		final float ox1 = (region.offsetX + region.packedWidth) / region.originalWidth;
		final float oy0 = region.offsetY / region.originalHeight;
		final float oy1 = (region.offsetY + region.packedHeight) / region.originalHeight;


		final float[] vertices = draw_vertices;
		vertices[5] = vertices[0] = dx + ox0 * dw;
		vertices[15] = vertices[10] = dx + ox1 * dw;
		vertices[16] = vertices[1] = dy + oy0 * dh;
		vertices[11] = vertices[6] = dy + oy1 * dh;

		vertices[8] = vertices[3] = region.getU();
		vertices[19] = vertices[4] = region.getV2();
		vertices[14] = vertices[9] = region.getV();
		vertices[18] = vertices[13] = region.getU2();
		batch.draw(region.getTexture(), vertices, 0, vertices.length);
	}

}
