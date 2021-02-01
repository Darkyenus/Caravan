package caravan;

import caravan.components.PlayerC;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static caravan.CaravanApplication.uiSkin;
import static caravan.util.Util.newScrollPane;

public class NotesScreen extends CaravanApplication.UIScreen {
    @Nullable
    private PlayerC player;
    private TextArea playerNotes;
    private TextField.TextFieldStyle textFieldStyle;

    public NotesScreen() {
        super(500, false, true);
    }

    @Override
    protected void initializeUI(@NotNull CaravanApplication application, @NotNull Table root) {
        final Skin skin = uiSkin();
        backgroundColor.set(skin.getColor("p-beige"));

        textFieldStyle = skin.get("default", TextField.TextFieldStyle.class);

        root.pad(20f);

        final Table topBar = new Table(skin);

        final TextButton leaveButton = new TextButton("Leave", skin);
        leaveButton.pad(0, 10f, 0, 10f);
        leaveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                player.notes = playerNotes.getText();
                player = null;
                NotesScreen.this.removeScreen(false);
            }
        });
        topBar.add(leaveButton).align(Align.right).expandX();
        root.add(topBar).growX().colspan(2).padBottom(10f).row();

        playerNotes = new TextArea("", textFieldStyle);
        playerNotes.setPrefRows(30);
        playerNotes.setWidth(70);
        playerNotes.setHeight(100);


        final ScrollPane pane = newScrollPane(playerNotes);
        root.add(pane).growX().row();
        root.validate();
        root.invalidateHierarchy();
    }

    public void reset(@NotNull PlayerC player) {
        this.player = player;
        refresh();
    }

    public void refresh() {
        playerNotes.setText(player.notes != null ? player.notes : "");
    }

}

