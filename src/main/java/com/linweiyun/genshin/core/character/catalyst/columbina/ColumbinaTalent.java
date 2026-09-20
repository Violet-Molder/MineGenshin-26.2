package com.linweiyun.genshin.core.character.catalyst.columbina;

import com.linweiyun.genshin.content.skill_node.AreaEntityCollector;
import com.linweiyun.genshin.content.skill_node.TargetSeeker;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.character.talent.TalentBase;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.combat.action.ActionSet;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.enums.AttachmentType;
import com.linweiyun.genshin.enums.AttackType;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.List;

public class ColumbinaTalent extends TalentBase {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float[] COMBO_MULTIPLIERS = {0.4f, 0.45f, 0.6f};
    private static final float CHARGED_HP_RATIO = 0.03f;

    // ==================== 参数覆盖 ====================

    @Override
    public int getMaxCombo() { return 3; }

    // ──── 动作集 ────
    // 时序来自 CharacterActionData，当前角色尚未定义数据驱动配置，暂时沿用父类默认（空集）。
    // 战斗回调（attack / chargeAttack / elementalSkill / elementalBurst）保持不变，独立于动作系统存在。

    @Override
    protected ActionSet buildDefaultActionSet(PGCharacter character) {
        return super.buildDefaultActionSet(character);
    }

    @Override
    public void attack(Player player, PGCharacter character, int comboStage) {
        Level level = player.level();
        if (level.isClientSide()) return;

        int stage = comboStage % getMaxCombo();
        float multiplier = stage < COMBO_MULTIPLIERS.length ? COMBO_MULTIPLIERS[stage] : 1.0f;

        LivingEntity primaryTarget = new TargetSeeker(player, 10.0, TargetSeeker.TargetingType.LINE_OF_SIGHT).execute();

        if (primaryTarget == null) {
            return;
        }


        float aoeRange = stage == 2 ? 1.0f : 0.5f;
        Vec3 targetPos = primaryTarget.position();
        List<LivingEntity> targets = new AreaEntityCollector(level,
                targetPos.add(-aoeRange, -aoeRange, -aoeRange),
                targetPos.add(aoeRange, aoeRange, aoeRange),
                aoeRange).execute();

        for (LivingEntity target : targets) {
            if (target != player) {
                ModDamageSpec spec = ModDamageSpec.builder(AttackType.NORMAL_ATTACK, ModElements.ANEMO.get())
                        .multiplier(multiplier)
                        .elementAmount(AttachmentType.ULTRA_STRONG.getInitialAmount())
                        .attackerCharacter(character)
                        .build();
                ModDamageSource source = ModDamageSource.from(spec, player);
                if (target.level() instanceof ServerLevel serverLevel) {
                    target.hurtServer(serverLevel, source, 0f);
                }
            }
        }
    }

    @Override
    public void chargeAttack(Player player, PGCharacter character) {
        Level level = player.level();
        if (level.isClientSide()) return;

        LivingEntity primaryTarget = new TargetSeeker(player, 10.0, TargetSeeker.TargetingType.LINE_OF_SIGHT).execute();
        if (primaryTarget == null) {
            return;
        }

        float aoeRange = 1.5f;
        Vec3 center = primaryTarget.position();
        List<LivingEntity> targets = new AreaEntityCollector(level,
                center.add(-aoeRange, -aoeRange, -aoeRange),
                center.add(aoeRange, aoeRange, aoeRange),
                aoeRange).execute();


        for (LivingEntity target : targets) {
            if (target != player) {
                ModDamageSpec spec = ModDamageSpec.lunarDirectHp(CHARGED_HP_RATIO);
                ModDamageSource source = ModDamageSource.from(spec, player);
                if (target.level() instanceof ServerLevel serverLevel) {
                    target.hurtServer(serverLevel, source, 0f);
                }
            }
        }
    }
}