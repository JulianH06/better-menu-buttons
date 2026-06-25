package com.julianh06.bettermenubuttons;

import com.julianh06.bettermenubuttons.platform.Platform;
import com.julianh06.bettermenubuttons.platform.PlatformHooks;
import net.fabricmc.api.ClientModInitializer;
import org.quiltmc.loader.api.QuiltLoader;

import java.nio.file.Path;

public class BetterMenuButtonsQuiltClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		PlatformHooks.init(new QuiltPlatform());
		BetterMenuButtons.initClient();
	}

	private record QuiltPlatform() implements Platform {
		@Override
		public Path configDir() {
			return QuiltLoader.getConfigDir();
		}

		@Override
		public boolean isModLoaded(String modId) {
			return QuiltLoader.isModLoaded(modId);
		}
	}
}