package caravan.services;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.darkyen.retinazer.EngineService;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for {@link EngineService}s that do rendering.
 */
public interface RenderingService extends EngineService {

	/**
	 * Do rendering into the provided batch.
	 * The batch is not in opened mode and it is not mandatory to use it, it is just for convenience.
	 * However, the projection matrix is set up by the {@link CameraFocusSystem} for the scene.
	 * @param frustum the world-space rectangle that is visible by the camera, set by {@link CameraFocusSystem}
	 */
	void render(@NotNull Batch batch, @NotNull Rectangle frustum);

}
