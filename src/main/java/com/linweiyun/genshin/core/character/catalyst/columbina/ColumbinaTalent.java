package com.linweiyun.genshin.core.character.catalyst.columbina;

import com.linweiyun.genshin.content.skill_node.AreaEntityCollector;
import com.linweiyun.genshin.content.skill_node.TargetSeeker;
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

public class ColumbinaTalent extends TalentBase {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float[] COMBO_MULTIPLIERS = {0.4f, 0.45f, 0.6f};

    public ColumbinaTalent() {
        super(3,
                List.of(0, 0, 0),
                List.of(0, 0, 0),
                List.of(15, 15, 0));
    }

    public void attack(Player player, PGCharacter character, int comboStage) {
        Level level = player.level();
        if (level.isClientSide()) return;

        int stage = comboStage % this.maxCombo;
        float multiplier = stage < COMBO_MULTIPLIERS.length ? COMBO_MULTIPLIERS[stage] : 1.0f;

        LivingEntity primaryTarget = new TargetSeeker(player, 10.0, TargetSeeker.TargetingType.LINE_OF_SIGHT).execute();

        if (primaryTarget == null) {
            LOGGER.info("[哥伦比娅] 索敌未发现目标");
            return;
        }

        LOGGER.info("[哥伦比娅] 第{}段 索敌锁定: {} ({})", stage + 1,
                primaryTarget.getName().getString(),
                primaryTarget.position().toString());

        float aoeRange = stage == 2 ? 1.0f : 0.5f;
        Vec3 targetPos = primaryTarget.position();
        List<LivingEntity> targets = new AreaEntityCollector(level,
                targetPos.add(-aoeRange, -aoeRange, -aoeRange),
                targetPos.add(aoeRange, aoeRange, aoeRange),
                aoeRange).execute();

        LOGGER.info("[哥伦比娅] AoE范围{}格, 命中{}个目标: {}",
                aoeRange, targets.size(),
                targets.stream().map(e -> e.getName().getString()).toList());

        for (LivingEntity target : targets) {
            if (target != player) {
                ModDamageSpec spec = ModDamageSpec.builder(AttackType.NORMAL_ATTACK, ModElements.HYDRO.get())
                        .multiplier(multiplier)
                        .elementAmount(AttachmentType.WEAK.getInitialAmount())
                        .attackerCharacter(character)
                        .build();
                ModDamageSource source = ModDamageSource.from(spec, player);
                if (target.level() instanceof ServerLevel serverLevel) {
                    target.hurtServer(serverLevel, source, 0f);
                }
            }
        }
    }
}