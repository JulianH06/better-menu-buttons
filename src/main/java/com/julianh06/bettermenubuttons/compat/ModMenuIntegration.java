package com.julianh06.bettermenubuttons.compat;

import com.julianh06.bettermenubuttons.config.MenuArea;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ModMenuIntegration {
	private static final String MODMENU_ID = "modmenu";
	private static final String MODS_SCREEN_CLASS = "com.terraformersmc.modmenu.gui.ModsScreen";
	private static final String MODMENU_CONFIG_CLASS = "com.terraformersmc.modmenu.config.ModMenuConfig";
	private static final String MODMENU_BUTTON_CLASS = "com.terraformersmc.modmenu.gui.widget.ModMenuButtonWidget";
	private static final String MODMENU_ICON_BUTTON_CLASS = "com.terraformersmc.modmenu.gui.widget.UpdateCheckerTexturedButtonWidget";

	private ModMenuIntegration() {
	}

	public static boolean isLoaded() {
		return FabricLoader.getInstance().isModLoaded(MODMENU_ID);
	}

	public static boolean hasIconButton(MenuArea area) {
		return switch (area) {
			case MAIN_MENU -> titleStyleIs("ICON");
			case PAUSE_MENU -> gameStyleIs("ICON");
			case OPTIONS_MENU -> isLoaded();
		};
	}

	public static boolean hasInsertedRow(MenuArea area) {
		return switch (area) {
			case MAIN_MENU -> titleStyleIs("CLASSIC");
			case PAUSE_MENU -> gameStyleIs("INSERT");
			case OPTIONS_MENU -> false;
		};
	}

	public static int buttonLayoutState(MenuArea area) {
		int state = 0;
		if (hasIconButton(area)) {
			state |= 1;
		}
		if (hasInsertedRow(area)) {
			state |= 2;
		}
		return state;
	}

	public static boolean isModsIconButton(AbstractWidget widget) {
		return isLoaded() && MODMENU_ICON_BUTTON_CLASS.equals(widget.getClass().getName());
	}

	public static boolean isModsRowButton(AbstractWidget widget) {
		return isLoaded() && MODMENU_BUTTON_CLASS.equals(widget.getClass().getName());
	}

	public static Screen createModsScreen(Screen previous) {
		try {
			Class<?> modsScreen = Class.forName(MODS_SCREEN_CLASS);
			return (Screen) modsScreen.getConstructor(Screen.class).newInstance(previous);
		} catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
				 InvocationTargetException ignored) {
			return previous;
		}
	}

	private static boolean titleStyleIs(String expectedStyle) {
		return isEnabled("MODIFY_TITLE_SCREEN") && expectedStyle.equals(enumOptionName("MODS_BUTTON_STYLE"));
	}

	private static boolean gameStyleIs(String expectedStyle) {
		return isEnabled("MODIFY_GAME_MENU") && expectedStyle.equals(enumOptionName("GAME_MENU_BUTTON_STYLE"));
	}

	private static boolean isEnabled(String fieldName) {
		Object value = optionValue(fieldName);
		return value instanceof Boolean enabled && enabled;
	}

	private static String enumOptionName(String fieldName) {
		Object value = optionValue(fieldName);
		return value instanceof Enum<?> enumValue ? enumValue.name() : "";
	}

	private static Object optionValue(String fieldName) {
		if (!isLoaded()) {
			return null;
		}

		try {
			Class<?> configClass = Class.forName(MODMENU_CONFIG_CLASS);
			Field field = configClass.getField(fieldName);
			Object option = field.get(null);
			Method getValue = option.getClass().getMethod("getValue");
			return getValue.invoke(option);
		} catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException | NoSuchMethodException |
				 InvocationTargetException ignored) {
			return null;
		}
	}
}
