package com.linweiyun.genshin.core.character.sword.vesna;

import java.util.List;

import com.linweiyun.genshin.config.character.ShenheTalentConfig;
import com.linweiyun.genshin.content.entities.teyvat.skill.vesna.VesnaAttackProjectile;
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
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class VesnaTalent extends TalentBase {
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public int getMaxCombo() { return 5; }

    @Override
    public int getPrecastTicks(int stage)  { return 1; }
    @Override
    public int getActiveTicks(int stage)   { return 2; }
    @Override
    public int getPostcastTicks(int stage) { return stage == 4 ? 0 : 15; }

    @Override
    public int getSkillPrecastTicks()  { return 3; }
    @Override
    public int getSkillPostcastTicks() { return 10; }

    @Override
    public int getBurstPrecastTicks()  { return 10; }
    @Override
    public int getBurstPostcastTicks() { return 20; }

    @Override
    public ActionSet buildActionSet(PGCharacter character, String stateKey) {
        return buildDefaultActionSet(character);
    }

    @Override
    protected ActionSet buildDefaultActionSet(PGCharacter character) {
        return setBuilder()
                .normalCombo(
                        timing(1, 2, 15),
                        timing(1, 2, 15),
                        timing(1, 2, 15),
                        timing(1, 2, 15),
                        timing(1, 2, 0)
                )
                .charged(timing(5, 1, 15))
                .skillTap(timing(3, 1, 10))
                .skillHold(timing(3, 1, 10))
                .burst(timing(10, 1, 20))
                .build();
    }

    // ==================== 普攻 ====================

    @Override
    public void attack(Player player, PGCharacter character, int stage) {
        Level level = player.level();

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
            level.addFreshEntity(projectile);
            vesna.addEnergy(1);
        }
    }

    // ==================== 重击 ====================

    @Override
    public void chargeAttack(Player player, PGCharacter character) {
        Level level = player.level();
        if (level.isClientSide()) return;

        LivingEntity primaryTarget = new TargetSeeker(player, 10.0, TargetSeeker.TargetingType.LINE_OF_SIGHT).execute();
        if (primaryTarget == null) return;

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

    // ==================== E ====================
    // 状态切换 / 扣能量已经在 Vesna.applyElementalSkillCooldown 里做了
    // 这里只做伤害

    @Override
    public void elementalSkill(Player player, PGCharacter character, int skillTime) {
        Level level = player.level();
        if (level.isClientSide()) return;

        LivingEntity primaryTarget = new TargetSeeker(player, 10.0, TargetSeeker.TargetingType.LINE_OF_SIGHT).execute();
        if (primaryTarget == null) return;

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