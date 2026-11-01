package com.julianh06.bettermenubuttons;

import com.julianh06.bettermenubuttons.config.BetterMenuButtonsConfigScreen;
import com.julianh06.bettermenubuttons.platform.PlatformHooks;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class BetterMenuButtonsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		PlatformHooks.init(new FabricPlatform());
		BetterMenuButtons.initClient();
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) -> {
			dispatcher.register(ClientCommands.literal("bettermenubuttons")
					.executes(context -> openConfigScreen(context.getSource().getClient())));
			dispatcher.register(ClientCommands.literal("bmb")
					.executes(context -> openConfigScreen(context.getSource().getClient())));
		});
	}

	private static int openConfigScreen(Minecraft client) {
		client.schedule(() -> {
			Screen screen = new BetterMenuButtonsConfigScreen(null);
			client.gui.setScreen(screen);
		});
		return 1;
	}

	private record FabricPlatform() implements com.julianh06.bettermenubuttons.platform.Platform {
		@Override
		public java.nio.file.Path configDir() {
			return FabricLoader.getInstance().getConfigDir();
		}

		@Override
		public boolean isModLoaded(String modId) {
			return FabricLoader.getInstance().isModLoaded(modId);
		}
	}
}