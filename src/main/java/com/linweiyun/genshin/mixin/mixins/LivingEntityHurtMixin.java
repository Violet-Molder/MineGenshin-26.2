package com.linweiyun.genshin.mixin.mixins;

import com.linweiyun.genshin.config.DamageIndicatorConfig;
import com.linweiyun.genshin.content.entities.teyvat.NonTeyvatEntity;
import com.linweiyun.genshin.content.entities.teyvat.TeyvatLiving;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.combat.attack.HurtEntityHelper;
import com.linweiyun.genshin.core.system.combat.damage.DamageIndicatorFactory;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.world.TeyvatWorldInvasion;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
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

        if (!(source instanceof ModDamageSource modSource)) {
            return;
        }

        if (!TeyvatWorldInvasion.get(level).isInvaded()) return;

        LivingEntity target = (LivingEntity) (Object) this;
        ModDamageSpec spec = modSource.getSpec();
        PGCharacter attackerCharacter = spec.getAttackerCharacter();
        GenshinElement element = spec.getElement();
        float finalDamage = HurtEntityHelper.calculateFinalModDamage(
                modSource, attackerCharacter, target);

        if (target instanceof NonTeyvatEntity && modSource.getEntity() instanceof Player player) {
            float playerAttack = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            finalDamage = finalDamage + playerAttack;
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
            }
        } else if (target instanceof TeyvatLiving) {
            target.setHealth(Math.max(target.getHealth() - finalDamage, 0));
        } else {
            target.setHealth(Math.max(target.getHealth() - finalDamage, 0));
        }

        LOGGER.info("[DI-Mixin] about to call DamageIndicatorFactory, finalDamage={} element={}", finalDamage, element);
        if (finalDamage > 0f) {
            try {
                if (element == ModElements.HYDRO.get()) {
                    int hydroColor = DamageIndicatorConfig.getColorForElement(ModElements.HYDRO.get());
                    DamageIndicatorFactory.damageGradient(
                            target, modSource, finalDamage,
                            0xFFFFFF,
                            hydroColor
                    );
                } else {
                    DamageIndicatorFactory.damage(target, modSource, finalDamage, element);
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
}