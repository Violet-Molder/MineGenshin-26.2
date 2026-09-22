package com.linweiyun.genshin.mixin.mixins;


import com.linweiyun.genshin.content.entities.teyvat.TeyvatEntityStats;
import com.linweiyun.genshin.content.entities.teyvat.TeyvatLiving;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.combat.attack.HurtEntityHelper;
import com.linweiyun.genshin.core.system.combat.damage.DamageIndicatorFactory;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.core.system.combat.damage.TeyvatConvertedDamageSource;
import com.linweiyun.genshin.core.system.shield.ShieldService;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.world.TeyvatWorldInvasion;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityHurtMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Shadow
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Shadow
    private void playSecondaryHurtSound(DamageSource source) {

    }

    @Shadow
    protected void playHurtSound(DamageSource source) {

    }

    @Shadow
    protected int lastHurtByPlayerMemoryTime;

    @Shadow
    protected EntityReference<Player> lastHurtByPlayer;

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void onLivingEntityHurtServer(ServerLevel level, DamageSource source, float damage,
                                          CallbackInfoReturnable<Boolean> cir) {

        LivingEntity self = (LivingEntity) (Object) this;

        // 护盾：伤害还没落地之前先问盾。
        //
        // ⚠️ 两个「不能在这里扣盾」的情况：
        //   ① ModDamageSource —— 它的真实伤害在下面才算出来，传进来的 damage 通常是 0；
        //   ② 怪物攻击的「换算」分支 —— 它会带着换算后的伤害<b>递归调用</b> hurtServer，
        //      在这里扣一次、递归里再扣一次就双倍消耗了。所以让它去递归那一层扣。
        boolean convertedBelow = usesTeyvatConversion(level, source);
        if (!convertedBelow && !(source instanceof ModDamageSource)) {
            // 原版伤害源没有 ModDamageSpec，也就没有削韧信息
            float through = ShieldService.absorbDamage(self, source, damage, 0f);
            if (damage > 0f && through <= 0f) {
                // 盾全吃下了：本次不构成受伤
                cir.setReturnValue(false);
                return;
            }
            damage = through;
        }

        if (TeyvatWorldInvasion.get(level).isInvaded()
                && !(source instanceof ModDamageSource)
                && !(source instanceof TeyvatConvertedDamageSource)) {
            Entity attacker = source.getEntity();
            if (attacker instanceof TeyvatLiving teyvatAttacker && attacker instanceof LivingEntity livingAttacker) {
                TeyvatEntityStats stats = teyvatAttacker.getEntityStats();
                float teyvatAttack = stats.attack();
                if (teyvatAttack > 0) {
                    float vanillaAttack = (float) livingAttacker.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
                    if (vanillaAttack > 0) {
                        float convertedDamage = damage / vanillaAttack * teyvatAttack;
                        cir.cancel();
                        TeyvatConvertedDamageSource newSource = new TeyvatConvertedDamageSource(source);
                        boolean result = self.hurtServer(level, newSource, convertedDamage);
                        cir.setReturnValue(result);
                        return;
                    }
                }
            }
        }

        if (!(source instanceof ModDamageSource modSource)) {
            return;
        }

        if (!TeyvatWorldInvasion.get(level).isInvaded()) return;

        LivingEntity target = self;
        ModDamageSpec spec = modSource.getSpec();
        PGCharacter attackerCharacter = spec.getAttackerCharacter();
        GenshinElement element = spec.getElement();
        float finalDamage = HurtEntityHelper.calculateFinalModDamage(
                modSource, attackerCharacter, target);

        // 护盾：ModDamageSource 的真实伤害到这里才算出来，所以在这里扣盾
        if (finalDamage > 0f) {
            finalDamage = ShieldService.absorbDamage(target, source, finalDamage, spec.getPoiseDamage());
        }

        DamageContainer container = new DamageContainer(source, finalDamage);
        if (CommonHooks.onEntityIncomingDamage(target, container)) {
            cir.setReturnValue(false);
            return;
        }

        if (target instanceof Player player
                && player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT)) {
            PlayerCharactersAttachment attachment =
                    player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            PGCharacter current = attachment.getCurrentCharacter();
            if (current != null) {
                current.hurt(finalDamage);
            } else {
                target.setHealth(Math.max(target.getHealth() - finalDamage, 0));
            }
        } else if (target instanceof TeyvatLiving) {
            target.setHealth(Math.max(target.getHealth() - finalDamage, 0));
        } else {
            target.setHealth(Math.max(target.getHealth() - finalDamage, 0));
        }

        if (finalDamage > 0f) {
            try {
                boolean isCrit = spec.isCrit();
                DamageIndicatorFactory.Options options = isCrit
                        ? DamageIndicatorFactory.Options.builder().baseScale(4.4f).startScale(12.4f).build()
                        : DamageIndicatorFactory.Options.DEFAULT;

                if (spec.getDamageType() == ModDamageSpec.DamageType.LUNAR) {
                    boolean isDirectLunar = spec.getAtkMultiplier() > 0 || spec.getHpMultiplier() > 0;
                    if (!isDirectLunar) {
                        int electroColor = DamageIndicatorFactory.getColorForElement(ModElements.ELECTRO.get());
                        DamageIndicatorFactory.reactionGradient(target,
                                ElementalReactionType.LUNAR_CHARGED, electroColor, 0xFFFFFF);
                    }

                    int topColor = 0xAA55FF;
                    int bottomColor = 0xFFFFFF;
                    String text = String.format("%.0f", finalDamage);
                    if (isCrit) {
                        DamageIndicatorFactory.textGradient(target, text, topColor, bottomColor,
                                DamageIndicatorFactory.Options.of(2.6f, 7.0f, 1100L));
                    } else {
                        DamageIndicatorFactory.textGradient(target, text, topColor, bottomColor,
                                DamageIndicatorFactory.Options.of(2.2f, 6.2f, 950L));
                    }
                } else if (spec.getDamageType() == ModDamageSpec.DamageType.STELLAR) {
                    handleStellarDamageIndicator(target, spec, finalDamage, isCrit, options);
                } else if (element == ModElements.HYDRO.get()) {
                    int hydroColor = DamageIndicatorFactory.getColorForElement(ModElements.HYDRO.get());
                    if (isCrit) {
                        DamageIndicatorFactory.critGradient(
                                target, modSource, finalDamage,
                                0xFFFFFF, hydroColor, options);
                    } else {
                        DamageIndicatorFactory.damageGradient(
                                target, modSource, finalDamage,
                                0xFFFFFF, hydroColor, options);
                    }
                } else {
                    if (isCrit) {
                        DamageIndicatorFactory.crit(target, modSource, finalDamage, element, options);
                    } else {
                        DamageIndicatorFactory.damage(target, modSource, finalDamage, element, options);
                    }
                }
            } catch (Throwable t) {
                LOGGER.error("[DI-Mixin] DamageIndicatorFactory threw", t);
            }
        } else {
            LOGGER.warn("[DI-Mixin] skip spawn because finalDamage <= 0");
        }

        level.broadcastDamageEvent(target, source);

        if (target.isDeadOrDying()) {
            target.makeSound(getDeathSound());
            playSecondaryHurtSound(source);

            if (modSource.getEntity() instanceof Player player) {
                lastHurtByPlayerMemoryTime = 100;
                lastHurtByPlayer = EntityReference.of(player);
            }

            target.die(source);
        } else {
            playHurtSound(source);
        }

        CommonHooks.onLivingDamagePost(target, container);

        cir.setReturnValue(true);
    }

    /**
     * 这次伤害会不会走下面那个「怪物攻击力换算」分支。
     *
     * <p>那个分支会带着换算后的伤害<b>递归调用</b> {@code hurtServer}，
     * 所以护盾必须在递归的那一层扣，否则同一次攻击会被扣两遍。
     */
    //TEMP
    private static boolean usesTeyvatConversion(ServerLevel level, DamageSource source) {
        if (!TeyvatWorldInvasion.get(level).isInvaded()) return false;
        if (source instanceof ModDamageSource || source instanceof TeyvatConvertedDamageSource) return false;
        Entity attacker = source.getEntity();
        if (!(attacker instanceof TeyvatLiving teyvatAttacker)
                || !(attacker instanceof LivingEntity livingAttacker)) {
            return false;
        }
        if (teyvatAttacker.getEntityStats().attack() <= 0f) return false;
        return livingAttacker.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) > 0;
    }

    private void handleStellarDamageIndicator(LivingEntity target, ModDamageSpec spec,
                                               float finalDamage, boolean isCrit,
                                               DamageIndicatorFactory.Options options) {
        ElementalReactionType reactionType = spec.getTransformativeReactionType();
        GenshinElement element = spec.getElement();

        int elementColor = DamageIndicatorFactory.getColorForElement(element);
        int bottomColor = elementColor | 0xFF000000;
        int topColor = 0xFFFFFFFF;

        String text = String.format("%.0f", finalDamage);
        DamageIndicatorFactory.textGradient(target, text, topColor, bottomColor, options);
    }
}