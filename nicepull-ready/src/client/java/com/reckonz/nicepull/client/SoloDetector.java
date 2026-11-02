package com.reckonz.nicepull.client;

import java.util.Locale;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class SoloDetector {
	// How many "was final killed" messages before the SOLO alert fires.
	private static final int FINAL_KILLS_FOR_SOLO = 3;
	// Show a red "SOLO!" alert on screen.
	private static final boolean SHOW_ALERT = true;
	// How long the alert stays on screen, in ticks (60 = 3 seconds).
	private static final int ALERT_TICKS = 60;
	// Send "SOLO!" to party chat. Set to false to turn it off.
	private static final boolean PARTY_MESSAGE = true;

	private static final String START = "[NPC] Elle: Talk with me to begin!";

	private static boolean active = false;
	private static int finalKills = 0;
	private static boolean announced = false;
	private static int alertTicks = 0;

	private SoloDetector() {}

	public static void init() {
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (overlay) return;
			String text = message.getString().strip();

			// A new run starts: reset the count.
			if (text.equals(START)) {
				active = true;
				finalKills = 0;
				announced = false;
				return;
			}

			if (!active || announced) return;
			if (text.toLowerCase(Locale.ROOT).contains("was final killed")) {
				finalKills++;
				if (finalKills >= FINAL_KILLS_FOR_SOLO) {
					announced = true;
					trigger();
				}
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (alertTicks > 0) alertTicks--;
		});

		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> {
			active = false;
			finalKills = 0;
			announced = false;
			alertTicks = 0;
		});

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("nicepull", "solo"), (graphics, delta) -> {
			if (alertTicks <= 0) return;
			Minecraft mc = Minecraft.getInstance();
			String text = "SOLO!";
			int x = (mc.getWindow().getGuiScaledWidth() - mc.font.width(text)) / 2;
			int y = mc.getWindow().getGuiScaledHeight() / 2 - 80;
			graphics.text(mc.font, text, x, y, 0xFFFF5555);
		});
	}

	private static void trigger() {
		if (SHOW_ALERT) alertTicks = ALERT_TICKS;

		if (PARTY_MESSAGE) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null && mc.getConnection() != null) {
				mc.getConnection().sendCommand("pc Your Solo!");
			}
		}
	}
}
