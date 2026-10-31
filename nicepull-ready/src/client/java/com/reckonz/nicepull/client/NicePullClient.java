
package com.reckonz.nicepull.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class NicePullClient implements ClientModInitializer {

    private static boolean humanCannonballPurchased = false;
    private static int pendingTicks = 0;
    private static boolean tinyMobsEnabled = false;

    private static final String TINY_MOBS_START =
            "[NPC] Elle: Talk with me to begin!";

    private static final String TINY_MOBS_STOP =
            "[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!";

    @Override
    public void onInitializeClient() {

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;

            String chatText = message.getString();

            // TinyMobs activation and deactivation
            if (TINY_MOBS_START.equals(chatText)) {
                tinyMobsEnabled = true;
            } else if (TINY_MOBS_STOP.equals(chatText)) {
                tinyMobsEnabled = false;
            }

            // Human Cannonball purchase detection
            if (chatText.equals("You purchased Human Cannonball!")) {
                Minecraft mc = Minecraft.getInstance();

                if (mc.player != null) {
                    humanCannonballPurchased = true;
                    pendingTicks = 40;

                    // Close the current shop immediately
                    mc.player.closeContainer();
                }
            }
        });

        // Reset purchase flag after 40 ticks
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (humanCannonballPurchased && --pendingTicks <= 0) {
                humanCannonballPurchased = false;
                pendingTicks = 0;
            }
        });
    }

    public static boolean isTinyMobsEnabled() {
        return tinyMobsEnabled;
    }

    public static boolean consumeHumanCannonballPurchase() {
        if (!humanCannonballPurchased) return false;

        humanCannonballPurchased = false;
        pendingTicks = 0;

        return true;
    }
}
