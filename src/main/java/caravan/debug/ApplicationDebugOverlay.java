package caravan.debug;

import caravan.CaravanApplication;
import com.badlogic.gdx.ApplicationLogger;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Queue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Overlay which shows debug data.
 */
public final class ApplicationDebugOverlay extends CaravanApplication.UIScreen {

	private final GraphPane.GraphData renderedData = new GraphPane.GraphData(256, Color.BLUE, 10f, 80f, true);
	private final GraphPane.GraphData stepTimeData = new GraphPane.GraphData(256, Color.RED, 0f, 0.1f, false);
	private final GraphPane.GraphData memData      = new GraphPane.GraphData(256, Color.GREEN, 0f, 20000f, true);

	private Label log;
	private ScrollPane logScroll;
	private static final int MAX_MESSAGES = 64;
	private final Queue<String> messages = new Queue<>(MAX_MESSAGES, String.class);

	public ApplicationDebugOverlay() {
		super(1000, true, false);
	}

	@Override
	protected void initializeUI(@NotNull CaravanApplication application,  @NotNull Table table) {
		stage.getRoot().setVisible(false);

		final GraphPane grapherPane = new GraphPane();
		grapherPane.graphs.add(renderedData);
		grapherPane.graphs.add(stepTimeData);
		grapherPane.graphs.add(memData);

		table.pad(10f);
		table.align(Align.left);

		// Debug graph
		table.add(grapherPane)
				.prefWidth(Value.percentWidth(0.3f, table))
				.prefHeight(Value.percentHeight(0.2f, table))
				.align(Align.left)
				.row();

		table.add().expand().row();

		// Log
		log = new Label("", CaravanApplication.uiSkin(), "log");
		log.setAlignment(Align.bottomLeft);
		log.setWrap(true);
		logScroll = new ScrollPane(log, CaravanApplication.uiSkin(), "light");
		logScroll.setFlickScroll(false);
		logScroll.setFadeScrollBars(true);
		final TextField commandEntry = new TextField("", CaravanApplication.uiSkin(), "faded");

		final Table chatTable = new Table(CaravanApplication.uiSkin());
		chatTable.add(logScroll).expand().fill().bottom().row();
		chatTable.add(commandEntry).expandX().fillX().row();

		table.add(chatTable)
				.prefWidth(Value.percentWidth(0.5f, table))
				.prefHeight(Value.percentHeight(1f/3f, table))
				.grow();
		table.add().expand(); // Forces chatTable to the left side

		commandEntry.setTextFieldListener((textField, c) -> {
			if(c == '\n' || c == '\r') {
				final String command = commandEntry.getText().trim();
				commandEntry.setText("");
				stage.setKeyboardFocus(null);

				if (command.isEmpty()) {
					return;
				}

				for (CaravanApplication.Screen screen : application.screens()) {
					if (screen instanceof CommandListener) {
						if (((CommandListener) screen).onCommand(command)) {
							return;
						}
					}
				}

				printToLog("Invalid command");
			}
		});
	}

	@Override
	public void create(@NotNull CaravanApplication application) {
		super.create(application);
		final ApplicationLogger originalLogger = Gdx.app.getApplicationLogger();
		Gdx.app.setApplicationLogger(new ConsolePrintingLogger(originalLogger, this));
	}

	@Override
	public void dispose() {
		final ApplicationLogger logger = Gdx.app.getApplicationLogger();
		if (logger instanceof ConsolePrintingLogger) {
			Gdx.app.setApplicationLogger(((ConsolePrintingLogger) logger).parent);
		}
		super.dispose();
	}

	@Override
	public void update(@NotNull CaravanApplication application, float delta) {
		renderedData.addDataPoint(0);
		stepTimeData.addDataPoint(Gdx.graphics.getDeltaTime());
		final Runtime rt = Runtime.getRuntime();
		memData.addDataPoint((rt.totalMemory() - rt.freeMemory())/1000f);

		super.update(application, delta);
	}

	@Override
	public boolean keyDown(int keycode) {
		if (keycode == Input.Keys.F3) {
			stage.getRoot().setVisible(!stage.getRoot().isVisible());
		}

		return super.keyDown(keycode);
	}

	public void printToLog(@NotNull String text){
		while (messages.size >= MAX_MESSAGES) {
			messages.removeFirst();
		}

		messages.addLast(text);

		StringBuilder result = new StringBuilder();
		for (String message : messages) {
			result.append(message).append('\n');
		}

		if (result.length() == 0) {
			log.setText("");
		} else {
			result.setLength(result.length() - 1);//Strip the last \n
			log.setText(result);
		}
		logScroll.setScrollPercentY(1f);
	}

	/** Implement this in your {@link caravan.CaravanApplication.Screen} to receive debug commands. */
	public interface CommandListener {
		/** @return true if the command was processed, false if not */
		boolean onCommand(@NotNull String command);
	}

	private static final class ConsolePrintingLogger implements ApplicationLogger {

		public final ApplicationLogger parent;
		private final ApplicationDebugOverlay dso;

		ConsolePrintingLogger(@NotNull ApplicationLogger parent, @NotNull ApplicationDebugOverlay dso) {
			this.parent = parent;
			this.dso = dso;
		}

		private final StringBuilder logBuilder = new StringBuilder();

		private void print(@NotNull String level, @NotNull String tag, @NotNull String message, @Nullable Throwable exception) {
			final StringBuilder sb = this.logBuilder;
			synchronized (this.logBuilder) {
				sb.setLength(0);
				sb.append('[').append(level).append("] ").append(tag).append(": ").append(message);
				if (exception != null) {
					sb.append("\n    ").append(exception);
				}
				dso.printToLog(sb.toString());
			}
		}

		@Override
		public void log(String tag, String message) {
			parent.log(tag, message);
			print("INFO", tag, message, null);
		}

		@Override
		public void log(String tag, String message, Throwable exception) {
			parent.log(tag, message, exception);
			print("INFO", tag, message, exception);
		}

		@Override
		public void error(String tag, String message) {
			parent.error(tag, message);
			print("ERROR", tag, message, null);
		}

		@Override
		public void error(String tag, String message, Throwable exception) {
			parent.error(tag, message, exception);
			print("ERROR", tag, message, exception);
		}

		@Override
		public void debug(String tag, String message) {
			parent.debug(tag, message);
			print("DEBUG", tag, message, null);
		}

		@Override
		public void debug(String tag, String message, Throwable exception) {
			parent.debug(tag, message, exception);
			print("DEBUG", tag, message, exception);
		}
	}
}
