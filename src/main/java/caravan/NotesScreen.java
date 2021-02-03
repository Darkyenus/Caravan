package caravan;

import caravan.components.PlayerC;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Pools;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static caravan.CaravanApplication.uiSkin;
import static caravan.util.Util.newScrollPane;

/**
 * Screen on which the player can keep their notes.
 */
public final class NotesScreen extends CaravanApplication.UIScreen {

	private @Nullable PlayerC player;
	private @Nullable TextArea playerNotes;

	public NotesScreen() {
		super(500, false, true);
	}

	@Override
	protected void initializeUI(@NotNull CaravanApplication application, @NotNull Table root) {
		final Skin skin = uiSkin();
		backgroundColor.set(skin.getColor("p-beige"));

		root.pad(20f);

		final Table topBar = new Table(skin);

		final TextButton leaveButton = new TextButton("Leave", skin);
		leaveButton.pad(0, 10f, 0, 10f);
		leaveButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				close();
			}
		});

		topBar.add("Notes", "title-medium");
		topBar.add(leaveButton).align(Align.right).expandX();
		root.add(topBar).growX().padBottom(10f).row();

		playerNotes = new TextArea("", skin, "no-background") {
            @Override
            public float getPrefHeight() {
                return getLines() * getStyle().font.getLineHeight();
            }

            @Override
            protected void calculateOffsets() {
                final int oldLines = getLines();
                super.calculateOffsets();
                if (getLines() != oldLines) {
                    invalidateHierarchy();
                }
            }

            private int oldCursorLine = -1;

            @Override
            public void act(float delta) {
                super.act(delta);

                final int cursorLine = getCursorLine();
                if (cursorLine != oldCursorLine) {
                    oldCursorLine = cursorLine;

                    ChangeListener.ChangeEvent changeEvent = Pools.obtain(ChangeListener.ChangeEvent.class);
                    fire(changeEvent);
                    Pools.free(changeEvent);
                }
            }
        };

        final Container<TextArea> playerNotesPadding = new Container<>(playerNotes);
        playerNotesPadding.pad(10f).fill();

        final ScrollPane pane = newScrollPane(playerNotesPadding);
		root.add(pane).grow().row();
		root.validate();
		root.invalidateHierarchy();

		// TODO(jp): Proper scrolling
		/*playerNotes.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                final float lineHeight = playerNotes.getStyle().font.getLineHeight();
                final float scrollY = 0;//pane.getHeight() - playerNotes.getCursorLine() * lineHeight;
                pane.scrollTo(0, scrollY, 0, lineHeight);
            }
        });*/
	}

	public void reset(@NotNull PlayerC player) {
		this.player = player;
		if (playerNotes != null) {
            playerNotes.setText(player.notes);
            if (playerNotes.hasKeyboardFocus()) {
                playerNotes.getStage().setKeyboardFocus(null);
            }
        }
	}

	public void close() {
        if (player != null && playerNotes != null) {
            player.notes = playerNotes.getText();
            player = null;
        }
        NotesScreen.this.removeScreen(false);
    }

    @Override
    public boolean keyUp(int keycode) {
        final boolean processed = super.keyUp(keycode);
        if (!processed && (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.N)) {
            close();
            return true;
        }
        return processed;
    }

    @Override
    public void dispose() {
	    close();
    }
}

