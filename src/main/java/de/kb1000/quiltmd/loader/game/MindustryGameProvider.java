/*
 * Copyright 2021 kb1000
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.kb1000.quiltmd.loader.game;

import de.kb1000.quiltmd.loader.entrypoint.mindustry.EntrypointPatchHook;
import de.kb1000.quiltmd.loader.entrypoint.mindustry.EntrypointPatchPlatform;
import de.kb1000.quiltmd.loader.mindustry.MindustryVersionLookup;
import net.fabricmc.api.EnvType;
import org.quiltmc.loader.impl.entrypoint.EntrypointTransformer;
import org.quiltmc.loader.impl.game.GameProvider;
import org.quiltmc.loader.impl.game.GameProviderHelper;
import org.quiltmc.loader.impl.metadata.BuiltinModMetadata;
import org.quiltmc.loader.impl.util.SystemProperties;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MindustryGameProvider implements GameProvider {
	private EnvType envType;
	private String entrypoint;
	private MindustryVersionLookup.MindustryVersion versionData;
	private Path gameJar;

	private Path launchDirectory;
	private String[] arguments;

	public static final EntrypointTransformer TRANSFORMER = new EntrypointTransformer(it -> Arrays.asList(
			new EntrypointPatchPlatform(it),
			new EntrypointPatchHook(it)
	));

	@Override
	public String getGameId() {
		return "mindustry";
	}

	@Override
	public String getGameName() {
		return "Mindustry";
	}

	@Override
	public String getRawGameVersion() {
		return versionData.raw;
	}

	@Override
	public String getNormalizedGameVersion() {
		return versionData.normalized;
	}

	@Override
	public Collection<BuiltinMod> getBuiltinMods() {
		URL url;

		try {
			url = gameJar.toUri().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

		return Collections.singletonList(
				new BuiltinMod(url, new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
						.setName(getGameName())
						.build())
		);
	}

	@Override
	public String getEntrypoint() {
		return entrypoint;
	}

	@Override
	public Path getLaunchDirectory() {
		if (launchDirectory != null) {
			return launchDirectory;
		} else {
			return launchDirectory = getLaunchDirectoryImpl();
		}
	}

	private Path getLaunchDirectoryImpl() {
		// For servers, the directory is just "config" in the current directory.
		if (envType == EnvType.SERVER) {
			return Paths.get("config");
		}
		// Right now, this is a reimplementation of what's found in arc.util.OS#getAppDataDirectoryString,
		// without the dependencies, and directly returning a Path. This is needed because this is called
		// just after the GameProvider is loaded (no references to the game code permitted).
		// The API appears to have been designed for Minecraft, where the directory is easily statically
		// determinable (game argument or current directory).
		// FIXME: add support for Steam
		final String osName = System.getProperty("os.name");
		if (osName.contains("Windows")) {
			return Paths.get(System.getenv("AppData"), "Mindustry");
		} else if (osName.contains("Linux")) {
			if (System.getenv("XDG_DATA_HOME") != null) {
				return Paths.get(System.getenv("XDG_DATA_HOME"), "Mindustry");
			} else {
				return Paths.get(System.getProperty("user.home"), ".local", "share", "Mindustry");
			}
		} else if (osName.contains("Mac")) {
			return Paths.get(System.getProperty("user.home"), "Library", "Application Support", "Mindustry");
		} else {
			throw new RuntimeException("Unknown operating system");
		}
	}

	@Override
	public boolean isObfuscated() {
		return false;
	}

	@Override
	public boolean requiresUrlClassLoader() {
		return false;
	}

	@Override
	public List<Path> getGameContextJars() {
		return Collections.singletonList(gameJar);
	}


	@Override
	public boolean locateGame(EnvType envType, String[] args, ClassLoader loader) {
		this.envType = envType;
		this.arguments = args;

		List<String> entrypointClasses;

		if (envType == EnvType.CLIENT) {
			entrypointClasses = Collections.singletonList("mindustry.desktop.DesktopLauncher");
		} else {
			entrypointClasses = Collections.singletonList("mindustry.server.ServerLauncher");
		}

		Optional<GameProviderHelper.EntrypointResult> entrypointResult = GameProviderHelper.findFirstClass(loader, entrypointClasses);

		if (!entrypointResult.isPresent()) {
			return false;
		}

		entrypoint = entrypointResult.get().entrypointName;
		gameJar = entrypointResult.get().entrypointPath;

		String version = System.getProperty(SystemProperties.GAME_VERSION);
		versionData = version != null ? MindustryVersionLookup.getVersion(version) : MindustryVersionLookup.getVersion(gameJar);

		return true;
	}

	@Override
	public EntrypointTransformer getEntrypointTransformer() {
		return TRANSFORMER;
	}

	@Override
	public void launch(ClassLoader loader) {
		try {
			Class<?> c = loader.loadClass(entrypoint);
			Method m = c.getMethod("main", String[].class);
			m.invoke(null, (Object) arguments);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String[] getLaunchArguments(boolean sanitize) {
		// TODO: does this work?
		return new String[0];
	}
}
