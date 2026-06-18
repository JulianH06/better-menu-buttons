package com.julianh06.bettermenubuttons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.julianh06.bettermenubuttons.BetterMenuButtons;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class BetterMenuButtonsConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("better-menu-buttons.json");
	private static BetterMenuButtonsConfig instance = new BetterMenuButtonsConfig();

	public EnumMap<MenuArea, EnumMap<MenuButton, ButtonPosition>> buttonPositions = createDefaultPositions();
	public EnumMap<MenuArea, List<MenuButton>> buttonOrder = createDefaultButtonOrder();

	public static void load() {
		if (Files.notExists(CONFIG_PATH)) {
			saveDefault();
			return;
		}

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
			BetterMenuButtonsConfig config = GSON.fromJson(reader, BetterMenuButtonsConfig.class);
			instance = config != null ? config : new BetterMenuButtonsConfig();
		} catch (Exception exception) {
			BetterMenuButtons.LOGGER.warn("Could not read better-menu-buttons config, using defaults", exception);
			instance = new BetterMenuButtonsConfig();
		}
	}

	public static ButtonPosition position(MenuArea area, MenuButton button) {
		return instance.buttonPositions
				.computeIfAbsent(area, ignored -> new EnumMap<>(MenuButton.class))
				.computeIfAbsent(button, ignored -> defaultPosition(area, button));
	}

	public static ButtonPosition effectivePosition(MenuArea area, MenuButton button) {
		ButtonPosition position = position(area, button);
		ButtonPosition effectivePosition = position == ButtonPosition.VANILLA ? vanillaSlot(area, button) : position;
		return isVanillaSocialSidePosition(area, effectivePosition) ? ButtonPosition.HIDDEN : effectivePosition;
	}

	public static void setPosition(MenuArea area, MenuButton button, ButtonPosition position) {
		instance.buttonPositions
				.computeIfAbsent(area, ignored -> new EnumMap<>(MenuButton.class))
				.put(button, position);
		ensureOrdered(area, button);
	}

	public static void moveWithSwap(MenuArea area, MenuButton button, ButtonPosition targetPosition) {
		ButtonPosition target = targetPosition == ButtonPosition.VANILLA ? vanillaSlot(area, button) : targetPosition;
		if (target == ButtonPosition.HIDDEN) {
			setPosition(area, button, target);
			return;
		}

		ButtonPosition previous = effectivePosition(area, button);
		MenuButton occupant = occupantAt(area, target, button);
		setPosition(area, button, target);
		if (occupant != null) {
			setPosition(area, occupant, previous);
		}
	}

	public static void moveToSharedPosition(MenuArea area, MenuButton button, ButtonPosition targetPosition) {
		ButtonPosition target = targetPosition == ButtonPosition.VANILLA ? vanillaSlot(area, button) : targetPosition;
		setPosition(area, button, target);
		if (isVanillaIconSlot(area, target) || target.isSide()) {
			moveToPositionOrder(area, button, target);
		}
	}

	public static void moveToVanillaIconRow(MenuArea area, MenuButton button, int insertionIndex) {
		ButtonPosition current = effectivePosition(area, button);
		ButtonPosition target = isVanillaIconSlot(area, current) ? current : firstAvailableVanillaIconSlot(area, button);
        moveToSharedRow(area, button, target, insertionIndex);
	}

	public static void moveToSharedRow(MenuArea area, MenuButton button, ButtonPosition position, int insertionIndex) {
		setPosition(area, button, position);
		moveToRowOrder(area, button, position, insertionIndex);
	}

	public static void swapButtons(MenuArea area, MenuButton first, MenuButton second) {
		if (first == second) {
			return;
		}

		ButtonPosition firstPosition = effectivePosition(area, first);
		ButtonPosition secondPosition = effectivePosition(area, second);
		setPosition(area, first, secondPosition);
		setPosition(area, second, firstPosition);

		List<MenuButton> order = instance.buttonOrder.computeIfAbsent(area, BetterMenuButtonsConfig::defaultButtonOrder);
		int firstIndex = order.indexOf(first);
		int secondIndex = order.indexOf(second);
		if (firstIndex >= 0 && secondIndex >= 0) {
			order.set(firstIndex, second);
			order.set(secondIndex, first);
		}
	}

	private static void moveToRowOrder(MenuArea area, MenuButton button, ButtonPosition position, int insertionIndex) {
		List<MenuButton> order = instance.buttonOrder.computeIfAbsent(area, BetterMenuButtonsConfig::defaultButtonOrder);
		order.remove(button);
		boolean vanillaIconRow = isVanillaIconSlot(area, position);

		int rowButtonIndex = 0;
		int insertIndex = order.size();
		int clampedInsertionIndex = Math.max(0, insertionIndex);
		for (int index = 0; index < order.size(); index++) {
			MenuButton orderedButton = order.get(index);
			ButtonPosition orderedPosition = effectivePosition(area, orderedButton);
			boolean sameRow = vanillaIconRow ? isVanillaIconSlot(area, orderedPosition) : orderedPosition == position;
			if (!orderedButton.isAvailableIn(area) || !sameRow) {
				continue;
			}

			if (rowButtonIndex == clampedInsertionIndex) {
				insertIndex = index;
				break;
			}

			rowButtonIndex++;
			insertIndex = index + 1;
		}

		order.add(insertIndex, button);
	}

	public static List<MenuButton> orderedButtons(MenuArea area) {
		List<MenuButton> order = instance.buttonOrder.computeIfAbsent(area, BetterMenuButtonsConfig::defaultButtonOrder);
		order.removeIf(button -> !button.isAvailableIn(area));
		for (MenuButton button : availableButtons(area)) {
			if (!order.contains(button)) {
				order.add(button);
			}
		}
		return List.copyOf(order);
	}

	public static List<MenuButton> availableButtons(MenuArea area) {
		List<MenuButton> buttons = new ArrayList<>();
		for (MenuButton button : MenuButton.values()) {
			if (button.isAvailableIn(area) && button != MenuButton.BETTER_MENU_BUTTONS) {
				buttons.add(button);
			}
		}
		if (MenuButton.BETTER_MENU_BUTTONS.isAvailableIn(area)) {
			buttons.add(MenuButton.BETTER_MENU_BUTTONS);
		}
		return buttons;
	}

	public static boolean hasVanillaIconRowButtons(MenuArea area) {
		for (MenuButton button : MenuButton.values()) {
			if (button.isAvailableIn(area) && isVanillaIconSlot(area, effectivePosition(area, button))) {
				return true;
			}
		}
		return false;
	}

	public static boolean isOccupied(MenuArea area, ButtonPosition position, MenuButton except) {
		return occupantAt(area, position, except) != null;
	}

	public static void reset() {
		instance = new BetterMenuButtonsConfig();
	}

	public static void reset(MenuArea area) {
		instance.buttonPositions.put(area, defaultPositions(area));
		instance.buttonOrder.put(area, defaultButtonOrder(area));
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException exception) {
			BetterMenuButtons.LOGGER.warn("Could not write better-menu-buttons config", exception);
		}
	}

	private static EnumMap<MenuArea, EnumMap<MenuButton, ButtonPosition>> createDefaultPositions() {
		EnumMap<MenuArea, EnumMap<MenuButton, ButtonPosition>> positions = new EnumMap<>(MenuArea.class);
		for (MenuArea area : MenuArea.values()) {
			positions.put(area, defaultPositions(area));
		}
		return positions;
	}

	private static EnumMap<MenuButton, ButtonPosition> defaultPositions(MenuArea area) {
		EnumMap<MenuButton, ButtonPosition> positions = new EnumMap<>(MenuButton.class);
		for (MenuButton button : MenuButton.values()) {
			if (button.isAvailableIn(area)) {
				positions.put(button, defaultPosition(area, button));
			}
		}
		return positions;
	}

	private static EnumMap<MenuArea, List<MenuButton>> createDefaultButtonOrder() {
		EnumMap<MenuArea, List<MenuButton>> order = new EnumMap<>(MenuArea.class);
		for (MenuArea area : MenuArea.values()) {
			order.put(area, defaultButtonOrder(area));
		}
		return order;
	}

	private static List<MenuButton> defaultButtonOrder(MenuArea area) {
		return new ArrayList<>(availableButtons(area));
	}

	private static ButtonPosition defaultPosition(MenuArea area, MenuButton button) {
		if (button == MenuButton.BETTER_MENU_BUTTONS) {
			return area == MenuArea.OPTIONS_MENU ? ButtonPosition.TOP_LEFT : ButtonPosition.HIDDEN;
		}
		if (area == MenuArea.OPTIONS_MENU) {
			return ButtonPosition.HIDDEN;
		}
		return ButtonPosition.VANILLA;
	}

	public static ButtonPosition vanillaSlot(MenuArea area, MenuButton button) {
		return switch (area) {
			case MAIN_MENU -> switch (button) {
				case BETTER_MENU_BUTTONS -> ButtonPosition.HIDDEN;
				case FRIENDS -> ButtonPosition.ICON_SLOT_1;
				case LANGUAGE -> ButtonPosition.ICON_SLOT_2;
				case ACCESSIBILITY -> ButtonPosition.ICON_SLOT_3;
				case MODS -> ButtonPosition.ICON_SLOT_4;
				default -> ButtonPosition.ICON_SLOT_1;
			};
			case PAUSE_MENU -> switch (button) {
				case BETTER_MENU_BUTTONS -> ButtonPosition.HIDDEN;
				case BUG_REPORT -> ButtonPosition.ICON_SLOT_1;
				case FEEDBACK -> ButtonPosition.ICON_SLOT_2;
				case FRIENDS -> ButtonPosition.ICON_SLOT_3;
				case PLAYER_REPORTING -> ButtonPosition.ICON_SLOT_4;
				case MODS -> ButtonPosition.ICON_SLOT_5;
				default -> ButtonPosition.ICON_SLOT_1;
			};
			case OPTIONS_MENU -> ButtonPosition.HIDDEN;
		};
	}

	public static boolean isVanillaIconSlot(MenuArea area, ButtonPosition position) {
		return area != MenuArea.OPTIONS_MENU && iconSlotIndex(position) >= 0;
	}

	public static int vanillaIconSlotCount(MenuArea area) {
		return area == MenuArea.OPTIONS_MENU ? 0 : availableButtons(area).size();
	}

	public static boolean isVanillaSocialSidePosition(MenuArea area, ButtonPosition position) {
		return switch (area) {
			case MAIN_MENU -> switch (position) {
				case LEFT_OF_MENU, RIGHT_OF_MENU, LEFT_ROW_4, RIGHT_ROW_4 -> true;
				default -> false;
			};
			case PAUSE_MENU -> switch (position) {
				case LEFT_OF_MENU, RIGHT_OF_MENU, LEFT_ROW_3, RIGHT_ROW_3 -> true;
				default -> false;
			};
			case OPTIONS_MENU -> false;
		};
	}

	private static MenuButton occupantAt(MenuArea area, ButtonPosition position, MenuButton except) {
		for (MenuButton button : orderedButtons(area)) {
			if (button != except && button.isAvailableIn(area) && effectivePosition(area, button) == position) {
				return button;
			}
		}
		return null;
	}

	private static ButtonPosition firstAvailableVanillaIconSlot(MenuArea area, MenuButton except) {
		for (int slot = 0; slot < 5; slot++) {
			ButtonPosition position = iconSlotPosition(slot);
			if (occupantAt(area, position, except) == null) {
				return position;
			}
		}
		return ButtonPosition.ICON_SLOT_1;
	}

	private static ButtonPosition iconSlotPosition(int slot) {
		return switch (slot) {
			case 0 -> ButtonPosition.ICON_SLOT_1;
			case 1 -> ButtonPosition.ICON_SLOT_2;
			case 2 -> ButtonPosition.ICON_SLOT_3;
			case 3 -> ButtonPosition.ICON_SLOT_4;
			default -> ButtonPosition.ICON_SLOT_5;
		};
	}

	private static int iconSlotIndex(ButtonPosition position) {
		return switch (position) {
			case ICON_SLOT_1 -> 0;
			case ICON_SLOT_2 -> 1;
			case ICON_SLOT_3 -> 2;
			case ICON_SLOT_4 -> 3;
			case ICON_SLOT_5 -> 4;
			default -> -1;
		};
	}

	private static void ensureOrdered(MenuArea area, MenuButton button) {
		List<MenuButton> order = instance.buttonOrder.computeIfAbsent(area, BetterMenuButtonsConfig::defaultButtonOrder);
		if (button.isAvailableIn(area) && !order.contains(button)) {
			order.add(button);
		}
	}

	private static void moveToPositionOrder(MenuArea area, MenuButton button, ButtonPosition position) {
		List<MenuButton> order = instance.buttonOrder.computeIfAbsent(area, BetterMenuButtonsConfig::defaultButtonOrder);
		order.remove(button);

		int insertIndex = order.size();
		if (position.isLeftSide()) {
			insertIndex = firstButtonAt(area, order, position);
		} else if (position.isRightSide()) {
			insertIndex = lastButtonAt(area, order, position) + 1;
		}

		order.add(insertIndex, button);
	}

	private static int firstButtonAt(MenuArea area, List<MenuButton> order, ButtonPosition position) {
		for (int index = 0; index < order.size(); index++) {
			MenuButton button = order.get(index);
			if (button.isAvailableIn(area) && effectivePosition(area, button) == position) {
				return index;
			}
		}
		return order.size();
	}

	private static int lastButtonAt(MenuArea area, List<MenuButton> order, ButtonPosition position) {
		for (int index = order.size() - 1; index >= 0; index--) {
			MenuButton button = order.get(index);
			if (button.isAvailableIn(area) && effectivePosition(area, button) == position) {
				return index;
			}
		}
		return order.size() - 1;
	}

	private static void saveDefault() {
		instance = new BetterMenuButtonsConfig();
		save();
	}
}