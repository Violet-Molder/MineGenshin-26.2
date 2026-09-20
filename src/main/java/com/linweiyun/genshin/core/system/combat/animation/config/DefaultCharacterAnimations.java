package com.linweiyun.genshin.core.system.combat.animation.config;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * 动画系统的兜底配置：给「只提供了渲染数据、还没写 Java 动画系统」的角色用。
 *
 * <p>只解决渲染问题（常态动画用最通用的名字），不提供任何动作动画、音效或动作编排。
 * 移植自参考2 的同名类。
 */
public final class DefaultCharacterAnimations implements CharacterAnimations {

    public static final DefaultCharacterAnimations INSTANCE = new DefaultCharacterAnimations();

    private DefaultCharacterAnimations() {
    }

    @Override
    public LocomotionAnims locomotion() {
        return LocomotionAnims.DEFAULT;
    }

    /** 空集合：不知道哪些是动作动画，一律按常态处理。 */
    @Override
    public Set<String> specialAnims() {
        return Set.of();
    }

    @Override
    public int exitTransitionTicks() {
        return 5;
    }

    @Nullable
    @Override
    public String soundForState(String stateName) {
        return null;
    }
}
