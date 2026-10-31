
package com.reckonz.nicepull.client.mixin;

import com.reckonz.nicepull.client.NicePullClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandleMixin {

    @Inject(
        method = "handleOpenScreen",
        at = @At("HEAD"),
        cancellable = true
    )
    private void nicepull$closeRefreshedHumanCannonballShop(
            ClientboundOpenScreenPacket packet,
            CallbackInfo ci) {

        if (!NicePullClient.consumeHumanCannonballPurchase()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.getConnection() != null) {
            mc.getConnection().send(
                new ServerboundContainerClosePacket(
                    packet.getContainerId()
                )
            );
        }

        ci.cancel();
    }
}
