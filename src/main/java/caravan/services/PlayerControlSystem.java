package caravan.services;

import caravan.Inputs;
import caravan.components.Components;
import caravan.components.MoveC;
import caravan.components.PlayerC;
import caravan.components.PositionC;
import caravan.input.BoundInputFunction;
import caravan.input.GameInput;
import caravan.util.PathFinding;
import caravan.util.Vec2;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.LongArray;
import com.darkyen.retinazer.Mapper;
import com.darkyen.retinazer.Wire;
import com.darkyen.retinazer.systems.EntityProcessorSystem;

/**
 * A system for controlling the player movement on the map.
 */
public final class PlayerControlSystem extends EntityProcessorSystem {

    @Wire
    private Mapper<PositionC> positionMapper;
    @Wire
    private Mapper<MoveC> moveMapper;
    @Wire
    private Mapper<PlayerC>   playerMapper;

    @Wire
    private SimulationService simulation;

    @Wire
    private MoveSystem moveSystem;
    @Wire
    private CameraFocusSystem cameraFocusSystem;
    @Wire
    private WorldService worldService;

    private final BoundInputFunction UP;
    private final BoundInputFunction DOWN;
    private final BoundInputFunction LEFT;
    private final BoundInputFunction RIGHT;

    private final BoundInputFunction MOVE;

    /** Used for alternating direction */
    private boolean nextMoveVertical = false;
    private boolean directionalMove = false;

    public PlayerControlSystem(GameInput gameInput) {
        super(Components.DOMAIN.familyWith(PlayerC.class, PositionC.class, MoveC.class));
        UP = gameInput.use(Inputs.UP);
        DOWN = gameInput.use(Inputs.DOWN);
        LEFT = gameInput.use(Inputs.LEFT);
        RIGHT = gameInput.use(Inputs.RIGHT);

        MOVE = gameInput.use(Inputs.MOVE);
    }

    @Override
    public void update() {
        if (simulation.simulating) {
            super.update();
        }
    }

    @Override
    protected void process(int entity) {
        final PlayerC playerC = playerMapper.get(entity);
        if (!playerC.selected) {
            return;
        }

        final MoveC move = moveMapper.get(entity);

        float speed = 2f;

        if (LEFT.isPressed() || RIGHT.isPressed() || UP.isPressed() || DOWN.isPressed()) {
            int deltaX = 0;
            int deltaY = 0;

            if (LEFT.isPressed()) {
                deltaX = -1;
            } else if (RIGHT.isPressed()) {
                deltaX = 1;
            }
            if (UP.isPressed()) {
                deltaY = +1;
            } else if (DOWN.isPressed()) {
                deltaY = -1;
            }

            if (deltaX != 0 && deltaY != 0) {
                if (nextMoveVertical) {
                    deltaX = 0;
                } else {
                    deltaY = 0;
                }
                nextMoveVertical = !nextMoveVertical;
            }

            move.waypoints.clear();
            moveSystem.addTileMoveWaypoint(entity, deltaX, deltaY, speed);
            directionalMove = true;
        } else if (directionalMove) {
            // Stop movement after keys are released
            move.waypoints.clear();
            directionalMove = false;
        }

        if (!directionalMove && MOVE.isPressed()) {
            // Move to clicked location
            final PositionC position = positionMapper.get(entity);
            final int originTileX = MathUtils.floor(position.x);
            final int originTileY = MathUtils.floor(position.y);

            final Vector3 clickedTarget = cameraFocusSystem.unproject(Gdx.input.getX(), Gdx.input.getY());
            final int targetTileX = MathUtils.floor(clickedTarget.x);
            final int targetTileY = MathUtils.floor(clickedTarget.y);

            final LongArray path = new LongArray(1);
            path.add(Vec2.make(targetTileX, targetTileY));
            final PathFinding.Path foundPath = worldService.pathFinding.findPathWithMaxComplexity(Vec2.make(originTileX, originTileY), path.get(0), path, 3f);

            move.waypoints.clear();
            if (foundPath != null) {
                int lastX = originTileX;
                int lastY = originTileY;

                for (int i = 0; i < foundPath.length(); i++) {
                    final int x = foundPath.nodeX(i);
                    final int y = foundPath.nodeY(i);
                    moveSystem.addTileMoveWaypoint(entity, x - lastX, y - lastY, speed);
                    lastX = x;
                    lastY = y;
                }
            }
        }
    }

    // TODO(jp): implement for NPC caravans
    /** Add waypoints to move the entity from its current position to the specified position. */
    private void moveTo(int entity, float x, float y) {
        final PositionC position = positionMapper.get(entity);
        final MoveC move = moveMapper.get(entity);

        move.waypoints.clear();
        
    }

}
