package com.linweiyun.genshin.core.character.sword.vesna;

import java.util.List;

import com.linweiyun.genshin.config.character.ShenheTalentConfig;
import com.linweiyun.genshin.content.entities.teyvat.skill.vesna.VesnaAttackProjectile;
import com.linweiyun.genshin.content.skill_node.AreaEntityCollector;
import com.linweiyun.genshin.content.skill_node.TargetSeeker;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.character.talent.TalentBase;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.enums.AttachmentType;
import com.linweiyun.genshin.enums.AttackType;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import static com.linweiyun.genshin.core.character.sword.vesna.Vesna.VESNA_ENERGY;

public class VesnaTalent extends TalentBase {
    public static final Logger LOGGER = LogUtils.getLogger();


    public VesnaTalent() {
        super(5,
                List.of(1, 1, 1, 1, 1),
                List.of(2, 2, 2, 2, 2),
                List.of(15, 15, 15, 15, 0));
    }

    @Override
    public void attack(Player player, PGCharacter character, int comboStage) {
        Level level = player.level();
        if (level.isClientSide()) return;

        int stage = comboStage % this.maxCombo;
        int naLevel = Math.max(1, character.getData().getNormalAttackLevel());

        float multiplier = (float) (
                ShenheTalentConfig.getNABase(stage)
                        + ShenheTalentConfig.getNAPerLevel(stage) * (naLevel - 1)
        );

        Vec3 startPos = player.position();
        Vec3 lookDir = player.getLookAngle();
        Vec3 endPos = startPos.add(lookDir.scale(2.5f));

        List<LivingEntity> targets = new AreaEntityCollector(level, startPos, endPos, 1.0f).execute();

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
                    if (stage == 4) {
                        target.hurtServer(serverLevel, source, 0f);
                    }
                }
            }
        }
        Vesna vesna = (Vesna) character;
        VesnaAttackProjectile projectile = VesnaAttackProjectile.create(level, vesna, player.position());
        if (projectile != null) {
            LOGGER.info(String.valueOf(level.addFreshEntity(projectile)));
            ;
        }

    }

    @Override
    public void chargeAttack(Player player, PGCharacter character) {
        Level level = player.level();
        if (level.isClientSide()) return;

        LivingEntity primaryTarget = new TargetSeeker(player, 10.0, TargetSeeker.TargetingType.LINE_OF_SIGHT).execute();
        if (primaryTarget == null) {
            LOGGER.info("[Test重击] 索敌未发现目标");
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
                ModDamageSpec spec = ModDamageSpec.stellarDirect(
                        ElementalReactionType.STELLAR_SWIRL_ICE, ModElements.CYRO.get(), 1.0f,
                        0.5f);
                spec.setStellarContributors(List.of(character));
                ModDamageSource source = ModDamageSource.from(spec, player);
                target.hurtServer((ServerLevel) level, source, 0f);
            }
        }
    }

    @Override
    public void elementalSkill(Player player, PGCharacter character, int skillType) {
        Level level = player.level();
        if (level.isClientSide()) return;

        LivingEntity primaryTarget = new TargetSeeker(player, 10.0, TargetSeeker.TargetingType.LINE_OF_SIGHT).execute();
        if (primaryTarget == null) {
            LOGGER.info("[Test战技] 索敌未发现目标");
            return;
        }

        float aoeRange = 2.0f;
        Vec3 center = primaryTarget.position();
        List<LivingEntity> targets = new AreaEntityCollector(level,
                center.add(-aoeRange, -aoeRange, -aoeRange),
                center.add(aoeRange, aoeRange, aoeRange),
                aoeRange).execute();

        for (LivingEntity target : targets) {
            if (target != player) {
                ModDamageSpec spec = ModDamageSpec.stellarDirect(
                        ElementalReactionType.STELLAR_SWIRL_WIND, ModElements.ANEMO.get(), 1.0f,
                        0.5f);
                spec.setStellarContributors(List.of(character));
                ModDamageSource source = ModDamageSource.from(spec, player);
                target.hurtServer((ServerLevel) level, source, 0f);
            }
        }
    }
}