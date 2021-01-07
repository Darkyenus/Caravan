package caravan.services;

import caravan.CaravanApplication;
import caravan.Inputs;
import caravan.TradingScreen;
import caravan.components.CaravanC;
import caravan.components.Components;
import caravan.components.MoveC;
import caravan.components.PlayerC;
import caravan.components.PositionC;
import caravan.components.TownC;
import caravan.input.BoundInputFunction;
import caravan.input.GameInput;
import caravan.util.PathFinding;
import caravan.util.Vec2;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.LongArray;
import com.darkyen.retinazer.EntitySetView;
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
    private Mapper<PlayerC> playerMapper;
	@Wire
	private Mapper<CaravanC> caravanMapper;
	@Wire
	private Mapper<TownC> townMapper;

    @Wire
    private SimulationService simulation;

    @Wire
    private MoveSystem moveSystem;
    @Wire
    private CameraFocusSystem cameraFocusSystem;
    @Wire
    private WorldService worldService;

    private EntitySetView towns;

    private final BoundInputFunction UP;
    private final BoundInputFunction DOWN;
    private final BoundInputFunction LEFT;
    private final BoundInputFunction RIGHT;

    private final BoundInputFunction MOVE;

    private final CaravanApplication application;
    private final TradingScreen tradingScreen;

    /** Used for alternating direction */
    private boolean nextMoveVertical = false;
    private boolean directionalMove = false;

    public PlayerControlSystem(CaravanApplication application, GameInput gameInput) {
        super(Components.DOMAIN.familyWith(PlayerC.class, PositionC.class, MoveC.class, CaravanC.class));
        this.application = application;

        UP = gameInput.use(Inputs.UP);
        DOWN = gameInput.use(Inputs.DOWN);
        LEFT = gameInput.use(Inputs.LEFT);
        RIGHT = gameInput.use(Inputs.RIGHT);

        MOVE = gameInput.use(Inputs.MOVE);

        tradingScreen = new TradingScreen();
    }

    @Override
    public void initialize() {
        super.initialize();
        towns = engine.getEntities(Components.DOMAIN.familyWith(PositionC.class, TownC.class));
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

        final PositionC position = positionMapper.get(entity);
        final int originTileX = MathUtils.floor(position.x);
        final int originTileY = MathUtils.floor(position.y);
        final MoveC move = moveMapper.get(entity);

        float speed = 2f;

        if (LEFT.isPressed() || RIGHT.isPressed() || UP.isPressed() || DOWN.isPressed()) {
            int deltaX = 0;
            int deltaY = 0;

            if (LEFT.isPressed()) {
                deltaX = -1;
            } else if (RIGHT.isPressed()) {
                deltaX = +1;
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
            moveSystem.addTileMoveWaypoint(entity, deltaX, deltaY,
                    worldService.defaultPathWorld.movementSpeedMultiplier(originTileX, originTileY) * speed,
                    worldService.defaultPathWorld.movementSpeedMultiplier(originTileX + deltaX, originTileY + deltaY) * speed);
            directionalMove = true;
            cameraFocusSystem.setFree(false);
            playerC.openTradeOnArrival = true;
        } else if (directionalMove) {
            // Stop movement after keys are released
            move.waypoints.clear();
            directionalMove = false;
        }

        if (!directionalMove && MOVE.isPressed()) {
            // Move to clicked location
            final Vector3 clickedTarget = cameraFocusSystem.unproject(Gdx.input.getX(), Gdx.input.getY());
            final int targetTileX = MathUtils.floor(clickedTarget.x);
            final int targetTileY = MathUtils.floor(clickedTarget.y);

            final LongArray path = new LongArray(1);
            path.add(Vec2.make(targetTileX, targetTileY));
            final PathFinding.Path foundPath = worldService.pathFinding.findPath(Vec2.make(originTileX, originTileY), path.get(0), path);

            move.waypoints.clear();
            if (foundPath != null) {
                int lastX = originTileX;
                int lastY = originTileY;
                float tileSpeed0 = worldService.defaultPathWorld.movementSpeedMultiplier(lastX, lastY) * speed;

                for (int i = 0; i < foundPath.length(); i++) {
                    final int x = foundPath.nodeX(i);
                    final int y = foundPath.nodeY(i);
                    float tileSpeed1 = worldService.defaultPathWorld.movementSpeedMultiplier(x, y) * speed;
                    moveSystem.addTileMoveWaypoint(entity, x - lastX, y - lastY, tileSpeed0, tileSpeed1);
                    lastX = x;
                    lastY = y;
                    tileSpeed0 = tileSpeed1;
                }

                playerC.openTradeOnArrival = true;
            }
            cameraFocusSystem.setFree(false);
        }

        if (move.waypoints.size == 0 && playerC.openTradeOnArrival) {
	        int town = -1;
            float townDistance = 1.5f;

            final IntArray townIndices = towns.getIndices();
            for (int i = 0; i < townIndices.size; i++) {
                final int townEntity = townIndices.get(i);
                final PositionC townPos = positionMapper.get(townEntity);
                final float distance = PositionC.manhattanDistance(townPos, position);
                if (distance < townDistance) {
                    town = townEntity;
                    townDistance = distance;
                }
            }

            if (town != -1) {
                if (application.addScreen(tradingScreen)) {
                    tradingScreen.reset(townMapper.get(town), caravanMapper.get(entity));
                    playerC.openTradeOnArrival = false;
                }
            } else {
                playerC.openTradeOnArrival = false;
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
