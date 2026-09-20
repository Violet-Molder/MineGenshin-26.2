package com.linweiyun.genshin.core.system.combat.action;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ActionSet {

    private final List<ActionDefinition> normalCombo;
    private final Map<ActionKind, ActionDefinition> singles;

    private ActionSet(Builder b) {
        this.normalCombo = List.copyOf(b.normalCombo);
        this.singles = new EnumMap<>(b.singles);
    }

    public ActionDefinition get(ActionKind kind, int stage) {
        if (kind == ActionKind.NORMAL_ATTACK) return getNormalAttack(stage);
        return singles.get(kind);
    }

    /** stage 从 1 开始（第 1 段 = 1，第 N 段 = N） */
    public ActionDefinition getNormalAttack(int stage) {
        if (normalCombo.isEmpty()) return null;
        int idx = (stage - 1) % normalCombo.size();
        if (idx < 0) idx += normalCombo.size();
        return normalCombo.get(idx);
    }

    public int getNormalComboSize()                 { return normalCombo.size(); }
    public ActionDefinition getChargedAttack()      { return singles.get(ActionKind.CHARGED_ATTACK); }
    public ActionDefinition getElementalSkillTap()  { return singles.get(ActionKind.ELEMENTAL_SKILL_TAP); }
    public ActionDefinition getElementalSkillHold() { return singles.get(ActionKind.ELEMENTAL_SKILL_HOLD); }
    public ActionDefinition getElementalBurst()     { return singles.get(ActionKind.ELEMENTAL_BURST); }
    public ActionDefinition getDodge()              { return singles.get(ActionKind.DODGE); }

    public static Builder builder() { return new Builder(); }

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
        public Builder dodge(ActionDefinition def)             { return set(ActionKind.DODGE, def); }

        public ActionSet build() { return new ActionSet(this); }
    }
}