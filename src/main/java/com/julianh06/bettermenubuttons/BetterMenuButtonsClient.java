package com.julianh06.bettermenubuttons;

import com.julianh06.bettermenubuttons.config.BetterMenuButtonsConfig;
import com.julianh06.bettermenubuttons.config.BetterMenuButtonsConfigScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

public class BetterMenuButtonsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BetterMenuButtonsConfig.load();
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
			dispatcher.register(ClientCommands.literal("bettermenubuttons")
					.executes(context -> openConfigScreen(context.getSource().getClient())));
			dispatcher.register(ClientCommands.literal("bmb")
					.executes(context -> openConfigScreen(context.getSource().getClient())));
		});
	}

	private static int openConfigScreen(Minecraft client) {
		client.schedule(() -> client.setScreenAndShow(new BetterMenuButtonsConfigScreen(null)));
		return 1;
	}
}
