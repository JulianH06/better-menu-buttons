package com.julianh06.bettermenubuttons;

import com.julianh06.bettermenubuttons.config.BetterMenuButtonsConfigScreen;
import com.julianh06.bettermenubuttons.platform.Platform;
import com.julianh06.bettermenubuttons.platform.PlatformHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

@Mod(BetterMenuButtons.FORGE_MOD_ID)
public class BetterMenuButtonsForge {
	public BetterMenuButtonsForge() {
		BetterMenuButtons.LOGGER.info("Initializing Better Menu Buttons for Forge");
		PlatformHooks.init(new ForgePlatform());
		BetterMenuButtons.initClient();
		RegisterClientCommandsEvent.BUS.addListener(this::registerClientCommands);
	}

	private void registerClientCommands(RegisterClientCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("bettermenubuttons")
				.executes(context -> openConfigScreen(Minecraft.getInstance())));
		event.getDispatcher().register(Commands.literal("bmb")
				.executes(context -> openConfigScreen(Minecraft.getInstance())));
	}

	private static int openConfigScreen(Minecraft client) {
		client.schedule(() -> {
			Screen screen = new BetterMenuButtonsConfigScreen(null);
			client.setScreenAndShow(screen);
		});
		return 1;
	}

	private record ForgePlatform() implements Platform {
		@Override
		public Path configDir() {
			return FMLPaths.CONFIGDIR.get();
		}

		@Override
		public boolean isModLoaded(String modId) {
			return ModList.isLoaded(modId);
		}
	}
}