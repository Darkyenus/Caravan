package caravan.services;

import caravan.components.Components;
import caravan.components.MoveC;
import caravan.components.PositionC;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.darkyen.retinazer.*;
import com.darkyen.retinazer.systems.EntityProcessorSystem;
import org.jetbrains.annotations.NotNull;

/**
 * System that moves entities with {@link MoveC} and {@link PositionC} as they desire.
 */
public final class MoveSystem extends EntityProcessorSystem {

    @Wire
    private Mapper<PositionC> positionMapper;
    @Wire
    private Mapper<MoveC> moveMapper;

    @Wire
    private TimeService simulation;

    public MoveSystem() {
        super(Components.DOMAIN.familyWith(PositionC.class, MoveC.class));
    }

    @Override
    public void update() {
        if (simulation.gameDelta > 0f) {
            super.update();
        }
    }

    private final Vector2 tmp_move = new Vector2();

    @Override
    protected void process(int entity) {
        float delta = simulation.gameDelta;

        final PositionC position = positionMapper.get(entity);
        final FloatArray path = moveMapper.get(entity).waypoints;

        while (path.size >= 3) {
            final float targetX = path.get(0);
            final float targetY = path.get(1);
            final float speed = path.get(2);

            final float maxPossibleMove = speed * delta;
            final float maxPossibleMove2 = maxPossibleMove * maxPossibleMove;

            final Vector2 requiredMove = tmp_move.set(targetX - position.x, targetY - position.y);
            final float requiredMoveLen2 = requiredMove.len2();
            if (maxPossibleMove2 >= requiredMoveLen2) {
                // Full move is possible
                position.x = targetX;
                position.y = targetY;
                // Move done, prepare move moving
                path.removeRange(0, 2);
                delta -= Math.sqrt(requiredMoveLen2) / speed;
            } else {
                // Only part of the move is possible
                final float requiredMoveScale = maxPossibleMove / (float) Math.sqrt(requiredMoveLen2);
                requiredMove.scl(requiredMoveScale);
                position.x += requiredMove.x;
                position.y += requiredMove.y;
                break; // No more moving
            }
        }
    }

    public static void addTileMoveWaypoint(@NotNull PositionC position, @NotNull MoveC move, int deltaX, int deltaY, float speedTile0, float speedTile1) {
        float previousX;
        float previousY;
        if (move.waypoints.size > 0) {
            previousX = move.waypoints.items[move.waypoints.size - 3];
            previousY = move.waypoints.items[move.waypoints.size - 2];
        } else {
            previousX = position.x;
            previousY = position.y;
        }

        if (deltaX != 0) {
            // Moving right or left
            float targetX = MathUtils.floor(previousX) + 0.5f + deltaX;
            move.addWaypoint(MathUtils.round((previousX + targetX) * 0.5f), previousY, speedTile0);
            move.addWaypoint(targetX, previousY, speedTile1);
        } else if (deltaY != 0) {
            float targetY = MathUtils.floor(previousY) + 0.5f + deltaY;
            move.addWaypoint(previousX, MathUtils.round((previousY + targetY) * 0.5f), speedTile0);
            move.addWaypoint(previousX, targetY, speedTile1);
        }
    }
}
