package caravan;

import caravan.input.InputFunction;
import com.badlogic.gdx.files.FileHandle;

/**
 * The persistent options of the application.
 */
public final class Options {

	//TODO

	private static final InputFunction[] ALL_INPUTS = new InputFunction[] {
			Inputs.UP,
			Inputs.DOWN,
			Inputs.LEFT,
			Inputs.RIGHT
	};

	private static FileHandle bindingsFile(FileHandle saveDir) {
		return saveDir.child("input.txt");
	}

	private static FileHandle optionsFile(FileHandle saveDir) {
		return saveDir.child("options.txt");
	}

	public static void load(FileHandle saveDir) {
		InputFunction.load(bindingsFile(saveDir), ALL_INPUTS);
	}

	public static void save(FileHandle saveDir) {
		InputFunction.save(bindingsFile(saveDir), ALL_INPUTS);
	}


}
