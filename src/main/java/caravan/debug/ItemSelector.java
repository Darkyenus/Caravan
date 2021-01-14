package caravan.debug;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import java.util.Iterator;

import static caravan.util.Util.ALL_HANDLING_INPUT_LISTENER;
import static caravan.util.Util.newScrollPane;

/**
 * @author Darkyen
 */
public final class ItemSelector <T> extends Window {

    private final ItemSelected<T> selectedCallback;
    private final DrawItem<T> renderer;
    private final Table tilesTable;
    private final Iterator<T> filler;

    public ItemSelector(Skin skin, String title, ItemSelected<T> selectedCallback, DrawItem<T> renderer, Iterator<T> filler) {
        super(title, skin);
        addListener(ALL_HANDLING_INPUT_LISTENER);
        getTitleTable().padLeft(10f);

        this.selectedCallback = selectedCallback;
        this.renderer = renderer;
        this.filler = filler;
        tilesTable = new Table();
        tilesTable.defaults().space(2f);
        ScrollPane tileScrollPane = newScrollPane(tilesTable, "light");
        add(tileScrollPane).expand().fill();
        setTouchable(Touchable.enabled);
    }

    private final ClickListener ITEM_SELECTOR_CLICK_LISTENER = new ClickListener(){
        @Override
        public void clicked(InputEvent event, float x, float y) {
            assert event.getListenerActor() instanceof ItemSelectorTile;
            //noinspection unchecked
            selectedCallback.selected(((ItemSelectorTile<T>) event.getListenerActor()).item);
        }
    };

    boolean initialized = false;

    @Override
    public void validate() {
        if(!initialized){
            fillTilesTable(tilesTable);
            initialized = true;
            pack();
        }
        super.validate();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        final Stage stage = getStage();
        if(stage.getKeyboardFocus() == this){ //Revert Window's autofocusing behavior
            stage.setKeyboardFocus(null);
        }
    }

    private void fillTilesTable(Table t){
        int wrap = 8;
        int i = 1;
        while(filler.hasNext()){
            t.add(new ItemSelectorTile<>(filler.next(), ITEM_SELECTOR_CLICK_LISTENER, renderer));
            if((i % wrap) == 0)t.row();
            i++;
        }
    }

    private static class ItemSelectorTile<T> extends Widget {

        protected final T item;
        private final DrawItem<T> drawItem;

        private ItemSelectorTile(T item, ClickListener listener, DrawItem<T> drawItem) {
            this.item = item;
            this.drawItem = drawItem;
            addListener(listener);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            drawItem.draw(item, batch, getX(), getY(), getWidth());
        }

        @Override
        public float getPrefWidth() {
            return 32f;
        }

        @Override
        public float getPrefHeight() {
            return getPrefWidth();
        }
    }

    @FunctionalInterface
    public interface ItemSelected <T> {
        void selected(T selected);
    }

    public interface DrawItem <T> {
        void draw(T item, Batch batch, float x, float y, float size);
    }
}
