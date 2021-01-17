package caravan.components;

import caravan.util.CaravanComponent;
import com.badlogic.gdx.utils.ObjectSet;
import com.darkyen.retinazer.ComponentSet;

/**
 * Things related to all components.
 */
public final class Components {

	/** The list of all components. */
	public static final ComponentSet DOMAIN = new ComponentSet(
			PositionC.class,
			MoveC.class,
			PlayerC.class,
			CameraFocusC.class,
			RenderC.class,
			TitleC.class,
			TownC.class,
			CaravanC.class,
			CaravanAIC.class
	);

	static {
		final ObjectSet<String> names = new ObjectSet<>();
		for (int i = 0; i < DOMAIN.size(); i++) {
			if (!CaravanComponent.class.isAssignableFrom(DOMAIN.component(i))) {
				throw new AssertionError(DOMAIN.component(i)+" does not inherit "+CaravanComponent.class);
			}
			final CaravanComponent.Serialized annotation = DOMAIN.component(i).getAnnotation(CaravanComponent.Serialized.class);
			if (annotation == null) {
				throw new AssertionError(DOMAIN.component(i)+" is not annotated with "+CaravanComponent.Serialized.class);
			}
			if (annotation.version() < 1) {
				throw new AssertionError(DOMAIN.component(i)+" serialized version is invalid");
			}
			if (!names.add(annotation.name())) {
				throw new AssertionError(DOMAIN.component(i)+" serialization name is not unique");
			}
		}
	}

}
