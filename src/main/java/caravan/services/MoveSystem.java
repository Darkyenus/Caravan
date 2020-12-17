package caravan.services;

import caravan.components.Components;
import caravan.components.MoveC;
import caravan.components.PositionC;
import com.badlogic.gdx.math.Vector2;
import com.darkyen.retinazer.*;
import com.darkyen.retinazer.systems.EntityProcessorSystem;

/**
 * System that moves entities with {@link MoveC} and {@link PositionC} as they desire.
 */
public final class MoveSystem extends EntityProcessorSystem {

    @Wire
    private Mapper<PositionC> positionMapper;
    @Wire
    private Mapper<MoveC> moveMapper;

    @Wire
    private SimulationService simulation;

    public MoveSystem() {
        super(Components.DOMAIN.familyWith(PositionC.class, MoveC.class));
    }

    @Override
    public void update() {
        if (simulation.simulating) {
            super.update();
        }
    }

    private final Vector2 tmp_move = new Vector2();

    @Override
    protected void process(int entity) {
        float delta = simulation.delta;

        final PositionC position = positionMapper.get(entity);
        final MoveC move = moveMapper.get(entity);

        final float maxPossibleMove = move.speed * delta;
        final float maxPossibleMove2 = maxPossibleMove * maxPossibleMove;

        final Vector2 requiredMove = tmp_move.set(move.targetX - position.x, move.targetY - position.y);
        final float requiredMoveLen2 = requiredMove.len2();
        if (maxPossibleMove2 >= requiredMoveLen2) {
            // Full move is possible
            position.x = move.targetX;
            position.y = move.targetY;
        } else {
            // Only part of the move is possible
            final float requiredMoveScale = maxPossibleMove / (float) Math.sqrt(requiredMoveLen2);
            requiredMove.scl(requiredMoveScale);
            position.x += requiredMove.x;
            position.y += requiredMove.y;
        }
    }
}
