@file:BuildDependencyPlugin("wemi-plugin-jvm-hotswap")
@file:BuildDependency("com.darkyen:ResourcePacker:2.5")

import com.darkyen.resourcepacker.PackingOperation
import com.darkyen.resourcepacker.PreferSymlinks
import wemi.Keys
import wemi.dependency.Jitpack
import wemi.dependency.ProjectDependency
import wemi.dependency.ScopeAggregate
import wemi.key
import wemi.util.FileSet
import wemi.util.SystemInfo
import wemi.util.plus
import wemiplugin.jvmhotswap.JvmHotswap.runHotswap

const val gdxVersion = "1.9.12"

val packedResourcesDir by key<Path>("Directory where packed resources are stored in")

val packResources by key<Unit>("Pack resources")

/**
 * The core of the project.
 */
val caravan:Project by project(Archetypes.JavaProject) {
	projectName set { "Caravan" }
	projectGroup set { "caravan" }
	projectVersion set { "0.0" }

	resources modify { it + FileSet(packedResourcesDir.get()) }

	repositories add { Jitpack }

	// Game Framework
	libraryDependencies add { dependency("com.badlogicgames.gdx", "gdx", gdxVersion) }

	// Nullability annotations
	libraryDependencies add { dependency("org.jetbrains", "annotations", "20.1.0", scope = ScopeProvided) }

	// Entity Component System
	libraryDependencies add { dependency("com.darkyen", "retinazer", "0.3.0") }

	packedResourcesDir set { path("assets") }
	packResources set {
		resourcePack(PackingOperation(path("resources").toFile(), packedResourcesDir.get().toFile(), listOf(PreferSymlinks to SystemInfo.IS_POSIX)))
	}

	run set { using(lwjgl3) { run.get() } } // Redirect running to lwjgl3 backend
	runHotswap set { using(lwjgl3) { runHotswap.get() } } // Redirect running to lwjgl3 backend
}

/**
 * LWJGL3 desktop launcher part.
 */
val lwjgl3 by project(path("lwjgl3"), Archetypes.JavaProject) {
	projectName set { using(caravan) { projectName.get() } }
	projectGroup set { using(caravan) { projectGroup.get() } }
	projectVersion set { using(caravan) { projectVersion.get() } }

	libraryDependencies add { wemi.dependency("com.badlogicgames.gdx", "gdx-backend-lwjgl3", gdxVersion) }
	libraryDependencies add { wemi.dependency("com.badlogicgames.gdx", "gdx-platform", gdxVersion, classifier = "natives-desktop") }

	projectDependencies add { ProjectDependency(caravan, scope = ScopeAggregate) }

	mainClass set { "caravan.Main" }

	runOptions add { "-agentlib:jdwp=transport=dt_socket,server=n,address=10.0.0.129:5005,suspend=y" }

	if (SystemInfo.IS_MAC_OS) {
		Keys.runOptions add { "-XstartOnFirstThread" }
	}
}