package com.linweiyun.genshin.mixin.mixins;

import com.linweiyun.genshin.client.action.ClientActionLock;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 移动锁 Mixin。
 * <p>
 * 【原理】
 * LocalPlayer.aiStep() 是玩家每 tick 处理移动输入（WASD 加速、碰撞、重力）的地方。
 * 在它开始前记录位置，结束后把 X/Z 还原 —— 等于"水平移动被抵消"。
 * <p>
 * 【只还原 X/Z 的原因】
 * Y 轴涉及跳跃、下落、击退、飞行，还原 Y 会破坏这些行为。
 * 玩家应该能"跳起来时被前摇锁在原地水平"，也能"被打飞出去"。
 * <p>
 * 【查询对象】
 * 用 {@link ClientActionLock} 而不是服务端的 ActionManager ——
 * 客户端本地状态，零延迟，同 tick 生效。
 */
@Mixin(LocalPlayer.class)
public abstract class MovementBlockMixin {

    /** 每 tick aiStep 前记录的位置，用于 aiStep 后还原 X/Z。 */
    @Unique
    private Vec3 mineGenshin$preAiStepPos = null;

    /**
     * aiStep 前：如果现在应当锁移动，记下当前 X/Z 位置。
     */
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void beforeAiStep(CallbackInfo ci) {
        if (ClientActionLock.isMovementBlocked()) {
            LocalPlayer self = (LocalPlayer) (Object) this;
            mineGenshin$preAiStepPos = self.position();
        }
    }

    /**
     * aiStep 后：把水平位置还原，Y 保持不动。
     * <p>
     * aiStep 期间发生的所有水平位移（WASD 加速、外部推力）都会被这一步抹掉。
     * 但 Y 轴上的跳跃、下落、击飞全部保留。
     */
    @Inject(method = "aiStep", at = @At("RETURN"))
    private void afterAiStep(CallbackInfo ci) {
        if (mineGenshin$preAiStepPos == null) return;
        LocalPlayer self = (LocalPlayer) (Object) this;
        self.setPos(mineGenshin$preAiStepPos.x, self.getY(), mineGenshin$preAiStepPos.z);
        mineGenshin$preAiStepPos = null;
    }
}