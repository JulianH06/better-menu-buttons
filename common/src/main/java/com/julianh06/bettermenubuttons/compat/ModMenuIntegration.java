package com.julianh06.bettermenubuttons.compat;

import com.julianh06.bettermenubuttons.config.MenuArea;
import com.julianh06.bettermenubuttons.platform.PlatformHooks;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ModMenuIntegration {
	private static final String MODMENU_ID = "modmenu";
	private static final String NEOFORGE_ID = "neoforge";
	private static final String MODS_SCREEN_CLASS = "com.terraformersmc.modmenu.gui.ModsScreen";
	private static final String MODMENU_CONFIG_CLASS = "com.terraformersmc.modmenu.config.ModMenuConfig";
	private static final String MODMENU_BUTTON_CLASS = "com.terraformersmc.modmenu.gui.widget.ModMenuButtonWidget";
	private static final String MODMENU_ICON_BUTTON_CLASS = "com.terraformersmc.modmenu.gui.widget.UpdateCheckerTexturedButtonWidget";
	private static final String NEOFORGE_MODS_BUTTON_CLASS = "net.neoforged.neoforge.client.gui.widget.ModsButton";
	public static final Identifier NEOFORGE_MODS_SPRITE = Identifier.fromNamespaceAndPath(NEOFORGE_ID, "icon/neo_logo");

	private ModMenuIntegration() {
	}

	public static boolean isLoaded() {
		return PlatformHooks.isModLoaded(MODMENU_ID);
	}

	public static boolean hasIconButton(MenuArea area) {
		if (isNeoForgeLoaded()) {
			return area == MenuArea.MAIN_MENU || area == MenuArea.PAUSE_MENU;
		}

		return switch (area) {
			case MAIN_MENU -> "ICON".equals(titleButtonStyle());
			case PAUSE_MENU -> "ICON".equals(gameButtonStyle());
			case OPTIONS_MENU -> isLoaded();
		};
	}

	public static boolean hasInsertedRow(MenuArea area) {
		return switch (area) {
			case MAIN_MENU -> "CLASSIC".equals(titleButtonStyle());
			case PAUSE_MENU -> "INSERT".equals(gameButtonStyle());
			case OPTIONS_MENU -> false;
		};
	}

	public static int buttonLayoutState(MenuArea area) {
		if (isNeoForgeLoaded()) {
			return ("neoforge:" + area).hashCode();
		}

		return switch (area) {
			case MAIN_MENU -> ("title:" + titleButtonStyle()).hashCode();
			case PAUSE_MENU -> ("game:" + gameButtonStyle()).hashCode();
			case OPTIONS_MENU -> isLoaded() ? 1 : 0;
		};
	}

	public static boolean isModsIconButton(AbstractWidget widget) {
		String className = widget.getClass().getName();
		return (isLoaded() && MODMENU_ICON_BUTTON_CLASS.equals(className))
				|| (isNeoForgeLoaded() && NEOFORGE_MODS_BUTTON_CLASS.equals(className));
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

	public static String titleButtonStyle() {
		if (!isEnabled("MODIFY_TITLE_SCREEN")) {
			return "";
		}
		return enumOptionName("MODS_BUTTON_STYLE");
	}

	public static String gameButtonStyle() {
		if (!isEnabled("MODIFY_GAME_MENU")) {
			return "";
		}
		return enumOptionName("GAME_MENU_BUTTON_STYLE");
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

	public static boolean isNeoForgeLoaded() {
		return PlatformHooks.isModLoaded(NEOFORGE_ID);
	}
}