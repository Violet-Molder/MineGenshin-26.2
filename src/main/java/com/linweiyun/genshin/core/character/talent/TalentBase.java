package com.linweiyun.genshin.core.character.talent;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.combat.action.ActionDefinition;
import com.linweiyun.genshin.core.system.combat.action.ActionKind;
import com.linweiyun.genshin.core.system.combat.action.ActionSet;
import net.minecraft.world.entity.player.Player;

/**
 * 天赋基类。
 * <p>
 * 角色通过 {@link #buildActionSet(PGCharacter, String)} 按状态 key 构建动作集。
 * 默认状态返回 {@link #buildDefaultActionSet(PGCharacter)}，
 * 子类可 switch 出任意多套（例如 "burst"、"windrider"、"e_1".."e_5"）。
 */
public class TalentBase {

    // ==================== 普攻参数（子类可覆盖） ====================

    public int getMaxCombo() { return 1; }

    public int getPrecastTicks(int stage)  { return 5; }
    public int getActiveTicks(int stage)   { return 1; }
    public int getPostcastTicks(int stage) { return 6; }

    // ==================== 重击参数 ====================

    /** 长按蓄力到触发重击所需的 tick 数（客户端 KeyInputHandler 用 *50 换算成 ms） */
    public int getChargeTicks() { return 20; }

    public int getChargedPrecastTicks()  { return 5; }
    public int getChargedActiveTicks()   { return 1; }
    public int getChargedPostcastTicks() { return 15; }

    // ==================== 元素战技参数 ====================

    public int getSkillPrecastTicks()  { return 5; }
    public int getSkillActiveTicks()   { return 1; }
    public int getSkillPostcastTicks() { return 10; }

    // ==================== 元素爆发参数 ====================

    public int getBurstPrecastTicks()  { return 10; }
    public int getBurstActiveTicks()   { return 1; }
    public int getBurstPostcastTicks() { return 20; }

    // ==================== 逻辑钩子（子类覆盖做实际伤害） ====================

    public void attack(Player player, PGCharacter character, int comboStage) {}
    public void chargeAttack(Player player, PGCharacter character) {}
    public void elementalSkill(Player player, PGCharacter character, int skillTime) {}
    public void elementalBurst(Player player, PGCharacter character) {}

    // ==================== 动作集构建 ====================

    /**
     * 【核心扩展点】按状态 key 构建动作集。
     * 子类按 key switch 出多套；缺省只返回默认集。
     */
    public ActionSet buildActionSet(PGCharacter character, String stateKey) {
        return buildDefaultActionSet(character);
    }

    /**
     * 默认动作集 —— 覆盖全部输入通道。
     * 子类可 {@code ActionSet.deriveFrom(buildDefaultActionSet(c))} 再部分覆盖。
     */
    protected ActionSet buildDefaultActionSet(PGCharacter character) {
        ActionSet.Builder b = ActionSet.builder();

        int maxCombo = getMaxCombo();
        for (int i = 0; i < maxCombo; i++) {
            final int stage = i;
            b.addNormalAttack(
                    ActionDefinition.builder(ActionKind.NORMAL_ATTACK)
                            .comboIndex(i)
                            .precast(getPrecastTicks(i))
                            .active(getActiveTicks(i))
                            .postcast(getPostcastTicks(i))
                            // TODO: 动画 hook 待动画系统接入
                            .onActiveStart(ctx -> attack(ctx.player, ctx.character, stage))
                            .build()
            );
        }

        b.chargedAttack(
                ActionDefinition.builder(ActionKind.CHARGED_ATTACK)
                        .precast(getChargedPrecastTicks())
                        .active(getChargedActiveTicks())
                        .postcast(getChargedPostcastTicks())
                        .onActiveStart(ctx -> chargeAttack(ctx.player, ctx.character))
                        .build()
        );

        b.elementalSkillTap(
                ActionDefinition.builder(ActionKind.ELEMENTAL_SKILL_TAP)
                        .precast(getSkillPrecastTicks())
                        .active(getSkillActiveTicks())
                        .postcast(getSkillPostcastTicks())
                        .onActiveStart(ctx -> elementalSkill(ctx.player, ctx.character, 0))
                        .build()
        );

        b.elementalSkillHold(
                ActionDefinition.builder(ActionKind.ELEMENTAL_SKILL_HOLD)
                        .precast(getSkillPrecastTicks())
                        .active(getSkillActiveTicks())
                        .postcast(getSkillPostcastTicks())
                        .onActiveStart(ctx -> elementalSkill(ctx.player, ctx.character, 1000))
                        .build()
        );

        b.elementalBurst(
                ActionDefinition.builder(ActionKind.ELEMENTAL_BURST)
                        .precast(getBurstPrecastTicks())
                        .active(getBurstActiveTicks())
                        .postcast(getBurstPostcastTicks())
                        .onActiveStart(ctx -> elementalBurst(ctx.player, ctx.character))
                        .build()
        );

        return b.build();
    }
}