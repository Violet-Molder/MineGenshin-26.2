package com.linweiyun.genshin.core.system.combat.decay;

import com.linweiyun.genshin.core.system.combat.damage.DecaySequence;

/**
 * 衰减组别 —— 定义攻击衰减系统的控制参数
 * <p>
 * 每个衰减组别包含4个参数：
 * 1. 清除时间（clearTimeTicks）—— 定时器倒计时时长（单位：tick）
 * 倒计时结束后，清除计数器和定时器，下次命中重新从第1次开始计数
 * 默认值：2.5秒 = 50 tick
 * 例：申鹤战技的实际清除时间为0.1秒 = 2 tick，所以几乎每次都能附着
 * <p>
 * 2. 元素量序列（elementSequence）—— 控制每次攻击实际施加的元素量
 * 实际元素量 = 攻击原始元素量 × 序列系数
 * 系数为0 = 不附着，系数为1 = 全额附着
 * <p>
 * 3. 伤害序列（damageSequence）—— 控制每次攻击的实际伤害输出
 * 实际伤害 = 计算伤害 × 序列系数
 * 大多数攻击此序列为全1（不衰减），但部分特殊攻击会衰减
 * 例：长柄重击只有第1次有伤害
 * <p>
 * 4. 削韧序列（poiseSequence）—— 控制每次攻击的削韧量
 * 削韧系统暂未实装，序列先占位
 * 韧性打到0视为破韧，会打断目标动作甚至造成控制
 * <p>
 * 衰减组别与衰减标签的关系：
 * - 标签决定"是否共用"计时计数器
 * - 组别决定"控制幅度"（时间 + 三序列）
 * - 同标签 + 同组别 = 完全共用一套计时计数器
 * - 同标签 + 不同组别 = 不共用（虽然标签相同但控制参数不同）
 *
 * @param clearTimeTicks  清除时间（单位：tick）—— 定时器倒计时时长 从第一次命中开始计时，到期后重置计数器和定时器
 * @param elementSequence 元素量序列 —— 控制每次攻击的元素附着量系数
 * @param damageSequence  伤害序列 —— 控制每次攻击的伤害输出系数
 * @param poiseSequence   削韧序列 —— 控制每次攻击的削韧量系数（暂未实装，先占位）
 */
public record DecayGroup(int clearTimeTicks, DecaySequence elementSequence, DecaySequence damageSequence,
                         DecaySequence poiseSequence) {

    // 2.5秒对应的tick数（MC中1秒=20tick）
    // 这是原神中大多数攻击使用的标准清除时间
    public static final int DEFAULT_CLEAR_TIME_TICKS = 50;

    /**
     * 完整构造函数
     *
     * @param clearTimeTicks  清除时间（tick）
     * @param elementSequence 元素量序列
     * @param damageSequence  伤害序列
     * @param poiseSequence   削韧序列
     */
    public DecayGroup {
        // ⚠️ 三条序列一条都不能是 null —— 这里做一次快速失败。
        //
        // 为什么需要：Java 的静态字段按书写顺序初始化。曾经 {@code DecaySequence.DEFAULT_DAMAGE}
        // 被写成「引用一个声明在它后面的单例」，于是那个常量被静默初始化成 null，
        // 一路传到 {@code DecayGroup.getDamageCoefficient()} 才在运行时炸 NPE
        // （表现是「所有攻击零伤害」+「领域实体一 tick 就崩服」，而且编译、启动都不报错）。
        // 在这里挡住的话，这种错会在类初始化那一刻就带着清楚的消息抛出来。
        java.util.Objects.requireNonNull(elementSequence, "DecayGroup.elementSequence 不能为 null");
        java.util.Objects.requireNonNull(damageSequence, "DecayGroup.damageSequence 不能为 null");
        java.util.Objects.requireNonNull(poiseSequence, "DecayGroup.poiseSequence 不能为 null");
    }

    // ========== Getter 方法 ==========

    /**
     * 获取清除时间（tick）
     */
    @Override
    public int clearTimeTicks() {
        return clearTimeTicks;
    }

    /**
     * 获取元素量序列
     */
    @Override
    public DecaySequence elementSequence() {
        return elementSequence;
    }

    /**
     * 获取伤害序列
     */
    @Override
    public DecaySequence damageSequence() {
        return damageSequence;
    }

    /**
     * 获取削韧序列
     */
    @Override
    public DecaySequence poiseSequence() {
        return poiseSequence;
    }

    // ========== 便捷方法 ==========

    /**
     * 获取指定命中次数对应的元素量系数
     *
     * @param hitCount 攻击命中次数（从0开始）
     * @return 元素量系数（0.0~1.0）
     */
    public float getElementCoefficient(int hitCount) {
        return elementSequence.getCoefficient(hitCount);    // 从元素量序列获取系数
    }

    /**
     * 获取指定命中次数对应的伤害系数
     *
     * @param hitCount 攻击命中次数（从0开始）
     * @return 伤害系数（0.0~1.0）
     */
    public float getDamageCoefficient(int hitCount) {
        return damageSequence.getCoefficient(hitCount);     // 从伤害序列获取系数
    }

    /**
     * 获取指定命中次数对应的削韧系数
     *
     * @param hitCount 攻击命中次数（从0开始）
     * @return 削韧系数（0.0~1.0）
     */
    public float getPoiseCoefficient(int hitCount) {
        return poiseSequence.getCoefficient(hitCount);      // 从削韧序列获取系数
    }
}
