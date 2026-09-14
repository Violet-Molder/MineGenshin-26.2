package com.linweiyun.genshin.mixin.mixins;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerAttackInterceptor {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onPlayerAttack(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;

        if (!(player.level() instanceof ServerLevel)) return;
        boolean isGenshinMode = player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
        if (!isGenshinMode) return;

        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        PGCharacter character = attachment.getCurrentCharacter();
        if (character == null) return;

        ci.cancel();
    }
}