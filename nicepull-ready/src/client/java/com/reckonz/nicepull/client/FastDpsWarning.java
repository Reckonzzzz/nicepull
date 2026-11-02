package com.reckonz.nicepull.client;

import java.util.Locale;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class FastDpsWarning {
	// A DPS phase shorter than this many ticks counts as fast (66 ticks = 3.3 seconds).
	private static final int FAST_TICKS = 66;
	// Show a red "FAST DPS!" alert on screen.
	private static final boolean SHOW_ALERT = true;
	// How long the alert stays on screen, in ticks (60 = 3 seconds).
	private static final int ALERT_TICKS = 60;
	// Send "DPS FAST! Dps Took: X.XXs" to party chat. Set to false to turn it off.
	private static final boolean PARTY_MESSAGE = true;

	private static boolean active = false;
	private static int dpsTicks = 0;
	private static int alertTicks = 0;

	private FastDpsWarning() {}

	public static void init() {
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (overlay || !NicePullClient.inFight) return;
			String text = message.getString().strip();

			if (text.contains("destroyed one of Kuudra's pods!")) {
				if (!active) {
					active = true;
					dpsTicks = 0;
				}
			} else if (active && (text.contains("SURELY THAT'S IT") || text.contains("POW! SURELY"))) {
				end();
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (active) dpsTicks++;
			if (alertTicks > 0) alertTicks--;
		});

		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> {
			active = false;
			dpsTicks = 0;
			alertTicks = 0;
		});

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("nicepull", "fastdps"), (graphics, delta) -> {
			if (alertTicks <= 0) return;
			Minecraft mc = Minecraft.getInstance();
			String text = "FAST DPS!";
			int x = (mc.getWindow().getGuiScaledWidth() - mc.font.width(text)) / 2;
			int y = mc.getWindow().getGuiScaledHeight() / 2 - 60;
			graphics.text(mc.font, text, x, y, 0xFFFF5555);
		});
	}

	private static void end() {
		active = false;
		int ticks = dpsTicks;
		dpsTicks = 0;
		if (ticks == 0 || ticks >= FAST_TICKS) return;

		if (SHOW_ALERT) alertTicks = ALERT_TICKS;

		if (PARTY_MESSAGE) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null && mc.getConnection() != null) {
				mc.getConnection().sendCommand(
					String.format(Locale.ROOT, "pc DPS FAST! Dps Took: %.2fs", ticks / 20.0));
			}
		}
	}
}
