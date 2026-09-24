package com.linweiyun.genshin.core.system.combat.animation.config;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * 一个角色的动画配置入口：常态动画集合 + 动作动画名单 + 过渡刻数 + 音效表。
 *
 * <p>由角色的动画常量类实现（如 {@code VesnaAnimations}），交给 {@code PlayerAnimationController}
 * 使用，使运动状态机逻辑全工程只写一份。
 *
 * <p>移植自参考2 的同名接口。
 */
public interface CharacterAnimations {

    /** 常态动画集合。 */
    LocomotionAnims locomotion();

    /**
     * 「特殊动画」名单（普攻 / 战技 / 闪避 / 大招）。
     *
     * <p>这些动画进出时硬切（0 刻过渡）而非平滑过渡，否则姿势会被插值混成四不像。
     */
    Set<String> specialAnims();

    /** 常态动画互相切换时的过渡刻数。 */
    int exitTransitionTicks();

    /**
     * 第一人称动画配置（每个角色一个开关）。
     *
     * <p>默认关。开了之后：写了 {@code fp_*} 动画就用它，没写就复用普通动画 + 转机位。
     * 详见 {@link FirstPersonAnims}。
     */
    default FirstPersonAnims firstPerson() {
        return FirstPersonAnims.DISABLED;
    }

    /** 某个动作状态对应的音效 id（如 {@code "minegenshin:vesna_attack_1"}），没有则返回 {@code null}。 */
    @Nullable
    String soundForState(String stateName);
}
