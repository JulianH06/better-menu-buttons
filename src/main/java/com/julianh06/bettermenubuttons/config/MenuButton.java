package com.julianh06.bettermenubuttons.config;

import com.julianh06.bettermenubuttons.compat.ModMenuIntegration;

public enum MenuButton {
	BETTER_MENU_BUTTONS,
	FRIENDS,
	LANGUAGE,
	ACCESSIBILITY,
	BUG_REPORT,
	FEEDBACK,
	PLAYER_REPORTING,
	MODS;

	public boolean isAvailableIn(MenuArea area) {
		return switch (area) {
			case MAIN_MENU -> this == BETTER_MENU_BUTTONS || this == FRIENDS || this == LANGUAGE || this == ACCESSIBILITY || (this == MODS && ModMenuIntegration.hasIconButton(area));
			case PAUSE_MENU -> this == BETTER_MENU_BUTTONS || this == BUG_REPORT || this == FEEDBACK || this == FRIENDS || this == PLAYER_REPORTING || (this == MODS && ModMenuIntegration.hasIconButton(area));
			case OPTIONS_MENU -> this == BETTER_MENU_BUTTONS || (this == MODS && ModMenuIntegration.isLoaded());
		};
	}
}
