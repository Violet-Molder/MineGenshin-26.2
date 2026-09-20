package com.linweiyun.genshin.mixin.mixins;

import com.linweiyun.genshin.client.render.character.CharacterRenderDispatcher;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(method = "submit("
            + "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
            + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;"
            + ")V",
            at = @At("HEAD"), cancellable = true)
    private void onRenderSubmit(AvatarRenderState state, PoseStack poseStack,
                                 SubmitNodeCollector submitNodeCollector,
                                 CameraRenderState cameraState, CallbackInfo ci) {
        if (CharacterRenderDispatcher.handleSubmit(state, poseStack, submitNodeCollector, cameraState)) {
            ci.cancel();
        }
    }
}