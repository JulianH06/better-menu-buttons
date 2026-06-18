package com.julianh06.bettermenubuttons.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ModMenuIconButton extends Button {
	private static final int ICON_SIZE = 20;
	private static final Identifier MODS_BUTTON_TEXTURE = Identifier.fromNamespaceAndPath("modmenu", "textures/gui/mods_button.png");

	public ModMenuIconButton(int x, int y, Button.OnPress onPress) {
		super(x, y, ICON_SIZE, ICON_SIZE, Component.literal("Mods"), onPress, DEFAULT_NARRATION);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
		this.extractDefaultSprite(graphics);
		int v = 0;
		if (!this.active) {
			v = ICON_SIZE * 2;
		} else if (this.isHoveredOrFocused()) {
			v = ICON_SIZE;
		}
		graphics.blit(RenderPipelines.GUI_TEXTURED, MODS_BUTTON_TEXTURE, this.getX(), this.getY(), 0.0F, v, ICON_SIZE, ICON_SIZE, 32, 64);
	}
}
