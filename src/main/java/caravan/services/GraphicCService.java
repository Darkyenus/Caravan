package caravan.services;

import caravan.components.Components;
import caravan.components.MoveC;
import caravan.components.PositionC;
import caravan.components.RenderC;
import com.darkyen.retinazer.Family;
import com.darkyen.retinazer.Mapper;
import com.darkyen.retinazer.Wire;
import com.darkyen.retinazer.systems.EntityProcessorSystem;
import org.jetbrains.annotations.NotNull;

import static caravan.world.Sprites.*;

public class GraphicCService extends EntityProcessorSystem {
    @Wire
    private Mapper<RenderC> rendererMapper;
    @Wire
    private Mapper<PositionC> positionMapper;
    @Wire
    private Mapper<MoveC> moveMapper;

    public GraphicCService() {
        super(Components.DOMAIN.familyWith(PositionC.class, MoveC.class));
    }

    @Override
    protected void process(int entity) {
        final RenderC renderer = rendererMapper.get(entity);
        final MoveC move = moveMapper.get(entity);
        final PositionC position = positionMapper.get(entity);
        if (move.waypoints.size >= 3) {
            final float targetX = move.waypoints.get(0);
            final float targetY = move.waypoints.get(1);
            final float deltaX = targetX - position.x;
            final float deltaY = targetY - position.y;
            if (deltaX > 0) {
                renderer.set(CARAVAN_RIGHT);
                renderer.scaleX = 1;
            } else if (deltaX < 0) {
                renderer.set(CARAVAN_RIGHT);
                renderer.scaleX = -1;
            } else if (deltaY > 0) {
                renderer.set(CARAVAN_UP);
            } else {
                renderer.set(CARAVAN_DOWN);
            }

        }
    }
}
