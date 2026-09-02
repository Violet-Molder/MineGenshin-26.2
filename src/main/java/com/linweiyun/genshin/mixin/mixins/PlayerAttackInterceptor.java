package com.linweiyun.genshin.mixin.mixins;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.combat.attack.HurtEntityHelper;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.enums.AttackType;
import com.linweiyun.genshin.enums.ElementalsGIM;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.linweiyun.genshin.Minegenshin.LOGGER;

@Mixin(Player.class)
public class PlayerAttackInterceptor {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onPlayerAttack(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;

        if (!(target instanceof LivingEntity livingTarget)) return;
        boolean isGenshinMode = player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
        if (!isGenshinMode) return;

        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        PGCharacter character = attachment.getCurrentCharacter();
        if (character == null) return;

        ci.cancel();

        ModDamageSource source = buildModDamageSource(player, character, livingTarget);
        HurtEntityHelper.hurtEntityForPlayer(source, character, livingTarget, 1.0f);
    }

    private ModDamageSource buildModDamageSource(Player player, PGCharacter character, LivingEntity target) {
        boolean genshinMode = player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
        //TEST
        if (genshinMode) {
            if (character.getCharacterUUID() == 145001) {
                ModDamageSpec spec = ModDamageSpec.elemental(
                        AttackType.NORMAL_ATTACK, ElementalsGIM.HYDRO, 1.0f);
                return ModDamageSource.from(spec, player);
            } else if (character.getCharacterUUID() == 135001) {
                ModDamageSpec spec = ModDamageSpec.elemental(
                        AttackType.NORMAL_ATTACK, ElementalsGIM.CYRO, 1.0f);
                return ModDamageSource.from(spec, player);
            } else if (character.getCharacterUUID() == 135002) {
                ModDamageSpec spec = ModDamageSpec.elemental(
                        AttackType.NORMAL_ATTACK, ElementalsGIM.PYRO, 1.0f);
                return ModDamageSource.from(spec, player);
            }

        }
        ModDamageSpec spec = ModDamageSpec.physical(AttackType.NORMAL_ATTACK, 1.0f);
        return ModDamageSource.from(spec, player);
    }
}