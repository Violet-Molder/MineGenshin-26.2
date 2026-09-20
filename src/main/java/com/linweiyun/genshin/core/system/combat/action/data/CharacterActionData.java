package com.linweiyun.genshin.core.system.combat.action.data;

import java.util.Collections;
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
        public final List<SoundRef> sounds;      // 音效列表
        public final float skillCharge;          // 战技能量回复
        public final float finalCharge;          // 大招能量回复
        public final int cooldown;               // 冷却（刻），0 表示无冷却
        public final int comboWindow;            // 连击窗口（刻），后摇结束后可接下一段的窗口
        public String comboEndAnim = null;       // 最后一段连招结束后的收尾动画（仅最终段生效）

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

    public static final class SoundRef {
        public final int delay;
        public final String name;
        public final float volume;
        public final float pitch;

        public SoundRef(int delay, String name, float volume, float pitch) {
            this.delay = delay;
            this.name = name;
            this.volume = volume;
            this.pitch = pitch;
        }
    }
}