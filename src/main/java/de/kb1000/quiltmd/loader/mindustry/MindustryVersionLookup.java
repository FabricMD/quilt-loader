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

package de.kb1000.quiltmd.loader.mindustry;

import org.quiltmc.loader.impl.util.FileSystemUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class MindustryVersionLookup {
	private static final String DEFAULT_MAJOR_VERSION = "4";
	private static final String DEFAULT_BUILD_NUMBER = "-1";

	public static MindustryVersion getVersion(Path gameJar) {
		try (FileSystemUtil.FileSystemDelegate jarFs = FileSystemUtil.getJarFileSystem(gameJar, false)) {
			FileSystem fs = jarFs.get();
			Path file;

			// version.properties - contains major version number and build number
			if (Files.isRegularFile(file = fs.getPath("version.properties"))) {
				return fromVersionProperties(Files.newInputStream(file));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new MindustryVersion(DEFAULT_MAJOR_VERSION, DEFAULT_BUILD_NUMBER);
	}

	private static MindustryVersion fromVersionProperties(InputStream inputStream) throws IOException {
		Properties properties = new Properties();
		properties.load(inputStream);
		return new MindustryVersion(properties.getProperty("number", DEFAULT_MAJOR_VERSION), properties.getProperty("build", DEFAULT_BUILD_NUMBER));
	}

	public static MindustryVersion getVersion(String gameVersion) {
		String[] version = gameVersion.split("\\.", 1);
		return new MindustryVersion(version[0], version[1]);
	}

	public static final class MindustryVersion {
		private MindustryVersion(String number, String version) {
			this.raw = version;
			this.normalized = number + "." + version;
		}

		@Override
		public String toString() {
			return String.format("%s/%s", raw, normalized);
		}

		public final String raw; // raw version, e.g. 18w12a
		public final String normalized; // normalized version, usually Semver compliant version containing release and pre-release as applicable
	}
}
