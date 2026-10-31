package com.reckonz.nicepull.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

public class NicePullClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Auto close the shop after buying Human Cannonball.
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay && message.getString().equals("You purchased Human Cannonball!")) {
				var player = Minecraft.getInstance().player;
				if (player != null) {
					player.closeContainer();
				}
			}
		});
	}
}
