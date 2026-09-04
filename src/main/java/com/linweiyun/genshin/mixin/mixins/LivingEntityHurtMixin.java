package com.linweiyun.genshin.mixin.mixins;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.combat.attack.HurtEntityHelper;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityHurtMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void onLivingEntityHurtServer(ServerLevel level, DamageSource source, float damage,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (!(source instanceof ModDamageSource modSource)) return;

        LivingEntity target = (LivingEntity) (Object) this;
        PGCharacter attackerCharacter = modSource.getSpec().getAttackerCharacter();
        float finalDamage = HurtEntityHelper.calculateFinalModDamage(
                modSource, attackerCharacter, target);

        if (target instanceof Player player
                && player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT)) {
            PlayerCharactersAttachment attachment =
                    player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            PGCharacter current = attachment.getCurrentCharacter();
            if (current != null) {
                //AI hurt() 内部已处理倒下逻辑，不再需要外部手动调用 incapacitate()
                current.hurt(finalDamage);
            }
        } else {
            target.setHealth(Math.max(target.getHealth() - finalDamage, 0));
        }

        level.broadcastDamageEvent(target, source);

        cir.setReturnValue(true);
    }
}