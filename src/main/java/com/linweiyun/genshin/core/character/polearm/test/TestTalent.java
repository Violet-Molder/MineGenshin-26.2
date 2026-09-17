package com.linweiyun.genshin.core.character.polearm.test;

import com.linweiyun.genshin.config.character.ShenheTalentConfig;
import com.linweiyun.genshin.content.skill_node.AreaEntityCollector;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.character.talent.TalentBase;
import com.linweiyun.genshin.core.element.ModElements;
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

public class TestTalent extends TalentBase {
    public static final Logger LOGGER = LogUtils.getLogger();

    public TestTalent() {
        super(5,
                List.of(1, 1, 1, 1, 1),
                List.of(2, 2, 2, 2, 2),
                List.of(15, 15, 15, 15, 0));
    }

    @Override
    public void attack(Player player, PGCharacter character, int comboStage) {
        LOGGER.info("TestTalent attack");
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
                ModDamageSpec spec = ModDamageSpec.builder(AttackType.NORMAL_ATTACK, ModElements.HYDRO.get())
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
    }

    @Override
    public void chargeAttack(Player player, PGCharacter character) {
    }

    @Override
    public void elementalSkill(Player player, PGCharacter character, int skillType) {
    }
}