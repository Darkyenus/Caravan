package caravan.services;

import caravan.CaravanApplication;
import caravan.Inputs;
import caravan.NotesScreen;
import caravan.TradingScreen;
import caravan.components.CaravanC;
import caravan.components.Components;
import caravan.components.MoveC;
import caravan.components.PlayerC;
import caravan.components.PositionC;
import caravan.components.TownC;
import caravan.input.BoundInputFunction;
import caravan.input.GameInput;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
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
    private TimeService timeService;

    @Wire
    private CameraFocusSystem cameraFocusSystem;
    @Wire
    private TownSystem townSystem;
    @Wire
    private WorldService worldService;

    private final BoundInputFunction UP;
    private final BoundInputFunction DOWN;
    private final BoundInputFunction LEFT;
    private final BoundInputFunction RIGHT;

    private final BoundInputFunction MOVE;
    private final BoundInputFunction NOTES;

    private final CaravanApplication application;
    private final TradingScreen tradingScreen;
    private final NotesScreen notesScreen;

    /** Used for alternating direction */
    private boolean nextMoveVertical = false;
    private static final float DIRECTIONAL_MOVE_ALTERNATION_DELAY = 0.3f;
    private float nextMoveVerticalChangeCountdown = DIRECTIONAL_MOVE_ALTERNATION_DELAY;
    private boolean directionalMove = false;

    public PlayerControlSystem(CaravanApplication application, GameInput gameInput) {
        super(Components.DOMAIN.familyWith(PlayerC.class, PositionC.class, MoveC.class, CaravanC.class));
        this.application = application;

        UP = gameInput.use(Inputs.UP);
        DOWN = gameInput.use(Inputs.DOWN);
        LEFT = gameInput.use(Inputs.LEFT);
        RIGHT = gameInput.use(Inputs.RIGHT);
        MOVE = gameInput.use(Inputs.MOVE);
        NOTES = gameInput.use(Inputs.NOTES);

        tradingScreen = new TradingScreen();
        notesScreen = new NotesScreen();
    }

    @Override
    protected void process(int entity) {
        final PlayerC playerC = playerMapper.get(entity);
        if (!playerC.selected) {
            return;
        }

        final PositionC position = positionMapper.get(entity);
        final CaravanC caravan = caravanMapper.get(entity);
        final MoveC move = moveMapper.get(entity);

        float speed = caravan.speed;

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
                nextMoveVerticalChangeCountdown -= timeService.gameDelta;
                if (nextMoveVerticalChangeCountdown <= 0f) {
                    nextMoveVerticalChangeCountdown += DIRECTIONAL_MOVE_ALTERNATION_DELAY;
                    nextMoveVertical = !nextMoveVertical;
                }
            } else if (deltaX != 0) {
                nextMoveVertical = true;
                nextMoveVerticalChangeCountdown = DIRECTIONAL_MOVE_ALTERNATION_DELAY;
            } else {
                nextMoveVertical = false;
                nextMoveVerticalChangeCountdown = DIRECTIONAL_MOVE_ALTERNATION_DELAY;
            }

            move.waypoints.clear();

            final int originTileX = MathUtils.floor(position.x);
            final int originTileY = MathUtils.floor(position.y);
            MoveSystem.addTileMoveWaypoint(position, move, deltaX, deltaY,
                    worldService.defaultPathWorld.movementSpeedMultiplier(originTileX, originTileY) * speed,
                    worldService.defaultPathWorld.movementSpeedMultiplier(originTileX + deltaX, originTileY + deltaY) * speed);
            directionalMove = true;
            cameraFocusSystem.setFree(false);
            playerC.openTradeOnArrival = true;
            timeService.requestResume();
        } else if (directionalMove) {
            // Stop movement after keys are released
            move.waypoints.clear();
            directionalMove = false;
        }
        if (NOTES.isJustReleased()){
            if (application.addScreen(notesScreen)){
                notesScreen.reset(playerC);
            }
        }
        if (!directionalMove && MOVE.isJustPressed()) {
            // Move to clicked location
            final Vector3 clickedTarget = cameraFocusSystem.unproject(Gdx.input.getX(), Gdx.input.getY());
            final int targetTileX = MathUtils.floor(clickedTarget.x);
            final int targetTileY = MathUtils.floor(clickedTarget.y);

            if (worldService.addMovePathTo(position, move, speed, targetTileX, targetTileY)) {
                playerC.openTradeOnArrival = true;
                timeService.requestResume();
            } else {
                move.waypoints.clear();
            }

            cameraFocusSystem.setFree(false);
        }

        if (move.waypoints.size == 0 && playerC.openTradeOnArrival) {
            timeService.requestPause();
            final int townEntity = townSystem.getNearbyTown(position);
            if (townEntity != -1) {
                final TownC town = townMapper.get(townEntity);
                caravan.priceMemory.remember(timeService.day, townEntity, town);

                if (application.addScreen(tradingScreen)) {
                    tradingScreen.reset(townMapper, town, caravan);
                    playerC.openTradeOnArrival = false;
                }
            } else {
                playerC.openTradeOnArrival = false;
            }
        }
    }

}
