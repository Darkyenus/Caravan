package caravan.services;

import caravan.CaravanApplication;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.darkyen.retinazer.EngineService;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for {@link EngineService}s that have custom UIs.
 */
public interface UIService {

	/** Create your UI here. */
	void createUI(@NotNull CaravanApplication application, @NotNull Stage stage);

}
