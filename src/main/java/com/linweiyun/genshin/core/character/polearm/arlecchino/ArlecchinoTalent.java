package com.linweiyun.genshin.core.character.polearm.arlecchino;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.character.talent.TalentBase;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.combat.action.ActionDefinition;
import com.linweiyun.genshin.core.system.combat.action.ActionKind;
import com.linweiyun.genshin.core.system.combat.action.ActionSet;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.enums.AttachmentType;
import com.linweiyun.genshin.enums.AttackType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ArlecchinoTalent extends TalentBase {

    private static final float[] COMBO_MULTIPLIERS = {0.41f, 0.42f, 0.55f, 0.35f, 0.68f};

    // ==================== 参数覆盖 ====================

    @Override
    public int getMaxCombo() { return 5; }

    @Override
    public int getPrecastTicks(int stage)  { return 1; }
    @Override
    public int getActiveTicks(int stage)   { return 1; }
    @Override
    public int getPostcastTicks(int stage) { return 15; }

    // ==================== 动作集 ====================

    @Override
    protected ActionSet buildDefaultActionSet(PGCharacter character) {
        ActionSet.Builder b = ActionSet.builder();

        for (int i = 1; i <= getMaxCombo(); i++) {
            final int stage = i;
            b.addNormalAttack(
                    ActionDefinition.builder(ActionKind.NORMAL_ATTACK)
                            .comboIndex(i)
                            .precast(getPrecastTicks(i))
                            .active(getActiveTicks(i))
                            .postcast(getPostcastTicks(i))
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

    // ==================== 普攻 ====================

    @Override
    public void attack(Player player, PGCharacter character, int comboStage) {
        Level level = player.level();
        if (level.isClientSide()) return;

        int stage = comboStage % getMaxCombo();
        float multiplier = stage < COMBO_MULTIPLIERS.length ? COMBO_MULTIPLIERS[stage] : 1.0f;

        ModDamageSpec spec = ModDamageSpec.builder(AttackType.NORMAL_ATTACK, ModElements.PYRO.get())
                .multiplier(multiplier)
                .elementAmount(AttachmentType.WEAK.getInitialAmount())
                .attackerCharacter(character)
                .build();
        ModDamageSource source = ModDamageSource.from(spec, player);
        if (player.level() instanceof ServerLevel serverLevel) {
            player.hurtServer(serverLevel, source, 0f);
        }
    }
}