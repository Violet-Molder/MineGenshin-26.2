package com.linweiyun.genshin.mixin.mixins;

import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.about.block.BlockElementHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FrostedIceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 浮冰 tick：完全接管原版逻辑。
 *
 * 原版 FrostedIceBlock.tick 按邻居数自动融化 —— 和 FROZEN 元素衰减系统冲突。
 * 这里 cancel 原版 tick，改为只走 FROZEN 衰减 + 相邻火/熔岩触发 PYRO 附着。
 * 处理后重调度 tick，确保衰减链不断。
 */
@Mixin(FrostedIceBlock.class)
public abstract class FrostedIceMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void genshin$tick(BlockState state, ServerLevel level,
                              BlockPos pos, RandomSource random, CallbackInfo ci) {
        // 取消原版邻居检查融化逻辑
        ci.cancel();

        // 冻元素衰减：耗尽则变回水
        BlockElementHelper.tickBlockElementDecay(level, pos);

        // 已变水则停止调度
        if (!level.getBlockState(pos).is(Blocks.FROSTED_ICE)) {
            return;
        }

        // 检测相邻及上方火/熔岩 → 附着 PYRO → 反应系统自动触发融化
        for (Direction dir : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(dir));
            if (neighbor.is(Blocks.FIRE) || neighbor.is(Blocks.SOUL_FIRE)
                    || neighbor.is(Blocks.LAVA)) {
                BlockElementHelper.applyElement(level, pos,
                        ModElements.PYRO.get(), 1.0f, 0.2f);
                level.scheduleTick(pos, state.getBlock(), 1);
                return;
            }
        }
        BlockState above = level.getBlockState(pos.above());
        if (above.is(Blocks.FIRE) || above.is(Blocks.SOUL_FIRE)
                || above.is(Blocks.LAVA)) {
            BlockElementHelper.applyElement(level, pos,
                    ModElements.PYRO.get(), 1.0f, 0.2f);
        }

        // 重调度 next tick —— 确保衰减链不断
        level.scheduleTick(pos, state.getBlock(), 1);
    }
}