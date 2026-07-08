package com.julianh06.bettermenubuttons.layout;

import com.julianh06.bettermenubuttons.config.ButtonPosition;
import com.julianh06.bettermenubuttons.config.MenuArea;
import com.julianh06.bettermenubuttons.config.MenuButton;
import com.julianh06.bettermenubuttons.config.BetterMenuButtonsConfig;
import com.julianh06.bettermenubuttons.config.SocialButtonMode;
import com.julianh06.bettermenubuttons.compat.ModMenuIntegration;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SocialButtonLayout {
	private static final int BUTTON_SIZE = 20;
	private static final int GAP = 4;
	private static final int MARGIN = 6;
	private static final int VANILLA_ICON_ROW_Y_OFFSET = -4;
	private static final int VANILLA_MENU_WIDTH = 204;
	private static final int[] MAIN_MENU_ROWS = {0, 24, 48, 76, 104};
	private static final int[] PAUSE_MENU_ROWS = {0, 24, 52, 80, 104};
	private static final int[] OPTIONS_MENU_ROWS = {0, 24, 48, 72, 96};

	private SocialButtonLayout() {
	}

	public static void apply(int screenWidth, int screenHeight, int menuTop, int menuBottom, List<AbstractWidget> socialButtons, SocialButtonMode mode) {
		if (socialButtons.isEmpty()) {
			return;
		}

		if (mode == SocialButtonMode.REMOVE) {
			for (AbstractWidget button : socialButtons) {
				button.visible = false;
				button.active = false;
			}
			return;
		}

		for (AbstractWidget button : socialButtons) {
			button.visible = true;
		}

		int menuLeft = screenWidth / 2 - VANILLA_MENU_WIDTH / 2;
		int menuRight = menuLeft + VANILLA_MENU_WIDTH;
		int referenceY = socialButtons.getFirst().getY();

		switch (mode) {
			case SPREAD -> placeSpread(menuLeft, menuRight, referenceY, socialButtons);
			case CORNERS -> placeCorners(screenWidth, screenHeight, socialButtons);
			case BELOW -> placeRowsCentered(screenWidth, screenWidth / 2, Math.min(screenHeight - BUTTON_SIZE - MARGIN, menuBottom + GAP), socialButtons);
			case SPLIT_AROUND -> placeSplitAround(menuLeft, menuRight, referenceY, socialButtons);
			default -> {
			}
		}
	}

	public static void apply(int screenWidth, int screenHeight, int menuTop, int menuBottom, MenuArea area, Map<MenuButton, AbstractWidget> buttons) {
		int menuLeft = screenWidth / 2 - VANILLA_MENU_WIDTH / 2;
		int menuRight = menuLeft + VANILLA_MENU_WIDTH;
		apply(screenWidth, screenHeight, menuLeft, menuRight, geometry(area, menuTop, menuBottom), area, buttons);
	}

	public static void apply(int screenWidth, int screenHeight, int menuLeft, int menuRight, int menuTop, int menuBottom, MenuArea area, Map<MenuButton, AbstractWidget> buttons) {
		apply(screenWidth, screenHeight, menuLeft, menuRight, geometry(area, menuTop, menuBottom), area, buttons);
	}

	public static void apply(int screenWidth, int screenHeight, int menuLeft, int menuRight, MenuGeometry geometry, MenuArea area, Map<MenuButton, AbstractWidget> buttons) {
		Map<String, AbstractWidget> buttonsById = new java.util.LinkedHashMap<>();
		for (Map.Entry<MenuButton, AbstractWidget> entry : buttons.entrySet()) {
			buttonsById.put(BetterMenuButtonsConfig.id(entry.getKey()), entry.getValue());
		}
		applyById(screenWidth, screenHeight, menuLeft, menuRight, geometry, area, buttonsById);
	}

	public static void applyById(int screenWidth, int screenHeight, int menuLeft, int menuRight, MenuGeometry geometry, MenuArea area, Map<String, AbstractWidget> buttons) {
		if (buttons.isEmpty()) {
			return;
		}

		int referenceY = buttons.values().stream().mapToInt(AbstractWidget::getY).min().orElse(geometry.menuBottom() + GAP);
		EnumMap<ButtonPosition, List<AbstractWidget>> grouped = new EnumMap<>(ButtonPosition.class);
		List<AbstractWidget> vanillaIconButtons = new ArrayList<>();
		List<AbstractWidget> belowMenuButtons = new ArrayList<>();

		for (String buttonId : BetterMenuButtonsConfig.orderedButtonIds(area)) {
			AbstractWidget button = buttons.get(buttonId);
			if (button == null) {
				continue;
			}

			ButtonPosition position = BetterMenuButtonsConfig.effectivePosition(area, buttonId);
			if (position == ButtonPosition.HIDDEN) {
				button.visible = false;
				button.active = false;
				continue;
			}

			button.visible = true;
			if (!BetterMenuButtonsConfig.isVanillaIconSlot(area, position) && position != ButtonPosition.BELOW_MENU) {
				grouped.computeIfAbsent(position, ignored -> new ArrayList<>()).add(button);
			}
		}

		for (String buttonId : BetterMenuButtonsConfig.orderedButtonIds(area)) {
			AbstractWidget button = buttons.get(buttonId);
			if (button == null) {
				continue;
			}

			ButtonPosition position = BetterMenuButtonsConfig.effectivePosition(area, buttonId);
			if (BetterMenuButtonsConfig.isVanillaIconSlot(area, position)) {
				button.visible = true;
				vanillaIconButtons.add(button);
			} else if (position == ButtonPosition.BELOW_MENU) {
				button.visible = true;
				belowMenuButtons.add(button);
			}
		}

		for (Map.Entry<ButtonPosition, List<AbstractWidget>> entry : grouped.entrySet()) {
			placePosition(entry.getKey(), screenWidth, screenHeight, menuLeft, menuRight, referenceY, geometry, area, entry.getValue());
		}

		int belowMenuY = Math.min(screenHeight - BUTTON_SIZE - MARGIN, geometry.menuBottom() + GAP);
		if (!vanillaIconButtons.isEmpty()) {
			int iconSlotY = iconSlotY(area, geometry, screenHeight);
			placeRowsCentered(screenWidth, screenWidth / 2, iconSlotY, vanillaIconButtons);
		}
		if (!belowMenuButtons.isEmpty()) {
			placeRowsCentered(screenWidth, screenWidth / 2, belowMenuY, belowMenuButtons);
		}
	}

	public static MenuGeometry geometry(MenuArea area, int menuTop, int menuBottom) {
		int[] offsets = switch (area) {
			case PAUSE_MENU -> PAUSE_MENU_ROWS;
			case OPTIONS_MENU -> OPTIONS_MENU_ROWS;
			case MAIN_MENU -> MAIN_MENU_ROWS;
		};
		int[] rowYs = new int[offsets.length];
		for (int index = 0; index < offsets.length; index++) {
			rowYs[index] = index == offsets.length - 1 ? menuBottom - BUTTON_SIZE : menuTop + offsets[index];
		}
		return new MenuGeometry(menuTop, menuBottom, rowYs, defaultIconRowY(area, rowYs));
	}

	public static MenuGeometry geometry(MenuArea area, int menuTop, int menuBottom, List<Integer> actualRows) {
		return geometry(area, menuTop, menuBottom, actualRows, null);
	}

	public static MenuGeometry geometry(MenuArea area, int menuTop, int menuBottom, List<Integer> actualRows, Integer actualIconRowY) {
		MenuGeometry fallback = geometry(area, menuTop, menuBottom);
		if (actualRows.isEmpty()) {
			return actualIconRowY == null ? fallback : fallback.withIconRowY(actualIconRowY);
		}

		List<Integer> rows = actualRows.stream().distinct().sorted().toList();
		int[] rowYs = fallback.rowYs().clone();
		if (area == MenuArea.PAUSE_MENU && rows.size() == 4) {
			rowYs[0] = rows.get(0);
			rowYs[1] = rows.get(1);
			rowYs[3] = rows.get(2);
			rowYs[4] = rows.get(3);
		} else {
			for (int index = 0; index < Math.min(rowYs.length, rows.size()); index++) {
				rowYs[index] = rows.get(index);
			}
		}
		int actualBottom = Math.max(menuBottom, rows.getLast() + BUTTON_SIZE);
		int iconRowY = actualIconRowY == null ? defaultIconRowY(area, rowYs) : actualIconRowY;
		return new MenuGeometry(Math.min(menuTop, rows.getFirst()), actualBottom, rowYs, iconRowY);
	}

	private static void placePosition(ButtonPosition position, int screenWidth, int screenHeight, int menuLeft, int menuRight, int referenceY, MenuGeometry geometry, MenuArea area, List<AbstractWidget> buttons) {
		switch (position) {
			case LEFT_OF_MENU -> placeRowRightAligned(menuLeft - GAP, referenceY, buttons);
			case RIGHT_OF_MENU -> placeRowLeftAligned(menuRight + GAP, referenceY, buttons);
			case LEFT_ROW_1 -> placeRowRightAligned(menuLeft - GAP, geometry.rowY(0), buttons);
			case RIGHT_ROW_1 -> placeRowLeftAligned(menuRight + GAP, geometry.rowY(0), buttons);
			case LEFT_ROW_2 -> placeRowRightAligned(menuLeft - GAP, geometry.rowY(1), buttons);
			case RIGHT_ROW_2 -> placeRowLeftAligned(menuRight + GAP, geometry.rowY(1), buttons);
			case LEFT_ROW_3 -> placeRowRightAligned(menuLeft - GAP, geometry.rowY(2), buttons);
			case RIGHT_ROW_3 -> placeRowLeftAligned(menuRight + GAP, geometry.rowY(2), buttons);
			case LEFT_ROW_4 -> placeRowRightAligned(menuLeft - GAP, geometry.rowY(3), buttons);
			case RIGHT_ROW_4 -> placeRowLeftAligned(menuRight + GAP, geometry.rowY(3), buttons);
			case LEFT_ROW_5 -> placeRowRightAligned(menuLeft - GAP, geometry.rowY(4), buttons);
			case RIGHT_ROW_5 -> placeRowLeftAligned(menuRight + GAP, geometry.rowY(4), buttons);
			case ICON_SLOT_1 -> placeRowLeftAligned(iconSlotX(area, screenWidth, 0), iconSlotY(area, geometry, screenHeight), buttons);
			case ICON_SLOT_2 -> placeRowLeftAligned(iconSlotX(area, screenWidth, 1), iconSlotY(area, geometry, screenHeight), buttons);
			case ICON_SLOT_3 -> placeRowLeftAligned(iconSlotX(area, screenWidth, 2), iconSlotY(area, geometry, screenHeight), buttons);
			case ICON_SLOT_4 -> placeRowLeftAligned(iconSlotX(area, screenWidth, 3), iconSlotY(area, geometry, screenHeight), buttons);
			case ICON_SLOT_5 -> placeRowLeftAligned(iconSlotX(area, screenWidth, 4), iconSlotY(area, geometry, screenHeight), buttons);
			case BELOW_MENU -> placeRowsCentered(screenWidth, screenWidth / 2, Math.min(screenHeight - BUTTON_SIZE - MARGIN, geometry.menuBottom() + GAP), buttons);
			case TOP_LEFT -> placeRowLeftAligned(MARGIN, MARGIN, buttons);
			case TOP_RIGHT -> placeRowRightAligned(screenWidth - MARGIN, MARGIN, buttons);
			case BOTTOM_LEFT -> placeRowLeftAligned(MARGIN, screenHeight - BUTTON_SIZE - MARGIN, buttons);
			case BOTTOM_RIGHT -> placeRowRightAligned(screenWidth - MARGIN, screenHeight - BUTTON_SIZE - MARGIN, buttons);
			default -> {
			}
		}
	}

	private static int iconSlotX(MenuArea area, int screenWidth, int slotIndex) {
		int count = Math.max(1, BetterMenuButtonsConfig.vanillaIconSlotCount(area));
		int left = screenWidth / 2 - (BUTTON_SIZE * count + GAP * (count - 1)) / 2;
		return left + (BUTTON_SIZE + GAP) * Math.min(slotIndex, count - 1);
	}

	private static int iconSlotY(MenuArea area, MenuGeometry geometry, int screenHeight) {
		if (ModMenuIntegration.hasInsertedRow(area)) {
			return Math.min(screenHeight - BUTTON_SIZE - MARGIN, geometry.insertedIconRowY(area));
		}
		return geometry.iconRowY();
	}

	private static int defaultIconRowY(MenuArea area, int[] rowYs) {
		return rowYs[area == MenuArea.PAUSE_MENU ? 2 : 3] + VANILLA_ICON_ROW_Y_OFFSET;
	}

	private static void placeSpread(int left, int right, int y, List<AbstractWidget> buttons) {
		if (buttons.size() == 1) {
			buttons.getFirst().setPosition((left + right - buttons.getFirst().getWidth()) / 2, y);
			return;
		}

		int available = right - left - BUTTON_SIZE;
		int last = buttons.size() - 1;
		for (int index = 0; index < buttons.size(); index++) {
			int x = left + Math.round((float) available * index / last);
			buttons.get(index).setPosition(x, y);
		}
	}

	private static void placeCorners(int screenWidth, int screenHeight, List<AbstractWidget> buttons) {
		int[][] corners = {
				{MARGIN, MARGIN},
				{screenWidth - BUTTON_SIZE - MARGIN, MARGIN},
				{MARGIN, screenHeight - BUTTON_SIZE - MARGIN},
				{screenWidth - BUTTON_SIZE - MARGIN, screenHeight - BUTTON_SIZE - MARGIN}
		};

		for (int index = 0; index < buttons.size(); index++) {
			int[] corner = corners[index % corners.length];
			int row = index / corners.length;
			int x = corner[0];
			int y = corner[1] + (corner[1] == MARGIN ? row : -row) * (BUTTON_SIZE + GAP);
			buttons.get(index).setPosition(x, y);
		}
	}

	private static void placeRowCentered(int centerX, int y, List<AbstractWidget> buttons) {
		int totalWidth = buttons.stream().mapToInt(AbstractWidget::getWidth).sum() + GAP * (buttons.size() - 1);
		int x = centerX - totalWidth / 2;
		for (AbstractWidget button : buttons) {
			button.setPosition(x, y);
			x += button.getWidth() + GAP;
		}
	}

	private static void placeRowsCentered(int screenWidth, int centerX, int y, List<AbstractWidget> buttons) {
		int maxButtonsPerRow = maxButtonsPerRow(screenWidth - MARGIN * 2);
		for (int start = 0; start < buttons.size(); start += maxButtonsPerRow) {
			int end = Math.min(buttons.size(), start + maxButtonsPerRow);
			placeRowCentered(centerX, y + (start / maxButtonsPerRow) * (BUTTON_SIZE + GAP), buttons.subList(start, end));
		}
	}

	private static void placeRowLeftAligned(int x, int y, List<AbstractWidget> buttons) {
		for (AbstractWidget button : buttons) {
			button.setPosition(x, y);
			x += button.getWidth() + GAP;
		}
	}

	private static void placeRowRightAligned(int right, int y, List<AbstractWidget> buttons) {
		int totalWidth = widthFor(buttons);
		placeRowLeftAligned(right - totalWidth, y, buttons);
	}

	private static void placeSplitAround(int menuLeft, int menuRight, int y, List<AbstractWidget> buttons) {
		int leftCount = (buttons.size() + 1) / 2;
		int rightCount = buttons.size() - leftCount;

		int leftWidth = widthFor(buttons.subList(0, leftCount));
		int x = menuLeft - GAP - leftWidth;
		for (int index = 0; index < leftCount; index++) {
			AbstractWidget button = buttons.get(index);
			button.setPosition(x, y);
			x += button.getWidth() + GAP;
		}

		x = menuRight + GAP;
		for (int index = leftCount; index < leftCount + rightCount; index++) {
			AbstractWidget button = buttons.get(index);
			button.setPosition(x, y);
			x += button.getWidth() + GAP;
		}
	}

	private static int widthFor(List<AbstractWidget> buttons) {
		if (buttons.isEmpty()) {
			return 0;
		}

		return buttons.stream().mapToInt(AbstractWidget::getWidth).sum() + GAP * (buttons.size() - 1);
	}

	private static int maxButtonsPerRow(int availableWidth) {
		return Math.max(1, (availableWidth + GAP) / (BUTTON_SIZE + GAP));
	}

	private static int rowsNeeded(int availableWidth, int buttonCount) {
		int maxButtonsPerRow = maxButtonsPerRow(availableWidth);
		return Math.max(1, (buttonCount + maxButtonsPerRow - 1) / maxButtonsPerRow);
	}

	public record MenuGeometry(int menuTop, int menuBottom, int[] rowYs, int iconRowY) {
		public int rowY(int rowIndex) {
			return this.rowYs[Math.min(rowIndex, this.rowYs.length - 1)];
		}

		public int insertedIconRowY(MenuArea area) {
			int insertedRow = area == MenuArea.PAUSE_MENU ? 2 : 3;
			return this.rowY(insertedRow) + BUTTON_SIZE + GAP;
		}

		public MenuGeometry withIconRowY(int iconRowY) {
			return new MenuGeometry(this.menuTop, this.menuBottom, this.rowYs, iconRowY);
		}
	}
}