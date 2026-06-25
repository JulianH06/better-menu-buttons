package com.julianh06.bettermenubuttons.config;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;

public class BetterMenuButtonsIconButton extends Button {
	private static final int ICON_SIZE = 20;
	private static final int DRAWN_ICON_SIZE = 14;
	private static final Component LABEL = Component.literal("Move Buttons");
	private static final Identifier BUTTON_TEXTURE = Identifier.fromNamespaceAndPath("better_menu_buttons", "textures/gui/better_menu_buttons.png");
	private static final String BUTTON_TEXTURE_RESOURCE = "/assets/better-menu-buttons/textures/gui/better_menu_buttons.png";
	private static boolean attemptedTextureRegistration;

	public BetterMenuButtonsIconButton(int x, int y, Button.OnPress onPress) {
		super(x, y, ICON_SIZE, ICON_SIZE, LABEL, onPress, DEFAULT_NARRATION);
	}

	public static Component label() {
		return LABEL;
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
		this.extractDefaultSprite(graphics);
		ensureTextureRegistered();
		int inset = (ICON_SIZE - DRAWN_ICON_SIZE) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, BUTTON_TEXTURE, this.getX() + inset, this.getY() + inset, 0.0F, 0.0F, DRAWN_ICON_SIZE, DRAWN_ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
	}

	private static void ensureTextureRegistered() {
		if (attemptedTextureRegistration) {
			return;
		}
		attemptedTextureRegistration = true;

		try (InputStream stream = BetterMenuButtonsIconButton.class.getResourceAsStream(BUTTON_TEXTURE_RESOURCE)) {
			if (stream == null) {
				return;
			}

			NativeImage image = NativeImage.read(stream);
			Minecraft.getInstance().getTextureManager().register(BUTTON_TEXTURE, new DynamicTexture(BUTTON_TEXTURE::toString, image));
		} catch (IOException ignored) {
		}
	}
}