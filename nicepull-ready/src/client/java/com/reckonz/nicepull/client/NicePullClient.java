package com.reckonz.nicepull.client;

import java.util.Locale;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public class NicePullClient implements ClientModInitializer {
	/** True while the tab list says you are in Kuudra. */
	public static volatile boolean inKuudra = false;

	private static int tickCounter = 0;

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

		// Work out whether we are in Kuudra by reading the "Area: ..." line of the tab list.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level == null || client.getConnection() == null) {
				inKuudra = false;
				return;
			}
			if (++tickCounter % 20 != 0) return;
			for (PlayerInfo info : client.getConnection().getOnlinePlayers()) {
				Component name = info.getTabListDisplayName();
				if (name == null) continue;
				String text = name.getString().strip();
				if (text.startsWith("Area:")) {
					inKuudra = text.toLowerCase(Locale.ROOT).contains("kuudra");
					break;
				}
			}
		});
	}
}
