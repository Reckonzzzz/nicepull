package com.reckonz.nicepull.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

public class NicePullClient implements ClientModInitializer {
	/** Tiny mobs: true between Elle's start message and the Ballista message. */
	public static volatile boolean inKuudra = false;
	/** Backbone alert: true from Elle's start message until you leave the world. */
	public static volatile boolean inFight = false;
	/** True for 2 seconds after buying Human Cannonball, so the shop can't reopen. */
	public static volatile boolean purchased = false;

	private static int purchasedTicks = 0;

	private static final String START = "[NPC] Elle: Talk with me to begin!";
	private static final String STOP =
		"[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!";

	@Override
	public void onInitializeClient() {
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (overlay) return;
			String text = message.getString().strip();

			if (text.equals(START)) {
				inKuudra = true;
				inFight = true;
			} else if (text.equals(STOP)) {
				inKuudra = false;
			}

			// Auto close the shop after buying Human Cannonball.
			if (text.equals("You purchased Human Cannonball!")) {
				purchased = true;
				purchasedTicks = 40;
				var player = Minecraft.getInstance().player;
				if (player != null) {
					player.closeContainer();
				}
			}
		});

		// The purchase flag only lasts 40 ticks (2 seconds).
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (purchased && --purchasedTicks <= 0) {
				purchased = false;
			}
		});

		// Reset when the world changes so nothing gets stuck on.
		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> {
			inKuudra = false;
			inFight = false;
			purchased = false;
		});

		Backbone.init();
		RendPullTracker.init();
		FastDpsWarning.init();
		SoloDetector.init();
	}
}
