package com.reckonz.nicepull.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class RendPullTracker {
	// A player counts as the puller if they swung within this many milliseconds.
	private static final long WINDOW_MS = 270;
	// Prevent the same player from being announced again within this time.
	private static final long ANNOUNCE_COOLDOWN_MS = 1100;
	// Only players this close to Kuudra (in blocks) can be credited. Raise or lower to taste.
	private static final double MAX_PULL_DISTANCE = 12.0;
	// Kuudra's health arrives as 0-25000, real damage is that drop times 9600.
	private static final float HP_TO_DAMAGE = 9600f;
	// Ignore health drops smaller than this (about 16M damage).
	private static final float MIN_DROP = 1666f;
	private static final float START_HP = 24999f;

	private static final Map<String, Long> swings = new ConcurrentHashMap<>();
	private static final Map<String, Long> lastAnnounced = new ConcurrentHashMap<>();
	private static float lastHp = START_HP;
	private static long timerStart = 0;
	private static LivingEntity kuudra = null;
	private static int scanTicks = 0;

	private RendPullTracker() {}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(RendPullTracker::tick);
		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> reset());
	}

	private static void reset() {
		swings.clear();
		lastAnnounced.clear();
		lastHp = START_HP;
		timerStart = 0;
		kuudra = null;
		scanTicks = 0;
	}

	/** Kuudra is the magma cube with exactly 100,000 max health. */
	private static boolean isKuudra(Entity entity) {
		if (!(entity instanceof LivingEntity living) || !living.isAlive()) return false;
		if (!BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath().equals("magma_cube")) return false;
		return living.getAttributeBaseValue(Attributes.MAX_HEALTH) == 100000.0;
	}

	private static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) return;
		long now = System.currentTimeMillis();

		// Timer starts when you drop into the pit.
		if (NicePullClient.inFight) {
			if (Math.round(mc.player.getY()) < 10 && timerStart == 0) timerStart = now;
		} else {
			timerStart = 0;
			kuudra = null;
			return;
		}

		// Find Kuudra (re-checked twice a second until found).
		if ((kuudra == null || !kuudra.isAlive()) && ++scanTicks >= 10) {
			scanTicks = 0;
			kuudra = null;
			for (Entity e : mc.level.entitiesForRendering()) {
				if (isKuudra(e)) {
					kuudra = (LivingEntity) e;
					break;
				}
			}
		}

		// Only record swings from players standing near Kuudra.
		if (kuudra == null) return;
		for (AbstractClientPlayer p : mc.level.players()) {
			if (p.swinging && p.distanceTo(kuudra) <= MAX_PULL_DISTANCE) {
				swings.put(p.getGameProfile().name(), now);
			}
		}
	}

	/** Called from RendPullMixin whenever the server sends entity data. */
	public static void onEntityData(ClientboundSetEntityDataPacket packet) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;

		Entity entity = mc.level.getEntity(packet.id());
		if (entity == null || !isKuudra(entity)) return;
		kuudra = (LivingEntity) entity;

		List<SynchedEntityData.DataValue<?>> items = packet.packedItems();
		if (items == null) return;
		float hp = -1f;
		for (SynchedEntityData.DataValue<?> v : items) {
			if (v.value() instanceof Float f) hp = f;
		}
		if (hp == -1f) return;

		// Only count damage while you are in the fight and down in the pit.
		if (!NicePullClient.inFight || mc.player.getY() > 30.0) {
			lastHp = hp;
			return;
		}
		if (hp > 25000f) return;

		float drop = Math.max(0f, lastHp - hp);
		if (drop > MIN_DROP) {
			float damage = drop * HP_TO_DAMAGE;
			ChatFormatting color = drop <= 4166f ? ChatFormatting.RED
				: drop <= 7291f ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
			String time = timerStart > 0
				? String.format(Locale.ROOT, "%.2fs", (System.currentTimeMillis() - timerStart) / 1000f)
				: "?";

			MutableComponent msg = Component.literal(recentPullers()).withStyle(ChatFormatting.WHITE)
				.append(Component.literal(" pulled for ").withStyle(ChatFormatting.WHITE))
				.append(Component.literal(formatDamage(damage)).withStyle(color))
				.append(Component.literal(" damage in " + time + ".").withStyle(ChatFormatting.WHITE));
			mc.player.sendSystemMessage(msg);
		}
		lastHp = hp;
	}

	private static String recentPullers() {
		long now = System.currentTimeMillis();
		List<String> names = new ArrayList<>();

		var it = swings.entrySet().iterator();
		while (it.hasNext()) {
			var e = it.next();
			String name = e.getKey();

			if (now - e.getValue() <= WINDOW_MS) {
				Long last = lastAnnounced.get(name);

				if (last == null || now - last >= ANNOUNCE_COOLDOWN_MS) {
					names.add(name);
					lastAnnounced.put(name, now);
				}
			}

			it.remove();
		}

		return names.isEmpty() ? "Someone" : String.join(" and ", names);
	}

	private static String formatDamage(float d) {
		if (d >= 1_000_000f) return String.format(Locale.ROOT, "%.2fM", d / 1_000_000f);
		if (d >= 1000f) return String.format(Locale.ROOT, "%.2fK", d / 1000f);
		return String.format(Locale.ROOT, "%.0f", d);
	}
}
