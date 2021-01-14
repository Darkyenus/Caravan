package caravan.debug;

import caravan.components.CaravanC;
import caravan.components.Components;
import caravan.components.PositionC;
import caravan.components.TownC;
import caravan.services.TownSystem;
import caravan.world.Merchandise;
import caravan.world.Production;
import com.badlogic.gdx.math.FloatCounter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Array;
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
				total.setText((int) counter.total);
				min.setText((int) counter.min);
				max.setText((int) counter.max);
				average.setText(String.format("%.2f", counter.average));
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
				min.setText(String.format("%.2f", counter.min));
				max.setText(String.format("%.2f", counter.max));
				average.setText(String.format("%.2f", counter.average));
			});
		}

		everythingTable.add("Other", "title-small");
		everythingTable.add("Total", "title-small");
		everythingTable.add("Min", "title-small");
		everythingTable.add("Max", "title-small");
		everythingTable.add("Average", "title-small");
		everythingTable.row();

		{
			everythingTable.add("Money");
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
				forEach(towns, town, (town) -> counter.put(town.money));
				total.setText(String.format("%.2f", counter.total));
				min.setText(String.format("%.2f", counter.min));
				max.setText(String.format("%.2f", counter.max));
				average.setText(String.format("%.2f", counter.average));
			});
		}

		{
			everythingTable.add("Wealth");
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
				forEach(towns, town, (town) -> counter.put(town.wealth));
				total.setText(String.format("%.2f", counter.total));
				min.setText(String.format("%.2f", counter.min));
				max.setText(String.format("%.2f", counter.max));
				average.setText(String.format("%.2f", counter.average));
			});
		}

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
					profit.setText("?");
					return;
				}
				profit.setText(String.format("%.2f", TownSystem.productionProfit(selectedTown, production)));
			});

		}
		//endregion

		pack();
		setResizable(true);
		setResizeBorder(20);
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
}
