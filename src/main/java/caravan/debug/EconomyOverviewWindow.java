package caravan.debug;

import caravan.components.CaravanC;
import caravan.components.Components;
import caravan.components.PositionC;
import caravan.components.TownC;
import caravan.services.TownSystem;
import caravan.util.Tooltip;
import caravan.world.Merchandise;
import caravan.world.Production;
import com.badlogic.gdx.math.FloatCounter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StringBuilder;
import com.darkyen.retinazer.Engine;
import com.darkyen.retinazer.EntitySetView;
import com.darkyen.retinazer.Mapper;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

import static caravan.CaravanApplication.uiSkin;
import static caravan.util.Util.ALL_HANDLING_INPUT_LISTENER;
import static caravan.util.Util.findClosest;
import static caravan.util.Util.forEach;
import static caravan.util.Util.newScrollPane;

/**
 * Window that shows total production of all merchandise,
 * active production, price statistics and so on.
 */
public class EconomyOverviewWindow extends Window {

	private final EntitySetView towns;
	private final Mapper<TownC> town;

	private final EntitySetView caravans;
	private final Mapper<CaravanC> caravan;

	private final Mapper<PositionC> position;

	private final Array<Runnable> updateProcessors = new Array<>();

	private final Vector2 worldSpaceCursor;
	private TownC selectedTown = null;

	public EconomyOverviewWindow(@NotNull Engine engine, @NotNull Vector2 worldSpaceCursor) {
		super("Economy Overview", uiSkin());
		this.worldSpaceCursor = worldSpaceCursor;
		addListener(ALL_HANDLING_INPUT_LISTENER);
		getTitleTable().padLeft(10f);

		final Skin skin = uiSkin();

		towns = engine.getEntities(Components.DOMAIN.familyWith(TownC.class));
		caravans = engine.getEntities(Components.DOMAIN.familyWith(CaravanC.class));
		town = engine.getMapper(TownC.class);
		caravan = engine.getMapper(CaravanC.class);

		position = engine.getMapper(PositionC.class);

		final Table everythingTable = new Table(skin);
		add(newScrollPane(everythingTable, "light")).grow().prefWidth(600).prefHeight(500);

		everythingTable.add("Production", "title-small");
		everythingTable.add("Total", "title-small");
		everythingTable.add("Min", "title-small");
		everythingTable.add("Max", "title-small");
		everythingTable.add("Average", "title-small");
		everythingTable.row();

		final Array<Production> productions = new Array<>();
		for (Production p : Production.REGISTRY) {
			productions.add(p);
		}
		productions.sort(Comparator.comparing(p -> p.name));

		for (Production production : productions) {
			everythingTable.add(production.name);
			final Label total = new Label("", skin);
			final Label min = new Label("", skin);
			final Label max = new Label("", skin);
			final Label average = new Label("", skin);
			everythingTable.add(total);
			everythingTable.add(min);
			everythingTable.add(max);
			everythingTable.add(average);
			everythingTable.row();

			final FloatCounter counter = new FloatCounter(0);
			updateProcessors.add(() -> {
				counter.reset();
				forEach(towns, town, (town) -> counter.put(town.production.get(production, 0)));
				setText(total, (int) counter.total);
				setText(min, (int) counter.min);
				setText(max, (int) counter.max);
				setText(average, String.format("%.2f", counter.average));
			});
		}

		everythingTable.add("Price", "title-small");
		everythingTable.add("", "title-small");
		everythingTable.add("Min", "title-small");
		everythingTable.add("Max", "title-small");
		everythingTable.add("Average", "title-small");
		everythingTable.row();

		for (Merchandise merch : Merchandise.VALUES) {
			everythingTable.add(merch.name);

			final Label min = new Label("", skin);
			final Label max = new Label("", skin);
			final Label average = new Label("", skin);
			everythingTable.add();
			everythingTable.add(min);
			everythingTable.add(max);
			everythingTable.add(average);
			everythingTable.row();

			final FloatCounter counter = new FloatCounter(0);
			updateProcessors.add(() -> {
				counter.reset();
				forEach(towns, town, (town) -> counter.put(town.prices.basePrice(merch)));
				setText(min, String.format("%.2f", counter.min));
				setText(max, String.format("%.2f", counter.max));
				setText(average, String.format("%.2f", counter.average));
			});
		}

		everythingTable.add("Other", "title-small");
		everythingTable.add("Total", "title-small");
		everythingTable.add("Min", "title-small");
		everythingTable.add("Max", "title-small");
		everythingTable.add("Average", "title-small");
		everythingTable.row();

		addOtherTownStatistic(skin, everythingTable, "Money", (town) -> town.money);
		addOtherTownStatistic(skin, everythingTable, "Wealth", (town) -> town.wealth);
		addOtherTownStatistic(skin, everythingTable, "Population", (town) -> town.population);
		addOtherTownStatistic(skin, everythingTable, "Trade Buy C.", (town) -> town.tradeBuyCounter);
		addOtherTownStatistic(skin, everythingTable, "Trade Sell C.", (town) -> town.tradeSellCounter);
		addOtherTownStatistic(skin, everythingTable, "Has Fresh Water", (town) -> town.environment.hasFreshWater ? 1 : 0);
		addOtherTownStatistic(skin, everythingTable, "Has Salt Water", (town) -> town.environment.hasSaltWater ? 1 : 0);
		addOtherTownStatistic(skin, everythingTable, "Wood Abundance", (town) -> town.environment.woodAbundance);
		addOtherTownStatistic(skin, everythingTable, "Field Space", (town) -> town.environment.fieldSpace);
		addOtherTownStatistic(skin, everythingTable, "Fish Abundance", (town) -> town.environment.fishAbundance);
		addOtherTownStatistic(skin, everythingTable, "Temperature", (town) -> town.environment.temperature);
		addOtherTownStatistic(skin, everythingTable, "Precipitation", (town) -> town.environment.precipitation);
		addOtherTownStatistic(skin, everythingTable, "Rare Metal Occurrence", (town) -> town.environment.rareMetalOccurrence);
		addOtherTownStatistic(skin, everythingTable, "Metal Occurrence", (town) -> town.environment.metalOccurrence);
		addOtherTownStatistic(skin, everythingTable, "Coal Occurrence", (town) -> town.environment.coalOccurrence);
		addOtherTownStatistic(skin, everythingTable, "Jewel Occurrence", (town) -> town.environment.jewelOccurrence);
		addOtherTownStatistic(skin, everythingTable, "Stone Occurrence", (town) -> town.environment.stoneOccurrence);
		addOtherTownStatistic(skin, everythingTable, "Limestone Occurrence", (town) -> town.environment.limestoneOccurrence);

		//region Selected town profit
		final Label townProductionName = new Label("", skin, "title-small");
		updateProcessors.add(() -> {
			if (selectedTown == null) {
				townProductionName.setText("No town selected");
			} else {
				townProductionName.setText(selectedTown.name);
			}
		});
		everythingTable.add(townProductionName);
		everythingTable.add("Profit Potential", "title-small").colspan(4);
		everythingTable.row();

		for (Production production : productions) {
			everythingTable.add(production.name);


			final Label profit = new Label("", skin);
			everythingTable.add(profit).colspan(4).row();

			updateProcessors.add(() -> {
				if (selectedTown == null) {
					setText(profit, "?");
					return;
				}
				setText(profit, String.format("%.2f", TownSystem.productionProfit(selectedTown, production)));
			});

		}
		//endregion

		pack();
		setResizable(true);
		setResizeBorder(20);
	}

	@FunctionalInterface
	private interface TownStatisticFunction {
		float get(@NotNull TownC town);
	}

	private void addOtherTownStatistic(Skin skin, Table everythingTable, String money, TownStatisticFunction func) {
		everythingTable.add(money);
		final Label total = new Label("", skin);
		final Label min = new Label("", skin);
		final Label max = new Label("", skin);
		final Label average = new Label("", skin);
		everythingTable.add(total);
		everythingTable.add(min);
		everythingTable.add(max);
		everythingTable.add(average);
		everythingTable.row();

		final FloatCounter counter = new FloatCounter(0);
		updateProcessors.add(() -> {
			counter.reset();
			forEach(towns, town, (town) -> counter.put(func.get(town)));
			setText(total, String.format("%.2f", counter.total));
			setText(min, String.format("%.2f", counter.min));
			setText(max, String.format("%.2f", counter.max));
			setText(average, String.format("%.2f", counter.average));
		});
	}

	public void refresh() {
		int closestTown = findClosest(towns, position, worldSpaceCursor);
		if (closestTown == -1) {
			this.selectedTown = null;
		} else {
			this.selectedTown = town.getOrNull(closestTown);
		}

		for (Runnable updateProcessor : updateProcessors) {
			updateProcessor.run();
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			refresh();
		}
	}

	@SuppressWarnings("unchecked")
	private static void setText(@NotNull Label label, @NotNull Object text) {
		Tooltip<Label> tooltip = (Tooltip<Label>) label.getUserObject();
		if (tooltip == null) {
			tooltip = new Tooltip<>(new Label("", uiSkin(), "tooltip"));
			tooltip.setParent(label);
			label.setUserObject(tooltip);
		}
		final StringBuilder labelText = label.getText();
		tooltip.getActor().setText(labelText);
		labelText.clear();
		labelText.append(text);
		label.invalidateHierarchy();
	}
}
