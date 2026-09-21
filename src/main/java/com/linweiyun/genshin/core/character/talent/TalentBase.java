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
 *
 * <h2>「这一招能不能放」写在别处</h2>
 * 出手的前置判断（能量、CD、姿态…）不在这里，而在
 * {@link PGCharacter#canCast(net.minecraft.world.entity.player.Player, ActionKind, int)} ——
 * 因为<b>客户端也要问一次</b>（不通过就不播动画），而天赋实例在客户端是 {@code null}
 * （反序列化不走子类构造器，{@code talent} 字段是 transient）。
 * 所以凡是<b>双端都需要的条件</b>都放角色那边；天赋只负责「放出来之后干什么」。
 */
public class TalentBase {

    public int getMaxCombo() { return 1; }

    public int getChargeTicks() { return 20; }

    // ==================== 回调（子类覆盖实现具体伤害/效果逻辑） ====================

    public void attack(Player player, PGCharacter character, int comboStage) {}
    public void chargeAttack(Player player, PGCharacter character) {}
    public void elementalSkill(Player player, PGCharacter character, int skillTime) {}
    public void elementalBurst(Player player, PGCharacter character) {}
    public void dodge(Player player, PGCharacter character) {}

    /**
     * 招式<b>触发那一刻</b>（动作刚开始，早于任何伤害点）调用一次。
     *
     * <h2>和 {@link #attack}/{@link #elementalSkill} 的分工</h2>
     * 那几个回调跑在 {@code hits[].delay} 的伤害点上 —— 它们是「效果在第几帧出」。
     * 这个方法跑在 tick 0，是「这一招<b>已经放出来了</b>」。
     *
     * <p>凡是<b>触发即生效</b>的东西都放这里：换姿态、开模式、扣子弹、进入某个状态。
     * 放在伤害点上的话，前摇/准备阶段被打断就会出现
     * 「CD 已经在转、资源已经扣了，但模式没进去、效果没出来」。
     *
     * <p>服务端触发（客户端那一帧只是开始播动画 + 发请求）。
     *
     * @param kind 这一招是什么（普攻/重击/战技/大招/闪避）
     */
    public void onCastStart(Player player, PGCharacter character, ActionKind kind) {}

    // ==================== ActionSet 构建 ====================

    public ActionSet buildActionSet(PGCharacter character, String stateKey) {
        return buildDefaultActionSet(character);
    }

    protected ActionSet buildDefaultActionSet(PGCharacter character) {
        int maxCombo = getMaxCombo();
        CharacterActionData actionData = character.getActionData();

        // 没有专属 ACTION_DATA 的角色（大部分角色现在还只有壳）走一套通用兜底，
        // 否则 buildActionSet 会返回空集，普攻/战技/大招全部静默失效。
        if (actionData == null) {
            actionData = CharacterActionData.fallback(maxCombo);
        }

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
                                        .onCastStart(ctx -> onCastStart(ctx.player, ctx.character,
                                                ActionKind.NORMAL_ATTACK))
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
                                    .onCastStart(ctx -> onCastStart(ctx.player, ctx.character,
                                            ActionKind.ELEMENTAL_SKILL_TAP))
                                    .onActiveStart(ctx -> elementalSkill(ctx.player, ctx.character, 0))
                                    .build()
                    );
                }
                if (skill.hold() != null) {
                    sb.addSkillHold(
                            ActionDefinition.builder(ActionKind.ELEMENTAL_SKILL_HOLD)
                                    .step(skill.hold())
                                    .onCastStart(ctx -> onCastStart(ctx.player, ctx.character,
                                            ActionKind.ELEMENTAL_SKILL_HOLD))
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
                                .onCastStart(ctx -> onCastStart(ctx.player, ctx.character,
                                        ActionKind.ELEMENTAL_BURST))
                                .onActiveStart(ctx -> elementalBurst(ctx.player, ctx.character))
                                .build()
                );
            }

            CharacterActionData.DodgeData dodgeData = actionData.dodge();
            if (dodgeData != null && dodgeData.step() != null) {
                sb.addDodge(
                        ActionDefinition.builder(ActionKind.DODGE)
                                .step(dodgeData.step())
                                .onCastStart(ctx -> onCastStart(ctx.player, ctx.character,
                                        ActionKind.DODGE))
                                .onActiveStart(ctx -> dodge(ctx.player, ctx.character))
                                .build()
                );
            }
        }

        // 兜底：重击用基础步（子类可覆盖 buildActionSet 自定义）
        if (actionData != null && actionData.skill() != null && actionData.skill().tap() != null) {
            sb.addCharged(
                    ActionDefinition.builder(ActionKind.CHARGED_ATTACK)
                            .step(actionData.skill().tap())
                            .onCastStart(ctx -> onCastStart(ctx.player, ctx.character,
                                    ActionKind.CHARGED_ATTACK))
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
        public SetBuilder addDodge(ActionDefinition def)        { inner.dodge(def); return this; }
        public SetBuilder clearNormalCombo()                    { inner.clearNormalCombo(); return this; }

        public ActionSet build() { return inner.build(); }
    }
}