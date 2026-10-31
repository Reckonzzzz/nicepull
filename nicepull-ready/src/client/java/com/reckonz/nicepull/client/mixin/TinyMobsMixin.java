
package com.reckonz.nicepull.client.mixin;

import com.reckonz.nicepull.client.NicePullClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class TinyMobsMixin {
    // 1.0 = normal size; 0.5 = half size.
    private static final float SCALE = 0.5f;

    @Inject(method = "submit",
        at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V",
            shift = At.Shift.AFTER))
    private void nicepull$tiny(EntityRenderState renderState, CameraRenderState camera,
            double x, double y, double z, PoseStack poseStack,
            SubmitNodeCollector collector, CallbackInfo ci) {

        if (NicePullClient.isTinyMobsEnabled()
                && BuiltInRegistries.ENTITY_TYPE.getKey(renderState.entityType)
                        .getPath().equals("zombie")) {
            poseStack.scale(SCALE, SCALE, SCALE);
        }
    }
}
