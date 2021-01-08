package caravan.services;

import caravan.Inputs;
import caravan.components.CameraFocusC;
import caravan.components.Components;
import caravan.components.PositionC;
import caravan.input.BoundInputFunction;
import caravan.input.GameInput;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
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
    private final Rectangle currentFraming = new Rectangle();
    /** Like {@link #currentFraming}, but with zoom applied and the origin being in the center of the rectangle. */
    private final Rectangle zoomedCurrentFraming = new Rectangle();
    /** Current framing can get detached from the focus and be controlled independently. */
    private boolean currentFramingDetached = false;
    /** If the framing is detached, is it trying to catch up to the real value or not? (After it catches up, it will re-attach.) */
    private boolean currentFramingCatchUp = false;

    /** Last used frustum in world-space. */
    public final Rectangle lastFrustum = new Rectangle();

    private final float minVisibleUnits;

    /** The viewport defined for the world. */
    public final Viewport viewport;

    private final Vector3 unprojectTmp = new Vector3();

    /** The screen dimensions, used to setup the viewport. */
    public int screenWidth = 800, screenHeight = 800;
    /** World size in game units. */
    public final float worldWidth, worldHeight;

    private float zoomExponent = 0f;
    private static final float MIN_ZOOM_EXPONENT = -1f;
    private static final float MAX_ZOOM_EXPONENT = 4f;
    private static final float ZOOM_EXPONENT_STEP = 0.1f;

    private final BoundInputFunction scrollFunction;

    @Wire
    private Mapper<PositionC> positionMapper;
    @Wire
    private Mapper<CameraFocusC> cameraTrackerMapper;

    /**
     * @param minVisibleUnits Defines the maximum zoom level in terms of units in smaller dimension.
     */
    public CameraFocusSystem(float worldWidth, float worldHeight, float minVisibleUnits, @NotNull GameInput gameInput) {
        super(Components.DOMAIN.familyWith(PositionC.class, CameraFocusC.class));
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.minVisibleUnits = minVisibleUnits;
        viewport = new Viewport() {
            {
                setCamera(new OrthographicCamera());
            }

            @Override
            public void update(int screenWidth, int screenHeight, boolean centerCamera) {
                setScreenBounds(0, 0, screenWidth, screenHeight);
                final float worldWidth = getWorldWidth();
                final float worldHeight = getWorldHeight();
                final float widthScaleFactor = screenWidth * worldHeight / (screenHeight * worldWidth);
                if (widthScaleFactor >= 1f) {
                    setWorldWidth(worldWidth * widthScaleFactor);
                } else {
                    setWorldHeight(worldHeight / widthScaleFactor);
                }
                super.update(screenWidth, screenHeight, centerCamera);
            }
        };
        final Camera camera = viewport.getCamera();
        camera.direction.set(0f, 0f, -1f);
        camera.up.set(0f, 1f, 0f);
        camera.near = 0.5f;
        camera.far = 1.5f;

        scrollFunction = gameInput.use(Inputs.SCROLL);
        gameInput.use(Inputs.ZOOM_OUT, (times, pressed) -> {
            if (zoomExponent < MAX_ZOOM_EXPONENT) {
                zoomExponent = Math.min(MAX_ZOOM_EXPONENT, zoomExponent + ZOOM_EXPONENT_STEP * times);
                return true;
            }
            return false;
        });
        gameInput.use(Inputs.ZOOM_IN, (times, pressed) -> {
            if (zoomExponent > MIN_ZOOM_EXPONENT) {
                zoomExponent = Math.max(MIN_ZOOM_EXPONENT, zoomExponent - ZOOM_EXPONENT_STEP * times);
                return true;
            }
            return false;
        });
    }

    /** Switch whether the camera is attached to the focused entities or not. */
    public void setFree(boolean free) {
        if (free) {
            currentFramingDetached = true;
            currentFramingCatchUp = false;
        } else if (currentFramingDetached) {
            currentFramingCatchUp = true;
        }
    }

    public Vector3 unproject(int screenX, int screenY) {
        return viewport.unproject(unprojectTmp.set(screenX, screenY, 0f));
    }

    @Override
    public void update() {
        if (scrollFunction.isPressed()) {
            currentFramingDetached = true;
            final float shiftX;
            final float shiftY;
            {
                final Vector3 beforeShift = unproject(Gdx.input.getX() - Gdx.input.getDeltaX(), Gdx.input.getY() - Gdx.input.getDeltaY());
                final float beforeX = beforeShift.x;
                final float beforeY = beforeShift.y;
                // do not touch beforeShift after this, it is the same instance as afterShift
                final Vector3 afterShift = unproject(Gdx.input.getX(), Gdx.input.getY());
                shiftX = afterShift.x - beforeX;
                shiftY = afterShift.y - beforeY;
            }
            currentFraming.x -= shiftX;
            currentFraming.y -= shiftY;
        }

        if (currentFramingDetached && !currentFramingCatchUp) {
            // No need to follow anything when the framing is independent.
            // Just make sure that it is still focused on the game area
            float minX = currentFraming.x;
            float minY = currentFraming.y;
            float maxX = minX + currentFraming.width;
            float maxY = minY + currentFraming.height;
            final float minWorldX = 0 - currentFraming.width * 0.5f;
            final float maxWorldX = worldWidth + currentFraming.width * 0.5f;
            final float minWorldY = 0 - currentFraming.height * 0.5f;
            final float maxWorldY = worldHeight + currentFraming.height * 0.5f;
            minX = Math.max(minX, minWorldX);
            minY = Math.max(minY, minWorldY);
            maxX = Math.min(maxX, maxWorldX);
            maxY = Math.min(maxY, maxWorldY);
            currentFraming.set(minX, minY, maxX - minX, maxY - minY);
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
            currentFraming.x = MathUtils.lerp(currentFraming.x, thisFrameFraming.x, 0.2f);
            currentFraming.y = MathUtils.lerp(currentFraming.y, thisFrameFraming.y, 0.2f);
            currentFraming.width = MathUtils.lerp(currentFraming.width, thisFrameFraming.width, 0.2f);
            currentFraming.height = MathUtils.lerp(currentFraming.height, thisFrameFraming.height, 0.2f);
            if (MathUtils.isEqual(currentFraming.x, thisFrameFraming.x, 0.1f)
                    && MathUtils.isEqual(currentFraming.y, thisFrameFraming.y, 0.1f)
                    && MathUtils.isEqual(currentFraming.width, thisFrameFraming.width, 0.1f)
                    && MathUtils.isEqual(currentFraming.height, thisFrameFraming.height, 0.1f)) {
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
        final Rectangle currentFraming = this.zoomedCurrentFraming;
        currentFraming.set(this.currentFraming);
        // Apply zoom
        currentFraming.x += currentFraming.width * 0.5f;
        currentFraming.y += currentFraming.height * 0.5f;
        final float zoomFactor = (float) Math.pow(2f, zoomExponent);
        currentFraming.width *= zoomFactor;
        currentFraming.height *= zoomFactor;

        // Update viewport
        viewport.getCamera().position.set(currentFraming.x, currentFraming.y, 1);
        viewport.setWorldWidth(currentFraming.width);
        viewport.setWorldHeight(currentFraming.height);
        viewport.update(screenWidth, screenHeight);

        // Update frustum
        final Vector3 frustumCorner = viewport.unproject(unprojectTmp.set(0, viewport.getScreenHeight(), 0));
        frustum.x = frustumCorner.x;
        frustum.y = frustumCorner.y;
        viewport.unproject(frustumCorner.set(viewport.getScreenWidth(), 0, 0));
        frustum.width = frustumCorner.x - frustum.x;
        frustum.height = frustumCorner.y - frustum.y;
        lastFrustum.set(frustum);

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
