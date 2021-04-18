/*
 * Copyright 2016 FabricMC
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

package org.quiltmc.loader.impl.game;

import de.kb1000.quiltmd.loader.game.MindustryGameProvider;

import java.util.Arrays;
import java.util.List;

public final class GameProviders {
	private GameProviders() { }

	public static List<GameProvider> create() {
		return Arrays.asList(new MinecraftGameProvider(), new MindustryGameProvider());
	}
}
