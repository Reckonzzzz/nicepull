package com.reckonz.nicepull.client.mixin;

import com.reckonz.nicepull.client.RendPullTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class RendPullMixin {
	@Inject(method = "handleSetEntityData", at = @At("HEAD"))
	private void nicepull$entityData(ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
		// Packets are handled twice (network thread, then game thread); only count the game-thread one.
		if (Minecraft.getInstance().isSameThread()) {
			RendPullTracker.onEntityData(packet);
		}
	}
}
