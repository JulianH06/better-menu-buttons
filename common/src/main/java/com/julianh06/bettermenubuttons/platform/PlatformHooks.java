package com.julianh06.bettermenubuttons.platform;

import java.nio.file.Path;

public final class PlatformHooks {
	private static Platform platform;

	private PlatformHooks() {
	}

	public static void init(Platform platform) {
		PlatformHooks.platform = platform;
	}

	public static Path configDir() {
		return platform.configDir();
	}

	public static boolean isModLoaded(String modId) {
		return platform.isModLoaded(modId);
	}
}