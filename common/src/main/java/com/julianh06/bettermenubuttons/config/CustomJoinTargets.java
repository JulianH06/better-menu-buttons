package com.julianh06.bettermenubuttons.config;

import com.julianh06.bettermenubuttons.BetterMenuButtons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;

import java.util.List;
import java.util.concurrent.CompletionException;

public final class CustomJoinTargets {
	private CustomJoinTargets() {
	}

	public static List<ServerData> servers(Minecraft minecraft) {
		ServerList serverList = new ServerList(minecraft);
		serverList.load();
		return java.util.stream.IntStream.range(0, serverList.size())
				.mapToObj(serverList::get)
				.toList();
	}

	public static ServerData server(Minecraft minecraft, String address) {
		for (ServerData server : servers(minecraft)) {
			if (server.ip.equals(address)) {
				return server;
			}
		}
		return null;
	}

	public static List<LevelSummary> worlds(Minecraft minecraft) {
		LevelStorageSource levelSource = minecraft.getLevelSource();
		try {
			return levelSource.loadLevelSummaries(levelSource.findLevelCandidates()).join();
		} catch (CompletionException | LevelStorageException exception) {
			BetterMenuButtons.LOGGER.warn("Could not load singleplayer worlds for custom join buttons", exception);
			return List.of();
		}
	}

	public static LevelSummary world(Minecraft minecraft, String levelId) {
		for (LevelSummary summary : worlds(minecraft)) {
			if (summary.getLevelId().equals(levelId)) {
				return summary;
			}
		}
		return null;
	}
}