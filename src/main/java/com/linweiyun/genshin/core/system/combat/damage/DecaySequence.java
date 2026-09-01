package com.linweiyun.genshin.core.system.combat.damage;

/**
 * 衰减序列 —— 攻击衰减系统中的序列数据
 *
 * 序列是一组系数数组，用于根据攻击命中次数控制对应属性的实际值：
 *   实际值 = 原始值 × 序列[命中次数 % 序列长度]
 *
 * 三种序列类型：
 * 1. 元素量序列 —— 控制每次攻击实际施加的元素量
 *    默认序列 [1,0,0,1,0,0,...] 长度24：第1、4、7...次附着，其余不附着
 *    例：申鹤战技 [1,0,0,0,0,0,0] 长度7：只有第1次附着，后续6次都不附着
 *
 * 2. 伤害序列 —— 控制每次攻击的实际伤害倍率
 *    默认序列 [1,1,1,...] 长度15：所有攻击伤害不衰减
 *    例：长柄重击 [1,0,0,0,0,0,0,0,0,...] 长度15：只有第1次有伤害，后续无伤害
 *    例：绫华重击 [1,1,1,0,0,0,0,0,0,...] 长度27：前3次有伤害，后续无伤害
 *
 * 3. 削韧序列 —— 控制每次攻击的削韧量（韧性系统暂未实装，先占位）
 *    默认序列 [1,1,1,...] 长度14：所有攻击削韧不衰减
 *    例：八重重击 [1,0,0,0,0,0,0] 长度7：只有第1次有削韧
 *
 * 序列长度 = 一次循环的攻击次数上限
 * 超过序列长度后，在计时器清除之前，后续攻击的系数为0（无效果）
 * 例：绫华100次攻击频率 → 超过序列长度24后，第25次起无附着
 */
public class DecaySequence {

    // 序列系数数组 —— 每个位置对应一次攻击命中时的调整系数
    // 系数为1.0表示不衰减，0.0表示完全无效，0.5表示减半
    private final float[] coefficients;

    /**
     * 构造函数 —— 从系数数组创建衰减序列
     * @param coefficients 系数数组（不会被复制，直接引用）
     */
    public DecaySequence(float[] coefficients) {
        this.coefficients = coefficients;               // 直接引用传入的数组
    }

    /**
     * 获取指定命中次数对应的序列系数
     * 如果命中次数超出序列长度，返回0（超出部分无效）
     * @param hitCount 攻击命中次数（从0开始计数，第1次命中=0）
     * @return 对应的序列系数，超出范围返回0
     */
    public float getCoefficient(int hitCount) {
        if (hitCount < 0 || hitCount >= coefficients.length) return 0.0f;   // 超出范围返回0
        return coefficients[hitCount];                                       // 返回对应位置的系数
    }

    /**
     * 获取序列长度 —— 一次循环的最大攻击次数
     * 超过此长度的攻击在计时器清除前系数为0
     * @return 序列长度
     */
    public int length() {
        return coefficients.length;                     // 返回数组长度
    }

    /**
     * 获取原始系数数组（只读引用）
     * @return 系数数组
     */
    public float[] getCoefficients() {
        return coefficients;                            // 返回数组引用
    }

    // ========== 预定义的常用序列 ==========

    /**
     * 默认元素量序列 —— [1,0,0] 循环展开至长度24
     * 对应：第1、4、7、10...次攻击附着元素，其余不附着
     * 这是原神中大多数攻击使用的标准1-0-0模式
     */
    public static final DecaySequence DEFAULT_ELEMENT = createRepeating(
            new float[]{1.0f, 0.0f, 0.0f}, 8);        // [1,0,0] 重复8次 = 长度24

    /**
     * 默认伤害序列 —— 全1序列，长度15
     * 对应：所有攻击伤害不衰减（系数始终为1）
     */
    public static final DecaySequence DEFAULT_DAMAGE = createFilled(1.0f, 15);

    /**
     * 默认削韧序列 —— 全1序列，长度14
     * 对应：所有攻击削韧不衰减（系数始终为1）
     * 削韧系统暂未实装，先占位
     */
    public static final DecaySequence DEFAULT_POISE = createFilled(1.0f, 14);

    // ========== 工厂方法 ==========

    /**
     * 创建一个重复模式的序列
     * 将基础模式重复指定次数，生成最终序列
     * 例：createRepeating([1,0,0], 8) → [1,0,0,1,0,0,1,0,0,...] 长度24
     *
     * @param pattern 基础模式数组
     * @param repeatCount 重复次数
     * @return 新的衰减序列
     */
    public static DecaySequence createRepeating(float[] pattern, int repeatCount) {
        float[] result = new float[pattern.length * repeatCount];   // 创建结果数组
        for (int i = 0; i < repeatCount; i++) {                     // 遍历重复次数
            System.arraycopy(pattern, 0, result, i * pattern.length, pattern.length); // 复制模式
        }
        return new DecaySequence(result);                           // 返回新序列
    }

    /**
     * 创建一个全填充序列
     * 所有位置都是相同的系数值
     * 例：createFilled(1.0f, 15) → [1,1,1,1,...] 长度15
     *
     * @param value 填充值
     * @param length 序列长度
     * @return 新的衰减序列
     */
    public static DecaySequence createFilled(float value, int length) {
        float[] result = new float[length];                         // 创建结果数组
        java.util.Arrays.fill(result, value);                       // 填充所有位置
        return new DecaySequence(result);                           // 返回新序列
    }

    /**
     * 从给定的系数数组直接创建序列
     * 等同于 new DecaySequence(coefficients)
     *
     * @param coefficients 系数数组
     * @return 新的衰减序列
     */
    public static DecaySequence of(float... coefficients) {
        return new DecaySequence(coefficients);                     // 直接创建
    }
}
