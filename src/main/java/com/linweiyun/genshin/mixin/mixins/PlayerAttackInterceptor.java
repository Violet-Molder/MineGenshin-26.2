package com.linweiyun.genshin.mixin.mixins;

import com.linweiyun.genshin.content.entities.teyvat.NonTeyvatEntity;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.enums.AttackType;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

        if (!(target instanceof LivingEntity livingTarget)) return;
        boolean isGenshinMode = player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
        if (!isGenshinMode) return;

        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        PGCharacter character = attachment.getCurrentCharacter();
        if (character == null) return;

        ci.cancel();

        ModDamageSource source = buildModDamageSource(player, character, livingTarget);

        if (livingTarget instanceof NonTeyvatEntity) {
            ModDamageSpec spec = source.getSpec();
            ModDamageSpec adjustedSpec = ModDamageSpec.builder(spec.getAttackType(), spec.getElement())
                    .multiplier(spec.getAtkMultiplier() / 8.0f)
                    .elementAmount(spec.getElementAmount())
                    .attackerCharacter(spec.getAttackerCharacter())
                    .build();
            source.setSpec(adjustedSpec);
        }

        if (livingTarget.level() instanceof ServerLevel serverLevel) {
            livingTarget.hurtServer(serverLevel, source, 0f);
        }
    }

    private ModDamageSource buildModDamageSource(Player player, PGCharacter character, LivingEntity target) {
        boolean genshinMode = player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
        if (genshinMode) {
            if (character.getCharacterUUID() == 145001) {
                ModDamageSpec spec = ModDamageSpec.builder(AttackType.NORMAL_ATTACK, ElementalsGIM.HYDRO)
                        .multiplier(1.0f).elementAmount(1.0f).attackerCharacter(character).build();
                return ModDamageSource.from(spec, player);
            } else if (character.getCharacterUUID() == 135001) {
                ModDamageSpec spec = ModDamageSpec.builder(AttackType.NORMAL_ATTACK, ElementalsGIM.CYRO)
                        .multiplier(1.0f).elementAmount(1.0f).attackerCharacter(character).build();
                return ModDamageSource.from(spec, player);
            } else if (character.getCharacterUUID() == 135002) {
                ModDamageSpec spec = ModDamageSpec.builder(AttackType.NORMAL_ATTACK, ElementalsGIM.PYRO)
                        .multiplier(1.0f).elementAmount(1.0f).attackerCharacter(character).build();
                return ModDamageSource.from(spec, player);
            }
        }
        ModDamageSpec spec = ModDamageSpec.builder(AttackType.NORMAL_ATTACK, ElementalsGIM.FYSIKOS)
                .multiplier(1.0f).attackerCharacter(character).build();
        return ModDamageSource.from(spec, player);
    }
}