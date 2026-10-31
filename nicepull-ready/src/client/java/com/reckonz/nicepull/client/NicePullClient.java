
package com.reckonz.nicepull.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class NicePullClient implements ClientModInitializer {
    private static boolean humanCannonballPurchased;
    private static int pendingTicks;
    private static boolean tinyMobsEnabled;

    private static final String TINY_MOBS_START =
            "[NPC] Elle: Talk with me to begin!";
    private static final String TINY_MOBS_STOP =
            "[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!";

    @Override
    public void onInitializeClient() {
        // Auto close the shop after buying Human Cannonball.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                String chatText = message.getString();
                if (TINY_MOBS_START.equals(chatText)) {
                    tinyMobsEnabled = true;
                } else if (TINY_MOBS_STOP.equals(chatText)) {
                    tinyMobsEnabled = false;
                }
            }

            if (!overlay && message.getString().equals("You purchased Human Cannonball!")) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    humanCannonballPurchased = true;
                    pendingTicks = 40;
                    mc.player.closeContainer();
                }
            }
        });

        // Prevent a delayed purchase flag from affecting an unrelated screen.
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
