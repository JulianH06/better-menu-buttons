package com.julianh06.bettermenubuttons.config;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CustomJoinRenameScreen extends Screen {
	private static final int BUTTON_HEIGHT = 20;
	private static final int MARGIN = 12;
	private static final int GAP = 4;

	private final Screen parent;
	private final String buttonId;
	private final String initialLabel;
	private EditBox labelBox;

	public CustomJoinRenameScreen(Screen parent, String buttonId, String initialLabel) {
		super(Component.literal("Rename custom button"));
		this.parent = parent;
		this.buttonId = buttonId;
		this.initialLabel = initialLabel;
	}

	@Override
	protected void init() {
		int width = Math.min(300, this.width - MARGIN * 2);
		int left = (this.width - width) / 2;
		int y = this.height / 2 - 24;
		this.labelBox = new EditBox(this.font, left, y, width, BUTTON_HEIGHT, Component.literal("Name"));
		this.labelBox.setMaxLength(64);
		this.labelBox.setValue(this.initialLabel);
		this.addRenderableWidget(this.labelBox);
		this.setInitialFocus(this.labelBox);

		int buttonWidth = (width - GAP) / 2;
		this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> this.saveAndClose())
				.bounds(left, y + BUTTON_HEIGHT + GAP, buttonWidth, BUTTON_HEIGHT)
				.build());
		this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.onClose())
				.bounds(left + buttonWidth + GAP, y + BUTTON_HEIGHT + GAP, buttonWidth, BUTTON_HEIGHT)
				.build());
	}

	private void saveAndClose() {
		BetterMenuButtonsConfig.renameCustomButton(this.buttonId, this.labelBox.getValue());
		BetterMenuButtonsConfig.save();
		this.onClose();
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			Screen screen = this.parent;
			this.minecraft.setScreenAndShow(screen);
		}
	}
}