package caravan;

import caravan.components.CaravanC;
import caravan.components.TownC;
import caravan.util.ConfirmWindow;
import caravan.util.PooledArray;
import caravan.util.Rumors;
import caravan.util.Tooltip;
import caravan.util.Util;
import caravan.world.Merchandise;
import caravan.world.Production;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.darkyen.retinazer.Mapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static caravan.CaravanApplication.uiSkin;
import static caravan.util.Util.newScrollPane;

/**
 * Screen overlay that appears when a player caravan arrives into a town to trade.
 */
@SuppressWarnings("unchecked")
public final class TradingScreen extends CaravanApplication.UIScreen {

	private Mapper<TownC> townMapper;
	@Nullable
	private TownC town;
	@Nullable
	private CaravanC caravan;

	private Label townNameLabel;
	private Label playerMoneyLabel;
	private final TextButton[] buyButtons = new TextButton[Merchandise.COUNT];
	private final Tooltip<Label>[] buyButtonTooltips = new Tooltip[Merchandise.COUNT];
	private final TextButton[] sellButtons = new TextButton[Merchandise.COUNT];
	private final Tooltip<Label>[] sellButtonTooltips = new Tooltip[Merchandise.COUNT];
	private final Label[] inventoryLabels = new Label[Merchandise.COUNT];
	private Table production;
	private Table rumors;

	private Label.LabelStyle labelStyleDefault;
	private Label.LabelStyle labelStyleDefaultDisabled;
	private TextButton.TextButtonStyle textButtonStyleDefault;
	private TextButton.TextButtonStyle textButtonStyleDefaultDangerous;

	private ConfirmWindow badPriceWarning;

	public TradingScreen() {
		super(500, false, true);
	}

	public void reset(@NotNull Mapper<TownC> townMapper, @NotNull TownC town, @NotNull CaravanC caravan) {
		this.town = town;
		this.townMapper = townMapper;
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
			if (!merch.tradeable) {
				continue;
			}

			final int ordinal = merch.ordinal();
			final TextButton buyButton = buyButtons[ordinal];
			final TextButton sellButton = sellButtons[ordinal];
			final Label inventoryLabel = inventoryLabels[ordinal];

			final boolean visible = caravan.categories[merch.category.ordinal()];

			final int buyPrice = town.prices.buyPrice(merch);
			buyButton.getLabel().setText(buyPrice);
			buyButton.setDisabled(buyPrice > caravan.money || !visible);

			final int amountInInventory = caravan.inventory.get(merch);
			final int rawSellPrice = town.prices.sellPrice(merch);
			final int sellPrice = town.realSellPrice(merch);
			sellButton.getLabel().setText(sellPrice);
			sellButton.setDisabled(amountInInventory < 1 || !visible);
			sellButton.setStyle(rawSellPrice == sellPrice ? textButtonStyleDefault : textButtonStyleDefaultDangerous);

			inventoryLabel.setVisible(visible);
			inventoryLabel.setText(amountInInventory);
			inventoryLabel.setStyle(amountInInventory == 0 ? labelStyleDefaultDisabled : labelStyleDefault);

			Tooltip<Label> buyButtonTooltip = buyButtonTooltips[ordinal];
			Tooltip<Label> sellButtonTooltip = sellButtonTooltips[ordinal];
			final short buyMemory = caravan.inventoryPriceBuyMemory[ordinal];
			final short sellMemory = caravan.inventoryPriceSellMemory[ordinal];
			if (buyMemory > 0) {
				if (buyButtonTooltip == null) {
					buyButtonTooltips[ordinal] = buyButtonTooltip = newTooltip();
				}
				buyButtonTooltip.getActor().setText("Last bought for "+buyMemory);
				buyButtonTooltip.setParent(buyButton);
			} else if (buyButtonTooltip != null) {
				buyButtonTooltip.setParent(null);
			}
			if (sellMemory > 0) {
				if (sellButtonTooltip == null) {
					sellButtonTooltips[ordinal] = sellButtonTooltip = newTooltip();
				}
				sellButtonTooltip.getActor().setText("Last sold for "+sellMemory);
				sellButtonTooltip.setParent(sellButton);
			} else if (sellButtonTooltip != null) {
				sellButtonTooltip.setParent(null);
			}
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
		if (town.wealth <= 0f) {
			wealthRumor = "This town is desperately poor";
		} else if (town.wealth < -0.5f) {
			wealthRumor = "This town is poor";
		} else if (town.wealth < 0.5f) {
			wealthRumor = "This town breaks even";
		} else if (town.wealth < 1f) {
			wealthRumor = "This town is wealthy";
		} else {
			wealthRumor = "This town is fabulously wealthy";
		}
		rumors.add(wealthRumor).row();

		{
			final PooledArray<Rumors.Rumor> rumors = town.rumors.rumors;
			for (int i = 0; i < rumors.size; i++) {
				final Rumors.Rumor rumor = rumors.get(i);

				final String merchName = Util.getName(rumor.aboutMerchandise);
				final String townName = Util.getName(rumor.aboutTownEntity, townMapper);

				final String text;
				switch (rumor.type) {
					default:
					case THING_EXISTS:
						text = merchName+" exists in "+townName;
						break;
					case BUY_PRICE:
						text = merchName+" is sold in "+townName+" for "+rumor.aboutPrice;
						break;
					case SELL_PRICE:
						text = merchName+" is bought by "+townName+" for "+rumor.aboutPrice;
						break;
				}
				this.rumors.add(text).row();
			}
		}
	}

	private static Tooltip<Label> newTooltip() {
		final Label label = new Label("", uiSkin(), "tooltip");
		return new Tooltip<>(label);
	}

	@Override
	protected void initializeUI(@NotNull CaravanApplication application, @NotNull Table root) {
		final Skin skin = uiSkin();
		backgroundColor.set(skin.getColor("p-beige"));

		labelStyleDefault = skin.get("default", Label.LabelStyle.class);
		labelStyleDefaultDisabled = skin.get("default-disabled", Label.LabelStyle.class);
		textButtonStyleDefault = skin.get("default", TextButton.TextButtonStyle.class);
		textButtonStyleDefaultDangerous = skin.get("default-dangerous", TextButton.TextButtonStyle.class);

		badPriceWarning = new ConfirmWindow();
		badPriceWarning.setYesText("Sell anyway");
		badPriceWarning.setNoText("Cancel");
		badPriceWarning.getTitleLabel().setText("Price warning");
		badPriceWarning.setMessageText("Town can't pay full price, because it does not have enough money.");

		root.pad(20f);

		{
			final Table topBar = new Table(skin);
			townNameLabel = new Label("TOWN NAME", skin, "title-medium");
			topBar.add(townNameLabel).expandX().align(Align.left);

			playerMoneyLabel = new Label("???", labelStyleDefault);
			topBar.add("Caravan gold:").padRight(10f).align(Align.right);
			topBar.add(playerMoneyLabel).align(Align.left);

			final TextButton leaveButton = new TextButton("Leave", skin);
			leaveButton.pad(0, 10f, 0, 10f);
			leaveButton.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					town = null;
					caravan = null;
					TradingScreen.this.removeScreen(false);
				}
			});
			topBar.add(leaveButton).align(Align.right).expandX();

			root.add(topBar).growX().colspan(2).padBottom(10f).row();
		}

		final Table merchTable = new Table(skin);
		merchTable.defaults().pad(2f);
		merchTable.pad(5f);
		merchTable.columnDefaults(0).expand(4, 0).align(Align.right).grow();// nameLabel
		merchTable.columnDefaults(1).expand(1, 0).align(Align.center).fill().minWidth(20f).padLeft(10f);// buyButton
		merchTable.columnDefaults(2).expand(1, 0).align(Align.center).fill().minWidth(20f).padLeft(10f);// sellButton
		merchTable.columnDefaults(3).expand(1, 0).align(Align.center).fill().minWidth(20f).padLeft(10f);// inventoryLabel
		final ScrollPane pane = newScrollPane(merchTable);
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
				if (!performBuy(town, caravan, merchandise, UIUtils.alt() || UIUtils.shift())) {
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

				final boolean allForPrice = UIUtils.alt() || UIUtils.shift();
				final Merchandise merchandise = (Merchandise) actor.getUserObject();

				if (((TextButton) actor).getStyle() == textButtonStyleDefaultDangerous) {
					// Ask for confirmation
					badPriceWarning.setYesAction(() -> {
						if (!performSell(town, caravan, merchandise, allForPrice)) {
							Gdx.app.log("TradingSystem", "Sell of " + merchandise + " failed");
						} else {
							refresh();
						}
					});
					badPriceWarning.show(actor.getStage());
				} else {
					if (!performSell(town, caravan, merchandise, allForPrice)) {
						Gdx.app.log("TradingSystem", "Sell of " + merchandise + " failed");
					} else {
						refresh();
					}
				}
			}
		};

		final Value.Fixed categoryRowPad = Value.Fixed.valueOf(8f);

		Merchandise.Category prevCategory = null;
		for (Merchandise merchandise : Merchandise.VALUES) {
			if (!merchandise.tradeable) {
				continue;
			}

			if (prevCategory != merchandise.category) {
				prevCategory = merchandise.category;

				final Label categoryLabel = new Label(merchandise.category.name, skin, "title-small");
				merchTable.add(categoryLabel).align(Align.center).fillX().padTop(categoryRowPad);
				final Label buyLabel = new Label("Buy", skin, "title-small");
				final Label sellLabel = new Label("Sell", skin, "title-small");
				final Label ownedLabel = new Label("Owned", skin, "title-small");
				buyLabel.setAlignment(Align.center);
				sellLabel.setAlignment(Align.center);
				ownedLabel.setAlignment(Align.center);
				merchTable.add(buyLabel).padTop(categoryRowPad);
				merchTable.add(sellLabel).padTop(categoryRowPad);
				merchTable.add(ownedLabel).padTop(categoryRowPad);

				merchTable.row();
				categoryLabel.setAlignment(Align.center);
			}

			final Label nameLabel = new Label(merchandise.name, labelStyleDefault);
			final TextButton buyButton = new TextButton("0", textButtonStyleDefault);
			final TextButton sellButton = new TextButton("0", textButtonStyleDefault);
			final Label inventoryLabel = new Label("0", labelStyleDefault);

			nameLabel.setAlignment(Align.right);
			inventoryLabel.setAlignment(Align.center);

			merchTable.add(nameLabel);
			merchTable.add(buyButton);
			merchTable.add(sellButton);
			merchTable.add(inventoryLabel);
			merchTable.row();

			final int m = merchandise.ordinal();
			buyButtons[m] = buyButton;
			sellButtons[m] = sellButton;
			inventoryLabels[m] = inventoryLabel;

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
		final Label productionLabel = new Label("Production", skin, "title-medium");
		productionLabel.setAlignment(Align.center);
		rightPanel.add(productionLabel).growX().pad(10f).row();
		rightPanel.add(newScrollPane(production)).minSize(100f, 100f).grow().row();

		final Label rumorsLabel = new Label("Rumors", skin, "title-medium");
		rumorsLabel.setAlignment(Align.center);
		rightPanel.add(rumorsLabel).growX().pad(10f).row();
		rumors = new Table(skin);
		rumors.pad(5f).align(Align.top);
		rumors.defaults().pad(5f).align(Align.center);
		rightPanel.add(newScrollPane(rumors)).minSize(100f, 100f).grow().row();

		// Initial layout is kinda weird
		root.validate();
		root.invalidateHierarchy();
	}

	public static boolean performBuy(@NotNull TownC town, @NotNull CaravanC caravan, @NotNull Merchandise m, boolean allForPrice) {
		boolean boughtSomething = false;
		final int buyPrice = town.prices.buyPrice(m);
		do {
			if (buyPrice > caravan.money) {
				return boughtSomething;
			}
			boughtSomething = true;
			town.money += buyPrice;
			caravan.money -= buyPrice;
			caravan.inventory.add(m, 1);
			town.prices.buyUnit(m);
			caravan.inventoryPriceBuyMemory[m.ordinal()] = Util.toShortClampUnsigned(buyPrice);
		} while (allForPrice && (buyPrice == town.prices.buyPrice(m)));
		return true;
	}

	public static boolean performSell(@NotNull TownC town, @NotNull CaravanC caravan, @NotNull Merchandise m, boolean allForPrice) {
		boolean soldSomething = false;
		final int sellPrice = town.realSellPrice(m);
		do {
			if (caravan.inventory.get(m) <= 0) {
				return soldSomething;
			}
			soldSomething = true;
			town.money -= sellPrice;
			caravan.money += sellPrice;
			caravan.inventory.add(m, -1);
			town.prices.sellUnit(m);
			caravan.inventoryPriceSellMemory[m.ordinal()] = Util.toShortClampUnsigned(sellPrice);
		} while (allForPrice && sellPrice == town.realSellPrice(m));
		return true;
	}
}
