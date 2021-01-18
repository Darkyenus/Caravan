package caravan.util;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static caravan.CaravanApplication.uiSkin;

/**
 * Confirmation window.
 */
public final class ConfirmWindow extends Window {

	private final Label messageLabel;
	private final Label yesButtonLabel;
	private final Label noButtonLabel;

	private @Nullable Runnable yesAction;
	private @Nullable Runnable noAction;

	public ConfirmWindow() {
		super("Confirmation", uiSkin(), "dialog");
		setModal(true);

		final Skin skin = uiSkin();
		messageLabel = new Label("", skin);
		final TextButton yesButton = new TextButton("Yes", skin);
		final TextButton noButton = new TextButton("No", skin);
		yesButtonLabel = yesButton.getLabel();
		noButtonLabel = noButton.getLabel();

		getTitleTable().padLeft(10f);

		add(messageLabel).colspan(2).growX().fillY().minHeight(20f).pad(10f).padBottom(Value.zero).row();
		add(yesButton).growX().pad(10f);
		add(noButton).growX().pad(10f).row();
		align(Align.center);

		yesButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (yesAction != null) {
					yesAction.run();
				}
				ConfirmWindow.this.remove();
			}
		});

		noButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (noAction != null) {
					noAction.run();
				}
				ConfirmWindow.this.remove();
			}
		});
	}

	public void setYesText(@NotNull String text) {
		yesButtonLabel.setText(text);
	}

	public void setNoText(@NotNull String text) {
		noButtonLabel.setText(text);
	}

	public void setMessageText(@NotNull String text) {
		messageLabel.setText(text);
	}

	public void setYesAction(@Nullable Runnable action) {
		this.yesAction = action;
	}

	public void setNoAction(@Nullable Runnable action) {
		this.noAction = action;
	}

	public void show(@NotNull Stage stage) {
		stage.addActor(this);
		pack();
		setPosition((stage.getWidth() - getWidth()) * 0.5f,(stage.getHeight() - getHeight()) * 0.5f);
	}
}
