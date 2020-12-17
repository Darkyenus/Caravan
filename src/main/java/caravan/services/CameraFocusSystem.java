package caravan.services;

import caravan.components.CameraFocusC;
import caravan.components.Components;
import caravan.components.PositionC;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.darkyen.retinazer.Mapper;
import com.darkyen.retinazer.Wire;
import com.darkyen.retinazer.systems.EntityProcessorSystem;
import org.jetbrains.annotations.NotNull;

/**
 * Manages camera focusing, camera control and viewport management.
 */
public final class CameraFocusSystem extends EntityProcessorSystem implements RenderingService {

    /** Internal result: what should be focused this frame. */
    private final Rectangle thisFrameFraming = new Rectangle();
    /** Internal temporary: framing of a single entity. */
    private final Rectangle thisEntityFraming = new Rectangle();
    /** Internal temporary: detecting first entity of the frame - it is handled differently. */
    private boolean firstEntity = true;

    /** The framing (where camera looks) of the frame - more can be shown. */
    public final Rectangle currentFraming = new Rectangle();
    /** Current framing can get detached from the focus and be controlled independently. */
    private boolean currentFramingDetached = false;
    /** If the framing is detached, is it trying to catch up to the real value or not? (After it catches up, it will re-attach.) */
    private boolean currentFramingCatchUp = false;

    private final float minVisibleUnits;

    /** The viewport defined for the world. */
    public final ExtendViewport viewport;

    private final Vector3 frustumTmp = new Vector3();

    /** The screen dimensions, used to setup the viewport. */
    public int screenWidth = 800, screenHeight = 800;

    @Wire
    private Mapper<PositionC> positionMapper;
    @Wire
    private Mapper<CameraFocusC> cameraTrackerMapper;

    public CameraFocusSystem() {
        this(5f, 100f);
    }

    /**
     * @param minVisibleUnits Defines the maximum zoom level in terms of units in smaller dimension.
     * @param maxVisibleUnits Defines the minimum zoom level in terms of units in larger dimension.
     */
    public CameraFocusSystem(float minVisibleUnits, float maxVisibleUnits) {
        super(Components.DOMAIN.familyWith(PositionC.class, CameraFocusC.class));
        this.minVisibleUnits = minVisibleUnits;
        viewport = new ExtendViewport(minVisibleUnits, minVisibleUnits, maxVisibleUnits, maxVisibleUnits, new OrthographicCamera());
        final Camera camera = viewport.getCamera();
        camera.direction.set(0f, 0f, -1f);
        camera.up.set(0f, 1f, 0f);
        camera.near = 0.5f;
        camera.far = 1.5f;
    }

    /** Switch whether the camera is attached to the focused entities or not. */
    public void setFree(boolean free) {
        if (free) {
            currentFramingDetached = true;
        } else if (currentFramingDetached) {
            currentFramingCatchUp = true;
        }
    }

    @Override
    public void update() {
        if (currentFramingDetached && !currentFramingCatchUp) {
            // No need to follow anything when the framing is independent.
            return;
        }
        firstEntity = true;
        super.update();
        if (firstEntity) {
            // Can't follow anything, there are no entities to be tracked.
            return;
        }

        if (currentFramingCatchUp) {
            // TODO(jp): Animate this part correctly
            currentFraming.x = MathUtils.lerp(currentFraming.x, thisFrameFraming.x, 0.5f);
            currentFraming.y = MathUtils.lerp(currentFraming.y, thisFrameFraming.y, 0.5f);
            currentFraming.width = MathUtils.lerp(currentFraming.width, thisFrameFraming.width, 0.5f);
            currentFraming.height = MathUtils.lerp(currentFraming.height, thisFrameFraming.height, 0.5f);
            if (MathUtils.isEqual(currentFraming.x, thisFrameFraming.x)
                    && MathUtils.isEqual(currentFraming.y, thisFrameFraming.y)
                    && MathUtils.isEqual(currentFraming.width, thisFrameFraming.width)
                    && MathUtils.isEqual(currentFraming.height, thisFrameFraming.height)) {
                currentFramingCatchUp = false;
                currentFramingDetached = false;
            }
        } else {
            currentFraming.set(thisFrameFraming);
        }

        // Rescale the framing base to match zoom limits

        // Move coordinates into center for easier manipulation
        currentFraming.x += currentFraming.width * 0.5f;
        currentFraming.y += currentFraming.height * 0.5f;

        float width = currentFraming.getWidth();
        float height = currentFraming.getHeight();
        final float smallerDimension = Math.min(width, height);
        if (smallerDimension < minVisibleUnits) {
            final float scaleUp = minVisibleUnits / smallerDimension;
            width *= scaleUp;
            height *= scaleUp;
        }

        // Max is taken care of by the viewport

        currentFraming.x -= width * 0.5f;
        currentFraming.y -= height * 0.5f;
        currentFraming.width = width;
        currentFraming.height = height;
    }

    @Override
    public void render(@NotNull Batch batch, @NotNull Rectangle frustum) {
        // Update viewport
        viewport.getCamera().position.set(currentFraming.x + currentFraming.width * 0.5f, currentFraming.y + currentFraming.height * 0.5f, 1);
        viewport.setMinWorldWidth(currentFraming.width);
        viewport.setMinWorldHeight(currentFraming.height);

        viewport.update(screenWidth, screenHeight);

        // Update frustum
        final Vector3 frustumCorner = viewport.unproject(frustumTmp.set(0, viewport.getScreenHeight(), 0));
        frustum.x = frustumCorner.x;
        frustum.y = frustumCorner.y;
        viewport.unproject(frustumCorner.set(viewport.getScreenWidth(), 0, 0));
        frustum.width = frustumCorner.x - frustum.x;
        frustum.height = frustumCorner.y - frustum.y;

        batch.setProjectionMatrix(viewport.getCamera().combined);
    }

    @Override
    protected void process(int entity) {
        final PositionC positionC = positionMapper.get(entity);
        final CameraFocusC cameraTrackerC = cameraTrackerMapper.get(entity);

        final float x = positionC.x;
        final float y = positionC.y;
        final float radiusVisibleAround = cameraTrackerC.radiusVisibleAround;

        final Rectangle thisEntityView = thisEntityFraming.set(x - radiusVisibleAround, y - radiusVisibleAround, radiusVisibleAround + radiusVisibleAround, radiusVisibleAround + radiusVisibleAround);
        if (firstEntity) {
            thisFrameFraming.set(thisEntityView);
            firstEntity = false;
        } else {
            thisFrameFraming.merge(thisEntityView);
        }
    }
}
