package caravan;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import org.jetbrains.annotations.NotNull;

/**
 * Screen that displays options, such as graphics settings and key rebinding.
 */
public final class OptionsScreen extends CaravanApplication.UIScreen {

	public OptionsScreen() {
		super(900, false, true);
	}

	@Override
	protected void initializeUI(@NotNull CaravanApplication application, @NotNull Table root) {
		root.align(Align.center);

		// TODO(jp): Actually show the options

		final TextButton back = new TextButton("Back", CaravanApplication.uiSkin());
		back.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				Options.save(application.saveDir());
				removeScreen(true);
			}
		});
		root.add(back);
	}
}
