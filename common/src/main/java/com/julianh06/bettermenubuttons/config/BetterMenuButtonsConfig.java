package com.julianh06.bettermenubuttons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.julianh06.bettermenubuttons.BetterMenuButtons;
import com.julianh06.bettermenubuttons.platform.PlatformHooks;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BetterMenuButtonsConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String VANILLA_PREFIX = "vanilla:";
	private static final String CUSTOM_PREFIX = "custom:";
	private static BetterMenuButtonsConfig instance = new BetterMenuButtonsConfig();

	public EnumMap<MenuArea, Map<String, ButtonPosition>> buttonPositionsById = createDefaultPositionsById();
	public EnumMap<MenuArea, List<String>> buttonOrderById = createDefaultButtonOrderById();
	public List<CustomJoinButton> customButtons = new ArrayList<>();
	@Deprecated public EnumMap<MenuArea, EnumMap<MenuButton, ButtonPosition>> buttonPositions;
	@Deprecated public EnumMap<MenuArea, List<MenuButton>> buttonOrder;

	public static void load() {
		Path configPath = configPath();
		if (Files.notExists(configPath)) {
			saveDefault();
			return;
		}

		try (Reader reader = Files.newBufferedReader(configPath)) {
			BetterMenuButtonsConfig config = GSON.fromJson(reader, BetterMenuButtonsConfig.class);
			instance = config != null ? config : new BetterMenuButtonsConfig();
			instance.migrate();
		} catch (Exception exception) {
			BetterMenuButtons.LOGGER.warn("Could not read better-menu-buttons config, using defaults", exception);
			instance = new BetterMenuButtonsConfig();
		}
	}

	public static ButtonPosition position(MenuArea area, MenuButton button) {
		return position(area, id(button));
	}

	public static ButtonPosition position(MenuArea area, String buttonId) {
		return instance.buttonPositionsById
				.computeIfAbsent(area, ignored -> new LinkedHashMap<>())
				.computeIfAbsent(buttonId, ignored -> defaultPosition(area, buttonId));
	}

	public static ButtonPosition effectivePosition(MenuArea area, MenuButton button) {
		ButtonPosition position = position(area, button);
		ButtonPosition effectivePosition = position == ButtonPosition.VANILLA ? vanillaSlot(area, button) : position;
		return isVanillaSocialSidePosition(area, effectivePosition) ? ButtonPosition.HIDDEN : effectivePosition;
	}

	public static ButtonPosition effectivePosition(MenuArea area, String buttonId) {
		ButtonPosition position = position(area, buttonId);
		ButtonPosition effectivePosition = position == ButtonPosition.VANILLA ? vanillaSlot(area, buttonId) : position;
		return isVanillaSocialSidePosition(area, effectivePosition) ? ButtonPosition.HIDDEN : effectivePosition;
	}

	public static void setPosition(MenuArea area, MenuButton button, ButtonPosition position) {
		setPosition(area, id(button), position);
	}

	public static void setPosition(MenuArea area, String buttonId, ButtonPosition position) {
		instance.buttonPositionsById
				.computeIfAbsent(area, ignored -> new LinkedHashMap<>())
				.put(buttonId, position);
		ensureOrdered(area, buttonId);
	}

	public static void moveWithSwap(MenuArea area, MenuButton button, ButtonPosition targetPosition) {
		moveWithSwap(area, id(button), targetPosition);
	}

	public static void moveWithSwap(MenuArea area, String buttonId, ButtonPosition targetPosition) {
		ButtonPosition target = targetPosition == ButtonPosition.VANILLA ? vanillaSlot(area, buttonId) : targetPosition;
		if (target == ButtonPosition.HIDDEN) {
			setPosition(area, buttonId, target);
			return;
		}

		ButtonPosition previous = effectivePosition(area, buttonId);
		String occupant = occupantAt(area, target, buttonId);
		setPosition(area, buttonId, target);
		if (occupant != null) {
			setPosition(area, occupant, previous);
		}
	}

	public static void moveToSharedPosition(MenuArea area, MenuButton button, ButtonPosition targetPosition) {
		moveToSharedPosition(area, id(button), targetPosition);
	}

	public static void moveToSharedPosition(MenuArea area, String buttonId, ButtonPosition targetPosition) {
		ButtonPosition target = targetPosition == ButtonPosition.VANILLA ? vanillaSlot(area, buttonId) : targetPosition;
		setPosition(area, buttonId, target);
		if (isVanillaIconSlot(area, target) || target.isSide()) {
			moveToPositionOrder(area, buttonId, target);
		}
	}

	public static void moveToVanillaIconRow(MenuArea area, MenuButton button, int insertionIndex) {
		moveToVanillaIconRow(area, id(button), insertionIndex);
	}

	public static void moveToVanillaIconRow(MenuArea area, String buttonId, int insertionIndex) {
		ButtonPosition current = effectivePosition(area, buttonId);
		ButtonPosition target = isVanillaIconSlot(area, current) ? current : firstAvailableVanillaIconSlot(area, buttonId);
        moveToSharedRow(area, buttonId, target, insertionIndex);
	}

	public static void moveToSharedRow(MenuArea area, MenuButton button, ButtonPosition position, int insertionIndex) {
		moveToSharedRow(area, id(button), position, insertionIndex);
	}

	public static void moveToSharedRow(MenuArea area, String buttonId, ButtonPosition position, int insertionIndex) {
		setPosition(area, buttonId, position);
		moveToRowOrder(area, buttonId, position, insertionIndex);
	}

	public static void swapButtons(MenuArea area, MenuButton first, MenuButton second) {
		swapButtons(area, id(first), id(second));
	}

	public static void swapButtons(MenuArea area, String first, String second) {
		if (first.equals(second)) {
			return;
		}

		ButtonPosition firstPosition = effectivePosition(area, first);
		ButtonPosition secondPosition = effectivePosition(area, second);
		setPosition(area, first, secondPosition);
		setPosition(area, second, firstPosition);

		List<String> order = instance.buttonOrderById.computeIfAbsent(area, BetterMenuButtonsConfig::defaultButtonOrderById);
		int firstIndex = order.indexOf(first);
		int secondIndex = order.indexOf(second);
		if (firstIndex >= 0 && secondIndex >= 0) {
			order.set(firstIndex, second);
			order.set(secondIndex, first);
		}
	}

	private static void moveToRowOrder(MenuArea area, String buttonId, ButtonPosition position, int insertionIndex) {
		List<String> order = instance.buttonOrderById.computeIfAbsent(area, BetterMenuButtonsConfig::defaultButtonOrderById);
		order.remove(buttonId);
		boolean vanillaIconRow = isVanillaIconSlot(area, position);

		int rowButtonIndex = 0;
		int insertIndex = order.size();
		int clampedInsertionIndex = Math.max(0, insertionIndex);
		for (int index = 0; index < order.size(); index++) {
			String orderedButton = order.get(index);
			ButtonPosition orderedPosition = effectivePosition(area, orderedButton);
			boolean sameRow = vanillaIconRow ? isVanillaIconSlot(area, orderedPosition) : orderedPosition == position;
			if (!isAvailableIn(area, orderedButton) || !sameRow) {
				continue;
			}

			if (rowButtonIndex == clampedInsertionIndex) {
				insertIndex = index;
				break;
			}

			rowButtonIndex++;
			insertIndex = index + 1;
		}

		order.add(insertIndex, buttonId);
	}

	public static List<MenuButton> orderedButtons(MenuArea area) {
		List<String> ids = orderedButtonIds(area);
		List<MenuButton> buttons = new ArrayList<>();
		for (String id : ids) {
			MenuButton button = vanillaButton(id);
			if (button != null) {
				buttons.add(button);
			}
		}
		return List.copyOf(buttons);
	}

	public static List<String> orderedButtonIds(MenuArea area) {
		List<String> order = instance.buttonOrderById.computeIfAbsent(area, BetterMenuButtonsConfig::defaultButtonOrderById);
		order.removeIf(buttonId -> !isAvailableIn(area, buttonId));
		for (String buttonId : availableButtonIds(area)) {
			if (!order.contains(buttonId)) {
				order.add(buttonId);
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

	public static List<String> availableButtonIds(MenuArea area) {
		List<String> buttons = new ArrayList<>();
		for (MenuButton button : availableButtons(area)) {
			buttons.add(id(button));
		}
		if (area == MenuArea.MAIN_MENU) {
			List<CustomJoinButton> customButtons = instance == null || instance.customButtons == null ? List.of() : instance.customButtons;
			for (CustomJoinButton button : customButtons) {
				if (button.enabled) {
					buttons.add(button.id);
				}
			}
		}
		return buttons;
	}

	public static List<CustomJoinButton> customButtons() {
		return List.copyOf(instance.customButtons);
	}

	public static CustomJoinButton customButton(String id) {
		for (CustomJoinButton button : instance.customButtons) {
			if (button.id.equals(id)) {
				return button;
			}
		}
		return null;
	}

	public static CustomJoinButton addCustomButton(CustomJoinButton.TargetType targetType, String targetId, String label) {
		CustomJoinButton button = new CustomJoinButton(CUSTOM_PREFIX + UUID.randomUUID(), label, targetType, targetId, true);
		instance.customButtons.add(button);
		setPosition(MenuArea.MAIN_MENU, button.id, ButtonPosition.BELOW_MENU);
		moveToSharedRow(MenuArea.MAIN_MENU, button.id, ButtonPosition.BELOW_MENU, Integer.MAX_VALUE);
		return button;
	}

	public static void removeCustomButton(String id) {
		instance.customButtons.removeIf(button -> button.id.equals(id));
		for (Map<String, ButtonPosition> positions : instance.buttonPositionsById.values()) {
			positions.remove(id);
		}
		for (List<String> order : instance.buttonOrderById.values()) {
			order.remove(id);
		}
	}

	public static void renameCustomButton(String id, String label) {
		CustomJoinButton button = customButton(id);
		if (button != null && label != null && !label.isBlank()) {
			button.label = label.trim();
		}
	}

	public static boolean hasVanillaIconRowButtons(MenuArea area) {
		for (MenuButton button : MenuButton.values()) {
			if (button.isAvailableIn(area) && isVanillaIconSlot(area, effectivePosition(area, button))) {
				return true;
			}
		}
		for (CustomJoinButton button : instance.customButtons) {
			if (button.enabled && isVanillaIconSlot(area, effectivePosition(area, button.id))) {
				return true;
			}
		}
		return false;
	}

	public static boolean isOccupied(MenuArea area, ButtonPosition position, MenuButton except) {
		return occupantAt(area, position, id(except)) != null;
	}

	public static void reset() {
		instance = new BetterMenuButtonsConfig();
	}

	public static void reset(MenuArea area) {
		instance.buttonPositionsById.put(area, defaultPositionsById(area));
		instance.buttonOrderById.put(area, defaultButtonOrderById(area));
	}

	public static void save() {
		Path configPath = configPath();
		try {
			Files.createDirectories(configPath.getParent());
			try (Writer writer = Files.newBufferedWriter(configPath)) {
				instance.buttonPositions = null;
				instance.buttonOrder = null;
				GSON.toJson(instance, writer);
			}
		} catch (IOException exception) {
			BetterMenuButtons.LOGGER.warn("Could not write better-menu-buttons config", exception);
		}
	}

	private static Path configPath() {
		return PlatformHooks.configDir().resolve("better-menu-buttons.json");
	}

	private static EnumMap<MenuArea, Map<String, ButtonPosition>> createDefaultPositionsById() {
		EnumMap<MenuArea, Map<String, ButtonPosition>> positions = new EnumMap<>(MenuArea.class);
		for (MenuArea area : MenuArea.values()) {
			positions.put(area, defaultPositionsById(area));
		}
		return positions;
	}

	private static Map<String, ButtonPosition> defaultPositionsById(MenuArea area) {
		Map<String, ButtonPosition> positions = new LinkedHashMap<>();
		for (MenuButton button : MenuButton.values()) {
			if (button.isAvailableIn(area)) {
				positions.put(id(button), defaultPosition(area, button));
			}
		}
		return positions;
	}

	private static EnumMap<MenuArea, List<String>> createDefaultButtonOrderById() {
		EnumMap<MenuArea, List<String>> order = new EnumMap<>(MenuArea.class);
		for (MenuArea area : MenuArea.values()) {
			order.put(area, defaultButtonOrderById(area));
		}
		return order;
	}

	private static List<String> defaultButtonOrderById(MenuArea area) {
		List<String> buttons = new ArrayList<>();
		for (MenuButton button : availableButtons(area)) {
			buttons.add(id(button));
		}
		return buttons;
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
		return vanillaSlot(area, id(button));
	}

	public static ButtonPosition vanillaSlot(MenuArea area, String buttonId) {
		MenuButton button = vanillaButton(buttonId);
		if (button == null) {
			return ButtonPosition.BELOW_MENU;
		}
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

	private static String occupantAt(MenuArea area, ButtonPosition position, String except) {
		for (String button : orderedButtonIds(area)) {
			if (!button.equals(except) && isAvailableIn(area, button) && effectivePosition(area, button) == position) {
				return button;
			}
		}
		return null;
	}

	private static ButtonPosition firstAvailableVanillaIconSlot(MenuArea area, String except) {
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

	private static void ensureOrdered(MenuArea area, String buttonId) {
		List<String> order = instance.buttonOrderById.computeIfAbsent(area, BetterMenuButtonsConfig::defaultButtonOrderById);
		if (isAvailableIn(area, buttonId) && !order.contains(buttonId)) {
			order.add(buttonId);
		}
	}

	private static void moveToPositionOrder(MenuArea area, String button, ButtonPosition position) {
		List<String> order = instance.buttonOrderById.computeIfAbsent(area, BetterMenuButtonsConfig::defaultButtonOrderById);
		order.remove(button);

		int insertIndex = order.size();
		if (position.isLeftSide()) {
			insertIndex = firstButtonAt(area, order, position);
		} else if (position.isRightSide()) {
			insertIndex = lastButtonAt(area, order, position) + 1;
		}

		order.add(insertIndex, button);
	}

	private static int firstButtonAt(MenuArea area, List<String> order, ButtonPosition position) {
		for (int index = 0; index < order.size(); index++) {
			String button = order.get(index);
			if (isAvailableIn(area, button) && effectivePosition(area, button) == position) {
				return index;
			}
		}
		return order.size();
	}

	private static int lastButtonAt(MenuArea area, List<String> order, ButtonPosition position) {
		for (int index = order.size() - 1; index >= 0; index--) {
			String button = order.get(index);
			if (isAvailableIn(area, button) && effectivePosition(area, button) == position) {
				return index;
			}
		}
		return order.size() - 1;
	}

	public static String id(MenuButton button) {
		return VANILLA_PREFIX + button.name();
	}

	public static boolean isCustomId(String buttonId) {
		return buttonId != null && buttonId.startsWith(CUSTOM_PREFIX);
	}

	public static MenuButton vanillaButton(String buttonId) {
		if (buttonId == null || !buttonId.startsWith(VANILLA_PREFIX)) {
			return null;
		}
		try {
			return MenuButton.valueOf(buttonId.substring(VANILLA_PREFIX.length()));
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	public static boolean isAvailableIn(MenuArea area, String buttonId) {
		MenuButton vanillaButton = vanillaButton(buttonId);
		if (vanillaButton != null) {
			return vanillaButton.isAvailableIn(area);
		}
		return area == MenuArea.MAIN_MENU && customButton(buttonId) != null;
	}

	private static ButtonPosition defaultPosition(MenuArea area, String buttonId) {
		MenuButton vanillaButton = vanillaButton(buttonId);
		return vanillaButton == null ? ButtonPosition.BELOW_MENU : defaultPosition(area, vanillaButton);
	}

	private void migrate() {
		if (this.buttonPositionsById == null) {
			this.buttonPositionsById = createDefaultPositionsById();
		}
		if (this.buttonOrderById == null) {
			this.buttonOrderById = createDefaultButtonOrderById();
		}
		if (this.customButtons == null) {
			this.customButtons = new ArrayList<>();
		}
		if (this.buttonPositions != null) {
			for (Map.Entry<MenuArea, EnumMap<MenuButton, ButtonPosition>> areaEntry : this.buttonPositions.entrySet()) {
				Map<String, ButtonPosition> positions = this.buttonPositionsById.computeIfAbsent(areaEntry.getKey(), ignored -> new LinkedHashMap<>());
				for (Map.Entry<MenuButton, ButtonPosition> buttonEntry : areaEntry.getValue().entrySet()) {
					positions.putIfAbsent(id(buttonEntry.getKey()), buttonEntry.getValue());
				}
			}
			this.buttonPositions = null;
		}
		if (this.buttonOrder != null) {
			for (Map.Entry<MenuArea, List<MenuButton>> areaEntry : this.buttonOrder.entrySet()) {
				List<String> order = this.buttonOrderById.computeIfAbsent(areaEntry.getKey(), ignored -> new ArrayList<>());
				if (order.isEmpty()) {
					for (MenuButton button : areaEntry.getValue()) {
						order.add(id(button));
					}
				}
			}
			this.buttonOrder = null;
		}
		for (MenuArea area : MenuArea.values()) {
			this.buttonPositionsById.computeIfAbsent(area, BetterMenuButtonsConfig::defaultPositionsById);
			this.buttonOrderById.computeIfAbsent(area, BetterMenuButtonsConfig::defaultButtonOrderById);
		}
	}

	public static class CustomJoinButton {
		public String id;
		public String label;
		public TargetType targetType;
		public String targetId;
		public boolean enabled = true;

		public CustomJoinButton() {
		}

		private CustomJoinButton(String id, String label, TargetType targetType, String targetId, boolean enabled) {
			this.id = id;
			this.label = label;
			this.targetType = targetType;
			this.targetId = targetId;
			this.enabled = enabled;
		}

		public enum TargetType {
			SERVER,
			WORLD
		}
	}

	private static void saveDefault() {
		instance = new BetterMenuButtonsConfig();
		save();
	}
}