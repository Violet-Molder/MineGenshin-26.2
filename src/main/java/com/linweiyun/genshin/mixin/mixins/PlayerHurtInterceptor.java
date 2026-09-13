package com.linweiyun.genshin.mixin.mixins;

import com.linweiyun.genshin.content.entities.teyvat.ITeyvatBoss;
import com.linweiyun.genshin.content.entities.teyvat.monster.TeyvatMonster;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerHurtInterceptor {

    @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
    private void onPlayerActuallyHurt(ServerLevel level, DamageSource source, float damage,
                                      CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (!player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT)) return;
        if (source instanceof com.linweiyun.genshin.core.system.combat.damage.ModDamageSource) return;

        Entity attacker = source.getEntity();
        boolean isTeyvatMonsterOrBoss = attacker instanceof TeyvatMonster
                || attacker instanceof ITeyvatBoss;

        float adjustedDamage = damage;
        if (!isTeyvatMonsterOrBoss && attacker != null) {
            adjustedDamage = damage * 2.5f;
        }

        PlayerCharactersAttachment attachment =
                player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        PGCharacter current = attachment.getCurrentCharacter();
        if (current != null) {
            current.hurt(adjustedDamage);
        }
        ci.cancel();
    }
}