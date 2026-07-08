package com.julianh06.bettermenubuttons.config;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class CustomJoinSelectionScreen extends Screen {
	private static final int BUTTON_HEIGHT = 24;
	private static final int ICON_SIZE = 16;
	private static final int ICON_PADDING = 4;
	private static final int GAP = 4;
	private static final int MARGIN = 12;

	private final Screen parent;
	private final List<Entry> entries;
	private final List<EntryButton> entryButtons = new ArrayList<>();
	private int page;

	public CustomJoinSelectionScreen(Screen parent, Component title, List<Entry> entries) {
		super(title);
		this.parent = parent;
		this.entries = entries;
	}

	@Override
	protected void init() {
		this.closeEntryButtons();
		int rowWidth = Math.min(360, this.width - MARGIN * 2);
		int rowLeft = (this.width - rowWidth) / 2;
		int top = 36;
		int maxRows = Math.max(1, (this.height - top - 44) / (BUTTON_HEIGHT + GAP));
		int pageCount = Math.max(1, (this.entries.size() + maxRows - 1) / maxRows);
		this.page = Math.max(0, Math.min(this.page, pageCount - 1));
		int start = this.page * maxRows;
		int end = Math.min(this.entries.size(), start + maxRows);

		if (this.entries.isEmpty()) {
			Button empty = Button.builder(Component.literal("No entries"), button -> {
			}).bounds(rowLeft, top, rowWidth, BUTTON_HEIGHT).build();
			empty.active = false;
			this.addRenderableWidget(empty);
		}

		for (int index = start; index < end; index++) {
			Entry entry = this.entries.get(index);
			int y = top + (index - start) * (BUTTON_HEIGHT + GAP);
			EntryButton button = new EntryButton(rowLeft, y, rowWidth, BUTTON_HEIGHT, entry);
			this.entryButtons.add(button);
			this.addRenderableWidget(button);
		}

		int footerY = this.height - BUTTON_HEIGHT - MARGIN;
		this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
					this.page--;
					this.rebuildWidgets();
				}).bounds(rowLeft, footerY, 36, BUTTON_HEIGHT)
				.build()).active = this.page > 0;
		this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.onClose())
				.bounds(this.width / 2 - 40, footerY, 80, BUTTON_HEIGHT)
				.build());
		this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
					this.page++;
					this.rebuildWidgets();
				}).bounds(rowLeft + rowWidth - 36, footerY, 36, BUTTON_HEIGHT)
				.build()).active = this.page < pageCount - 1;
	}

	@Override
	public void onClose() {
		this.closeEntryButtons();
		if (this.minecraft != null) {
			this.minecraft.setScreenAndShow(this.parent);
		}
	}

	@Override
	public void removed() {
		this.closeEntryButtons();
		super.removed();
	}

	private void closeEntryButtons() {
		for (EntryButton button : this.entryButtons) {
			button.close();
		}
		this.entryButtons.clear();
	}

	public record Entry(Component label, BetterMenuButtonsConfig.CustomJoinButton.TargetType targetType, String targetId, Runnable onSelect) {
	}

	private static class EntryButton extends Button implements AutoCloseable {
		private final FaviconTexture icon;

		EntryButton(int x, int y, int width, int height, Entry entry) {
			super(x, y, width, height, entry.label(), button -> entry.onSelect().run(), DEFAULT_NARRATION);
			Minecraft minecraft = Minecraft.getInstance();
			this.icon = entry.targetType() == BetterMenuButtonsConfig.CustomJoinButton.TargetType.SERVER
					? FaviconTexture.forServer(minecraft.getTextureManager(), entry.targetId())
					: FaviconTexture.forWorld(minecraft.getTextureManager(), entry.targetId());
			this.loadIcon(minecraft, entry);
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
			this.extractDefaultSprite(graphics);
			int iconX = this.getX() + ICON_PADDING;
			int iconY = this.getY() + (this.getHeight() - ICON_SIZE) / 2;
			graphics.blit(RenderPipelines.GUI_TEXTURED, this.icon.textureLocation(), iconX, iconY, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

			int textX = iconX + ICON_SIZE + ICON_PADDING;
			int textWidth = this.getRight() - textX - ICON_PADDING;
			String label = Minecraft.getInstance().font.plainSubstrByWidth(this.getMessage().getString(), textWidth);
			int textY = this.getY() + (this.getHeight() - Minecraft.getInstance().font.lineHeight) / 2;
			graphics.text(Minecraft.getInstance().font, label, textX, textY, this.active ? 0xFFFFFFFF : 0xFFA0A0A0);
		}

		private void loadIcon(Minecraft minecraft, Entry entry) {
			try {
				if (entry.targetType() == BetterMenuButtonsConfig.CustomJoinButton.TargetType.SERVER) {
					ServerData server = CustomJoinTargets.server(minecraft, entry.targetId());
					if (server != null && server.getIconBytes() != null) {
						this.icon.upload(NativeImage.read(server.getIconBytes()));
					}
				} else {
					LevelSummary world = CustomJoinTargets.world(minecraft, entry.targetId());
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
}