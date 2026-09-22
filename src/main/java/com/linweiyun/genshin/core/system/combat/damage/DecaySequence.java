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
 *    默认序列 <b>不衰减</b>（命中多少次都是 1.0，见 {@link #DEFAULT_DAMAGE}）
 *    例：长柄重击 [1,0,0,0,0,0,0,0,0,...] 长度15：只有第1次有伤害，后续无伤害
 *    例：绫华重击 [1,1,1,0,0,0,0,0,0,...] 长度27：前3次有伤害，后续无伤害
 *
 * 3. 削韧序列 —— 控制每次攻击的削韧量（韧性系统暂未实装，先占位）
 *    默认序列 <b>不衰减</b>（命中多少次都是 1.0）
 *    例：八重重击 [1,0,0,0,0,0,0] 长度7：只有第1次有削韧
 *
 * 序列长度 = 一次循环的攻击次数上限
 * 超过序列长度后，在计时器清除之前，后续攻击的系数为0（无效果）
 * 例：绫华100次攻击频率 → 超过序列长度24后，第25次起无附着
 * （伤害 / 削韧的默认序列是「不衰减」的特例，不受这条限制）
 */
public class DecaySequence {

    // 序列系数数组 —— 每个位置对应一次攻击命中时的调整系数
    // 系数为1.0表示不衰减，0.0表示完全无效，0.5表示减半
    private final float[] coefficients;

    /**
     * 「永不衰减」标记 —— 无论命中多少次都返回 1.0。
     *
     * <p>给<b>伤害 / 削韧</b>的默认序列用：它们语义上就是「不衰减」，
     * 而按序列取值的实现会在命中数超过长度后返回 0。
     * 原来的 {@code createFilled(1.0f, 15)} 因此变成「第 16 次起伤害为 0」——
     * 注释写着「不衰减」、行为却是归零（普攻的清除窗口是 50 刻 = 2.5 秒，
     * 薇斯娜一整套六段共 16 个 hits，最后一下正好撞上）。
     */
    private final boolean alwaysOne;

    /**
     * 构造函数 —— 从系数数组创建衰减序列
     * @param coefficients 系数数组（不会被复制，直接引用）
     */
    public DecaySequence(float[] coefficients) {
        this(coefficients, false);
    }

    private DecaySequence(float[] coefficients, boolean alwaysOne) {
        this.coefficients = coefficients;
        this.alwaysOne = alwaysOne;
    }

    /**
     * 获取指定命中次数对应的序列系数。
     *
     * <p>「永不衰减」序列恒返回 1.0；普通序列在命中次数超出序列长度后返回 0（超出部分无效，
     * 附着冷却就是靠这个表达）。
     *
     * @param hitCount 攻击命中次数（从0开始计数，第1次命中=0）
     * @return 对应的序列系数（永不衰减序列恒为 1.0），超出范围返回0
     */
    public float getCoefficient(int hitCount) {
        if (alwaysOne) return 1.0f;                                          // 不衰减：永远全额
        if (hitCount < 0 || hitCount >= coefficients.length) return 0.0f;   // 超出范围返回0
        return coefficients[hitCount];                                       // 返回对应位置的系数
    }

    /** 这个序列是不是「永不衰减」（命中多少次都是 1.0）。 */
    public boolean isAlwaysOne() {
        return alwaysOne;
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
     * 「不衰减」单例 —— <b>必须声明在用到它的常量之前</b>。
     *
     * <p>⚠️ Java 的静态字段按<b>书写顺序</b>初始化。原来这个单例写在
     * {@link #DEFAULT_DAMAGE} / {@link #DEFAULT_POISE} <b>后面</b>，
     * 于是 {@code noDecay()} 在那两行执行时读到的还是 {@code null} ——
     * {@code DEFAULT_DAMAGE} / {@code DEFAULT_POISE} 双双被初始化成 null，
     * 直到运行时在 {@code DecayGroup.getDamageCoefficient} 上
     * 抛 {@code NullPointerException}（所有攻击零伤害、领域一 tick 就崩）。
     */
    private static final DecaySequence ALWAYS_ONE = new DecaySequence(new float[0], true);

    /**
     * <b>不衰减序列</b> —— 命中多少次都返回 1.0。
     *
     * <p>给「伤害 / 削韧」这类语义上就是「不衰减」的序列用；
     * 需要「超出长度即无效」的（元素附着冷却）仍然用 {@link #createRepeating} 之类。
     */
    public static DecaySequence noDecay() {
        return ALWAYS_ONE;
    }

    /**
     * 默认伤害序列 —— <b>不衰减</b>：无论命中多少次，伤害系数都是 1.0。
     *
     * <p>⚠️ 原来是 {@code createFilled(1.0f, 15)}：注释写「不衰减」，但序列取值的实现在
     * {@code hitCount >= 15} 时返回 <b>0</b> —— 普攻的清除窗口是 50 刻（2.5 秒），
     * 同一目标在窗口内累积到第 16 次命中就会<b>完全没有伤害</b>。
     * 薇斯娜一整套六段共 16 个 hits，最后一下正好撞上这个边界。
     */
    public static final DecaySequence DEFAULT_DAMAGE = noDecay();

    /**
     * 默认削韧序列 —— <b>不衰减</b>：无论命中多少次，削韧系数都是 1.0。
     * 削韧系统暂未实装，先占位。
     */
    public static final DecaySequence DEFAULT_POISE = noDecay();

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
