package caravan.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

/**
 * A software defined function that needs to be bound to something.
 */
public final class InputFunction {

	private static final int DEFAULT_REPEAT_TIMEOUT = 0;
	private static final boolean DEFAULT_TOGGLE = false;
	private static final String TAG = "InputFunction";

	/** The machine readable key under which this is saved. */
	public final String  key;
	/** Human readable name under which this function is shown to users. */
	public final String  humanReadableName;
	/** The default bindings of this function. */
	public final Binding[] defaultBinding;

	//region Settings
	// These fields are user-modifiable and stored in config file
	/** The actual bindings that are used, modifiable by player. */
	public Binding[] realBinding;

	/**
	 * How much time must elapse for "times" to reset
	 * In millis.
	 * <p>
	 * When ZERO (default), fast triggers can be collapsed into one call with "times" set to whatever times it triggered.
	 */
	public int repeatTimeout;

	/**
	 * Whether or not this function works like a toggle
	 * When FALSE:
	 * trigger(pressed = true) is called when the button is pressed
	 * trigger(pressed = false) is called when it is released again
	 * When TRUE:
	 * trigger(pressed = true) is called when the button is pressed
	 * trigger(pressed = false) is called when it is PRESSED again
	 * <p>
	 * When toggle is true, trigger repeat counting is not supported and always returns 1!
	 */
	public boolean toggle;
	//endregion

	// What was loaded - used to detect changes and to allow undo
	private Binding[] loadedRealBinding;
	private int loadedRepeatTimeout;
	private boolean loadedToggle;

	public InputFunction(String key, String humanReadableName, Binding...defaultBinding) {
		this.key = key;
		this.humanReadableName = humanReadableName;
		this.defaultBinding = defaultBinding;

		this.realBinding = this.loadedRealBinding = this.defaultBinding;
		this.repeatTimeout = this.loadedRepeatTimeout = DEFAULT_REPEAT_TIMEOUT;
		this.toggle = this.loadedToggle = DEFAULT_TOGGLE;
	}

	private boolean modified() {
		if (loadedRepeatTimeout != repeatTimeout || loadedToggle != toggle) {
			return true;
		}

		if (loadedRealBinding.length != realBinding.length) {
			return true;
		}
		for (int i = 0; i < loadedRealBinding.length; i++) {
			if (!loadedRealBinding[i].equals(realBinding[i])) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return key+" "+humanReadableName;
	}

	public static void load(@NotNull FileHandle from, @NotNull InputFunction[] inputFunctions) {
		for (InputFunction function : inputFunctions) {
			function.loadedToggle = function.toggle;
			function.loadedRepeatTimeout = function.repeatTimeout;
			function.loadedRealBinding = null;
		}

        /*
        Savefile syntax:

		# comment
        key type value [toggle [repeatTimeout]]

        key - String
        type - String (valueof of BindingType)
        value - int
        toggle - boolean ("true"/"false")
        timeout - int
         */
		if (from.exists() && !from.isDirectory()) {
			try (BufferedReader r = from.reader(4096, "UTF-8")) {
				int lineNumber = 1;
				String line;
				while ((line = r.readLine()) != null) {
					lineNumber++;
					line = line.trim();
					if (line.isEmpty() || line.startsWith("#")) continue;
					final String[] split = line.split(" ");
					if (split.length < 3) {
						Gdx.app.error(TAG, lineNumber+": Line is incomplete");
						continue;
					}
					final String key = split[0];
					InputFunction function = null;
					for (InputFunction f : inputFunctions) {
						if (f.key.equalsIgnoreCase(key)) {
							function = f;
							break;
						}
					}
					if (function == null) {
						Gdx.app.error(TAG, lineNumber + ": '" + split[0] + "' does not match any existing input function");
						continue;
					}

					final BindingType type;
					try {
						type = BindingType.valueOf(split[1].toUpperCase());
					} catch (IllegalArgumentException e) {
						Gdx.app.error(TAG, lineNumber + ": invalid type '"+split[1]+"'");
						continue;
					}

					final int value;
					try {
						value = Integer.parseInt(split[2]);
					} catch (NumberFormatException e) {
						Gdx.app.error(TAG, lineNumber + ": value is not a number '"+split[2]+"'");
						continue;
					}

					if (split.length >= 4) function.loadedToggle = "true".equals(split[3]);

					if (split.length >= 5) {
						try {
							function.loadedRepeatTimeout = Integer.parseInt(split[4]);
						} catch (NumberFormatException e) {
							Gdx.app.error(TAG, lineNumber + ": value is not a number '"+split[4]+"'");
							continue;
						}
					}

					final Binding binding = new Binding(type, value);
					function.loadedRealBinding = function.loadedRealBinding == null ? new Binding[1] : Arrays.copyOf(function.loadedRealBinding, function.loadedRealBinding.length + 1);
					function.loadedRealBinding[function.loadedRealBinding.length - 1] = binding;
				}
			} catch (Exception exception) {
				Gdx.app.error(TAG, "Could not load input file '" + from + "'", exception);
			}
		}

		for (InputFunction function : inputFunctions) {
			function.toggle = function.loadedToggle;
			function.repeatTimeout = function.loadedRepeatTimeout;
			function.realBinding = function.loadedRealBinding == null ? function.defaultBinding : function.loadedRealBinding;
		}
	}

	public static void save(@NotNull FileHandle to, @NotNull InputFunction[] inputFunctions) {
		boolean needToSave = !to.exists();
		if (!needToSave) {
			for (InputFunction function : inputFunctions) {
				if (function.modified()) {
					needToSave = true;
					break;
				}
			}
		}

		if (!needToSave) {
			Gdx.app.debug(TAG, "Skipping save, no changes detected");
			return;
		}

		final FileHandle saveTmp = to.sibling(to.name() + "~");
		try (Writer writer = new BufferedWriter(saveTmp.writer(false, "UTF-8"))) {
			for (InputFunction function : inputFunctions) {
				writer.append("# ");
				writer.append(function.humanReadableName);
				for (Binding binding : function.realBinding) {
					writer.append(' ');
					writer.append(binding.toMenuString());
				}
				writer.append('\n');

				boolean first = true;
				for (Binding binding : function.realBinding) {
					writer.append(function.key);
					writer.append(' ');
					writer.append(binding.type.name());
					writer.append(' ');
					writer.append(Integer.toString(binding.value));
					if (first && (function.repeatTimeout != DEFAULT_REPEAT_TIMEOUT || function.toggle != DEFAULT_TOGGLE)) {
						writer.append(' ');
						writer.append(function.toggle ? "true" : "false");
						writer.append(' ');
						writer.append(Integer.toString(function.repeatTimeout));
					}
					writer.append('\n');
					first = false;
				}

				function.loadedRealBinding = function.realBinding;
				function.loadedToggle = function.toggle;
				function.loadedRepeatTimeout = function.repeatTimeout;
			}
			saveTmp.moveTo(to);
		} catch (GdxRuntimeException | IOException exception) {
			Gdx.app.error(TAG, "Could not save file " + to + ".", exception);
		} finally {
			saveTmp.delete();
		}
	}

	private static String generateKey(String fromLabel) {
		StringBuilder sb = new StringBuilder();
		assert !fromLabel.isEmpty();
		for (int i = 0; i < fromLabel.length(); i++) {
			char c = Character.toLowerCase(fromLabel.charAt(i));
			if (c == ' ') {
				sb.append('.');
			} else if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static InputFunction function(String key, String label, Binding...defaultBinding) {
		return new InputFunction(key, label, defaultBinding);
	}

	public static InputFunction function(String label, Binding...defaultBinding) {
		return new InputFunction(generateKey(label), label, defaultBinding);
	}

	public static InputFunction toggleFunction(String key, String label, Binding...defaultBinding) {
		final InputFunction boundFunction = new InputFunction(key, label, defaultBinding);
		boundFunction.toggle = true;
		return boundFunction;
	}

	public static InputFunction toggleFunction(String label, Binding...defaultBinding) {
		final InputFunction boundFunction = new InputFunction(generateKey(label), label, defaultBinding);
		boundFunction.toggle = true;
		return boundFunction;
	}

}
