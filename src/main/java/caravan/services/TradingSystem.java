package caravan.services;

import caravan.CaravanApplication;
import caravan.components.CaravanC;
import caravan.components.Components;
import caravan.components.PlayerC;
import caravan.components.PositionC;
import caravan.components.TownC;
import caravan.world.Merchandise;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.IntArray;
import com.darkyen.retinazer.EntitySetView;
import com.darkyen.retinazer.EntitySystem;
import com.darkyen.retinazer.Mapper;
import com.darkyen.retinazer.Wire;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public final class TradingSystem extends EntitySystem implements UIService {

	@Wire
	private Mapper<PositionC> position;
	@Wire
	private Mapper<PlayerC> player;
	@Wire
	private Mapper<CaravanC> caravan;
	@Wire
	private Mapper<TownC> town;

	public TradingSystem() {
		super(Components.DOMAIN.familyWith(PositionC.class, PlayerC.class, CaravanC.class));
	}

	private EntitySetView towns;

	@Override
	public void initialize() {
		super.initialize();
		towns = engine.getEntities(Components.DOMAIN.familyWith(PositionC.class, TownC.class));
	}

	private int tradingPlayerEntity = -1;
	private int tradingWithTown = -1;
	private int tradingWithTownModificationCounter = Integer.MAX_VALUE;
	private Window tradingWindow;

	private final TextButton[] buyButtons = new TextButton[Merchandise.VALUES.length];
	private final TextButton[] sellButtons = new TextButton[Merchandise.VALUES.length];
	private final Label[] inventoryLabels = new Label[Merchandise.VALUES.length];

	@Override
	public void createUI(@NotNull CaravanApplication application, @NotNull Stage stage) {
		tradingWindow = new Window("Trading", CaravanApplication.uiSkin());
		tradingWindow.setVisible(false);
		stage.addActor(tradingWindow);
		tradingWindow.setTouchable(Touchable.enabled);
		tradingWindow.getTitleLabel().setAlignment(Align.center);

		final Table merchTable = new Table(CaravanApplication.uiSkin());
		merchTable.defaults().pad(2f);
		final ScrollPane pane = new ScrollPane(merchTable, CaravanApplication.uiSkin());
		tradingWindow.add(pane).grow();
		pane.setFadeScrollBars(false);
		pane.setFlickScroll(false);
		pane.setScrollingDisabled(true, false);
		pane.setSmoothScrolling(true);

		final ChangeListener buyListener = new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (tradingWithTown != -1 && tradingPlayerEntity != -1) {
					final Merchandise merchandise = (Merchandise) actor.getUserObject();
					if (!performBuy(tradingWithTown, tradingPlayerEntity, merchandise)) {
						Gdx.app.log("TradingSystem", "Buy of " + merchandise + " failed");
					}
				}
			}
		};
		final ChangeListener sellListener = new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (tradingWithTown != -1 && tradingPlayerEntity != -1) {
					final Merchandise merchandise = (Merchandise) actor.getUserObject();
					if (!performSell(tradingWithTown, tradingPlayerEntity, merchandise)) {
						Gdx.app.log("TradingSystem", "Sell of " + merchandise + " failed");
					}
				}
			}
		};

		int m = 0;
		for (Merchandise merchandise : Merchandise.VALUES) {
			final Label nameLabel = new Label(merchandise.name, CaravanApplication.uiSkin());
			final TextButton buyButton = new TextButton("Buy 000", CaravanApplication.uiSkin());
			final TextButton sellButton = new TextButton("Sell 000", CaravanApplication.uiSkin());
			final Label inventoryLabel = new Label("0", CaravanApplication.uiSkin());

			nameLabel.setAlignment(Align.right);

			merchTable.add(nameLabel).align(Align.right);
			merchTable.add(buyButton);
			merchTable.add(sellButton);
			merchTable.add(inventoryLabel);
			merchTable.row();

			buyButtons[m] = buyButton;
			sellButtons[m] = sellButton;
			inventoryLabels[m] = inventoryLabel;
			m++;

			buyButton.setUserObject(merchandise);
			buyButton.addListener(buyListener);
			sellButton.setUserObject(merchandise);
			sellButton.addListener(sellListener);
		}

		tradingWindow.pack();
		tradingWindow.setSize(merchTable.getPrefWidth() + 60f, 600f);
	}

	private boolean performBuy(int townEntity, int caravanEntity, @NotNull Merchandise m) {
		final TownC town = this.town.get(townEntity);
		final CaravanC caravan = this.caravan.get(caravanEntity);
		final int buyPrice = town.prices.buyPrice(m);
		if (buyPrice > caravan.money) {
			return false;
		}
		town.money += buyPrice;
		caravan.money -= buyPrice;
		caravan.inventory.add(m, 1);
		town.prices.buyUnit(m);
		town.modificationCounter++;
		return true;
	}

	private boolean performSell(int townEntity, int caravanEntity, @NotNull Merchandise m) {
		final TownC town = this.town.get(townEntity);
		final CaravanC caravan = this.caravan.get(caravanEntity);
		if (caravan.inventory.get(m) <= 0) {
			return false;
		}
		final int sellPrice = Math.min(town.prices.sellPrice(m), town.money);
		town.money -= sellPrice;
		caravan.money += sellPrice;
		caravan.inventory.add(m, -1);
		town.prices.sellUnit(m);
		town.modificationCounter++;
		return true;
	}

	private int getPlayer() {
		final IntArray playerIndices = getEntities().getIndices();
		for (int i = 0; i < playerIndices.size; i++) {
			final int entity = playerIndices.get(i);
			if (player.get(entity).selected) {
				return entity;
			}
		}
		return -1;
	}

	private int getNearbyTown(int player) {
		final PositionC playerPos = position.get(player);

		int town = -1;
		float townDistance = 1.5f;

		final IntArray townIndices = towns.getIndices();
		for (int i = 0; i < townIndices.size; i++) {
			final int townEntity = townIndices.get(i);
			final PositionC townPos = position.get(townEntity);
			final float distance = PositionC.manhattanDistance(townPos, playerPos);
			if (distance < townDistance) {
				town = townEntity;
				townDistance = distance;
			}
		}

		return town;
	}

	@Override
	public void update() {
		final int playerEntity = getPlayer();
		tradingPlayerEntity = playerEntity;
		final int townEntity = playerEntity == -1 ? -1 : getNearbyTown(playerEntity);
		if (townEntity == -1) {
			tradingWithTown = -1;
			tradingWindow.setVisible(false);
			tradingWithTownModificationCounter = Integer.MAX_VALUE;
			return;
		}

		final TownC town = this.town.get(townEntity);
		if (townEntity == tradingWithTown && tradingWithTownModificationCounter == town.modificationCounter) {
			return; // No change
		}
		// Something has changed, refresh UI
		tradingWindow.setVisible(true);
		tradingWithTown = townEntity;
		tradingWithTownModificationCounter = town.modificationCounter;

		tradingWindow.getTitleLabel().setText("Trading - "+town.name);

		final CaravanC caravan = this.caravan.get(playerEntity);
		for (Merchandise merch : Merchandise.VALUES) {
			final TextButton buyButton = buyButtons[merch.ordinal()];
			final int buyPrice = town.prices.buyPrice(merch);
			buyButton.setText("Buy for "+ buyPrice);
			buyButton.setDisabled(buyPrice > caravan.money);
			final int amountInInventory = caravan.inventory.get(merch);
			final TextButton sellButton = sellButtons[merch.ordinal()];
			final int sellPrice = town.prices.sellPrice(merch);
			final int realSellPrice = Math.min(sellPrice, town.money);
			sellButton.setText("Sell for "+ realSellPrice);
			sellButton.setDisabled(amountInInventory < 1);
			sellButton.getLabel().setColor(sellPrice == realSellPrice ? sellButton.getStyle().fontColor : Color.RED);
			inventoryLabels[merch.ordinal()].setText(amountInInventory);
		}
	}
}
