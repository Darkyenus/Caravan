package caravan.services;

import caravan.Inputs;
import caravan.components.Components;
import caravan.components.MoveC;
import caravan.components.PlayerC;
import caravan.components.PositionC;
import caravan.input.BoundInputFunction;
import caravan.input.GameInput;
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

    private final BoundInputFunction UP;
    private final BoundInputFunction DOWN;
    private final BoundInputFunction LEFT;
    private final BoundInputFunction RIGHT;

    public PlayerControlSystem(GameInput gameInput) {
        super(Components.DOMAIN.familyWith(PlayerC.class, PositionC.class, MoveC.class));
        UP = gameInput.use(Inputs.UP);
        DOWN = gameInput.use(Inputs.DOWN);
        LEFT = gameInput.use(Inputs.LEFT);
        RIGHT = gameInput.use(Inputs.RIGHT);
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

        final PositionC positionC = positionMapper.get(entity);
        final MoveC moveC = moveMapper.get(entity);

        moveC.speed = 2f;
        moveC.targetX = positionC.x;
        moveC.targetY = positionC.y;

        if (LEFT.isPressed()) {
            moveC.targetX -= 1f;
        } else if (RIGHT.isPressed()) {
            moveC.targetX += 1f;
        }
        if (UP.isPressed()) {
            moveC.targetY += 1f;
        } else if (DOWN.isPressed()) {
            moveC.targetY -= 1f;
        }
    }

}
