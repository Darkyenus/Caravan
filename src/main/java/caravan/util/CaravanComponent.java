package caravan.util;

import com.badlogic.gdx.utils.Pool;
import com.darkyen.retinazer.Component;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Base interface for all components in this game. */
public abstract class CaravanComponent implements Component, Pool.Poolable {

	/** Save state to the output */
	public void save(@NotNull Output output) {}

	/** Load state from previously stored data with the same version. */
	public void load(@NotNull Input input, int version) {}

	/** Mark the class with this annotation to make it persistent. */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface Serialized {
		@NotNull String name();
		int version();
	}
}
