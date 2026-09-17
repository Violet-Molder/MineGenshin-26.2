package com.linweiyun.genshin.core.system.combat.attack;

import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.config.reaction.ReactionConfig;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.combat.damage.*;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.status.StatusAccessor;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.genshin.core.system.combat.decay.DecayCounterManager;
import com.linweiyun.genshin.core.system.combat.decay.DecayResult;
import com.linweiyun.genshin.core.system.combat.decay.IDecayCounterHolder;
import com.linweiyun.genshin.core.system.reaction.ElementalReactionManager;
import com.linweiyun.genshin.core.system.reaction.ReactionContext;
import com.linweiyun.genshin.core.system.reaction.ReactionResult;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class HurtEntityHelper {
    public static final Logger LOGGER = LogUtils.getLogger();

    // ============================================================
    // 统一伤害入口 —— 所有伤害都从这里进，按 DamageType 分发
    // ============================================================

    public static float calculateFinalModDamage(ModDamageSource damageSource,
                                                PGCharacter attacker,
                                                LivingEntity target) {
        ModDamageSpec spec = damageSource.getSpec();

        // 非直伤 → 剧变/激化/月曜/星烁 独立管线
        if (spec.getDamageType() != ModDamageSpec.DamageType.DIRECT) {
            LivingEntity sourceEntity = (LivingEntity) damageSource.getEntity();
            float damage = switch (spec.getDamageType()) {
                case TRANSFORMATIVE -> calculateTransformativeDamage(
                        sourceEntity, target, spec.getTransformativeReactionType(), spec.getElement());
                case LUNAR -> calculateLunarDirectDamage(
                        sourceEntity, attacker, target, spec);
                case QUICKEN -> 0f;
                case STELLAR -> calculateStellarDirectDamage(attacker, target, spec);
                default -> 0f;
            };
            //            LOGGER.info("[{}管线] reaction={} | final={}",
//                    spec.getDamageType(), spec.getTransformativeReactionType(), damage);
            return damage;
        }

        // 直伤 → 角色属性 → 衰减/附着/反应 → 直伤计算
        if (attacker != null) {
            for (CharacterEffectInstance effect : new ArrayList<>(attacker.getData().getEffectContainer().getEffects())) {
                if (effect != null) {
                    Objects.requireNonNull(effect.getEffect()).onAttacked(
                            damageSource.getEntity() instanceof Player p ? p : null,
                            attacker, target, effect, damageSource);
                }
            }
        }
        spec = damageSource.getSpec();

        float baseDamage = attacker != null
                ? calculateCharacterDamage(spec, attacker)
                : 0.0f;

        return processPipeline(damageSource, attacker, target, baseDamage);
    }

    // ============================================================
    // 公用工具
    // ============================================================

    private static PGCharacter resolveCharacter(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT).getCurrentCharacter();
        }
        return null;
    }

    private static AttachmentProfile chooseProfile(float elementAmount) {
        if (elementAmount >= 4.0f) return AttachmentProfile.ULTRA_STRONG;
        if (elementAmount >= 2.0f) return AttachmentProfile.STRONG;
        if (elementAmount >= 1.5f) return AttachmentProfile.MEDIUM;
        return AttachmentProfile.WEAK;
    }

    // ============================================================
    // 直伤管线 — 入口 → 衰减/附着/反应 → 基础伤害区 + 暴击区 + 增伤区 + 防御区 + 抗性区 + 反应乘区
    // ============================================================

    private static float processPipeline(ModDamageSource damageSource, PGCharacter attacker,
                                         LivingEntity target, float baseDamage) {
        ModDamageSpec spec = damageSource.getSpec();
        LivingEntity sourceEntity = (LivingEntity) damageSource.getEntity();
        PGCharacter targetCharacter = resolveCharacter(target);

        DecayResult decayResult = DecayResult.NONE;
        if (spec.hasDecayTag()) {
            IDecayCounterHolder holder = (IDecayCounterHolder) target;
            DecayCounterManager manager = holder.getDecayCounterManager();
            long currentTick = target.level().getGameTime();
            manager.getOrCreateCounter(sourceEntity, attacker, spec, currentTick);
            decayResult = manager.processHit(sourceEntity, attacker, spec, currentTick);
        }
        float elementCoefficient = decayResult.getElementCoefficient();

        AttachmentProfile profile = chooseProfile(spec.getElementAmount());
        if (spec.getElement().isInstant()) {
            profile = new AttachmentProfile(
                    profile.getBaseQuantity(), profile.getLossMultiplier(),
                    1.0f, 0.5f);
        }
        boolean canAttach = spec.hasAuraPotential() && elementCoefficient > 0;
        if (canAttach && spec.getElement().isInstant()) {
            StatusContainer checkContainer = target.getData(AttachmentRegistration.CONTAINER);
            if (checkContainer == null
                    || !ElementalReactionManager.canElementReact(spec.getElement(), checkContainer)) {
                canAttach = false;
            }
        }
        if (canAttach) {
            ElementalAttachmentHelper.attach(target,
                    StatusAccessor.of(target), spec.getElement(),
                    AttachmentSource.NORMAL_ATTACK, profile);

            GenshinElement elem = spec.getElement();
            if (attacker != null && sourceEntity != null
                    && (elem == ModElements.HYDRO.get() || elem == ModElements.ELECTRO.get())) {
                String key = sourceEntity.getUUID() + "::" + attacker.getClass().getSimpleName();
                long decayTicks = (long) (profile.getDurationSeconds() * 20);
                StatusContainer container = target.getData(AttachmentRegistration.CONTAINER);
                if (container != null) {
                    container.recordLunarContributor(key, target.level().getGameTime(), decayTicks);
                }
            }
        }

        StatusContainer container = null;
        ReactionResult reactionResult = null;
        if (canAttach) {
            container = target.getData(AttachmentRegistration.CONTAINER);
            ReactionContext ctx = new ReactionContext(
                    spec.getElement(), spec.getElementAmount() * elementCoefficient,
                    AttachmentSource.NORMAL_ATTACK, profile, spec,
                    damageSource.getEntity(), container, target);
            reactionResult = ElementalReactionManager.tryReactAfterAttach(ctx);
        }

        if (reactionResult != null && reactionResult.isReacted()
                && reactionResult.getReactionType() != null) {
            ElementalReactionType rt = reactionResult.getReactionType();
            if (rt != ElementalReactionType.LUNAR_CHARGED
                    && rt != ElementalReactionType.STELLAR_SWIRL_WIND
                    && rt != ElementalReactionType.STELLAR_SWIRL_ICE) {
                DamageIndicatorFactory.reaction(target, rt);
            }
        }

        float finalDamage = calculateFinalDamage(
                baseDamage, spec, sourceEntity, attacker, target, targetCharacter, reactionResult);

        //        LOGGER.info("[最终伤害] base={} → final={}", baseDamage, finalDamage);
        finalDamage *= decayResult.getDamageCoefficient();
        return finalDamage;
    }

    private static float calculateCharacterDamage(ModDamageSpec spec, PGCharacter attacker) {
        var data = attacker.getData();

        double atk = data.getAttributeTotalValue(ModAttributes.ATK.value());
        double hp  = data.getAttributeTotalValue(ModAttributes.MAX_HP.value());
        double def = data.getAttributeTotalValue(ModAttributes.DEF.value());
        double em  = data.getAttributeTotalValue(ModAttributes.ELEMENTAL_MASTERY.value());

        //        LOGGER.info("[属性快照] ATK={} | HP={} | DEF={} | EM={}", atk, hp, def, em);
//        LOGGER.info("[倍率快照] atkMult={} | hpMult={} | defMult={} | emMult={} | skillMultBonus={} | flatBonus={}",
//                spec.getAtkMultiplier(), spec.getHpMultiplier(), spec.getDefMultiplier(), spec.getEmMultiplier(),
//                spec.getSkillMultiplierBonus(), spec.getFlatDamageBonus());

        float base = (float) ((
                atk * spec.getAtkMultiplier() +
                        hp  * spec.getHpMultiplier() +
                        def * spec.getDefMultiplier() +
                        em  * spec.getEmMultiplier()
        ) * (1 + spec.getSkillMultiplierBonus()) + spec.getFlatDamageBonus());
        return base;
    }

    private static float calculateFinalDamage(float baseDamage, ModDamageSpec spec,
                                              LivingEntity attacker, PGCharacter attackerCharacter,
                                              LivingEntity target, PGCharacter targetCharacter,
                                              ReactionResult reaction) {
        float crit = critZone(attackerCharacter, spec);
        float bonus = dmgBonusZone(attackerCharacter, spec);
        float def = defenseZone(attacker, attackerCharacter, target, targetCharacter);
        float res = resistanceZone(spec.getElement(), target, targetCharacter);
        //        LOGGER.info("[最终伤害区] crit={} | bonus={} | def={} | res={}", crit, bonus, def, res);

        if (reaction == null || !reaction.isReacted()) {
            return baseDamage * crit * bonus * def * res;
        }

        return switch (reaction.getReactionType()) {
            case MELT, VAPORIZE -> baseDamage
                    * crit * reaction.getAmplifyMultiplier()
                    * reactionBonusZone(attackerCharacter, reaction.getReactionType())
                    * bonus * def * res;
            case AGGRAVATE, SPREAD, QUICKEN -> baseDamage * crit * bonus * def * res;
            case OVERLOAD, SUPERCONDUCT, ELECTRO_CHARGED, BURNING, BLOOM, HYPERBLOOM, BURGEON
                    -> baseDamage * crit * bonus * def * res;
            default -> baseDamage * crit * bonus * def * res;
        };
    }

    private static float critZone(PGCharacter attacker, ModDamageSpec spec) {
        if (attacker == null) return 1.0f;
        var data = attacker.getData();
        float critRate = (float) data.getAttributeTotalValue(ModAttributes.CR.value());
        float critDmg = (float) data.getAttributeTotalValue(ModAttributes.CDG.value());
        boolean isCrit = Math.random() < critRate;
        spec.setCrit(isCrit);
        return isCrit ? (1f + critDmg) : 1.0f;
    }

    private static float critZone(PGCharacter attacker) {
        if (attacker == null) return 1.0f;
        var data = attacker.getData();
        float critRate = (float) data.getAttributeTotalValue(ModAttributes.CR.value());
        float critDmg = (float) data.getAttributeTotalValue(ModAttributes.CDG.value());
        boolean isCrit = Math.random() < critRate;
        return isCrit ? (1f + critDmg) : 1.0f;
    }

    private static float dmgBonusZone(PGCharacter attacker, ModDamageSpec spec) {
        float elementalBonus = CombatEntityAccessor.getDamageBonus(attacker, spec.getElement());
        float effectBonus = attacker.getData().getEffectContainer().getTotalDamageBonus(spec.getAttackType(), spec.getElement());
        return CombatMath.dmgBonusZone(elementalBonus + effectBonus);
    }

    private static float defenseZone(LivingEntity attacker, PGCharacter attackerCharacter,
                                     LivingEntity defender, PGCharacter defenderCharacter) {
        int attackerLevel = CombatEntityAccessor.getAttackerLevel(attacker, attackerCharacter);
        int defenderLevel = CombatEntityAccessor.getDefenderLevel(defender, defenderCharacter);
        double defenderDef = CombatEntityAccessor.getDefenderDefense(defender, defenderCharacter);
        return 1 - CombatMath.defenseZone(attackerLevel, defenderDef);
    }

    // ============================================================
    // 公用乘区 —— 直伤和剧变都用的抗性区、EM乘区、反应加成区
    // ============================================================

    private static float resistanceZone(GenshinElement element,
                                        LivingEntity defender, PGCharacter defenderCharacter) {
        float res = CombatEntityAccessor.getDefenderResistance(defender, defenderCharacter, element);
        float resZone = CombatMath.resistanceZone(res);
        //        LOGGER.debug("[抗性区] element={} | 原始抗性={} | resZone={}", element, res, resZone);
        return resZone;
    }

    private static float emBonusZone(PGCharacter attacker, ElementalReactionType reactionType) {
        if (attacker == null) {
//            LOGGER.info("[EM乘区] attacker=null → emBonus=0");
            return 0f;
        }
        double em = attacker.getData().getAttributeTotalValue(ModAttributes.ELEMENTAL_MASTERY.value());
        float bonus = switch (reactionType) {
            case MELT, VAPORIZE -> (float) ((2.78 * em) / (em + 1400.0));
            case OVERLOAD, SUPERCONDUCT, ELECTRO_CHARGED, SWIRL, BURNING, BLOOM, HYPERBLOOM, BURGEON
                    -> (float) ((16.0 * em) / (em + 2000.0));
            case LUNAR_CHARGED, LUNAR_BLOOM, LUNAR_CRYSTALLIZE
                    -> (float) ((6.0 * em) / (em + 2000.0));
            default -> 0f;
        };
        //        LOGGER.info("[EM乘区] EM={} | reaction={} | emBonus={}", em, reactionType, bonus);
        return bonus;
    }

    private static float reactionBonusZone(PGCharacter attacker, ElementalReactionType reactionType) {
        return 1.0f + emBonusZone(attacker, reactionType);
    }

    private static float lunarReactionBonusZone(PGCharacter attacker, ElementalReactionType reactionType) {
        float emBonus = emBonusZone(attacker, reactionType);
        float reactionDmgBonus = 0f;
        return 1.0f + emBonus + reactionDmgBonus;
    }

    private static float stellarSwirlReactionBonusZone(PGCharacter attacker, ElementalReactionType reactionType) {
        float emBonus = emBonusZone(attacker, reactionType);
        float reactionDmgBonus = 0f;
        return 1.0f + emBonus + reactionDmgBonus;
    }

    private static float sovereigntyZone(PGCharacter attacker, ElementalReactionType reactionType) {
        return 1.0f;
    }

    private static float elevationZone() {
        return 1.0f;
    }

    // ============================================================
    // 剧变反应管线 — 等级系数区 × 反应倍率区 × 反应加成区 × 抗性区（无基础伤害/暴击/增伤/防御）
    // ============================================================

    public static float calculateTransformativeDamage(LivingEntity attacker, LivingEntity target,
                                                       ElementalReactionType reactionType,
                                                       GenshinElement dmgElementOverride) {
        PGCharacter character = resolveCharacter(attacker);
        int level = CombatEntityAccessor.getAttackerLevel(attacker, character);
        double levelCoef = ReactionConfig.getReactionFusion(level);

        float reactionMult = switch (reactionType) {
            case ELECTRO_CHARGED -> ReactionConfig.ELECTROCHARGED.getFloat();
            case SWIRL -> ReactionConfig.SWIRL.getFloat();
            default -> 1.0f;
        };

        float reactionBonus = reactionBonusZone(character, reactionType);

        GenshinElement dmgElement;
        if (dmgElementOverride != null) {
            dmgElement = dmgElementOverride;
        } else {
            dmgElement = switch (reactionType) {
                case ELECTRO_CHARGED -> ModElements.ELECTRO.get();
                default -> ModElements.FYSIKOS.get();
            };
        }
        PGCharacter targetChar = resolveCharacter(target);
        float resZone = resistanceZone(dmgElement, target, targetChar);

        float damage = (float) (levelCoef * reactionMult * reactionBonus * resZone);

        return damage;
    }

    // ============================================================
    // 月曜反应管线 — 基础区 × 基础提升 × 倍率 × 反应加成区 × 抗性区 × 暴击区 × 擢升区
    //   基础区：反应月感电=等级系数，直伤月感电=3×攻击力（配置项）
    //   反应加成区=1 + (6×EM)/(EM+2000) + 反应伤害加成
    //   无视防御，不吃增伤区
    // ============================================================

    public static float calculateLunarDirectDamage(LivingEntity attacker, PGCharacter character,
                                                    LivingEntity target, ModDamageSpec spec) {
        List<PGCharacter> contributors = spec.getLunarContributors();
        if (contributors != null && !contributors.isEmpty()) {
            List<LunarContributorResult> results = new ArrayList<>();
            for (PGCharacter ch : contributors) {
                int level = CombatEntityAccessor.getAttackerLevel(null, ch);
                results.add(calculateLunarPerCharacter(
                        ch, level, target,
                        ElementalReactionType.LUNAR_CHARGED, null));
            }
            LunarCombinedResult combined = combineLunarDamage(results);
            spec.setCrit(combined.isCrit);
            return combined.totalDamage;
        }

        if (character == null) {
            character = resolveCharacter(attacker);
        }

        float reactionBonus = lunarReactionBonusZone(character, ElementalReactionType.LUNAR_CHARGED);
        float res = resistanceZone(ModElements.ELECTRO.get(), target, resolveCharacter(target));
        float crit = critZone(character, spec);
        float elevation = elevationZone();

        float base;
        if (spec.getHpMultiplier() > 0) {
            float hp = character != null
                    ? (float) character.getData().getAttributeTotalValue(ModAttributes.MAX_HP.value())
                    : 0f;
            base = hp * spec.getHpMultiplier();
        } else {
            float atk = character != null
                    ? (float) character.getData().getAttributeTotalValue(ModAttributes.ATK.value())
                    : 0f;
            base = (float) (ReactionConfig.LUNAR_DIRECT_BASE_COEFFICIENT.get() * atk);
        }

        float baseBoost = base * (1f + spec.getLunarBaseBonus()) + spec.getLunarBaseFlat();
        float multiplier = spec.getAtkMultiplier();

        float damage = baseBoost * multiplier * reactionBonus * res * crit * elevation;

        return damage;
    }

    public static class LunarContributorResult {
        public final UUID playerUUID;
        public final PGCharacter character;
        public final float theoryDamage;
        public final boolean isCrit;

        public LunarContributorResult(UUID uuid, PGCharacter ch, float dmg, boolean crit) {
            this.playerUUID = uuid;
            this.character = ch;
            this.theoryDamage = dmg;
            this.isCrit = crit;
        }
    }

    public static LunarContributorResult calculateLunarPerCharacter(PGCharacter character, int level,
                                                                     LivingEntity target, ElementalReactionType reactionType,
                                                                     UUID playerUUID) {
        double levelCoef = ReactionConfig.getReactionFusion(level);
        double baseBoost = levelCoef;
        double multiplier = ReactionConfig.LUNAR_CHARGED_MULT.get();
        float reactionBonus = lunarReactionBonusZone(character, reactionType);
        float res = resistanceZone(ModElements.ELECTRO.get(), target, resolveCharacter(target));
        float crit = critZone(character);
        float elevation = elevationZone();

        float damage = (float) (baseBoost * multiplier * reactionBonus * res * crit * elevation);

        LOGGER.info("[月感电单人理论伤害] char={} | level={} | levelCoef={} | baseBoost={} | mult={} | reactBonus={} | res={} | crit={} | elev={} | final={}",
                character != null ? character.getName() : "?",
                level, levelCoef, baseBoost, multiplier, reactionBonus, res, crit, elevation, damage);

        return new LunarContributorResult(playerUUID, character, damage, crit > 1.0f);
    }

    public static class LunarCombinedResult {
        public final float totalDamage;
        public final boolean isCrit;

        public LunarCombinedResult(float total, boolean crit) {
            this.totalDamage = total;
            this.isCrit = crit;
        }
    }

    public static LunarCombinedResult combineLunarDamage(List<LunarContributorResult> contributors) {
        if (contributors.isEmpty()) return new LunarCombinedResult(0f, false);

        contributors.sort(Comparator.comparingDouble(r -> -r.theoryDamage));

        float total = 0f;
        int n = contributors.size();

        if (n >= 1) total += contributors.get(0).theoryDamage;
        if (n >= 2) total += contributors.get(1).theoryDamage / 2f;
        if (n >= 3) total += contributors.get(2).theoryDamage / 12f;
        if (n >= 4) total += contributors.get(3).theoryDamage / 12f;
        for (int i = 4; i < n && i < 8; i++) {
            total += contributors.get(i).theoryDamage / 24f;
        }

        boolean crit = contributors.get(0).isCrit;

        LOGGER.info("[月感电合并伤害] contributors={} | total={} | topCrit={}",
                n, total, crit);
        return new LunarCombinedResult(total, crit);
    }

    // ============================================================
    // 星扩散反应伤害计算
    //  公式：【等级系数 × 星辉基础系数 × (1+基础倍率加成) + 基础附加】× 反应倍率 × (1+精通加成+反应伤害加成) × 抗性区 × 暴击区 × 擢升区 × 大权区
    //  合并：排序后权重 0.6 / 0.3 / 0.05 / 0.05
    // ============================================================

    public static float calculateStellarDirectDamage(PGCharacter attacker, LivingEntity target,
                                                      ModDamageSpec spec) {
        List<PGCharacter> contributors = spec.getStellarContributors();
        if (contributors != null && !contributors.isEmpty()) {
            ElementalReactionType reactionType = spec.getTransformativeReactionType();
            double coefficient = spec.getStellarCoefficient();
            float baseBonusMult = spec.getStellarBaseBonusMult();
            float baseBonusFlat = spec.getStellarBaseBonusFlat();

            List<StellarContributorResult> results = new ArrayList<>();
            for (PGCharacter ch : contributors) {
                int level = CombatEntityAccessor.getAttackerLevel(null, ch);
                results.add(calculateStellarSwirlPerCharacter(
                        ch, level, target, reactionType, null,
                        coefficient, baseBonusMult, baseBonusFlat));
            }
            StellarCombinedResult combined = combineStellarSwirlDamage(results);
            spec.setCrit(combined.isCrit);
            return combined.totalDamage;
        }
        return 0f;
    }

    public static StellarContributorResult calculateStellarSwirlPerCharacter(
            PGCharacter character, int level, LivingEntity target,
            ElementalReactionType reactionType, UUID playerUUID,
            double stellarCoefficient, float baseBonusMult, float baseBonusFlat) {

        double levelCoef = ReactionConfig.getReactionFusion(level);
        double baseBoost = levelCoef * stellarCoefficient * (1.0 + baseBonusMult) + baseBonusFlat;

        float reactionBonus = stellarSwirlReactionBonusZone(character, reactionType);
        float res = resistanceZone(
                reactionType == ElementalReactionType.STELLAR_SWIRL_WIND
                        ? ModElements.ANEMO.get() : ModElements.CYRO.get(),
                target, resolveCharacter(target));
        float crit = critZone(character);
        float elevation = elevationZone();
        float sovereignty = sovereigntyZone(character, reactionType);

        float damage = (float) (baseBoost * reactionBonus * res * crit * elevation * sovereignty);

        LOGGER.info("[星扩散单人理论伤害] char={} | level={} | levelCoef={} | coefficient={} | baseBoost={} | reactBonus={} | res={} | crit={} | elev={} | sov={} | final={}",
                character != null ? character.getName() : "?",
                level, levelCoef, stellarCoefficient, baseBoost, reactionBonus, res, crit, elevation, sovereignty, damage);

        return new StellarContributorResult(playerUUID, character, damage, crit > 1.0f);
    }

    public static class StellarContributorResult {
        public final UUID playerUUID;
        public final PGCharacter character;
        public final float theoryDamage;
        public final boolean isCrit;

        public StellarContributorResult(UUID uuid, PGCharacter ch, float dmg, boolean crit) {
            this.playerUUID = uuid;
            this.character = ch;
            this.theoryDamage = dmg;
            this.isCrit = crit;
        }
    }

    public static class StellarCombinedResult {
        public final float totalDamage;
        public final boolean isCrit;
        public final PGCharacter topCharacter;

        public StellarCombinedResult(float total, boolean crit, PGCharacter topChar) {
            this.totalDamage = total;
            this.isCrit = crit;
            this.topCharacter = topChar;
        }
    }

    public static StellarCombinedResult combineStellarSwirlDamage(List<StellarContributorResult> contributors) {
        if (contributors.isEmpty()) return new StellarCombinedResult(0f, false, null);

        contributors.sort(Comparator.comparingDouble(r -> -r.theoryDamage));

        float total = 0f;
        int n = contributors.size();

        if (n >= 1) total += contributors.get(0).theoryDamage * 0.6f;
        if (n >= 2) total += contributors.get(1).theoryDamage * 0.3f;
        if (n >= 3) total += contributors.get(2).theoryDamage * 0.05f;
        if (n >= 4) total += contributors.get(3).theoryDamage * 0.05f;

        boolean crit = contributors.get(0).isCrit;

        LOGGER.info("[星扩散合并伤害] contributors={} | total={} | topCrit={}",
                n, total, crit);
        return new StellarCombinedResult(total, crit, contributors.get(0).character);
    }
}