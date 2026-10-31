package com.reckonz.nicepull.client.mixin;

import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LocalPlayer.class)
public class HollowWandMixin {
	@ModifyArgs(method = "raycastHitResult",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/component/AttackRange;getClosesetHit(Lnet/minecraft/world/entity/Entity;FLjava/util/function/Predicate;)Lnet/minecraft/world/phys/HitResult;"))
	private void nicepull$closestHit(Args args) {
		if (nicepull$holdingWand()) {
			args.set(2, (Predicate<Entity>) e -> false);
		}
	}

	@ModifyArgs(method = "pick",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"))
	private static void nicepull$entityHit(Args args) {
		if (nicepull$holdingWand()) {
			args.set(4, (Predicate<Entity>) e -> false);
		}
	}

	private static boolean nicepull$holdingWand() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return false;
		var tag = player.getMainHandItem()
			.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
			.copyTag();
		return tag.getString("id").map("HOLLOW_WAND"::equals).orElse(false);
	}
}
