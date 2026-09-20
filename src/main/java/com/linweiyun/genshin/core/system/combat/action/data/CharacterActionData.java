package com.linweiyun.genshin.core.system.combat.action.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据驱动的动作配置（来自 character/{id}/behavior/ 下的 JSON）。
 * 双端安全（只存字符串与数值，不引用客户端/服务端类型）。
 * <p>
 * 包含普攻连段、战技、大招、闪避的完整配置。
 */
public final class CharacterActionData {

    private final ComboData combo;
    private final SkillData skill;
    private final BurstData burst;
    private final DodgeData dodge;

    public CharacterActionData(ComboData combo, SkillData skill, BurstData burst, DodgeData dodge) {
        this.combo = combo;
        this.skill = skill;
        this.burst = burst;
        this.dodge = dodge;
    }

    public ComboData combo() { return combo; }
    public SkillData skill() { return skill; }
    public BurstData burst() { return burst; }
    public DodgeData dodge() { return dodge; }

    // ==================== 兜底配置 ====================

    /**
     * 没写 {@code XxxResources.ACTION_DATA} 的角色的通用动作配置。
     *
     * <p>没有它的话 {@code TalentBase.buildDefaultActionSet()} 会返回空集，
     * 角色的普攻 / 战技 / 大招会全部静默失效（按键毫无反应）。
     *
     * <p>时序只是「够用」的量级：动画名走通用命名，角色动画文件里没有对应名字时
     * {@code AnimationAvailability} 会拦住切换，只结算伤害 + 摆臂。
     * 想调手感就在角色自己的 {@code XxxResources} 里写一份真正的配置。
     *
     * @param maxCombo 该角色天赋声明的连段数
     */
    public static CharacterActionData fallback(int maxCombo) {
        int stages = Math.max(1, maxCombo);

        Map<Integer, ActionStep> comboSteps = new LinkedHashMap<>();
        for (int stage = 1; stage <= stages; stage++) {
            comboSteps.put(stage, new ActionStep(
                    "attack_" + stage, 20, 0, 2,
                    List.of(new Move(0, 1.0)),
                    List.of(new Hit(4, 1.5, 1.5, 1.0, 0.0, 3.0, false)),
                    List.of(),
                    0, 0, 0, 8
            ));
        }

        ActionStep skillStep = new ActionStep(
                "skill", 25, 6, 3,
                List.of(new Move(0, 1.0)),
                List.of(new Hit(8, 2.0, 1.5, 1.0, 0.0, 3.0, false)),
                List.of(),
                0, 0, 0, 0
        );

        ActionStep burstStep = new ActionStep(
                "burst", 40, 40, 4,
                List.of(),
                List.of(new Hit(10, 3.0, 1.5, 1.0, 0.0, 4.0, false)),
                List.of(),
                0, 0, 0, 0
        );

        ActionStep dodgeStep = new ActionStep(
                "dodge_front", 12, 0, 3,
                List.of(new Move(0, 1.5)),
                List.of(),
                List.of(),
                0, 0, 0, 0
        );

        return new CharacterActionData(
                new ComboData(stages, comboSteps),
                new SkillData(skillStep, null),
                new BurstData(burstStep, 80f),
                new DodgeData(dodgeStep)
        );
    }

    // ==================== 普攻连段 ====================

    public static final class ComboData {
        private final int maxCombo;
        private final Map<Integer, ActionStep> steps;

        public ComboData(int maxCombo, Map<Integer, ActionStep> steps) {
            this.maxCombo = maxCombo;
            this.steps = steps == null ? Collections.emptyMap() : steps;
        }

        public int maxCombo() { return maxCombo; }
        public ActionStep getStep(int stage) { return steps.get(stage); }
    }

    // ==================== 战技 ====================

    public static final class SkillData {
        private final ActionStep tap;
        private final ActionStep hold;

        public SkillData(ActionStep tap, ActionStep hold) {
            this.tap = tap;
            this.hold = hold;
        }

        public ActionStep tap() { return tap; }
        public ActionStep hold() { return hold; }
        public boolean hasHold() { return hold != null; }
    }

    // ==================== 大招 ====================

    public static final class BurstData {
        private final ActionStep step;
        private final float energyCost;

        public BurstData(ActionStep step, float energyCost) {
            this.step = step;
            this.energyCost = energyCost;
        }

        public ActionStep step() { return step; }
        public float energyCost() { return energyCost; }
    }

    // ==================== 闪避 ====================

    public static final class DodgeData {
        private final ActionStep step;

        public DodgeData(ActionStep step) {
            this.step = step;
        }

        public ActionStep step() { return step; }
    }

    // ==================== 单步动作 ====================

    /**
     * 一个动作步骤的完整配置。
     */
    public static final class ActionStep {
        public final String animation;           // 动画名
        public final int duration;               // 动画总时长（刻）
        public final int protectDuration;        // 保护时长（刻）：期间无法被打断
        public final int priority;               // 优先级
        public final List<Move> moves;           // 位移列表
        public final List<Hit> hits;             // 伤害列表
        public final List<SoundRef> sounds;      // 音效列表（旧写法：每格固定播一条）
        /** 音效编排（新写法）：序列 + 随机可组合，见 {@link SoundCue}。 */
        public final List<SoundCue> soundCues = new java.util.ArrayList<>();
        public final float skillCharge;          // 战技能量回复
        public final float finalCharge;          // 大招能量回复
        public final int cooldown;               // 冷却（刻），0 表示无冷却
        public final int comboWindow;            // 连击窗口（刻），后摇结束后可接下一段的窗口
        public String comboEndAnim = null;       // 最后一段连招结束后的收尾动画（仅最终段生效）
        public int comboEndTicks = DEFAULT_COMBO_END_TICKS; // 收尾动画播多少刻

        /**
         * 这一段的「生效攻击距离」（格）。索敌距离通常大于它，差额就是突进要补的距离。
         *
         * <p>为 0 时用 {@link #DEFAULT_MELEE_RANGE}（3 格）。
         * <b>不会</b>去猜 {@code hits[].forward + scope} —— 那是 AoE 半径不是够得着的距离，
         * 早期版本拿它算过，结果角色在 8 格外就停下不出手。
         */
        public float attackRange = 0f;

        /**
         * 这一招的<b>交战形态</b>：远程还是近战、要不要突进、索敌多远。
         *
         * <p>默认 {@link Engagement#melee()}（近战普攻的那套）。远程招式写
         * {@code .ranged()} 或 {@code .withEngagement(Engagement.ranged())}。
         *
         * <p><b>为什么不写进 {@link Hit}</b>：一个招式可以有多个 hit（各自 delay/forward/scope），
         * 而「这一招是不是远程」是<b>整段动作</b>的属性 —— 它决定的是「要不要贴上去、
         * 索敌多远」，不是某一次伤害结算的属性。写成 hit 的字段会出现「同一招里第一个 hit
         * 说是远程、第二个说是近战」这种没有意义的组合，所以放在 step 上，整招共用一份。
         */
        public Engagement engagement = Engagement.melee();

        /**
         * 这一段的 {@code moves} 位移<b>允不允许带 Y 轴</b>。
         *
         * <p>默认 {@code false}：位移只在<b>水平面</b>上给（视线方向投影到水平面再归一化）。
         *
         * <h2>为什么默认关掉</h2>
         * 位移是沿<b>视线方向</b>给的冲量，而视线是有俯仰的：抬头砍一刀 → 人也跟着往上窜
         * （俗称原地起飞）。绝大多数地面招式不想要这个，而且低头时还会把人往地里按。
         *
         * <p>想保留「上挑把人带起来」这种手感，就在那一段上
         * {@code .withVerticalMove(true)}。
         *
         * <p>注意<b>这不影响索敌突进</b>：突进（{@code AttackApproach}）是朝<b>目标位置</b>去的，
         * 打飞在空中的敌人时它必须能往上走 —— 那是「追目标」，不是「跟你抬头的方向走」。
         */
        public boolean moveAllowsVertical = false;

        /** 收尾动画的默认时长（刻）。 */
        public static final int DEFAULT_COMBO_END_TICKS = 60;

        /**
         * 默认攻击距离（格）—— 近战够得着的范围。
         *
         * <p>「索敌距离 &gt; 攻击距离」的差额就是突进要补的距离。
         */
        public static final float DEFAULT_MELEE_RANGE = 3.0f;

        public ActionStep(String animation, int duration, int protectDuration, int priority,
                          List<Move> moves, List<Hit> hits, List<SoundRef> sounds,
                          float skillCharge, float finalCharge, int cooldown, int comboWindow) {
            this.animation = animation;
            this.duration = duration;
            this.protectDuration = protectDuration;
            this.priority = priority;
            this.moves = moves == null ? Collections.emptyList() : moves;
            this.hits = hits == null ? Collections.emptyList() : hits;
            this.sounds = sounds == null ? Collections.emptyList() : sounds;
            this.skillCharge = skillCharge;
            this.finalCharge = finalCharge;
            this.cooldown = cooldown;
            this.comboWindow = comboWindow;
        }

        /** 便捷写法：设置收尾动画与它的时长。 */
        public ActionStep withComboEnd(String animation, int ticks) {
            this.comboEndAnim = animation;
            this.comboEndTicks = ticks > 0 ? ticks : DEFAULT_COMBO_END_TICKS;
            return this;
        }

        /** 便捷写法：显式指定这一段的生效攻击距离（格）。 */
        public ActionStep withAttackRange(float range) {
            this.attackRange = Math.max(0f, range);
            return this;
        }

        /** 便捷写法：换掉这一招的交战形态（远程/近战、突不进、索敌多远）。 */
        public ActionStep withEngagement(Engagement value) {
            if (value != null) {
                this.engagement = value;
            }
            return this;
        }

        /**
         * 便捷写法：这一段的位移要不要带 Y 轴。
         *
         * <p>{@code true} = 沿视线冲（抬头会起飞，上挑、跳劈这类要它）；
         * {@code false}（默认）= 只走水平面。
         */
        public ActionStep withVerticalMove(boolean allowVertical) {
            this.moveAllowsVertical = allowVertical;
            return this;
        }

        /**
         * 便捷写法：标记为<b>远程招式</b>。
         *
         * <p>等价于 {@code withEngagement(Engagement.ranged())}，效果：
         * <ul>
         *   <li>不突进（永远只转向）；</li>
         *   <li>索敌距离 = {@link #effectiveAttackRange()}（够得着才锁）。</li>
         * </ul>
         *
         * <p><b>远程招式记得配 {@link #withAttackRange(float)}</b> —— 不配的话
         * {@code effectiveAttackRange()} 会用近战默认的 3 格，索敌也就只有 3 格。
         */
        public ActionStep ranged() {
            this.engagement = Engagement.ranged();
            return this;
        }

        /**
         * 追加一格音效编排（序列 + 随机）。
         *
         * <p>和旧的 {@link #sounds} 是叠加关系：旧列表里每一条会当成「固定播一格」，
         * 这里追加的是额外编排。新代码建议只用这个。
         */
        public ActionStep withSoundCue(SoundCue cue) {
            if (cue != null) {
                this.soundCues.add(cue);
            }
            return this;
        }

        /**
         * 实际生效的攻击距离。
         *
         * <p><b>默认就是近战够得着的距离</b>，不需要每个动作都配 —— 大多数攻击只用默认值。
         * 想特调（比如长枪、大范围横斩）再用 {@link #withAttackRange(float)} 覆盖。
         *
         * <p>注意<b>不能拿 {@code hits[].scope} 当攻击距离</b>：那是 AoE 半径，
         * 不是「够得着多远」。早期版本用它算，结果薇斯娜会在 8 格外就停下不出手。
         */
        public float effectiveAttackRange() {
            return attackRange > 0f ? attackRange : DEFAULT_MELEE_RANGE;
        }
    }

    // ==================== 交战形态（逐招式） ====================

    /**
     * 一个招式的<b>交战形态</b> —— 「这一招怎么接近目标」。
     *
     * <h2>只分两种大形态</h2>
     * <table border="1">
     *   <caption>近战 vs 远程</caption>
     *   <tr><th></th><th>近战招式（默认）</th><th>远程招式（{@link #ranged()}）</th></tr>
     *   <tr><td>突进</td><td>目标在攻击距离外 → 贴上去</td><td><b>永不突进</b></td></tr>
     *   <tr><td>转向</td><td>滑向目标（快但不瞬移）</td><td>一样滑向目标</td></tr>
     *   <tr><td>索敌距离</td><td>比攻击距离大（默认 6 格），差额交给突进补</td>
     *       <td><b>= 攻击距离</b>（够得着才锁）</td></tr>
     * </table>
     *
     * <h2>为什么判定单位是「招式」而不是「角色」</h2>
     * 同一个角色完全可能既有近战普攻又有远程战技（弓手砍人、法师抡杖）。
     * 按角色分只能二选一，按招式分则各招各的。
     *
     * <h2>怎么调</h2>
     * <pre>
     * // 1. 远程招式 —— 最常见的写法
     * step.ranged();
     *
     * // 2. 近战但不想突进（原地挥砍的大招）
     * step.withEngagement(Engagement.melee().withDash(false));
     *
     * // 3. 长枪：够得着 5 格，索敌 8 格，冲过去停 3 格
     * step.withAttackRange(5f).withEngagement(
     *         Engagement.melee().withAcquireRange(8).withDashProfile(3.0, 0.9, 24));
     *
     * // 4. 飞刀：索敌 12 格、只转向、甩得慢一点
     * step.withEngagement(
     *         Engagement.ranged().withAcquireRange(12).withAngles(40, 65).withTurnSpeed(28));
     * </pre>
     *
     * <p>所有数值为 {@link #USE_DEFAULT}（-1）时用类里的全局默认，见
     * {@code CombatTargeting} / {@code AttackApproach}。
     */
    public static final class Engagement {

        /** 该字段不覆盖，用全局默认。 */
        public static final double USE_DEFAULT = -1.0;

        /** 是不是远程招式：不突进 + 索敌距离跟着攻击距离走。 */
        public final boolean ranged;
        /** 允不允许突进（远程招式里这个开关无效，远程永远不突进）。 */
        public final boolean dash;
        /** 索敌距离（格）；{@link #USE_DEFAULT} = 近战 6 格 / 远程 = 攻击距离。 */
        public final double acquireRange;
        /** 保持锁定的距离（格）；{@link #USE_DEFAULT} = 近战 9 格 / 远程 = 索敌 + 2。 */
        public final double keepRange;
        /** 索敌视角半角（度）；{@link #USE_DEFAULT} = 全局默认。 */
        public final double acquireAngle;
        /** 保持锁定的视角半角（度）；{@link #USE_DEFAULT} = 全局默认。 */
        public final double keepAngle;
        /** 突进停到离目标几格；{@link #USE_DEFAULT} = 全局默认。 */
        public final double stopDistance;
        /** 突进速度（格/刻）；{@link #USE_DEFAULT} = 全局默认。 */
        public final double dashSpeed;
        /** 突进最多几刻；&lt;= 0 表示用全局默认。 */
        public final int maxApproachTicks;
        /** 转向最大角速度（度/刻）；{@link #USE_DEFAULT} = 全局默认。 */
        public final float turnSpeed;

        public Engagement(boolean ranged, boolean dash,
                          double acquireRange, double keepRange,
                          double acquireAngle, double keepAngle,
                          double stopDistance, double dashSpeed,
                          int maxApproachTicks, float turnSpeed) {
            this.ranged = ranged;
            this.dash = dash;
            this.acquireRange = acquireRange;
            this.keepRange = keepRange;
            this.acquireAngle = acquireAngle;
            this.keepAngle = keepAngle;
            this.stopDistance = stopDistance;
            this.dashSpeed = dashSpeed;
            this.maxApproachTicks = maxApproachTicks;
            this.turnSpeed = turnSpeed;
        }

        /** 近战默认：索敌 6 / 保持 9 / 攻击距离外突进贴脸。 */
        public static Engagement melee() {
            return new Engagement(false, true, USE_DEFAULT, USE_DEFAULT, USE_DEFAULT, USE_DEFAULT,
                    USE_DEFAULT, USE_DEFAULT, 0, (float) USE_DEFAULT);
        }

        /** 远程：不突进、索敌 = 攻击距离、只转向。 */
        public static Engagement ranged() {
            return new Engagement(true, false, USE_DEFAULT, USE_DEFAULT, USE_DEFAULT, USE_DEFAULT,
                    USE_DEFAULT, USE_DEFAULT, 0, (float) USE_DEFAULT);
        }

        /** 这一招实际会不会突进。 */
        public boolean wantsDash() {
            return !ranged && dash;
        }

        public Engagement withDash(boolean enabled) {
            return new Engagement(ranged, enabled, acquireRange, keepRange, acquireAngle, keepAngle,
                    stopDistance, dashSpeed, maxApproachTicks, turnSpeed);
        }

        public Engagement withAcquireRange(double range) {
            return new Engagement(ranged, dash, range, keepRange, acquireAngle, keepAngle,
                    stopDistance, dashSpeed, maxApproachTicks, turnSpeed);
        }

        public Engagement withKeepRange(double range) {
            return new Engagement(ranged, dash, acquireRange, range, acquireAngle, keepAngle,
                    stopDistance, dashSpeed, maxApproachTicks, turnSpeed);
        }

        /** 索敌与保持的视角半角（度）。 */
        public Engagement withAngles(double acquire, double keep) {
            return new Engagement(ranged, dash, acquireRange, keepRange, acquire, keep,
                    stopDistance, dashSpeed, maxApproachTicks, turnSpeed);
        }

        /** 突进手感三件套：停几格 / 多快 / 最多几刻。 */
        public Engagement withDashProfile(double stopDistance, double dashSpeed, int maxApproachTicks) {
            return new Engagement(ranged, dash, acquireRange, keepRange, acquireAngle, keepAngle,
                    stopDistance, dashSpeed, maxApproachTicks, turnSpeed);
        }

        /** 转向最大角速度（度/刻）——想让人物转得更急/更从容就调这个。 */
        public Engagement withTurnSpeed(float degreesPerTick) {
            return new Engagement(ranged, dash, acquireRange, keepRange, acquireAngle, keepAngle,
                    stopDistance, dashSpeed, maxApproachTicks, degreesPerTick);
        }
    }

    // ==================== 伤害 ====================

    public static final class Hit {
        public final int delay;          // 延时（刻）
        public final double forward;     // 向前距离
        public final double yOffset;     // Y 偏移
        public final double damage;      // 基础伤害倍率
        public final double damageSp;    // 特殊伤害倍率
        public final double scope;       // 伤害范围
        public final boolean ignoreInvuln; // 是否无视无敌

        public Hit(int delay, double forward, double yOffset, double damage,
                   double damageSp, double scope, boolean ignoreInvuln) {
            this.delay = delay;
            this.forward = forward;
            this.yOffset = yOffset;
            this.damage = damage;
            this.damageSp = damageSp;
            this.scope = scope;
            this.ignoreInvuln = ignoreInvuln;
        }
    }

    // ==================== 位移 ====================

    public static final class Move {
        public final int delay;      // 延时（刻）
        public final double speed;   // 速度

        public Move(int delay, double speed) {
            this.delay = delay;
            this.speed = speed;
        }
    }

    // ==================== 音效 ====================

    /**
     * 一条音效条目。
     *
     * <p>{@code name} 为 null/空 = <b>静音</b>，用来占一份概率（「这刀没喊」）。
     * {@code weight} 是随机权重，只有在 {@link SoundCue.PickMode#PICK_ONE} 里才有意义。
     */
    public static final class SoundRef {
        public final int delay;
        public final String name;
        public final float volume;
        public final float pitch;
        /** 随机权重；默认 1。想要「七成 A、三成 B」就配 7 和 3，不必凑成 100。 */
        public final int weight;

        public SoundRef(int delay, String name, float volume, float pitch) {
            this(delay, name, volume, pitch, 1);
        }

        public SoundRef(int delay, String name, float volume, float pitch, int weight) {
            this.delay = delay;
            this.name = name;
            this.volume = volume;
            this.pitch = pitch;
            this.weight = Math.max(1, weight);
        }

        /** 静音条目：抽中它这一格就不出声。 */
        public static SoundRef silent() {
            return new SoundRef(0, null, 1.0f, 1.0f, 1);
        }

        /** 设置权重。 */
        public SoundRef weighted(int value) {
            return new SoundRef(delay, name, volume, pitch, value);
        }

        public boolean isSilent() {
            return name == null || name.isEmpty();
        }
    }

    /**
     * 一个时间点上的音效编排 —— <b>序列与随机的组合单位</b>。
     *
     * <pre>
     * // 序列播四段，第三段从四个里随机抽一个（含「不出声」）
     * step.withSoundCue(SoundCue.play(0, "a"))
     *     .withSoundCue(SoundCue.play(6, "b"))
     *     .withSoundCue(SoundCue.pickOne(12, SoundRef.silent(), "c1", "c2", "c3"))
     *     .withSoundCue(SoundCue.play(18, "d"));
     * </pre>
     */
    public static final class SoundCue {

        /** 同一格里怎么播。 */
        public enum PickMode {
            /** 全部播（叠在一起，一般只放一条）。 */
            PLAY_ALL,
            /** 按权重抽一条；抽到静音条目就什么都不播。 */
            PICK_ONE
        }

        public final int delay;
        public final PickMode mode;
        public final List<SoundRef> variants;

        public SoundCue(int delay, PickMode mode, List<SoundRef> variants) {
            this.delay = delay;
            this.mode = mode == null ? PickMode.PLAY_ALL : mode;
            this.variants = variants == null ? List.of() : List.copyOf(variants);
        }

        /** 这一格固定播这些（可多条叠播）。 */
        public static SoundCue play(int delay, String... soundNames) {
            List<SoundRef> refs = new java.util.ArrayList<>(soundNames.length);
            for (String name : soundNames) {
                refs.add(new SoundRef(delay, name, 1.0f, 1.0f));
            }
            return new SoundCue(delay, PickMode.PLAY_ALL, refs);
        }

        /** 这一格按权重从候选里抽一条；候选里可以放 {@link SoundRef#silent()} 表示「不出声」。 */
        public static SoundCue pickOne(int delay, SoundRef... candidates) {
            return new SoundCue(delay, PickMode.PICK_ONE, List.of(candidates));
        }

        /** 这一格不播任何东西（占位用，方便对齐时序）。 */
        public static SoundCue silent(int delay) {
            return new SoundCue(delay, PickMode.PLAY_ALL, List.of());
        }

        public boolean isEmpty() {
            return variants.isEmpty();
        }
    }
}