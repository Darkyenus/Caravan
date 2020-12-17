package caravan;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/**
 * Caravan game entry point for the LWJGL3 backend.
 */
public class Main {

	public static void main(String[] args) {
		Lwjgl3ApplicationConfiguration c = new Lwjgl3ApplicationConfiguration();
		c.setTitle("Caravan");
		c.useVsync(true);
		c.setResizable(true);
		c.setWindowedMode(800, 600);
		c.setWindowSizeLimits(200, 150, 40000, 30000);

		new Lwjgl3Application(new CaravanApplication(), c);
	}

}
