package com.reckonz.nicepull.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

public final class Backbone {
	// How long the BACKBONE text stays on screen, in ticks. 0 = never show it.
	private static final int HUD_TICKS = 0;
	// Prints "Backbone hit with <item> and <helmet> at X.Xs" in chat.
	private static final boolean SHOW_GEAR_IN_CHAT = true;
	private static final String BOOMERANG_ID = "STARRED_BONE_BOOMERANG";

	private static boolean awaitBone;
	private static int awaitTicks;
	private static Vec3 throwOrigin;

	private static Display.ItemDisplay pendingEntity;
	private static int pendingTicks;

	private static Display.ItemDisplay boneStand;
	private static Vec3 prev, curr, initialDir;
	private static boolean returning;

	private static int hudTicks;
	private static int p4Ticks = -1;

	private Backbone() {}

	public static void init() {
		// Right-clicking with the Starred Bone Boomerang starts a short watch for the thrown bone.
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (level.isClientSide() && NicePullClient.inFight) {
				if (BOOMERANG_ID.equals(skyblockId(player.getItemInHand(hand)))) {
					awaitBone = true;
					awaitTicks = 12;
					throwOrigin = player.position();
				}
			}
			return InteractionResult.PASS;
		});

		// The thrown bone appears as an item display close to where you threw it.
		ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!awaitBone || throwOrigin == null) return;
			if (entity instanceof Display.ItemDisplay display) {
				Vec3 diff = throwOrigin.subtract(entity.position());
				if (Math.abs(diff.x) <= 0.3 && Math.abs(diff.z) <= 0.3) {
					pendingEntity = display;
					pendingTicks = 1;
				}
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(Backbone::tick);
		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> reset());

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("nicepull", "backbone"), (graphics, delta) -> {
			if (hudTicks <= 0) return;
			Minecraft mc = Minecraft.getInstance();
			String text = "BACKBONE";
			int x = (mc.getWindow().getGuiScaledWidth() - mc.font.width(text)) / 2;
			int y = mc.getWindow().getGuiScaledHeight() / 2 - 40;
			graphics.text(mc.font, text, x, y, 0xFFFF5555);
		});
	}

	public static void reset() {
		awaitBone = false;
		throwOrigin = null;
		pendingEntity = null;
		boneStand = null;
		prev = curr = initialDir = null;
		returning = false;
		hudTicks = 0;
		p4Ticks = -1;
	}

	private static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) return;

		// Time since you dropped into the pit (phase 4).
		if (NicePullClient.inFight) {
			if (p4Ticks < 0) {
				if (mc.player.getY() < 20.0) p4Ticks = 0;
			} else {
				p4Ticks++;
			}
		}

		if (awaitBone && --awaitTicks <= 0) awaitBone = false;

		if (pendingEntity != null && --pendingTicks <= 0) {
			addBone(pendingEntity);
			pendingEntity = null;
		}

		if (hudTicks > 0) hudTicks--;

		handleBone(mc);
	}

	private static void addBone(Display.ItemDisplay entity) {
		if (entity.getItemStack().getItem() == Items.GHAST_TEAR) {
			boneStand = entity;
			prev = entity.position();
			curr = null;
			initialDir = null;
			returning = false;
		}
	}

	private static void handleBone(Minecraft mc) {
		if (boneStand == null) return;
		if (!boneStand.isAlive()) {
			prev = curr = initialDir = null;
			returning = false;
			return;
		}

		curr = boneStand.position();
		Vec3 delta = prev == null ? null : curr.subtract(prev);
		if (delta != null && delta.lengthSqr() > 1.0E-6) {
			if (initialDir == null) {
				initialDir = delta.normalize();
			} else if (!returning) {
				double dot = delta.normalize().dot(initialDir);
				if (dot < 0.995) {
					returning = true;
					onBackbone(mc);
				}
			}
		}
		prev = curr;
	}

	private static void onBackbone(Minecraft mc) {
		hudTicks = HUD_TICKS;
		if (SHOW_GEAR_IN_CHAT && mc.player != null) {
			String hand = mc.player.getMainHandItem().getDisplayName().getString();
			String helmet = mc.player.getItemBySlot(EquipmentSlot.HEAD).getDisplayName().getString();
			double seconds = Math.max(p4Ticks, 0) / 20.0;
			mc.player.displayClientMessage(
				Component.literal(String.format("Backbone hit with %s and %s at %.1fs.", hand, helmet, seconds)),
				false);
		}
	}

	private static String skyblockId(ItemStack stack) {
		var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		return tag.getString("id").orElse("");
	}
}
