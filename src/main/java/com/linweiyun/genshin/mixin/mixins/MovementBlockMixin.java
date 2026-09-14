package com.linweiyun.genshin.mixin.mixins;

import com.linweiyun.genshin.core.system.combat.ComboSystem;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MovementBlockMixin {

    @Unique
    private Vec3 mineGenshin$preAiStepPos = null;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void beforeAiStep(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (ComboSystem.isMovementBlocked(self)) {
            mineGenshin$preAiStepPos = self.position();
        }
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void afterAiStep(CallbackInfo ci) {
        if (mineGenshin$preAiStepPos == null) return;
        LocalPlayer self = (LocalPlayer) (Object) this;
        self.setPos(mineGenshin$preAiStepPos.x, self.getY(), mineGenshin$preAiStepPos.z);
        mineGenshin$preAiStepPos = null;
    }
}