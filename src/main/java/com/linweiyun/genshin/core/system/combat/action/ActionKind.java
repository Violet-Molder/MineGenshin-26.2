package com.linweiyun.genshin.core.system.combat.action;

/**
 * 输入类型。同一 ActionKind 在一个 ActionSet 里对应一个动作
 * （NORMAL_ATTACK 除外，是连招序列）。
 * <p>
 * 一个角色有几种前后摇模式 → 通过状态 key 表达，而不是扩展 ActionKind。
 */
public enum ActionKind {
    NORMAL_ATTACK,
    CHARGED_ATTACK,
    PLUNGING_ATTACK,
    ELEMENTAL_SKILL_TAP,
    ELEMENTAL_SKILL_HOLD,
    ELEMENTAL_BURST,
    SPECIAL;

    /** 是否参与连招（可在后摇期间缓冲下一段） */
    public boolean isCombo() {
        return this == ActionKind.NORMAL_ATTACK;
    }
}