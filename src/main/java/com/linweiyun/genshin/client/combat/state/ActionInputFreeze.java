package com.linweiyun.genshin.client.combat.state;

import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;

/**
 * 动作锁的「定身」实现：把本地玩家的输入实例换成零向量替身。
 *
 * <p>整块逻辑原先内嵌在 {@code ActionStateMachine} 里（{@code FrozenInput} 加保存/还原），
 * 属于纯输入层操作，与状态推进无关，故独立成类。
 * 状态机只负责决定「什么时候该冻、什么时候该解」，实际替换动作由这里完成。
 */
public final class ActionInputFreeze {

    private static final ClientInput FROZEN_INPUT = new FrozenInput();

    @Nullable
    private static ClientInput savedInput;

    private ActionInputFreeze() {
    }

    /**
     * 用零向量替身顶替玩家当前输入实例。
     *
     * <p>重复调用不会覆盖已保存的原实例 —— 否则「冻结期间又被冻结一次」
     * 会把替身自己存成原实例，解冻后玩家就永久定身了。
     */
    public static void install(LocalPlayer player) {
        if (player.input != FROZEN_INPUT) {
            savedInput = player.input;
            player.input = FROZEN_INPUT;
        }
    }

    /**
     * 移动锁结束后把玩家原本的输入实例还回去（由每 tick 的状态机统一处理）。
     *
     * <p>{@code savedInput} 为空时<b>什么都不做</b>：宁可多冻一 tick，也不能塞一个
     * 不会被 tick 的空 {@code ClientInput} 进去 —— 那会让玩家这一局再也动不了。
     */
    public static void restore(LocalPlayer player) {
        if (player.input != FROZEN_INPUT || savedInput == null) {
            return;
        }
        player.input = savedInput;
        savedInput = null;
    }

    /**
     * 定身用的输入替身：{@code moveVector} 恒为零向量，按键全空。
     *
     * <p>26.2 的 {@code Input} 已经变成 record，参考2 那种「就地改 forwardImpulse」的写法不再可行，
     * 所以这里换成一个只读的 {@link ClientInput} 子类，锁移动期间顶替玩家原本的输入实例。
     */
    private static final class FrozenInput extends ClientInput {
        @Override
        public Vec2 getMoveVector() {
            return Vec2.ZERO;
        }

        @Override
        public boolean hasForwardImpulse() {
            return false;
        }

        /**
         * 原版自动跳跃会在 {@code aiStep} 里对这个实例调 {@code makeJump()}，
         * 而 {@code ClientInput.makeJump()} 是「把 jump 置 true」的就地修改 ——
         * 共享的静态实例一旦被改过一次，之后每次定身都会自己起跳。所以这里直接吞掉。
         */
        @Override
        public void makeJump() {
        }
    }
}
