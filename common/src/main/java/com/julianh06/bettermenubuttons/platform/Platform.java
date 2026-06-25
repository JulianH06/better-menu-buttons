package com.julianh06.bettermenubuttons.platform;

import java.nio.file.Path;

public interface Platform {
	Path configDir();

	boolean isModLoaded(String modId);
}