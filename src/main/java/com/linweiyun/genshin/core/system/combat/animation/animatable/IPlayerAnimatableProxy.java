package com.linweiyun.genshin.core.system.combat.animation.animatable;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * 能从动画对象反查回玩家实体。
 *
 * <p>移植自参考2 的同名接口：动画控制器需要知道「这一帧在给谁播动画」，
 * 而不是每帧都去遍历世界。
 */
public interface IPlayerAnimatableProxy {

    @Nullable
    Player getPlayerEntity();

    void setPlayerEntity(@Nullable Player player);
}
