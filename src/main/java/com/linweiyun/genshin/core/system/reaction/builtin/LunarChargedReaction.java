package com.linweiyun.genshin.core.system.reaction.builtin;

import com.linweiyun.genshin.content.entities.area.ThunderCloudEntity;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.genshin.core.system.combat.damage.DamageIndicatorFactory;
import com.linweiyun.genshin.core.system.reaction.ElementalReaction;
import com.linweiyun.genshin.core.system.reaction.ReactionContext;
import com.linweiyun.genshin.core.system.reaction.ReactionPriorityCalculator;
import com.linweiyun.genshin.core.system.reaction.ReactionResult;
import com.linweiyun.genshin.content.entities.ModEntities;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LunarChargedReaction extends ElementalReaction {
    public static final Logger LOGGER = LogUtils.getLogger();

    public LunarChargedReaction(ElementalReactionType type,
                                 String elementAId, String elementBId,
                                 float ratioA, float ratioB, int basePriority) {
        super(type, elementAId, elementBId, ratioA, ratioB, basePriority);
    }

    @Override
    public boolean isBlocked(ReactionContext context) {
        return !ReactionPriorityCalculator.hasColumbinaInParty(context)
                || ReactionPriorityCalculator.hasFrozen(context.targetContainer());
    }

    @Override
    public ReactionResult execute(ReactionContext ctx) {
        ElementalAttachmentInstance hydroInst = ElectroChargedReaction.findElement(
                ctx.targetContainer(), ModElements.HYDRO.get());
        ElementalAttachmentInstance electroInst = ElectroChargedReaction.findElement(
                ctx.targetContainer(), ModElements.ELECTRO.get());

        if (hydroInst == null || electroInst == null) {
            return ReactionResult.builder(reactionType).build();
        }

        if (hydroInst.getUnit() <= 0 || electroInst.getUnit() <= 0) {
            return ReactionResult.builder(reactionType).build();
        }

        LivingEntity target = ctx.targetEntity();
        if (!(target.level() instanceof ServerLevel level)) {
            return ReactionResult.builder(reactionType).build();
        }

        // 从目标实体的 StatusContainer 获取所有活跃的水/雷附着角色
        long gameTime = level.getGameTime();
        Set<PGCharacter> activeContributors = ctx.targetContainer().getActiveContributors(
                gameTime, ModElements.HYDRO.get(), ModElements.ELECTRO.get());
        List<PGCharacter> contributorList = new ArrayList<>(activeContributors);

        ThunderCloudEntity existingCloud = findExistingCloud(target);

        if (existingCloud != null) {
            existingCloud.addContributors(contributorList);
            existingCloud.refreshDuration();
            LOGGER.info("[月感电] 刷新雷暴云 duration={} | contributors={}",
                    existingCloud.getDuration(), contributorList.size());
        } else {
            ThunderCloudEntity cloud = new ThunderCloudEntity(
                    ModEntities.THUNDER_CLOUD.get(), level);
            cloud.setPos(target.getX(), target.getEyeY() + 2.0, target.getZ());
            cloud.setInitialContributors(contributorList);
            level.addFreshEntity(cloud);

            cloud.dealLunarDamage(target, level);

            LOGGER.info("[月感电] 生成雷暴云 pos={},{},{} | contributors={}",
                    target.getX(), target.getEyeY() + 2.0, target.getZ(), contributorList.size());
        }

        DamageIndicatorFactory.lunarReactionGradient(target, ElementalReactionType.LUNAR_CHARGED);

        return ReactionResult.builder(reactionType).reacted()
                .consumedAttacker(ctx.attackerUnit()).consumedDefender(0).build();
    }

    private ThunderCloudEntity findExistingCloud(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) return null;

        double r = 8.0;
        AABB searchBox = new AABB(
                target.getX() - r, target.getY() - 8, target.getZ() - r,
                target.getX() + r, target.getY() + 8, target.getZ() + r);

        List<ThunderCloudEntity> clouds = level.getEntitiesOfClass(
                ThunderCloudEntity.class, searchBox, e -> e.isAlive()
                        && e.getAreaOfInfluence().contains(target.getX(), target.getY(), target.getZ()));
        return clouds.isEmpty() ? null : clouds.get(0);
    }
}