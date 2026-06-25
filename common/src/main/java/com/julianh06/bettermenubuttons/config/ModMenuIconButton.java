package com.julianh06.bettermenubuttons.config;

import com.julianh06.bettermenubuttons.BetterMenuButtons;
import com.julianh06.bettermenubuttons.compat.ModMenuIntegration;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class ModMenuIconButton extends Button {
	private static final int ICON_SIZE = 20;
	private static final int NEOFORGE_ICON_SIZE = 15;
	private static final Identifier MODS_BUTTON_TEXTURE = Identifier.fromNamespaceAndPath("modmenu", "textures/gui/mods_button.png");
	private static final Identifier FALLBACK_MODS_BUTTON_TEXTURE = Identifier.fromNamespaceAndPath(BetterMenuButtons.MOD_ID, "textures/gui/modmenu_mods_button.png");
	private static final String MODS_BUTTON_TEXTURE_RESOURCE = "/assets/modmenu/textures/gui/mods_button.png";
	private static boolean attemptedTextureRegistration;
	private static boolean modMenuTextureAvailable;
	private static boolean fallbackTextureAvailable;

	public ModMenuIconButton(int x, int y, Button.OnPress onPress) {
		super(x, y, ICON_SIZE, ICON_SIZE, Component.literal("Mods"), onPress, DEFAULT_NARRATION);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
		this.extractDefaultSprite(graphics);
		if (ModMenuIntegration.isNeoForgeLoaded()) {
			int inset = (ICON_SIZE - NEOFORGE_ICON_SIZE) / 2;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ModMenuIntegration.NEOFORGE_MODS_SPRITE, this.getX() + inset + 1, this.getY() + inset, NEOFORGE_ICON_SIZE, NEOFORGE_ICON_SIZE);
			return;
		}

		int v = 0;
		if (!this.active) {
			v = ICON_SIZE * 2;
		} else if (this.isHoveredOrFocused()) {
			v = ICON_SIZE;
		}
		ensureModMenuTextureRegistered();
		if (modMenuTextureAvailable) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, MODS_BUTTON_TEXTURE, this.getX(), this.getY(), 0.0F, v, ICON_SIZE, ICON_SIZE, 32, 64, ARGB.color(this.getAlpha(), 0xffffff));
		} else if (fallbackTextureAvailable) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, FALLBACK_MODS_BUTTON_TEXTURE, this.getX(), this.getY(), 0.0F, v, ICON_SIZE, ICON_SIZE, 32, 64, ARGB.color(this.getAlpha(), 0xffffff));
		} else {
			extractFallbackIcon(graphics);
		}
	}

	private static void ensureModMenuTextureRegistered() {
		if (attemptedTextureRegistration) {
			return;
		}
		attemptedTextureRegistration = true;

		Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(MODS_BUTTON_TEXTURE);
		if (resource.isPresent()) {
			modMenuTextureAvailable = true;
			return;
		}

		Optional<Resource> fallbackResource = Minecraft.getInstance().getResourceManager().getResource(FALLBACK_MODS_BUTTON_TEXTURE);
		if (fallbackResource.isPresent()) {
			fallbackTextureAvailable = true;
			return;
		}

		try (InputStream stream = ModMenuIconButton.class.getResourceAsStream(MODS_BUTTON_TEXTURE_RESOURCE)) {
			if (stream == null) {
				return;
			}

			NativeImage image = NativeImage.read(stream);
			Minecraft.getInstance().getTextureManager().register(MODS_BUTTON_TEXTURE, new DynamicTexture(MODS_BUTTON_TEXTURE::toString, image));
			modMenuTextureAvailable = true;
		} catch (IOException ignored) {
		}
	}

	private void extractFallbackIcon(GuiGraphicsExtractor graphics) {
		int x = this.getX();
		int y = this.getY();
		int alpha = Math.round(this.getAlpha() * 255.0F);
		int dark = ARGB.color(alpha, 0x2f3136);
		int light = ARGB.color(alpha, 0x8bd3ff);
		int accent = ARGB.color(alpha, 0xf4f4f4);

		graphics.fill(x + 5, y + 5, x + 15, y + 15, dark);
		graphics.fill(x + 7, y + 3, x + 9, y + 7, light);
		graphics.fill(x + 11, y + 3, x + 13, y + 7, light);
		graphics.fill(x + 7, y + 13, x + 9, y + 17, light);
		graphics.fill(x + 11, y + 13, x + 13, y + 17, light);
		graphics.fill(x + 3, y + 7, x + 7, y + 9, light);
		graphics.fill(x + 13, y + 7, x + 17, y + 9, light);
		graphics.fill(x + 3, y + 11, x + 7, y + 13, light);
		graphics.fill(x + 13, y + 11, x + 17, y + 13, light);
		graphics.fill(x + 8, y + 8, x + 10, y + 10, accent);
		graphics.fill(x + 11, y + 10, x + 13, y + 12, accent);
	}
}