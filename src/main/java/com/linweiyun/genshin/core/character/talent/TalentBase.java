package com.linweiyun.genshin.core.character.talent;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.combat.action.ActionDefinition;
import com.linweiyun.genshin.core.system.combat.action.ActionKind;
import com.linweiyun.genshin.core.system.combat.action.ActionSet;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData.ActionStep;
import net.minecraft.world.entity.player.Player;

/**
 * 天赋基类。
 * <p>
 * 时序由 {@link CharacterActionData.ActionStep} 提供（唯一来源），不再自行定义前摇/执行/后摇。
 * 子类只需覆盖回调方法和 maxCombo。
 */
public class TalentBase {

    public int getMaxCombo() { return 1; }

    public int getChargeTicks() { return 20; }

    // ==================== 回调（子类覆盖实现具体伤害/效果逻辑） ====================

    public void attack(Player player, PGCharacter character, int comboStage) {}
    public void chargeAttack(Player player, PGCharacter character) {}
    public void elementalSkill(Player player, PGCharacter character, int skillTime) {}
    public void elementalBurst(Player player, PGCharacter character) {}

    // ==================== ActionSet 构建 ====================

    public ActionSet buildActionSet(PGCharacter character, String stateKey) {
        return buildDefaultActionSet(character);
    }

    protected ActionSet buildDefaultActionSet(PGCharacter character) {
        int maxCombo = getMaxCombo();
        CharacterActionData actionData = character.getActionData();

        SetBuilder sb = setBuilder();
        if (actionData != null) {
            CharacterActionData.ComboData combo = actionData.combo();
            if (combo != null) {
                int steps = Math.min(maxCombo, combo.maxCombo());
                for (int stage = 1; stage <= steps; stage++) {
                    ActionStep step = combo.getStep(stage);
                    if (step != null) {
                        final int s = stage;
                        sb.addNormalAttack(
                                ActionDefinition.builder(ActionKind.NORMAL_ATTACK)
                                        .comboIndex(s)
                                        .step(step)
                                        .onActiveStart(ctx -> attack(ctx.player, ctx.character, s))
                                        .build()
                        );
                    }
                }
            }

            CharacterActionData.SkillData skill = actionData.skill();
            if (skill != null) {
                if (skill.tap() != null) {
                    sb.addSkillTap(
                            ActionDefinition.builder(ActionKind.ELEMENTAL_SKILL_TAP)
                                    .step(skill.tap())
                                    .onActiveStart(ctx -> elementalSkill(ctx.player, ctx.character, 0))
                                    .build()
                    );
                }
                if (skill.hold() != null) {
                    sb.addSkillHold(
                            ActionDefinition.builder(ActionKind.ELEMENTAL_SKILL_HOLD)
                                    .step(skill.hold())
                                    .onActiveStart(ctx -> elementalSkill(ctx.player, ctx.character, 1000))
                                    .build()
                    );
                }
            }

            CharacterActionData.BurstData burst = actionData.burst();
            if (burst != null && burst.step() != null) {
                sb.addBurst(
                        ActionDefinition.builder(ActionKind.ELEMENTAL_BURST)
                                .step(burst.step())
                                .onActiveStart(ctx -> elementalBurst(ctx.player, ctx.character))
                                .build()
                );
            }
        }

        // 兜底：重击用基础步（子类可覆盖 buildActionSet 自定义）
        if (actionData != null && actionData.skill() != null && actionData.skill().tap() != null) {
            sb.addCharged(
                    ActionDefinition.builder(ActionKind.CHARGED_ATTACK)
                            .step(actionData.skill().tap())
                            .onActiveStart(ctx -> chargeAttack(ctx.player, ctx.character))
                            .build()
            );
        }

        return sb.build();
    }

    // ==================== 构建器 ====================

    protected SetBuilder setBuilder() { return new SetBuilder(null); }
    protected SetBuilder setBuilder(ActionSet parent) { return new SetBuilder(parent); }

    public final class SetBuilder {
        private final ActionSet.Builder inner;

        private SetBuilder(ActionSet parent) {
            this.inner = (parent != null) ? ActionSet.deriveFrom(parent) : ActionSet.builder();
        }

        public SetBuilder addNormalAttack(ActionDefinition def) { inner.addNormalAttack(def); return this; }
        public SetBuilder addCharged(ActionDefinition def)      { inner.chargedAttack(def); return this; }
        public SetBuilder addSkillTap(ActionDefinition def)     { inner.elementalSkillTap(def); return this; }
        public SetBuilder addSkillHold(ActionDefinition def)    { inner.elementalSkillHold(def); return this; }
        public SetBuilder addBurst(ActionDefinition def)        { inner.elementalBurst(def); return this; }
        public SetBuilder clearNormalCombo()                    { inner.clearNormalCombo(); return this; }

        public ActionSet build() { return inner.build(); }
    }
}