package caravan.components;

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
			RenderC.class
	);

}