package caravan.services;

import caravan.components.CaravanC;
import caravan.components.Components;
import caravan.components.MoveC;
import caravan.components.PositionC;
import caravan.components.RenderC;
import com.darkyen.retinazer.Mapper;
import com.darkyen.retinazer.Wire;
import com.darkyen.retinazer.systems.EntityProcessorSystem;

import static caravan.world.Sprites.CARAVAN_DOWN;
import static caravan.world.Sprites.CARAVAN_RIGHT;
import static caravan.world.Sprites.CARAVAN_UP;

public class CaravanAnimationService extends EntityProcessorSystem {

    @Wire
    private Mapper<RenderC> rendererMapper;
    @Wire
    private Mapper<PositionC> positionMapper;
    @Wire
    private Mapper<MoveC> moveMapper;

    public CaravanAnimationService() {
        super(Components.DOMAIN.familyWith(PositionC.class, MoveC.class, CaravanC.class));
    }

    @Override
    protected void process(int entity) {
        final RenderC renderer = rendererMapper.get(entity);
        final MoveC move = moveMapper.get(entity);
        final PositionC position = positionMapper.get(entity);
        if (move.waypoints.size >= 3) {
            final float deltaX = move.waypoints.get(0) - position.x;
            final float deltaY = move.waypoints.get(1) - position.y;

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
