package com.linweiyun.genshin.core.system.combat.animation.config;

import com.geckolib.animation.RawAnimation;

/**
 * 一个角色的常态动画集合（站 / 走 / 跑 / 蹲 / 睡 / 爬 / 游泳 / 跳跃）。
 *
 * <p>把「动画名」抽成数据，让 {@code PlayerAnimationController}
 * 的运动状态机只写一份、所有角色共用。
 *
 * <p>移植自参考2 的同名类，仅把 GeckoLib 4 的 API 换成 GeckoLib 5（包名 software.bernie → com.geckolib）。
 */
public record LocomotionAnims(
        RawAnimation idle,
        RawAnimation walk,
        RawAnimation run,
        RawAnimation walkBack,
        RawAnimation crouch,
        RawAnimation crouchWalk,
        RawAnimation sleep,
        RawAnimation climb,
        RawAnimation waterIdle,
        RawAnimation waterWalk,
        RawAnimation waterWalkBack,
        RawAnimation swim,
        RawAnimation jump,
        RawAnimation jumpDown) {

    /** 便捷工厂：把动画名一次性包成循环播放的 RawAnimation。 */
    public static LocomotionAnims of(
            String idle, String walk, String run, String walkBack,
            String crouch, String crouchWalk, String sleep, String climb,
            String waterIdle, String waterWalk, String waterWalkBack, String swim,
            String jump, String jumpDown) {
        return new LocomotionAnims(
                loop(idle), loop(walk), loop(run), loop(walkBack),
                loop(crouch), loop(crouchWalk), loop(sleep), loop(climb),
                loop(waterIdle), loop(waterWalk), loop(waterWalkBack), loop(swim),
                loop(jump), loop(jumpDown));
    }

    /** 参考2 / 本项目动画 json 里的通用常态动画名。 */
    public static final LocomotionAnims DEFAULT = of(
            "idle", "walk", "run", "walk_back",
            "crouch", "crouch_walk", "sleep", "climb",
            "water", "water_walk", "water_walk_back", "swim",
            "jump", "jump_down");

    private static RawAnimation loop(String animationName) {
        return RawAnimation.begin().thenLoop(animationName);
    }
}
