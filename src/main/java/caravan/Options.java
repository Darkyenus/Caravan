package caravan;

import caravan.input.InputFunction;
import com.badlogic.gdx.files.FileHandle;

/**
 * The persistent options of the application.
 */
public final class Options {

	//TODO



	private static FileHandle bindingsFile(FileHandle saveDir) {
		return saveDir.child("input.txt");
	}

	private static FileHandle optionsFile(FileHandle saveDir) {
		return saveDir.child("options.txt");
	}

	public static void load(FileHandle saveDir) {
		final FileHandle file = bindingsFile(saveDir);
		if (!InputFunction.load(file, Inputs.ALL_INPUTS)) {
			InputFunction.save(file, Inputs.ALL_INPUTS);
		}
	}

	public static void save(FileHandle saveDir) {
		InputFunction.save(bindingsFile(saveDir), Inputs.ALL_INPUTS);
	}


}
