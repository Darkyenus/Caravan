package caravan;

import caravan.components.CaravanC;
import caravan.components.TownC;
import caravan.util.PooledArray;
import caravan.world.Merchandise;
import caravan.world.Production;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectIntMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Screen overlay that appears when a player caravan arrives into a town to trade.
 */
public final class TradingScreen extends CaravanApplication.UIScreen {

	@Nullable
	private TownC town;
	@Nullable
	private CaravanC caravan;

	private Label townNameLabel;
	private Label playerMoneyLabel;
	private final TextButton[] buyButtons = new TextButton[Merchandise.VALUES.length];
	private final TextButton[] sellButtons = new TextButton[Merchandise.VALUES.length];
	private final Label[] inventoryLabels = new Label[Merchandise.VALUES.length];
	private Table production;
	private Table rumors;

	public TradingScreen() {
		super(500, false, true);
	}

	public void reset(@NotNull TownC town, @NotNull CaravanC caravan) {
		this.town = town;
		this.caravan = caravan;
		refresh();
	}

	private static final class ProductionAmountPair implements Comparable<ProductionAmountPair> {
		Production production;
		int amount;

		@Override
		public int compareTo(@NotNull TradingScreen.ProductionAmountPair o) {
			return Float.compare(o.amount, amount);
		}
	}
	private final PooledArray<ProductionAmountPair> productionTmp = new PooledArray<>(ProductionAmountPair::new);

	private void refresh() {
		final TownC town = this.town;
		final CaravanC caravan = this.caravan;
		if (town == null || caravan == null) {
			return;
		}

		townNameLabel.setText(town.name);
		playerMoneyLabel.setText(caravan.money);

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

		final PooledArray<ProductionAmountPair> productionEntries = this.productionTmp;
		for (ObjectIntMap.Entry<caravan.world.Production> entry : town.production) {
			final ProductionAmountPair p = productionEntries.add();
			p.production = entry.key;
			p.amount = entry.value;
		}
		productionEntries.sort();
		production.clear();
		for (int i = 0; i < productionEntries.size; i++) {
			final ProductionAmountPair pair = productionEntries.get(i);
			production.add(pair.production.name);
			production.add(Integer.toString(pair.amount));
			production.row();
		}
		productionEntries.clear();

		rumors.clear();
		String wealthRumor;
		if (town.wealth < -0.5f) {
			wealthRumor = "This town is poor";
		} else if (town.wealth < 0.5f) {
			wealthRumor = "This town breaks even";
		} else {
			wealthRumor = "This town is wealthy";
		}
		rumors.add(wealthRumor).row();
		// TODO(jp): Add rumors
	}

	@Override
	protected void initializeUI(@NotNull CaravanApplication application, @NotNull Table root) {
		final Skin skin = CaravanApplication.uiSkin();
		root.pad(20f);

		final Table topBar = new Table(skin);
		townNameLabel = new Label("TOWN NAME", skin);
		topBar.add(townNameLabel).growX();
		final TextButton leaveButton = new TextButton("Leave", skin);
		leaveButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				town = null;
				caravan = null;
				TradingScreen.this.removeScreen(false);
			}
		});
		topBar.add(leaveButton).align(Align.right);

		root.add(topBar).growX().colspan(2).padBottom(10f).row();

		final Table merchTable = new Table(skin);
		merchTable.defaults().pad(2f);
		merchTable.pad(5f);
		merchTable.columnDefaults(0).align(Align.right).grow();// nameLabel
		merchTable.columnDefaults(1).fill().padLeft(10f);// buyButton
		merchTable.columnDefaults(2).fill().padLeft(10f);// sellButton
		merchTable.columnDefaults(3).fill().minWidth(15f).padLeft(10f);// inventoryLabel
		final ScrollPane pane = scrollPane(merchTable);
		root.add(pane).grow().padRight(10f);

		final ChangeListener buyListener = new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				final TownC town = TradingScreen.this.town;
				final CaravanC caravan = TradingScreen.this.caravan;
				if (town == null || caravan == null) {
					return;
				}

				final Merchandise merchandise = (Merchandise) actor.getUserObject();
				if (!performBuy(town, caravan, merchandise)) {
					Gdx.app.log("TradingSystem", "Buy of " + merchandise + " failed");
				} else {
					refresh();
				}
			}
		};
		final ChangeListener sellListener = new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				final TownC town = TradingScreen.this.town;
				final CaravanC caravan = TradingScreen.this.caravan;
				if (town == null || caravan == null) {
					return;
				}

				final Merchandise merchandise = (Merchandise) actor.getUserObject();
				if (!performSell(town, caravan, merchandise)) {
					Gdx.app.log("TradingSystem", "Sell of " + merchandise + " failed");
				} else {
					refresh();
				}
			}
		};

		int m = 0;
		for (Merchandise merchandise : Merchandise.VALUES) {
			final Label nameLabel = new Label(merchandise.name, skin);
			final TextButton buyButton = new TextButton("Buy 000", skin);
			final TextButton sellButton = new TextButton("Sell 000", skin);
			final Label inventoryLabel = new Label("0", skin);

			nameLabel.setAlignment(Align.right);

			merchTable.add(nameLabel);
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

		final Table rightPanel = new Table(skin);
		root.add(rightPanel).padLeft(10f).grow().row();

		production = new Table(skin);
		production.pad(5f).align(Align.top);
		production.columnDefaults(0).align(Align.right).padRight(10f);
		production.columnDefaults(1).align(Align.center).fillX().minWidth(20f);
		final Label productionLabel = new Label("Production", skin);
		productionLabel.setAlignment(Align.center);
		rightPanel.add(productionLabel).growX().pad(10f).row();
		rightPanel.add(scrollPane(production)).grow().row();

		final Label rumorsLabel = new Label("Rumors", skin);
		rumorsLabel.setAlignment(Align.center);
		rightPanel.add(rumorsLabel).growX().pad(10f).row();
		rumors = new Table(skin);
		rumors.pad(5f).align(Align.top);
		rumors.defaults().pad(5f).align(Align.left);
		rightPanel.add(scrollPane(rumors)).grow().row();

		final Table playerMoney = new Table(skin);
		playerMoney.align(Align.center);
		playerMoneyLabel = new Label("???", skin);
		playerMoney.add("Caravan gold:").padRight(10f).align(Align.right);
		playerMoney.add(playerMoneyLabel).align(Align.left);

		rightPanel.add(playerMoney).expandX().align(Align.center).row();
	}

	private static ScrollPane scrollPane(Actor inside) {
		final ScrollPane pane = new ScrollPane(inside, CaravanApplication.uiSkin());
		pane.setFadeScrollBars(false);
		pane.setFlickScroll(false);
		pane.setScrollingDisabled(true, false);
		pane.setSmoothScrolling(true);
		return pane;
	}


	public static boolean performBuy(@NotNull TownC town, @NotNull CaravanC caravan, @NotNull Merchandise m) {
		final int buyPrice = town.prices.buyPrice(m);
		if (buyPrice > caravan.money) {
			return false;
		}
		town.money += buyPrice;
		caravan.money -= buyPrice;
		caravan.inventory.add(m, 1);
		town.prices.buyUnit(m);
		return true;
	}

	public static boolean performSell(@NotNull TownC town, @NotNull CaravanC caravan, @NotNull Merchandise m) {
		if (caravan.inventory.get(m) <= 0) {
			return false;
		}
		final int sellPrice = Math.min(town.prices.sellPrice(m), town.money);
		town.money -= sellPrice;
		caravan.money += sellPrice;
		caravan.inventory.add(m, -1);
		town.prices.sellUnit(m);
		return true;
	}
}
