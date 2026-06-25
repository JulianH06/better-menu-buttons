package com.julianh06.bettermenubuttons;

import com.julianh06.bettermenubuttons.config.BetterMenuButtonsConfigScreen;
import com.julianh06.bettermenubuttons.platform.Platform;
import com.julianh06.bettermenubuttons.platform.PlatformHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.nio.file.Path;

@Mod(BetterMenuButtons.FORGE_MOD_ID)
public class BetterMenuButtonsNeoForge {
	public BetterMenuButtonsNeoForge() {
		BetterMenuButtons.LOGGER.info("Initializing Better Menu Buttons for NeoForge");
		PlatformHooks.init(new NeoForgePlatform());
		BetterMenuButtons.initClient();
		NeoForge.EVENT_BUS.addListener(this::registerClientCommands);
	}

	private void registerClientCommands(RegisterClientCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("bettermenubuttons")
				.executes(context -> openConfigScreen(Minecraft.getInstance())));
		event.getDispatcher().register(Commands.literal("bmb")
				.executes(context -> openConfigScreen(Minecraft.getInstance())));
	}

	private static int openConfigScreen(Minecraft client) {
		client.schedule(() -> client.setScreenAndShow(new BetterMenuButtonsConfigScreen(null)));
		return 1;
	}

	private record NeoForgePlatform() implements Platform {
		@Override
		public Path configDir() {
			return FMLPaths.CONFIGDIR.get();
		}

		@Override
		public boolean isModLoaded(String modId) {
			return ModList.get().isLoaded(modId);
		}
	}
}