package com.julianh06.bettermenubuttons.config;

import com.julianh06.bettermenubuttons.mixin.OptionsScreenAccessor;
import com.julianh06.bettermenubuttons.compat.ModMenuIntegration;
import com.julianh06.bettermenubuttons.layout.SocialButtonLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommonButtons;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class BetterMenuButtonsConfigScreen extends Screen {
	private static final int BUTTON_HEIGHT = 20;
	private static final int ICON_SIZE = 20;
	private static final int GAP = 4;
	private static final int AREA_BUTTON_WIDTH = 96;
	private static final int SCREEN_MARGIN = 8;
	private static final int POPUP_PADDING = 6;
	private static final int DESIGN_PREVIEW_WIDTH = 480;
	private static final int DESIGN_PREVIEW_HEIGHT = 240;
	private static final float MIN_PREVIEW_SCALE = 0.55F;
	private static final int SIDE_BUTTON_WIDTH = 132;
	private static final int VANILLA_MENU_WIDTH = 204;
	private static final int OPTIONS_MENU_WIDTH = 308;
	private static final int OPTIONS_COLUMN_GAP = 8;
	private static final int VANILLA_ICON_ROW_Y_OFFSET = 0;
	private static final int HALF_BUTTON_WIDTH = 98;
	private static final int[] MAIN_MENU_ROWS = {0, 24, 48, 76, 104};
	private static final int[] PAUSE_MENU_ROWS = {0, 24, 52, 80, 104};
	private static final int[] OPTIONS_MENU_ROWS = {64, 88, 112, 136, 160};
	private static final Identifier BUG_REPORT_SPRITE = Identifier.withDefaultNamespace("pause_menu/bug");
	private static final Identifier FEEDBACK_SPRITE = Identifier.withDefaultNamespace("pause_menu/social_interactions");
	private static final Identifier PLAYER_REPORTING_SPRITE = Identifier.withDefaultNamespace("pause_menu/player_reporting");

	private final Screen parent;
	private final List<AbstractWidget> positionPreviewWidgets = new ArrayList<>();
	private final List<PlacedPreviewButton> placedPreviewButtons = new ArrayList<>();
	private final List<PlacedPreviewButton> popupWidgets = new ArrayList<>();
	private MenuArea selectedArea = MenuArea.MAIN_MENU;
	private ButtonPosition popupPosition;
	private String popupSwapButton;
	private int popupInsertionIndex;
	private int popupX;
	private int popupY;
	private int popupWidth;
	private int popupHeight;

	public BetterMenuButtonsConfigScreen(Screen parent) {
		this(parent, MenuArea.MAIN_MENU);
	}

	public BetterMenuButtonsConfigScreen(Screen parent, MenuArea selectedArea) {
		super(Component.literal("Move buttons"));
		this.parent = parent;
		this.selectedArea = selectedArea;
	}

	@Override
	protected void init() {
		this.closeCustomPreviewButtons();
		this.positionPreviewWidgets.clear();
		this.placedPreviewButtons.clear();
		this.popupWidgets.clear();

		PreviewLayout layout = this.previewLayout();
		int areaButtonLeft = layout.centerX() - (AREA_BUTTON_WIDTH * 3 + GAP * 2) / 2;
		this.addRenderableWidget(Button.builder(areaText(MenuArea.MAIN_MENU), button -> this.selectArea(MenuArea.MAIN_MENU))
				.bounds(areaButtonLeft, SCREEN_MARGIN, AREA_BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		this.addRenderableWidget(Button.builder(areaText(MenuArea.PAUSE_MENU), button -> this.selectArea(MenuArea.PAUSE_MENU))
				.bounds(areaButtonLeft + AREA_BUTTON_WIDTH + GAP, SCREEN_MARGIN, AREA_BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		this.addRenderableWidget(Button.builder(areaText(MenuArea.OPTIONS_MENU), button -> this.selectArea(MenuArea.OPTIONS_MENU))
				.bounds(areaButtonLeft + (AREA_BUTTON_WIDTH + GAP) * 2, SCREEN_MARGIN, AREA_BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
		if (this.selectedArea == MenuArea.MAIN_MENU) {
			this.addCustomButtonControls(layout);
		}

		if (this.selectedArea == MenuArea.MAIN_MENU) {
			this.addMainMenuPreview(layout);
		} else if (this.selectedArea == MenuArea.PAUSE_MENU) {
			this.addPauseMenuPreview(layout);
		} else {
			this.addOptionsMenuPreview(layout);
		}
		this.addPositionPreviewButtons(layout);
		this.addPlacedPreviewButtons(layout);
		this.addButtonPickerPopup(layout);

		int footerY = this.footerY();
		int footerButtonWidth = 64;
		int footerLeft = this.footerLeft();
		this.addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
					BetterMenuButtonsConfig.reset(this.selectedArea);
					this.popupPosition = null;
					this.popupSwapButton = null;
					this.rebuildWidgets();
				})
				.bounds(footerLeft, footerY, footerButtonWidth, BUTTON_HEIGHT)
				.build());
		this.addRenderableWidget(Button.builder(Component.literal("Hide"), button -> this.openButtonPicker(ButtonPosition.HIDDEN, 0, button.getX(), button.getY()))
				.bounds(footerLeft + footerButtonWidth + GAP, footerY, footerButtonWidth, BUTTON_HEIGHT)
				.build());
		this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.saveAndClose())
				.bounds(footerLeft + (footerButtonWidth + GAP) * 2, footerY, footerButtonWidth, BUTTON_HEIGHT)
				.build());
	}

	private void addCustomButtonControls(PreviewLayout layout) {
		int y = SCREEN_MARGIN + BUTTON_HEIGHT + GAP;
		int width = 76;
		int left = layout.centerX() - (width * 4 + GAP * 3) / 2;
		this.addRenderableWidget(Button.builder(Component.literal("Add Server"), button -> this.openAddServerScreen())
				.bounds(left, y, width, BUTTON_HEIGHT)
				.build());
		this.addRenderableWidget(Button.builder(Component.literal("Add World"), button -> this.openAddWorldScreen())
				.bounds(left + width + GAP, y, width, BUTTON_HEIGHT)
				.build());
		this.addRenderableWidget(Button.builder(Component.literal("Rename"), button -> this.openRenameCustomButtonScreen())
				.bounds(left + (width + GAP) * 2, y, width, BUTTON_HEIGHT)
				.build());
		this.addRenderableWidget(Button.builder(Component.literal("Remove"), button -> this.openRemoveCustomButtonScreen())
				.bounds(left + (width + GAP) * 3, y, width, BUTTON_HEIGHT)
				.build());
	}

	private int footerLeft() {
		int footerWidth = 64 * 3 + GAP * 2;
		return (this.width - footerWidth) / 2;
	}

	private int footerY() {
		return this.height - BUTTON_HEIGHT - SCREEN_MARGIN;
	}

	private void addMainMenuPreview(PreviewLayout layout) {
		SocialButtonLayout.MenuGeometry geometry = this.previewGeometry(layout);
		int menuLeft = layout.menuLeft();
		this.addDecorativeButton(layout, "Singleplayer", menuLeft + layout.scaled(2), geometry.rowY(0), layout.scaled(200));
		this.addDecorativeButton(layout, "Multiplayer", menuLeft + layout.scaled(2), geometry.rowY(1), layout.scaled(200));
		this.addDecorativeButton(layout, "Realms", menuLeft + layout.scaled(2), geometry.rowY(2), layout.scaled(200));
		if (ModMenuIntegration.hasInsertedRow(MenuArea.MAIN_MENU)) {
			this.addDecorativeButton(layout, "Mods", menuLeft + layout.scaled(2), geometry.rowY(3), layout.scaled(200));
		}

		int bottomRowY = ModMenuIntegration.hasInsertedRow(MenuArea.MAIN_MENU) ? geometry.rowY(4) : this.rawPreviewRowY(layout, layout.mainMenuTop(), 4);
		this.addDecorativeButton(layout, "Options", menuLeft + layout.scaled(2), bottomRowY, layout.halfButtonWidth());
		this.addDecorativeButton(layout, "Quit", layout.menuRight() - layout.halfButtonWidth() - layout.scaled(2), bottomRowY, layout.halfButtonWidth());
	}

	private void addPauseMenuPreview(PreviewLayout layout) {
		SocialButtonLayout.MenuGeometry geometry = this.previewGeometry(layout);
		int menuLeft = layout.menuLeft();
		this.addDecorativeButton(layout, "Back to Game", menuLeft, geometry.rowY(0), layout.menuWidth());
		this.addDecorativeButton(layout, "Advancements", menuLeft, geometry.rowY(1), layout.halfButtonWidth());
		this.addDecorativeButton(layout, "Stats", layout.menuRight() - layout.halfButtonWidth(), geometry.rowY(1), layout.halfButtonWidth());
		if (ModMenuIntegration.hasInsertedRow(MenuArea.PAUSE_MENU)) {
			this.addDecorativeButton(layout, "Mods", menuLeft, geometry.rowY(2), layout.menuWidth());
		}

		this.addDecorativeButton(layout, "Options", menuLeft, geometry.rowY(3), layout.menuWidth());
		this.addDecorativeButton(layout, "Disconnect", menuLeft, geometry.rowY(4), layout.menuWidth());
	}

	private void addOptionsMenuPreview(PreviewLayout layout) {
		int menuLeft = layout.optionsMenuLeft();
		int menuTop = layout.optionsMenuTop();
		int menuRight = layout.optionsMenuRight();
		int columnWidth = layout.optionsColumnWidth();
		this.addDecorativeButton(layout, "FOV: ...", menuLeft, menuTop, columnWidth);
		this.addDecorativeButton(layout, "Online...", menuRight - columnWidth, menuTop, columnWidth);
		this.addDecorativeButton(layout, "Skin Customization...", menuLeft, this.rawPreviewRowY(layout, menuTop, 0), columnWidth);
		this.addDecorativeButton(layout, "Music & Sounds...", menuRight - columnWidth, this.rawPreviewRowY(layout, menuTop, 0), columnWidth);
		this.addDecorativeButton(layout, "Video Settings...", menuLeft, this.rawPreviewRowY(layout, menuTop, 1), columnWidth);
		this.addDecorativeButton(layout, "Controls...", menuRight - columnWidth, this.rawPreviewRowY(layout, menuTop, 1), columnWidth);
		this.addDecorativeButton(layout, "Language...", menuLeft, this.rawPreviewRowY(layout, menuTop, 2), columnWidth);
		this.addDecorativeButton(layout, "Chat Settings...", menuRight - columnWidth, this.rawPreviewRowY(layout, menuTop, 2), columnWidth);
		this.addDecorativeButton(layout, "Resource Packs...", menuLeft, this.rawPreviewRowY(layout, menuTop, 3), columnWidth);
		this.addDecorativeButton(layout, "Accessibility Settings...", menuRight - columnWidth, this.rawPreviewRowY(layout, menuTop, 3), columnWidth);
		this.addDecorativeButton(layout, "Telemetry Data...", menuLeft, this.rawPreviewRowY(layout, menuTop, 4), columnWidth);
		this.addDecorativeButton(layout, "Credits & Attribution...", menuRight - columnWidth, this.rawPreviewRowY(layout, menuTop, 4), columnWidth);
	}

	private void addDecorativeButton(PreviewLayout layout, String label, int x, int y, int width) {
		Button button = Button.builder(Component.literal(label), ignored -> {
		}).bounds(x, y, width, layout.buttonHeight()).build();
		this.addRenderableWidget(button);
	}

	private void addPlacedPreviewButtons(PreviewLayout layout) {
		EnumMap<ButtonPosition, Integer> indexes = new EnumMap<>(ButtonPosition.class);
		Map<String, Integer> vanillaIconIndexes = this.vanillaIconIndexes();
		Map<String, Integer> belowMenuIndexes = this.rowIndexes(ButtonPosition.BELOW_MENU);
		int vanillaIconCount = vanillaIconIndexes.size();
		int belowMenuCount = belowMenuIndexes.size();
		for (String button : BetterMenuButtonsConfig.orderedButtonIds(this.selectedArea)) {
			if (!BetterMenuButtonsConfig.isAvailableIn(this.selectedArea, button)) {
				continue;
			}

			ButtonPosition position = BetterMenuButtonsConfig.effectivePosition(this.selectedArea, button);
			if (position == ButtonPosition.HIDDEN) {
				continue;
			}

			int index;
			int count;
			if (BetterMenuButtonsConfig.isVanillaIconSlot(this.selectedArea, position)) {
				index = vanillaIconIndexes.getOrDefault(button, 0);
				count = vanillaIconCount;
			} else if (position == ButtonPosition.BELOW_MENU) {
				index = belowMenuIndexes.getOrDefault(button, 0);
				count = belowMenuCount;
			} else {
				index = indexes.merge(position, 1, Integer::sum) - 1;
				count = this.countButtonsAt(position);
			}
			int insertionIndex = index;
			AbstractWidget widget = this.createIconButton(button, clicked -> this.openButtonPicker(position, insertionIndex, button, clicked.getX(), clicked.getY()));
			int[] coordinates = this.coordinatesForPosition(layout, position, index, count);
			widget.setPosition(coordinates[0], coordinates[1]);
			widget.setAlpha(0.92F);
			this.placedPreviewButtons.add(new PlacedPreviewButton(widget, button));
			this.addRenderableWidget(widget);
		}
	}

	private void addPositionPreviewButtons(PreviewLayout layout) {
		SocialButtonLayout.MenuGeometry geometry = this.previewGeometry(layout);
		int menuLeft = this.selectedArea == MenuArea.OPTIONS_MENU ? layout.optionsMenuLeft() : layout.menuLeft();
		int menuRight = this.selectedArea == MenuArea.OPTIONS_MENU ? layout.optionsMenuRight() : layout.menuRight();
		for (int row = 0; row < 5; row++) {
			ButtonPosition leftPosition = ButtonPosition.row(row, true);
			ButtonPosition rightPosition = ButtonPosition.row(row, false);
			if (!BetterMenuButtonsConfig.isVanillaSocialSidePosition(this.selectedArea, leftPosition)) {
				int rowY = geometry.rowY(row);
				this.addSidePositionButtons(layout, leftPosition, menuLeft - ICON_SIZE - layout.scaled(GAP), rowY);
				this.addSidePositionButtons(layout, rightPosition, menuRight + layout.scaled(GAP), rowY);
			}
		}
		this.addVanillaIconPositionButtons(layout, geometry);
		this.addBelowMenuPositionButtons(layout, geometry);
		int topLeftX = layout.safeLeft() + layout.cornerInset();
		int topRightX = layout.safeRight() - ICON_SIZE - layout.cornerInset();
		this.addCornerPositionButtons(layout, ButtonPosition.TOP_LEFT, topLeftX, this.topCornerY(layout, topLeftX));
		this.addCornerPositionButtons(layout, ButtonPosition.TOP_RIGHT, topRightX, this.topCornerY(layout, topRightX));
		this.addCornerPositionButtons(layout, ButtonPosition.BOTTOM_LEFT, layout.safeLeft() + layout.cornerInset(), layout.previewBottom() - ICON_SIZE - layout.cornerInset());
		this.addCornerPositionButtons(layout, ButtonPosition.BOTTOM_RIGHT, layout.safeRight() - ICON_SIZE - layout.cornerInset(), layout.previewBottom() - ICON_SIZE - layout.cornerInset());
	}

	private void addPositionButton(PreviewLayout layout, ButtonPosition position, int insertionIndex, int x, int y) {
		AbstractWidget widget = Button.builder(Component.literal("+"), button -> this.openButtonPicker(position, insertionIndex, button.getX(), button.getY()))
				.bounds(0, 0, ICON_SIZE, ICON_SIZE)
				.build();
		widget.setPosition(layout.clampX(x), layout.clampY(y));
		widget.setAlpha(this.previewAlpha());
		this.positionPreviewWidgets.add(widget);
		this.addRenderableWidget(widget);
	}

	private void addVanillaIconPositionButtons(PreviewLayout layout, SocialButtonLayout.MenuGeometry geometry) {
		if (BetterMenuButtonsConfig.vanillaIconSlotCount(this.selectedArea) <= 0) {
			return;
		}

		int y = this.iconSlotY(layout, geometry);
		int count = this.countVanillaIconButtons();
		ButtonPosition rowPosition = ButtonPosition.ICON_SLOT_1;
		if (count == 0) {
			this.addPositionButton(layout, rowPosition, 0, layout.centerX() - ICON_SIZE / 2, y);
			return;
		}

		int leftX = this.dynamicIconRowX(layout, count, 0) - ICON_SIZE - GAP;
		int rightX = this.dynamicIconRowX(layout, count, count - 1) + ICON_SIZE + GAP;
		this.addPositionButton(layout, rowPosition, 0, leftX, y);
		this.addPositionButton(layout, rowPosition, count, rightX, this.iconSlotY(layout, geometry, count - 1));
	}

	private void addBelowMenuPositionButtons(PreviewLayout layout, SocialButtonLayout.MenuGeometry geometry) {
		int y = this.belowMenuY(layout, geometry);
		int count = this.countButtonsAt(ButtonPosition.BELOW_MENU);
		if (count == 0) {
			this.addPositionButton(layout, ButtonPosition.BELOW_MENU, 0, layout.centerX() - ICON_SIZE / 2, y);
			return;
		}

		int leftX = this.dynamicIconRowX(layout, count, 0) - ICON_SIZE - GAP;
		int rightX = this.dynamicIconRowX(layout, count, count - 1) + ICON_SIZE + GAP;
		this.addPositionButton(layout, ButtonPosition.BELOW_MENU, 0, leftX, y);
		this.addPositionButton(layout, ButtonPosition.BELOW_MENU, count, rightX, this.rowWrappedY(layout, y, count - 1));
	}

	private void addSidePositionButtons(PreviewLayout layout, ButtonPosition position, int baseX, int y) {
		int count = this.countButtonsAt(position);
		if (count == 0) {
			this.addPositionButton(layout, position, 0, baseX, y);
			return;
		}

		if (count >= BetterMenuButtonsConfig.availableButtonIds(this.selectedArea).size()) {
			return;
		}

		int x = position.isLeftSide() ? baseX - count * (ICON_SIZE + GAP) : baseX + count * (ICON_SIZE + GAP);
		int insertionIndex = position.isLeftSide() ? 0 : count;
		this.addPositionButton(layout, position, insertionIndex, x, y);
	}

	private void addCornerPositionButtons(PreviewLayout layout, ButtonPosition position, int baseX, int baseY) {
		int count = this.countButtonsAt(position);
		if (count == 0) {
			this.addPositionButton(layout, position, 0, baseX, baseY);
			return;
		}

		if (count >= BetterMenuButtonsConfig.availableButtonIds(this.selectedArea).size()) {
			return;
		}

		boolean leftCorner = position.isLeftCorner();
		int x = leftCorner ? baseX + count * (ICON_SIZE + GAP) : baseX - count * (ICON_SIZE + GAP);
		int insertionIndex = leftCorner ? count : 0;
		this.addPositionButton(layout, position, insertionIndex, x, baseY);
	}

	private void addButtonPickerPopup(PreviewLayout layout) {
		if (this.popupPosition == null) {
			this.popupWidth = 0;
			this.popupHeight = 0;
			return;
		}

		List<String> buttons = BetterMenuButtonsConfig.availableButtonIds(this.selectedArea);
		int contentWidth = buttons.size() * ICON_SIZE + (buttons.size() - 1) * GAP;
		this.popupWidth = contentWidth + POPUP_PADDING * 2;
		this.popupHeight = ICON_SIZE + POPUP_PADDING * 2;
		this.popupX = layout.clampX(this.popupX, this.popupWidth);
		this.popupY = Math.max(SCREEN_MARGIN, Math.min(this.height - this.popupHeight - SCREEN_MARGIN, this.popupY));

		SolidPanelWidget panel = new SolidPanelWidget(this.popupX, this.popupY, this.popupWidth, this.popupHeight);
		panel.active = false;
		this.addRenderableWidget(panel);

		int x = this.popupX + POPUP_PADDING;
		int y = this.popupY + POPUP_PADDING;
		for (String button : buttons) {
			AbstractWidget widget = this.createIconButton(button, clicked -> this.moveButtonToPopupPosition(button));
			widget.setPosition(x, y);
			widget.setAlpha(this.isButtonAtPopupPosition(button) ? 1.0F : 0.78F);
			this.popupWidgets.add(new PlacedPreviewButton(widget, button));
			this.addRenderableWidget(widget);
			x += ICON_SIZE + GAP;
		}
	}

	private void openButtonPicker(ButtonPosition position, int insertionIndex, int x, int y) {
		this.openButtonPicker(position, insertionIndex, null, x, y);
	}

	private void openButtonPicker(ButtonPosition position, int insertionIndex, String swapButton, int x, int y) {
		this.popupPosition = position;
		this.popupSwapButton = swapButton;
		this.popupInsertionIndex = Math.max(0, insertionIndex);
		this.popupX = x;
		this.popupY = y - ICON_SIZE - POPUP_PADDING * 2 - GAP;
		this.rebuildWidgets();
	}

	private void moveButtonToPopupPosition(String button) {
		if (this.popupPosition == null) {
			return;
		}

		if (this.popupSwapButton != null) {
			BetterMenuButtonsConfig.swapButtons(this.selectedArea, button, this.popupSwapButton);
		} else if (this.popupPosition == ButtonPosition.HIDDEN) {
			BetterMenuButtonsConfig.moveWithSwap(this.selectedArea, button, ButtonPosition.HIDDEN);
		} else if (this.popupPosition == ButtonPosition.BELOW_MENU) {
			BetterMenuButtonsConfig.moveToSharedRow(this.selectedArea, button, ButtonPosition.BELOW_MENU, this.popupInsertionIndex);
		} else if (BetterMenuButtonsConfig.isVanillaIconSlot(this.selectedArea, this.popupPosition)) {
			this.moveToVanillaPopupSlot(button);
		} else if (this.popupPosition.isSide() || this.popupPosition.isCorner()) {
			BetterMenuButtonsConfig.moveToSharedRow(this.selectedArea, button, this.popupPosition, this.popupInsertionIndex);
		} else {
			BetterMenuButtonsConfig.moveWithSwap(this.selectedArea, button, this.popupPosition);
		}

		this.popupPosition = null;
		this.popupSwapButton = null;
		this.rebuildWidgets();
	}

	private boolean isButtonAtPopupPosition(String button) {
		return this.popupPosition != null && BetterMenuButtonsConfig.effectivePosition(this.selectedArea, button) == this.popupPosition;
	}

	private void moveToVanillaPopupSlot(String button) {
		BetterMenuButtonsConfig.moveToVanillaIconRow(this.selectedArea, button, this.popupInsertionIndex);
	}

	private int countVanillaIconButtons() {
		int count = 0;
		for (String button : BetterMenuButtonsConfig.availableButtonIds(this.selectedArea)) {
			if (BetterMenuButtonsConfig.isVanillaIconSlot(this.selectedArea, BetterMenuButtonsConfig.effectivePosition(this.selectedArea, button))) {
				count++;
			}
		}
		return count;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.popupPosition != null) {
			for (PlacedPreviewButton popupButton : this.popupWidgets.reversed()) {
				AbstractWidget widget = popupButton.widget();
				if (widget.isMouseOver(event.x(), event.y()) && widget.mouseClicked(event, doubleClick)) {
					return true;
				}
			}

			if (event.x() >= this.popupX && event.x() <= this.popupX + this.popupWidth && event.y() >= this.popupY && event.y() <= this.popupY + this.popupHeight) {
				return true;
			}

			this.popupPosition = null;
			this.popupSwapButton = null;
			this.rebuildWidgets();
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
		graphics.fill(0, 0, this.width, this.height, 0x66000000);
		this.positionPreviewWidgets.forEach(widget -> widget.setAlpha(this.previewAlpha()));
		this.placedPreviewButtons.forEach(button -> button.widget().setAlpha(0.92F));
		if (this.popupConsumesHover(mouseX, mouseY)) {
			super.extractRenderState(graphics, Integer.MIN_VALUE, Integer.MIN_VALUE, deltaTicks);
			for (PlacedPreviewButton popupButton : this.popupWidgets) {
				popupButton.widget().extractRenderState(graphics, mouseX, mouseY, deltaTicks);
			}
		} else {
			super.extractRenderState(graphics, mouseX, mouseY, deltaTicks);
		}
		this.drawSelectedOutlines(graphics);
		if (this.popupPosition != null) {
			graphics.outline(this.popupX - 1, this.popupY - 1, this.popupWidth + 2, this.popupHeight + 2, 0xFFFFFFFF);
			this.extractPopupTooltip(graphics, mouseX, mouseY);
		}
	}

	@Override
	public void onClose() {
		this.closeCustomPreviewButtons();
		this.saveAndClose();
	}

	private void closeCustomPreviewButtons() {
		for (PlacedPreviewButton placedButton : this.placedPreviewButtons) {
			if (placedButton.widget() instanceof CustomJoinIconButton customButton) {
				customButton.close();
			}
		}
		for (PlacedPreviewButton placedButton : this.popupWidgets) {
			if (placedButton.widget() instanceof CustomJoinIconButton customButton) {
				customButton.close();
			}
		}
	}

	private void selectArea(MenuArea area) {
		this.selectedArea = area;
		this.popupPosition = null;
		this.popupSwapButton = null;
		this.rebuildWidgets();
	}

	private void openAddServerScreen() {
		if (this.minecraft == null) {
			return;
		}
		List<CustomJoinSelectionScreen.Entry> entries = new ArrayList<>();
		for (net.minecraft.client.multiplayer.ServerData server : CustomJoinTargets.servers(this.minecraft)) {
			entries.add(new CustomJoinSelectionScreen.Entry(Component.literal(server.name + " - " + server.ip), BetterMenuButtonsConfig.CustomJoinButton.TargetType.SERVER, server.ip, () -> {
				BetterMenuButtonsConfig.addCustomButton(BetterMenuButtonsConfig.CustomJoinButton.TargetType.SERVER, server.ip, server.name);
				BetterMenuButtonsConfig.save();
				Screen screen = new BetterMenuButtonsConfigScreen(this.parent, MenuArea.MAIN_MENU);
				this.minecraft.getInstance().gui.setScreen(screen);
			}));
		}
		Screen screen = new CustomJoinSelectionScreen(this, Component.literal("Add server button"), entries);
		this.minecraft.getInstance().gui.setScreen(screen);
	}

	private void openAddWorldScreen() {
		if (this.minecraft == null) {
			return;
		}
		List<CustomJoinSelectionScreen.Entry> entries = new ArrayList<>();
		for (net.minecraft.world.level.storage.LevelSummary world : CustomJoinTargets.worlds(this.minecraft)) {
			entries.add(new CustomJoinSelectionScreen.Entry(Component.literal(world.getLevelName() + " - " + world.getLevelId()), BetterMenuButtonsConfig.CustomJoinButton.TargetType.WORLD, world.getLevelId(), () -> {
				BetterMenuButtonsConfig.addCustomButton(BetterMenuButtonsConfig.CustomJoinButton.TargetType.WORLD, world.getLevelId(), world.getLevelName());
				BetterMenuButtonsConfig.save();
				Screen screen = new BetterMenuButtonsConfigScreen(this.parent, MenuArea.MAIN_MENU);
				this.minecraft.getInstance().gui.setScreen(screen);
			}));
		}
		Screen screen = new CustomJoinSelectionScreen(this, Component.literal("Add world button"), entries);
		this.minecraft.getInstance().gui.setScreen(screen);
	}

	private void openRemoveCustomButtonScreen() {
		if (this.minecraft == null) {
			return;
		}
		List<CustomJoinSelectionScreen.Entry> entries = new ArrayList<>();
		for (BetterMenuButtonsConfig.CustomJoinButton customButton : BetterMenuButtonsConfig.customButtons()) {
			entries.add(new CustomJoinSelectionScreen.Entry(Component.literal(customButton.label), customButton.targetType, customButton.targetId, () -> {
				BetterMenuButtonsConfig.removeCustomButton(customButton.id);
				BetterMenuButtonsConfig.save();
				Screen screen = new BetterMenuButtonsConfigScreen(this.parent, MenuArea.MAIN_MENU);
				this.minecraft.getInstance().gui.setScreen(screen);
			}));
		}
		Screen screen = new CustomJoinSelectionScreen(this, Component.literal("Remove custom button"), entries);
		this.minecraft.getInstance().gui.setScreen(screen);
	}

	private void openRenameCustomButtonScreen() {
		if (this.minecraft == null) {
			return;
		}
		List<CustomJoinSelectionScreen.Entry> entries = new ArrayList<>();
		for (BetterMenuButtonsConfig.CustomJoinButton customButton : BetterMenuButtonsConfig.customButtons()) {
			entries.add(new CustomJoinSelectionScreen.Entry(Component.literal(customButton.label), customButton.targetType, customButton.targetId, () -> {
				Screen screen = new CustomJoinRenameScreen(this, customButton.id, customButton.label);
				this.minecraft.getInstance().gui.setScreen(screen);
			}));
		}
		Screen screen = new CustomJoinSelectionScreen(this, Component.literal("Rename custom button"), entries);
		this.minecraft.getInstance().gui.setScreen(screen);
	}

	private PreviewLayout previewLayout() {
		return PreviewLayout.create(this.width, this.height, BetterMenuButtonsConfig.availableButtonIds(this.selectedArea).size());
	}

	private void saveAndClose() {
		BetterMenuButtonsConfig.save();

		if (this.minecraft != null) {
			Screen screen;
			if (this.parent instanceof OptionsScreen optionsScreen) {
				screen = new OptionsScreen(optionsScreen.getLastScreen(), this.minecraft.options, ((OptionsScreenAccessor) optionsScreen).better_menu_buttons$inWorld());
			} else {
				screen = this.parent;
			}
			this.minecraft.getInstance().gui.setScreen(screen);
		}
	}

	private Component areaText(MenuArea area) {
		String label = areaName(area).getString();
		return Component.literal(area == this.selectedArea ? "> " + label : label);
	}

	private static Component areaName(MenuArea area) {
		return switch (area) {
			case MAIN_MENU -> Component.literal("Main menu");
			case PAUSE_MENU -> Component.literal("Pause menu");
			case OPTIONS_MENU -> Component.literal("Options menu");
		};
	}

	private static Component buttonLabel(String buttonId) {
		BetterMenuButtonsConfig.CustomJoinButton customButton = BetterMenuButtonsConfig.customButton(buttonId);
		if (customButton != null) {
			return Component.literal(customButton.label);
		}
		MenuButton button = BetterMenuButtonsConfig.vanillaButton(buttonId);
		if (button == null) {
			return Component.literal(buttonId);
		}
		return switch (button) {
			case BETTER_MENU_BUTTONS -> BetterMenuButtonsIconButton.label();
			case FRIENDS -> Component.literal("Friends");
			case LANGUAGE -> Component.literal("Change language");
			case ACCESSIBILITY -> Component.literal("Accessibility settings");
			case BUG_REPORT -> Component.literal("Report bugs");
			case FEEDBACK -> Component.literal("Feedback");
			case PLAYER_REPORTING -> Component.literal("Player reporting");
			case MODS -> Component.literal("Mods");
		};
	}

	private AbstractWidget createIconButton(String buttonId, Button.OnPress onPress) {
		BetterMenuButtonsConfig.CustomJoinButton customButton = BetterMenuButtonsConfig.customButton(buttonId);
		if (customButton != null) {
			AbstractWidget widget = new CustomJoinIconButton(0, 0, customButton, onPress);
			widget.setTooltip(Tooltip.create(buttonLabel(buttonId)));
			return widget;
		}
		MenuButton button = BetterMenuButtonsConfig.vanillaButton(buttonId);
		AbstractWidget widget = switch (button) {
			case BETTER_MENU_BUTTONS -> new BetterMenuButtonsIconButton(0, 0, onPress);
			case FRIENDS -> CommonButtons.friends(ICON_SIZE, onPress, true);
			case LANGUAGE -> CommonButtons.language(ICON_SIZE, onPress, true);
			case ACCESSIBILITY -> CommonButtons.accessibility(ICON_SIZE, onPress, true);
			case BUG_REPORT -> spriteButton(buttonLabel(buttonId), BUG_REPORT_SPRITE, onPress);
			case FEEDBACK -> spriteButton(buttonLabel(buttonId), FEEDBACK_SPRITE, onPress);
			case PLAYER_REPORTING -> spriteButton(buttonLabel(buttonId), PLAYER_REPORTING_SPRITE, onPress);
			case MODS -> new ModMenuIconButton(0, 0, onPress);
		};
		widget.setTooltip(Tooltip.create(buttonLabel(buttonId)));
		return widget;
	}

	private static SpriteIconButton spriteButton(Component message, Identifier sprite, Button.OnPress onPress) {
		return SpriteIconButton.builder(message, onPress, true)
				.width(ICON_SIZE)
				.sprite(sprite, 15, 15)
				.build();
	}

	private float previewAlpha() {
		long time = System.currentTimeMillis();
		return 0.45F + (float) Math.sin(time / 850.0D) * 0.08F;
	}

	private void drawSelectedOutlines(GuiGraphicsExtractor graphics) {
		for (AbstractWidget widget : this.positionPreviewWidgets) {
			if (this.intersectsPopup(widget)) {
				continue;
			}
			graphics.outline(widget.getX() - 1, widget.getY() - 1, widget.getWidth() + 2, widget.getHeight() + 2, 0x99FFFFFF);
			if (widget.isHovered()) {
				graphics.outline(widget.getX() - 2, widget.getY() - 2, widget.getWidth() + 4, widget.getHeight() + 4, 0xFFFFFFFF);
			}
		}

		for (PlacedPreviewButton placedButton : this.placedPreviewButtons) {
			AbstractWidget widget = placedButton.widget();
			if (this.intersectsPopup(widget)) {
				continue;
			}
			graphics.outline(widget.getX() - 2, widget.getY() - 2, widget.getWidth() + 4, widget.getHeight() + 4, 0x80FFFFFF);
		}
	}

	private boolean intersectsPopup(AbstractWidget widget) {
		if (this.popupPosition == null) {
			return false;
		}

		return widget.getX() < this.popupX + this.popupWidth
				&& widget.getRight() > this.popupX
				&& widget.getY() < this.popupY + this.popupHeight
				&& widget.getBottom() > this.popupY;
	}

	private boolean popupConsumesHover(int mouseX, int mouseY) {
		return this.popupPosition != null
				&& mouseX >= this.popupX
				&& mouseX <= this.popupX + this.popupWidth
				&& mouseY >= this.popupY
				&& mouseY <= this.popupY + this.popupHeight;
	}

	private void extractPopupTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		for (PlacedPreviewButton popupButton : this.popupWidgets.reversed()) {
			if (popupButton.widget().isMouseOver(mouseX, mouseY)) {
				graphics.setTooltipForNextFrame(this.font, buttonLabel(popupButton.button()), mouseX, mouseY);
				return;
			}
		}
	}

	private int rawPreviewRowY(PreviewLayout layout, int menuTop, int row) {
		int[] rows = switch (this.selectedArea) {
			case PAUSE_MENU -> PAUSE_MENU_ROWS;
			case OPTIONS_MENU -> OPTIONS_MENU_ROWS;
			case MAIN_MENU -> MAIN_MENU_ROWS;
		};
		return menuTop + layout.scaled(rows[Math.min(row, rows.length - 1)]);
	}

	private int belowMenuY(PreviewLayout layout, SocialButtonLayout.MenuGeometry geometry) {
		return geometry.menuBottom() + layout.scaled(GAP);
	}

	private int[] coordinatesForPosition(PreviewLayout layout, ButtonPosition position, int index, int count) {
		SocialButtonLayout.MenuGeometry geometry = this.previewGeometry(layout);
		int[] coordinates;
		if (BetterMenuButtonsConfig.isVanillaIconSlot(this.selectedArea, position)) {
			coordinates = new int[]{this.dynamicIconRowX(layout, count, index), this.iconSlotY(layout, geometry, index)};
		} else if (position == ButtonPosition.BELOW_MENU) {
			coordinates = new int[]{this.dynamicIconRowX(layout, count, index), this.rowWrappedY(layout, this.belowMenuY(layout, geometry), index)};
		} else if (position.isLeftSide()) {
			coordinates = this.baseCoordinatesForPosition(layout, position);
			coordinates[0] -= (count - index - 1) * (ICON_SIZE + GAP);
		} else if (position.isRightSide()) {
			coordinates = this.baseCoordinatesForPosition(layout, position);
			coordinates[0] += index * (ICON_SIZE + GAP);
		} else if (position.isLeftCorner()) {
			coordinates = this.baseCoordinatesForPosition(layout, position);
			coordinates[0] += index * (ICON_SIZE + GAP);
		} else if (position.isRightCorner()) {
			coordinates = this.baseCoordinatesForPosition(layout, position);
			coordinates[0] -= (count - index - 1) * (ICON_SIZE + GAP);
		} else {
			coordinates = this.baseCoordinatesForPosition(layout, position);
		}
		coordinates[0] = layout.clampX(coordinates[0]);
		coordinates[1] = layout.clampY(coordinates[1]);
		return coordinates;
	}

	private int[] baseCoordinatesForPosition(PreviewLayout layout, ButtonPosition position) {
		SocialButtonLayout.MenuGeometry geometry = this.previewGeometry(layout);
		int menuLeft = this.selectedArea == MenuArea.OPTIONS_MENU ? layout.optionsMenuLeft() : layout.menuLeft();
		int menuRight = this.selectedArea == MenuArea.OPTIONS_MENU ? layout.optionsMenuRight() : layout.menuRight();
		return switch (position) {
			case LEFT_OF_MENU, LEFT_ROW_4 -> new int[]{menuLeft - ICON_SIZE - layout.scaled(GAP), geometry.rowY(3)};
			case RIGHT_OF_MENU, RIGHT_ROW_4 -> new int[]{menuRight + layout.scaled(GAP), geometry.rowY(3)};
			case LEFT_ROW_1 -> new int[]{menuLeft - ICON_SIZE - layout.scaled(GAP), geometry.rowY(0)};
			case RIGHT_ROW_1 -> new int[]{menuRight + layout.scaled(GAP), geometry.rowY(0)};
			case LEFT_ROW_2 -> new int[]{menuLeft - ICON_SIZE - layout.scaled(GAP), geometry.rowY(1)};
			case RIGHT_ROW_2 -> new int[]{menuRight + layout.scaled(GAP), geometry.rowY(1)};
			case LEFT_ROW_3 -> new int[]{menuLeft - ICON_SIZE - layout.scaled(GAP), geometry.rowY(2)};
			case RIGHT_ROW_3 -> new int[]{menuRight + layout.scaled(GAP), geometry.rowY(2)};
			case LEFT_ROW_5 -> new int[]{menuLeft - ICON_SIZE - layout.scaled(GAP), geometry.rowY(4)};
			case RIGHT_ROW_5 -> new int[]{menuRight + layout.scaled(GAP), geometry.rowY(4)};
			case ICON_SLOT_1 -> new int[]{this.iconSlotX(layout, 0), this.iconSlotY(layout, geometry)};
			case ICON_SLOT_2 -> new int[]{this.iconSlotX(layout, 1), this.iconSlotY(layout, geometry)};
			case ICON_SLOT_3 -> new int[]{this.iconSlotX(layout, 2), this.iconSlotY(layout, geometry)};
			case ICON_SLOT_4 -> new int[]{this.iconSlotX(layout, 3), this.iconSlotY(layout, geometry)};
			case ICON_SLOT_5 -> new int[]{this.iconSlotX(layout, 4), this.iconSlotY(layout, geometry)};
			case BELOW_MENU -> new int[]{layout.centerX() - ICON_SIZE / 2, this.belowMenuY(layout, geometry)};
			case TOP_LEFT -> {
				int x = layout.safeLeft() + layout.cornerInset();
				yield new int[]{x, this.topCornerY(layout, x)};
			}
			case TOP_RIGHT -> {
				int x = layout.safeRight() - ICON_SIZE - layout.cornerInset();
				yield new int[]{x, this.topCornerY(layout, x)};
			}
			case BOTTOM_LEFT -> new int[]{layout.safeLeft() + layout.cornerInset(), layout.previewBottom() - ICON_SIZE - layout.cornerInset()};
			case BOTTOM_RIGHT -> new int[]{layout.safeRight() - ICON_SIZE - layout.cornerInset(), layout.previewBottom() - ICON_SIZE - layout.cornerInset()};
			default -> new int[]{layout.centerX() - ICON_SIZE / 2, this.iconSlotY(layout, geometry)};
		};
	}

	private int countButtonsAt(ButtonPosition position) {
		int count = 0;
		for (String button : BetterMenuButtonsConfig.availableButtonIds(this.selectedArea)) {
			if (BetterMenuButtonsConfig.isAvailableIn(this.selectedArea, button) && BetterMenuButtonsConfig.effectivePosition(this.selectedArea, button) == position) {
				count++;
			}
		}
		return count;
	}

	private Map<String, Integer> vanillaIconIndexes() {
		Map<String, Integer> indexes = new java.util.LinkedHashMap<>();
		int index = 0;
		for (String button : BetterMenuButtonsConfig.orderedButtonIds(this.selectedArea)) {
			if (BetterMenuButtonsConfig.isAvailableIn(this.selectedArea, button) && BetterMenuButtonsConfig.isVanillaIconSlot(this.selectedArea, BetterMenuButtonsConfig.effectivePosition(this.selectedArea, button))) {
				indexes.put(button, index++);
			}
		}
		return indexes;
	}

	private Map<String, Integer> rowIndexes(ButtonPosition position) {
		Map<String, Integer> indexes = new java.util.LinkedHashMap<>();
		int index = 0;
		for (String button : BetterMenuButtonsConfig.orderedButtonIds(this.selectedArea)) {
			if (BetterMenuButtonsConfig.isAvailableIn(this.selectedArea, button) && BetterMenuButtonsConfig.effectivePosition(this.selectedArea, button) == position) {
				indexes.put(button, index++);
			}
		}
		return indexes;
	}

	private int iconSlotX(PreviewLayout layout, int slot) {
		int count = Math.max(1, BetterMenuButtonsConfig.vanillaIconSlotCount(this.selectedArea));
		int iconLeft = layout.centerX() - (ICON_SIZE * count + GAP * (count - 1)) / 2;
		return iconLeft + (ICON_SIZE + GAP) * Math.min(slot, count - 1);
	}

	private int dynamicIconRowX(PreviewLayout layout, int count, int index) {
		int clampedCount = Math.max(1, count);
		int maxButtonsPerRow = this.maxButtonsPerRow(layout.safeWidth() - SCREEN_MARGIN * 2);
		int rowStart = Math.max(0, index / maxButtonsPerRow * maxButtonsPerRow);
		int rowCount = Math.min(maxButtonsPerRow, clampedCount - rowStart);
		int totalWidth = ICON_SIZE * rowCount + GAP * (rowCount - 1);
		return layout.centerX() - totalWidth / 2 + (ICON_SIZE + GAP) * (index - rowStart);
	}

	private int iconSlotY(PreviewLayout layout, SocialButtonLayout.MenuGeometry geometry) {
		return this.iconSlotY(layout, geometry, 0);
	}

	private int iconSlotY(PreviewLayout layout, SocialButtonLayout.MenuGeometry geometry, int index) {
		if (ModMenuIntegration.hasInsertedRow(this.selectedArea)) {
			return this.rowWrappedY(layout, geometry.iconRowY(), index);
		}
		return this.rowWrappedY(layout, geometry.iconRowY(), index);
	}

	private int rowWrappedY(PreviewLayout layout, int y, int index) {
		int maxButtonsPerRow = this.maxButtonsPerRow(layout.safeWidth() - SCREEN_MARGIN * 2);
		int row = Math.max(0, index / maxButtonsPerRow);
		return y + row * (ICON_SIZE + layout.scaled(GAP));
	}

	private int topCornerY(PreviewLayout layout, int x) {
		int y = layout.previewTop() + layout.cornerInset();
		if (this.intersectsAreaSelector(layout, x, y)) {
			return SCREEN_MARGIN + BUTTON_HEIGHT + GAP;
		}
		return y;
	}

	private boolean intersectsAreaSelector(PreviewLayout layout, int x, int y) {
		int selectorLeft = layout.centerX() - (AREA_BUTTON_WIDTH * 3 + GAP * 2) / 2;
		int selectorRight = selectorLeft + AREA_BUTTON_WIDTH * 3 + GAP * 2;
		int selectorTop = SCREEN_MARGIN;
		int selectorBottom = selectorTop + BUTTON_HEIGHT;
		return x < selectorRight
				&& x + ICON_SIZE > selectorLeft
				&& y < selectorBottom
				&& y + ICON_SIZE > selectorTop;
	}

	private int maxButtonsPerRow(int availableWidth) {
		return Math.max(1, (availableWidth + GAP) / (ICON_SIZE + GAP));
	}

	private int rowsNeeded(PreviewLayout layout, int buttonCount) {
		int maxButtonsPerRow = this.maxButtonsPerRow(layout.safeWidth() - SCREEN_MARGIN * 2);
		return Math.max(1, (buttonCount + maxButtonsPerRow - 1) / maxButtonsPerRow);
	}

	private int menuTop(PreviewLayout layout) {
		return switch (this.selectedArea) {
			case MAIN_MENU -> layout.mainMenuTop();
			case PAUSE_MENU -> layout.pauseMenuTop();
			case OPTIONS_MENU -> layout.optionsMenuTop();
		};
	}

	private SocialButtonLayout.MenuGeometry previewGeometry(PreviewLayout layout) {
		int menuTop = this.menuTop(layout);
		int[] offsets = switch (this.selectedArea) {
			case MAIN_MENU -> MAIN_MENU_ROWS;
			case PAUSE_MENU -> PAUSE_MENU_ROWS;
			case OPTIONS_MENU -> OPTIONS_MENU_ROWS;
		};
		int[] rows = new int[offsets.length];
		for (int index = 0; index < offsets.length; index++) {
			rows[index] = menuTop + Math.round(offsets[index] * layout.scale());
		}

		if (ModMenuIntegration.hasInsertedRow(MenuArea.MAIN_MENU) && this.selectedArea == MenuArea.MAIN_MENU) {
			rows[0] = menuTop + Math.round(-12 * layout.scale());
			rows[1] = menuTop + Math.round(12 * layout.scale());
			rows[2] = menuTop + Math.round(36 * layout.scale());
			rows[3] = menuTop + Math.round(60 * layout.scale());
			rows[4] = menuTop + Math.round(108 * layout.scale());
		} else if (ModMenuIntegration.hasInsertedRow(MenuArea.PAUSE_MENU) && this.selectedArea == MenuArea.PAUSE_MENU) {
			rows[2] -= Math.round(4 * layout.scale());
			rows[3] += Math.round(16 * layout.scale());
			rows[4] += Math.round(16 * layout.scale());
		}

		int menuBottom = rows[rows.length - 1] + layout.buttonHeight();
		int iconRowY = ModMenuIntegration.hasInsertedRow(this.selectedArea)
				? rows[this.selectedArea == MenuArea.PAUSE_MENU ? 2 : 3] + layout.buttonHeight() + layout.scaled(GAP)
				: rows[this.selectedArea == MenuArea.PAUSE_MENU ? 2 : 3] + VANILLA_ICON_ROW_Y_OFFSET;
		return new SocialButtonLayout.MenuGeometry(rows[0], menuBottom, rows, iconRowY);
	}

	private static class SolidPanelWidget extends AbstractWidget {
		SolidPanelWidget(int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty());
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
			graphics.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), 0xFF202020);
			graphics.outline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), 0xFFFFFFFF);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
		}
	}

	private record PlacedPreviewButton(AbstractWidget widget, String button) {
	}

	private record PreviewLayout(int safeLeft, int safeRight, int previewTop, int previewBottom, int centerX, float scale) {
		private static PreviewLayout create(int screenWidth, int screenHeight, int selectorButtonCount) {
			float widthScale = screenWidth / (float) DESIGN_PREVIEW_WIDTH;
			float heightScale = screenHeight / (float) DESIGN_PREVIEW_HEIGHT;
			float scale = Math.max(MIN_PREVIEW_SCALE, Math.min(1.0F, Math.min(widthScale, heightScale)));
			int safeLeft = 0;
			int safeRight = screenWidth;
			int previewTop = 0;
			int previewBottom = screenHeight;
			int centerX = safeLeft + (safeRight - safeLeft) / 2;
			return new PreviewLayout(safeLeft, safeRight, previewTop, previewBottom, centerX, scale);
		}

		private int safeWidth() {
			return this.safeRight - this.safeLeft;
		}

		private int previewHeight() {
			return this.previewBottom - this.previewTop;
		}

		private int menuWidth() {
			return Math.max(ICON_SIZE, Math.min(this.safeWidth() - SCREEN_MARGIN * 2, this.scaled(VANILLA_MENU_WIDTH)));
		}

		private int halfButtonWidth() {
			return Math.max(ICON_SIZE, (this.menuWidth() - this.scaled(GAP)) / 2);
		}

		private int buttonHeight() {
			return this.scaled(BUTTON_HEIGHT);
		}

		private int menuLeft() {
			return this.centerX - this.menuWidth() / 2;
		}

		private int menuRight() {
			return this.menuLeft() + this.menuWidth();
		}

		private int optionsMenuWidth() {
			return Math.max(ICON_SIZE, Math.min(this.safeWidth() - SCREEN_MARGIN * 2, this.scaled(OPTIONS_MENU_WIDTH)));
		}

		private int optionsColumnWidth() {
			return Math.max(ICON_SIZE, (this.optionsMenuWidth() - this.scaled(OPTIONS_COLUMN_GAP)) / 2);
		}

		private int optionsMenuLeft() {
			return this.centerX - this.optionsMenuWidth() / 2;
		}

		private int optionsMenuRight() {
			return this.optionsMenuLeft() + this.optionsMenuWidth();
		}

		private int mainMenuTop() {
			return this.previewTop + Math.max(42, (this.previewHeight() - this.scaled(132)) / 2);
		}

		private int pauseMenuTop() {
			return this.previewTop + Math.max(42, (this.previewHeight() - this.scaled(132)) / 2);
		}

		private int optionsMenuTop() {
			return this.previewTop + Math.max(32, (this.previewHeight() - this.scaled(244)) / 2);
		}

		private int cornerInset() {
			return this.scaled(10);
		}

		private int scaled(int value) {
			return Math.max(1, Math.round(value * this.scale));
		}

		private int clampX(int x) {
			return this.clampX(x, ICON_SIZE);
		}

		private int clampX(int x, int width) {
			int left = this.safeLeft + SCREEN_MARGIN;
			int right = Math.max(left, this.safeRight - width - SCREEN_MARGIN);
			return Math.max(left, Math.min(right, x));
		}

		private int clampY(int y) {
			return Math.max(this.previewTop + SCREEN_MARGIN, Math.min(this.previewBottom - ICON_SIZE - SCREEN_MARGIN, y));
		}

		private int hideButtonY() {
			return this.previewTop + Math.max(34, this.scaled(46));
		}
	}
}