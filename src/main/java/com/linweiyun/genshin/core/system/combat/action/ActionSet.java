package com.linweiyun.genshin.core.system.combat.action;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 一组动作。一个角色在某个状态下持有其中一份。
 * 用 {@link #deriveFrom(ActionSet)} 继承已有集合再覆盖部分动作。
 */
public class ActionSet {

    private final List<ActionDefinition> normalCombo;
    private final Map<ActionKind, ActionDefinition> singles;

    private ActionSet(Builder b) {
        this.normalCombo = List.copyOf(b.normalCombo);
        this.singles = new EnumMap<>(b.singles);
    }

    public ActionDefinition get(ActionKind kind, int comboIndex) {
        if (kind == ActionKind.NORMAL_ATTACK) return getNormalAttack(comboIndex);
        return singles.get(kind);
    }

    public ActionDefinition getNormalAttack(int comboIndex) {
        if (normalCombo.isEmpty()) return null;
        int idx = comboIndex % normalCombo.size();
        if (idx < 0) idx += normalCombo.size();
        return normalCombo.get(idx);
    }

    public int getNormalComboSize()                 { return normalCombo.size(); }
    public ActionDefinition getChargedAttack()      { return singles.get(ActionKind.CHARGED_ATTACK); }
    public ActionDefinition getElementalSkillTap()  { return singles.get(ActionKind.ELEMENTAL_SKILL_TAP); }
    public ActionDefinition getElementalSkillHold() { return singles.get(ActionKind.ELEMENTAL_SKILL_HOLD); }
    public ActionDefinition getElementalBurst()     { return singles.get(ActionKind.ELEMENTAL_BURST); }

    public static Builder builder() { return new Builder(); }

    /** 从已有集合派生出 builder，用于"继承 + 覆盖" */
    public static Builder deriveFrom(ActionSet parent) {
        Builder b = new Builder();
        b.normalCombo.addAll(parent.normalCombo);
        b.singles.putAll(parent.singles);
        return b;
    }

    public static final class Builder {
        private final List<ActionDefinition> normalCombo = new ArrayList<>();
        private final Map<ActionKind, ActionDefinition> singles = new EnumMap<>(ActionKind.class);

        public Builder addNormalAttack(ActionDefinition def) { normalCombo.add(def); return this; }
        public Builder clearNormalCombo()                     { normalCombo.clear(); return this; }

        public Builder set(ActionKind kind, ActionDefinition def) {
            if (kind == ActionKind.NORMAL_ATTACK) {
                throw new IllegalArgumentException("NORMAL_ATTACK 请用 addNormalAttack()");
            }
            singles.put(kind, def);
            return this;
        }

        public Builder chargedAttack(ActionDefinition def)      { return set(ActionKind.CHARGED_ATTACK, def); }
        public Builder elementalSkillTap(ActionDefinition def) { return set(ActionKind.ELEMENTAL_SKILL_TAP, def); }
        public Builder elementalSkillHold(ActionDefinition def){ return set(ActionKind.ELEMENTAL_SKILL_HOLD, def); }
        public Builder elementalBurst(ActionDefinition def)    { return set(ActionKind.ELEMENTAL_BURST, def); }

        public ActionSet build() { return new ActionSet(this); }
    }
}