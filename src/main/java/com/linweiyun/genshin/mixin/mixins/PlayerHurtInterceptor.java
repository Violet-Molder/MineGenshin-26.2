package com.linweiyun.genshin.mixin.mixins;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
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

        PlayerCharactersAttachment attachment =
                player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        PGCharacter current = attachment.getCurrentCharacter();
        if (current != null) {
            boolean dead = current.hurt(damage);
            if (dead) {
                current.incapacitate(); //AI 改为无参调用
            }
        }
        ci.cancel();
    }
}