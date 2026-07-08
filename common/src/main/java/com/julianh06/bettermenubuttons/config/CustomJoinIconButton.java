package com.julianh06.bettermenubuttons.config;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class CustomJoinIconButton extends Button implements AutoCloseable {
	private static final int ICON_SIZE = 20;
	private static final int DRAWN_ICON_SIZE = 16;

	private final FaviconTexture icon;

	public CustomJoinIconButton(int x, int y, BetterMenuButtonsConfig.CustomJoinButton config, Button.OnPress onPress) {
		super(x, y, ICON_SIZE, ICON_SIZE, Component.literal(config.label), onPress, DEFAULT_NARRATION);
		Minecraft minecraft = Minecraft.getInstance();
		this.icon = config.targetType == BetterMenuButtonsConfig.CustomJoinButton.TargetType.SERVER
				? FaviconTexture.forServer(minecraft.getTextureManager(), config.targetId)
				: FaviconTexture.forWorld(minecraft.getTextureManager(), config.targetId);
		this.loadIcon(minecraft, config);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
		this.extractDefaultSprite(graphics);
		int inset = (ICON_SIZE - DRAWN_ICON_SIZE) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, this.icon.textureLocation(), this.getX() + inset, this.getY() + inset, 0.0F, 0.0F, DRAWN_ICON_SIZE, DRAWN_ICON_SIZE, DRAWN_ICON_SIZE, DRAWN_ICON_SIZE, DRAWN_ICON_SIZE, DRAWN_ICON_SIZE);
	}

	private void loadIcon(Minecraft minecraft, BetterMenuButtonsConfig.CustomJoinButton config) {
		try {
			if (config.targetType == BetterMenuButtonsConfig.CustomJoinButton.TargetType.SERVER) {
				ServerData server = CustomJoinTargets.server(minecraft, config.targetId);
				if (server != null && server.getIconBytes() != null) {
					this.icon.upload(NativeImage.read(server.getIconBytes()));
				}
			} else {
				LevelSummary world = CustomJoinTargets.world(minecraft, config.targetId);
				if (world != null && Files.isRegularFile(world.getIcon())) {
					try (InputStream stream = Files.newInputStream(world.getIcon())) {
						this.icon.upload(NativeImage.read(stream));
					}
				}
			}
		} catch (IOException | IllegalArgumentException ignored) {
			this.icon.clear();
		}
	}

	@Override
	public void close() {
		this.icon.close();
	}
}