package com.julianh06.bettermenubuttons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BetterMenuButtons {
	public static final String MOD_ID = "better-menu-buttons";
	public static final String FORGE_MOD_ID = "better_menu_buttons";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private BetterMenuButtons() {
	}

	public static void initClient() {
		com.julianh06.bettermenubuttons.config.BetterMenuButtonsConfig.load();
	}
}