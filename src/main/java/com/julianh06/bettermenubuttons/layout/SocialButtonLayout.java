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
		apply(screenWidth, screenHeight, menuLeft, menuRight, menuTop, menuBottom, area, buttons);
	}

	public static void apply(int screenWidth, int screenHeight, int menuLeft, int menuRight, int menuTop, int menuBottom, MenuArea area, Map<MenuButton, AbstractWidget> buttons) {
		if (buttons.isEmpty()) {
			return;
		}

		int referenceY = buttons.values().stream().mapToInt(AbstractWidget::getY).min().orElse(menuBottom + GAP);
		EnumMap<ButtonPosition, List<AbstractWidget>> grouped = new EnumMap<>(ButtonPosition.class);
		List<AbstractWidget> vanillaIconButtons = new ArrayList<>();
		List<AbstractWidget> belowMenuButtons = new ArrayList<>();

		for (MenuButton buttonId : BetterMenuButtonsConfig.orderedButtons(area)) {
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

		for (MenuButton buttonId : BetterMenuButtonsConfig.orderedButtons(area)) {
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
			placePosition(entry.getKey(), screenWidth, screenHeight, menuLeft, menuRight, menuTop, referenceY, menuBottom, area, entry.getValue());
		}

		int belowMenuY = Math.min(screenHeight - BUTTON_SIZE - MARGIN, menuBottom + GAP);
		if (!vanillaIconButtons.isEmpty()) {
			int iconSlotY = iconSlotY(area, menuTop, menuBottom, screenHeight);
			placeRowsCentered(screenWidth, screenWidth / 2, iconSlotY, vanillaIconButtons);
			if (ModMenuIntegration.hasInsertedRow(area)) {
				int iconRows = rowsNeeded(screenWidth - MARGIN * 2, vanillaIconButtons.size());
				belowMenuY = Math.min(screenHeight - BUTTON_SIZE - MARGIN, iconSlotY + iconRows * (BUTTON_SIZE + GAP));
			}
		}
		if (!belowMenuButtons.isEmpty()) {
			placeRowsCentered(screenWidth, screenWidth / 2, belowMenuY, belowMenuButtons);
		}
	}

	private static void placePosition(ButtonPosition position, int screenWidth, int screenHeight, int menuLeft, int menuRight, int menuTop, int referenceY, int menuBottom, MenuArea area, List<AbstractWidget> buttons) {
		switch (position) {
			case LEFT_OF_MENU -> placeRowRightAligned(menuLeft - GAP, referenceY, buttons);
			case RIGHT_OF_MENU -> placeRowLeftAligned(menuRight + GAP, referenceY, buttons);
			case LEFT_ROW_1 -> placeRowRightAligned(menuLeft - GAP, rowY(area, menuTop, menuBottom, 0), buttons);
			case RIGHT_ROW_1 -> placeRowLeftAligned(menuRight + GAP, rowY(area, menuTop, menuBottom, 0), buttons);
			case LEFT_ROW_2 -> placeRowRightAligned(menuLeft - GAP, rowY(area, menuTop, menuBottom, 1), buttons);
			case RIGHT_ROW_2 -> placeRowLeftAligned(menuRight + GAP, rowY(area, menuTop, menuBottom, 1), buttons);
			case LEFT_ROW_3 -> placeRowRightAligned(menuLeft - GAP, rowY(area, menuTop, menuBottom, 2), buttons);
			case RIGHT_ROW_3 -> placeRowLeftAligned(menuRight + GAP, rowY(area, menuTop, menuBottom, 2), buttons);
			case LEFT_ROW_4 -> placeRowRightAligned(menuLeft - GAP, rowY(area, menuTop, menuBottom, 3), buttons);
			case RIGHT_ROW_4 -> placeRowLeftAligned(menuRight + GAP, rowY(area, menuTop, menuBottom, 3), buttons);
			case LEFT_ROW_5 -> placeRowRightAligned(menuLeft - GAP, rowY(area, menuTop, menuBottom, 4), buttons);
			case RIGHT_ROW_5 -> placeRowLeftAligned(menuRight + GAP, rowY(area, menuTop, menuBottom, 4), buttons);
			case ICON_SLOT_1 -> placeRowLeftAligned(iconSlotX(area, screenWidth, 0), iconSlotY(area, menuTop, menuBottom, screenHeight), buttons);
			case ICON_SLOT_2 -> placeRowLeftAligned(iconSlotX(area, screenWidth, 1), iconSlotY(area, menuTop, menuBottom, screenHeight), buttons);
			case ICON_SLOT_3 -> placeRowLeftAligned(iconSlotX(area, screenWidth, 2), iconSlotY(area, menuTop, menuBottom, screenHeight), buttons);
			case ICON_SLOT_4 -> placeRowLeftAligned(iconSlotX(area, screenWidth, 3), iconSlotY(area, menuTop, menuBottom, screenHeight), buttons);
			case ICON_SLOT_5 -> placeRowLeftAligned(iconSlotX(area, screenWidth, 4), iconSlotY(area, menuTop, menuBottom, screenHeight), buttons);
			case BELOW_MENU -> placeRowsCentered(screenWidth, screenWidth / 2, Math.min(screenHeight - BUTTON_SIZE - MARGIN, menuBottom + GAP), buttons);
			case TOP_LEFT -> placeRowLeftAligned(MARGIN, MARGIN, buttons);
			case TOP_RIGHT -> placeRowRightAligned(screenWidth - MARGIN, MARGIN, buttons);
			case BOTTOM_LEFT -> placeRowLeftAligned(MARGIN, screenHeight - BUTTON_SIZE - MARGIN, buttons);
			case BOTTOM_RIGHT -> placeRowRightAligned(screenWidth - MARGIN, screenHeight - BUTTON_SIZE - MARGIN, buttons);
			default -> {
			}
		}
	}

	private static int rowY(MenuArea area, int menuTop, int menuBottom, int rowIndex) {
		int[] rows = switch (area) {
			case PAUSE_MENU -> PAUSE_MENU_ROWS;
			case OPTIONS_MENU -> OPTIONS_MENU_ROWS;
			case MAIN_MENU -> MAIN_MENU_ROWS;
		};
		int clampedRowIndex = Math.min(rowIndex, rows.length - 1);
		if (clampedRowIndex == rows.length - 1) {
			return menuBottom - BUTTON_SIZE;
		}
		return menuTop + rows[clampedRowIndex];
	}

	private static int iconSlotX(MenuArea area, int screenWidth, int slotIndex) {
		int count = Math.max(1, BetterMenuButtonsConfig.vanillaIconSlotCount(area));
		int left = screenWidth / 2 - (BUTTON_SIZE * count + GAP * (count - 1)) / 2;
		return left + (BUTTON_SIZE + GAP) * Math.min(slotIndex, count - 1);
	}

	private static int iconSlotY(MenuArea area, int menuTop, int menuBottom, int screenHeight) {
		if (ModMenuIntegration.hasInsertedRow(area)) {
			return Math.min(screenHeight - BUTTON_SIZE - MARGIN, menuBottom + GAP);
		}
		return rowY(area, menuTop, menuBottom, area == MenuArea.PAUSE_MENU ? 2 : 3) + VANILLA_ICON_ROW_Y_OFFSET;
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
}
