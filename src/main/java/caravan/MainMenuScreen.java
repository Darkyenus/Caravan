package caravan;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import org.jetbrains.annotations.NotNull;

/**
 * The main menu of the game.
 */
public final class MainMenuScreen extends CaravanApplication.UIScreen {

	@Override
	protected void initializeUI(@NotNull CaravanApplication application, @NotNull Table root) {
		root.align(Align.center);

		root.add(new Label("Caravan", CaravanApplication.uiSkin(), "title-large"))
				.align(Align.center)
				.pad(30f)
				.row();

		final TextButton playButton = new TextButton("Play", CaravanApplication.uiSkin());
		playButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				application.setScreen(new GameScreen());
			}
		});
		root.add(playButton)
				.align(Align.center)
				.pad(10f)
				.row();

		final TextButton optionsButton = new TextButton("Options", CaravanApplication.uiSkin());
		optionsButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				application.addScreen(new OptionsScreen());
			}
		});
		root.add(optionsButton)
				.align(Align.center)
				.pad(10f)
				.row();

		if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
			final TextButton quitButton = new TextButton("Quit", CaravanApplication.uiSkin());
			quitButton.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					Gdx.app.exit();
				}
			});
			root.add(quitButton)
					.align(Align.center)
					.pad(10f)
					.row();
		}
	}

}
