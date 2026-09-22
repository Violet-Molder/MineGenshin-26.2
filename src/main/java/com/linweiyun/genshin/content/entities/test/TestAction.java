package com.linweiyun.genshin.content.entities.test;

import com.geckolib.animation.RawAnimation;

/**
 * 测试实体用得到的动作 —— <b>服务端状态</b>与<b>动画名</b>之间的唯一映射。
 *
 * <h2>动画名契约</h2>
 * 名字就是 {@code entity/&lt;id&gt;/&lt;id&gt;.animation.json} 里的动画名，
 * 资源文件里没有对应名字时 GeckoLib 只会打一条警告、模型保持静止，不会崩。
 * <pre>
 * idle             待机（原地蠕动）
 * move.wriggle     蠕动前进
 * move.hop         有攻击目标时的小跳前进
 * attack.leap      大跳扑击（起跳 → 空中）
 * attack.smash     撞击（撞上目标那一刻）
 * attack.land      回位落地
 * attack.cast      施法抬手（test2 召唤冰块）
 * </pre>
 *
 * <p>顺序号会被写进 {@code SynchedEntityData}，所以<b>只能在末尾追加，不能插队</b>。
 */
//TEMP
public enum TestAction {

    /** 原地待机。 */
    IDLE("idle", true),

    /** 蠕动前进。 */
    WRIGGLE("move.wriggle", true),

    /** 有目标时的小跳前进。 */
    HOP("move.hop", true),

    /** 大跳扑击。 */
    LEAP("attack.leap", false),

    /** 撞击。 */
    SMASH("attack.smash", false),

    /** 回位落地。 */
    LAND("attack.land", false),

    /** 施法抬手（test2 召唤冰块）。 */
    CAST("attack.cast", false);

    //TEMP
    private final String animationName;

    //TEMP
    private final boolean loop;

    //TEMP
    private final RawAnimation animation;

    //TEMP
    TestAction(String animationName, boolean loop) {
        this.animationName = animationName;
        this.loop = loop;
        this.animation = loop
                ? RawAnimation.begin().thenLoop(animationName)
                : RawAnimation.begin().thenPlayAndHold(animationName);
    }

    /** 动画文件里的名字。 */
    //TEMP
    public String animationName() {
        return this.animationName;
    }

    /**
     * 喂给 {@code AnimationController} 的实例。
     *
     * <p>是缓存好的常量对象：GeckoLib 的 {@code AnimationController#setAnimation}
     * 用 {@code equals} 判重，每次新建会让同一个动画反复重播。
     */
    //TEMP
    public RawAnimation animation() {
        return this.animation;
    }

    /** 是不是常态循环动作。 */
    //TEMP
    public boolean loop() {
        return this.loop;
    }

    /** 按 {@code SynchedEntityData} 里的顺序号取，越界时回落到 {@link #IDLE}。 */
    //TEMP
    public static TestAction byOrdinal(int ordinal) {
        TestAction[] values = values();
        return ordinal < 0 || ordinal >= values.length ? IDLE : values[ordinal];
    }
}
