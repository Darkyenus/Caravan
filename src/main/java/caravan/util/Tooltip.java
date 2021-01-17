package caravan.util;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;
import com.badlogic.gdx.utils.Array;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public final class Tooltip<T extends Actor & Layout> extends InputListener {

	private @Nullable Actor tooltipOwner;
	private @NotNull T tooltipActor;

	public Tooltip(@NotNull T tooltipActor) {
		this.tooltipActor = tooltipActor;
	}

	@NotNull
	public T getActor() {
		return tooltipActor;
	}

	public void setActor(@NotNull T actor) {
		if (tooltipActor == actor) {
			return;
		}
		final Stage stage = tooltipActor.getStage();
		if (stage != null && tooltipOwner != null) {
			final Array<Action> actions = tooltipActor.getActions();
			tooltipActor.remove();
			stage.addActor(actor);
			setTooltipBounds(actor, tooltipOwner);
			for (Action action : actions) {
				actor.addAction(action);
			}
			actions.clear();
		}
		tooltipActor = actor;
	}

	public void setParent(@Nullable Actor actor) {
		if (tooltipOwner == actor) {
			return;
		}

		if (tooltipOwner != null) {
			tooltipActor.clearActions();
			tooltipActor.remove();
			tooltipActor.removeListener(this);
		}
		tooltipOwner = actor;
		if (actor != null) {
			actor.addListener(this);
		}
	}

	@Override
	public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
		if (tooltipOwner == null) {
			return;
		}
		tooltipActor.clearActions();
		tooltipActor.remove();

		tooltipOwner.getStage().addActor(tooltipActor);
		setTooltipBounds(tooltipActor, tooltipOwner);
		tooltipActor.addAction(Actions.fadeIn(0.1f, Interpolation.fastSlow));
	}

	@Override
	public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
		if (tooltipOwner == null) {
			return;
		}
		tooltipActor.addAction(Actions.sequence(Actions.fadeOut(0.1f, Interpolation.slowFast), Actions.removeActor()));
	}

	private static final Vector2 TMP = new Vector2();

	private void setTooltipBounds(@NotNull T tooltip, @NotNull Actor tooltipOwner) {
		final Vector2 tooltipCoords = tooltipOwner.localToStageCoordinates(TMP.set(tooltipOwner.getWidth() * 0.5f, tooltipOwner.getHeight()));

		final float prefWidth = tooltip.getPrefWidth();
		final float prefHeight = tooltip.getPrefHeight();

		tooltip.setBounds(tooltipCoords.x - prefWidth * 0.5f, tooltipCoords.y, prefWidth, prefHeight);
	}
}
