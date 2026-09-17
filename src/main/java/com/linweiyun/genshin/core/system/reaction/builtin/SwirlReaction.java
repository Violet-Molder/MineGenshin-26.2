package com.linweiyun.genshin.core.system.reaction.builtin;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.genshin.core.system.combat.damage.DamageIndicatorFactory;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.core.system.reaction.ElementalReaction;
import com.linweiyun.genshin.core.system.reaction.ElementalReactionManager;
import com.linweiyun.genshin.core.system.reaction.ReactionContext;
import com.linweiyun.genshin.content.effect.character.CharacterEffectHelper;
import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.content.effect.character.ICharacterEffect;
import com.linweiyun.genshin.content.effect.character.impl.RadianceStellarSwirlEffect;
import com.linweiyun.genshin.core.character.IStellarSwirlParticipant;
import com.linweiyun.genshin.core.system.reaction.ReactionPriorityCalculator;
import com.linweiyun.genshin.core.system.registry.register.ModCharacterEffects;
import com.linweiyun.genshin.core.system.reaction.ReactionResult;
import com.linweiyun.genshin.core.system.registry.ModRegistries;
import com.linweiyun.genshin.enums.AttackType;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.linweiyun.genshin.content.entities.area.StellarVortexEntity;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.registry.register.ModEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.*;

public class SwirlReaction extends ElementalReaction {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final float WEAK_SWIRL_SPREAD = 2.2f;
    public static final float STRONG_SWIRL_SPREAD = 3.4f;

    private static final int SWIRL_COOLDOWN_TICKS = 20;
    private static final double SWIRL_RADIUS = 5.0;

    private static final Map<UUID, Long> lastSwirlTick = new HashMap<>();

    private static final String PYRO_ID = ModElements.PYRO.getId().toString();
    private static final String HYDRO_ID = ModElements.HYDRO.getId().toString();
    private static final String ELECTRO_ID = ModElements.ELECTRO.getId().toString();
    private static final String CYRO_ID = ModElements.CYRO.getId().toString();
    private static final String ANEMO_ID = ModElements.ANEMO.getId().toString();
    private static final String FROZEN_ID = ModElements.FROZEN.getId().toString();

    private static final Set<String> SWIRLABLE_IDS = Set.of(PYRO_ID, HYDRO_ID, ELECTRO_ID, CYRO_ID);
    private static final List<String> SPREAD_PRIORITY_IDS = List.of(PYRO_ID, HYDRO_ID, ELECTRO_ID, CYRO_ID);

    public SwirlReaction(ElementalReactionType type,
                         String elementAId, String elementBId,
                         float ratioA, float ratioB, int basePriority) {
        super(type, elementAId, elementBId, ratioA, ratioB, basePriority);
    }

    @Override
    public boolean canMatch(GenshinElement attackerElement, GenshinElement defenderElement) {
        GenshinElement attackerMain = attackerElement.getMainElement();
        GenshinElement defenderMain = defenderElement.getMainElement();
        if (attackerMain == null || defenderMain == null) return false;

        GenshinElement anemo = resolveElement(ANEMO_ID);
        if (attackerMain == anemo && isSwirlable(defenderMain)) return true;
        return false;
    }

    @Override
    public boolean isBlocked(ReactionContext context) {
        long gameTime = context.targetEntity().level().getGameTime();
        UUID targetId = context.targetEntity().getUUID();
        Long last = lastSwirlTick.get(targetId);
        if (last != null && gameTime - last < SWIRL_COOLDOWN_TICKS) {
            return true;
        }
        if (ReactionPriorityCalculator.hasFrozen(context.targetContainer())) {
            return !hasHiddenSwirlable(context.targetContainer());
        }
        return false;
    }

    @Override
    public ReactionResult execute(ReactionContext ctx) {
        GenshinElement anemoEl = resolveElement(ANEMO_ID);
        GenshinElement attackerMain = ctx.attackerElement().getMainElement();
        boolean attackerIsAnemo = (attackerMain == anemoEl);

        GenshinElement spreadElement;
        GenshinElement defenderTarget;

        if (attackerIsAnemo) {
            spreadElement = findSpreadElement(ctx.targetContainer());
            if (spreadElement == null) {
                return ReactionResult.builder(reactionType).build();
            }
            defenderTarget = spreadElement;
        } else {
            defenderTarget = anemoEl;
            spreadElement = attackerMain;
        }

        float totalDefenderUnit = sumConsumable(ctx.targetContainer(), defenderTarget);
        if (totalDefenderUnit <= 0f) {
            return ReactionResult.builder(reactionType).build();
        }

        float attackerQty = ctx.attackerUnit();
        float consumedPyroSide;
        float consumedAnemoSide;

        if (attackerIsAnemo) {
            float[] consumed = calculateConsumption(totalDefenderUnit, attackerQty);
            consumedPyroSide = consumed[0];
            consumedAnemoSide = consumed[1];
        } else {
            float[] consumed = calculateConsumption(attackerQty, totalDefenderUnit);
            consumedPyroSide = consumed[0];
            consumedAnemoSide = consumed[1];
        }
        if (consumedPyroSide <= 0f || consumedAnemoSide <= 0f) {
            return ReactionResult.builder(reactionType).build();
        }

        if (attackerIsAnemo) {
            consumeElementUnit(ctx.targetContainer(), defenderTarget, consumedPyroSide);
            consumeElementUnit(ctx.targetContainer(), anemoEl, consumedAnemoSide);
        } else {
            consumeElementUnit(ctx.targetContainer(), defenderTarget, consumedAnemoSide);
            consumeElementUnit(ctx.targetContainer(), spreadElement, consumedPyroSide);
        }

        lastSwirlTick.put(ctx.targetEntity().getUUID(), ctx.targetEntity().level().getGameTime());

        // 星扩散转化检测：扩冰 + 队伍有星扩散参与者
        if (attackerIsAnemo && spreadElement == ModElements.CYRO.get()
                && ctx.targetEntity().level() instanceof ServerLevel serverLevel
                && ReactionPriorityCalculator.hasStellarSwirlParticipant(serverLevel)) {
            handleStellarSwirl(ctx, serverLevel, spreadElement);
        } else {
            float spreadQuantity = calculateSpreadQuantity(consumedAnemoSide);
            applySwirlDamage(ctx, spreadElement, ctx.targetEntity());
            spreadToNearby(ctx, spreadElement, spreadQuantity, ctx.targetEntity());
        }

        return ReactionResult.builder(reactionType)
                .reacted()
                .consumedAttacker(attackerIsAnemo ? consumedAnemoSide : consumedPyroSide)
                .consumedDefender(attackerIsAnemo ? consumedPyroSide : consumedAnemoSide)
                .build();
    }

    private static GenshinElement resolveElement(String id) {
        if (PYRO_ID.equals(id)) return ModElements.PYRO.get();
        if (HYDRO_ID.equals(id)) return ModElements.HYDRO.get();
        if (ELECTRO_ID.equals(id)) return ModElements.ELECTRO.get();
        if (CYRO_ID.equals(id)) return ModElements.CYRO.get();
        if (ANEMO_ID.equals(id)) return ModElements.ANEMO.get();
        if (FROZEN_ID.equals(id)) return ModElements.FROZEN.get();
        return null;
    }

    private boolean isSwirlable(GenshinElement element) {
        Identifier key = ModRegistries.ELEMENT_REGISTRY.getKey(element);
        return key != null && SWIRLABLE_IDS.contains(key.toString());
    }

    private boolean hasHiddenSwirlable(StatusContainer container) {
        for (String id : SPREAD_PRIORITY_IDS) {
            GenshinElement elem = resolveElement(id);
            if (elem != null && ElectroChargedReaction.findElement(container, elem) != null) {
                return true;
            }
        }
        return false;
    }

    private GenshinElement findSpreadElement(StatusContainer container) {
        GenshinElement frozenEl = resolveElement(FROZEN_ID);
        ElementalAttachmentInstance frozenInst =
                ElectroChargedReaction.findElement(container, frozenEl);
        if (frozenInst != null) {
            for (String id : SPREAD_PRIORITY_IDS) {
                GenshinElement elem = resolveElement(id);
                if (elem == null) continue;
                ElementalAttachmentInstance hidden =
                        ElectroChargedReaction.findElement(container, elem);
                if (hidden != null) {
                    return elem;
                }
            }
            return resolveElement(CYRO_ID);
        }

        for (String id : SPREAD_PRIORITY_IDS) {
            GenshinElement elem = resolveElement(id);
            if (elem == null) continue;
            ElementalAttachmentInstance inst =
                    ElectroChargedReaction.findElement(container, elem);
            if (inst != null && inst.getUnit() > 0) {
                return elem;
            }
        }
        return null;
    }

    private float calculateSpreadQuantity(float consumedAnemoSide) {
        if (consumedAnemoSide >= 2.0f) return STRONG_SWIRL_SPREAD;
        return WEAK_SWIRL_SPREAD;
    }

    private void applySwirlDamage(ReactionContext ctx, GenshinElement spreadElement, LivingEntity target) {
        ModDamageSpec spec = ModDamageSpec.transformative(reactionType, spreadElement, AttackType.SWIRL);
        ModDamageSource source = ModDamageSource.from(spec, ctx.attackerEntity());
        target.hurt(source, 0f);
        DamageIndicatorFactory.reaction(target, reactionType);
    }

    private void spreadToNearby(ReactionContext ctx, GenshinElement spreadElement,
                                float spreadQuantity, LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) return;
        double rSq = SWIRL_RADIUS * SWIRL_RADIUS;

        ModDamageSpec dmgSpec = ModDamageSpec.transformative(reactionType, spreadElement, AttackType.SWIRL);
        AttachmentProfile spreadProfile = createSpreadProfile(spreadQuantity);

        for (LivingEntity nearby : level.getEntitiesOfClass(
                LivingEntity.class,
                target.getBoundingBox().inflate(SWIRL_RADIUS),
                e -> e != target && e.isAlive() && target.distanceToSqr(e) <= rSq)) {

            ModDamageSource dmgSource = ModDamageSource.from(dmgSpec, ctx.attackerEntity());
            nearby.hurt(dmgSource, 0f);
            DamageIndicatorFactory.reaction(nearby, reactionType);

            StatusContainer nearbyContainer = nearby.getData(AttachmentRegistration.CONTAINER);
            if (nearbyContainer != null) {
                ElementalAttachmentHelper.attach(
                        nearby, nearbyContainer, spreadElement,
                        AttachmentSource.SPECIAL, spreadProfile);

                ReactionContext spreadCtx = new ReactionContext(
                        spreadElement, spreadQuantity,
                        AttachmentSource.SPECIAL, spreadProfile,
                        dmgSpec, ctx.attackerEntity(),
                        nearbyContainer, nearby);
                ElementalReactionManager.tryReactAfterAttach(spreadCtx);
            }
        }
    }

    private static AttachmentProfile createSpreadProfile(float quantity) {
        float t = 7f + 2.5f * quantity;
        float v = quantity / t;
        return new AttachmentProfile(quantity, 1.0f, v, t);
    }

    private static final Map<UUID, Long> stellarSwirlCooldown = new HashMap<>();

    private void handleStellarSwirl(ReactionContext ctx, ServerLevel level, GenshinElement spreadElement) {
        Entity attacker = ctx.attackerEntity();
        if (!(attacker instanceof Player player)) return;

        long gameTime = level.getGameTime();
        UUID targetId = ctx.targetEntity().getUUID();
        Long lastSS = stellarSwirlCooldown.get(targetId);
        if (lastSS != null && gameTime - lastSS < SWIRL_COOLDOWN_TICKS) {
            return;
        }
        stellarSwirlCooldown.put(targetId, gameTime);

        if (ctx.targetEntity() instanceof LivingEntity livingTarget) {
            DamageIndicatorFactory.stellarWindReactionGradient(livingTarget,
                    ElementalReactionType.STELLAR_SWIRL_WIND);
        }

        double x = ctx.targetEntity().getX();
        double y = ctx.targetEntity().getY();
        double z = ctx.targetEntity().getZ();

        List<PGCharacter> stellarContributors = collectStellarParticipants(level);

        StellarVortexEntity existing = StellarVortexEntity.findExisting(level, x, y, z, 10.0f);

        if (existing != null) {
            existing.addAllContributors(stellarContributors);
            existing.incrementLevel();
            existing.triggerWindDamage();
            LOGGER.info("[星扩散] 已有风旋，等级+1 -> {} contributors={}",
                    existing.getVortexLevel(), stellarContributors.size());
        } else {
            StellarVortexEntity vortex = new StellarVortexEntity(ModEntities.STELLAR_VORTEX.get(), level);
            if (vortex == null) return;

            vortex.setPos(x, y, z);
            vortex.setLevel(1);
            level.addFreshEntity(vortex);
            vortex.addAllContributors(stellarContributors);
            vortex.triggerWindDamage();

            LOGGER.info("[星扩散] 创建新星辉风旋 at ({},{},{}) contributors={}",
                    x, y, z, stellarContributors.size());
        }

        applyRadianceToParticipants(level);
    }

    private List<PGCharacter> collectStellarParticipants(ServerLevel level) {
        List<PGCharacter> result = new ArrayList<>();
        for (Player p : level.players()) {
            PlayerCharactersAttachment att = p.getData(
                    AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            for (int i = 0; i < 4; i++) {
                PGCharacter character = att.getPartyCharacter(i);
                if (character instanceof IStellarSwirlParticipant) {
                    result.add(character);
                }
            }
        }
        return result;
    }

    private void applyRadianceToParticipants(ServerLevel level) {
        ICharacterEffect radianceEffect = ModCharacterEffects.RADIANCE_STELLAR_SWIRL_EFFECT.get();
        if (radianceEffect == null) return;

        int duration = RadianceStellarSwirlEffect.DURATION_TICKS;

        for (Player p : level.players()) {
            PlayerCharactersAttachment att = p.getData(
                    AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            for (int i = 0; i < 4; i++) {
                PGCharacter character = att.getPartyCharacter(i);
                if (character instanceof IStellarSwirlParticipant) {
                    CharacterEffectInstance instance = new CharacterEffectInstance(
                            radianceEffect, duration, 0, false);
                    CharacterEffectHelper.addEffect(p, character, instance);
                    LOGGER.info("[辉映-星扩散] 添加至 {} / {}", p.getName().getString(), character.getName());
                }
            }
        }
    }
}