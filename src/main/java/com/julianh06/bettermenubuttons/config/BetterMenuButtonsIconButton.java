package com.julianh06.bettermenubuttons.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class BetterMenuButtonsIconButton extends Button {
	private static final int ICON_SIZE = 20;
	private static final int TEXTURE_SIZE = 20;
	private static final int DRAW_SIZE = 14;
	private static final Identifier BUTTON_TEXTURE = Identifier.fromNamespaceAndPath("better-menu-buttons", "textures/gui/better_menu_buttons.png");

	public BetterMenuButtonsIconButton(int x, int y, Button.OnPress onPress) {
		super(x, y, ICON_SIZE, ICON_SIZE, Component.translatable("better-menu-buttons.options_button"), onPress, DEFAULT_NARRATION);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
		this.extractDefaultSprite(graphics);
		int inset = (ICON_SIZE - DRAW_SIZE) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, BUTTON_TEXTURE, this.getX() + inset, this.getY() + inset, 0.0F, 0.0F, DRAW_SIZE, DRAW_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
	}
}