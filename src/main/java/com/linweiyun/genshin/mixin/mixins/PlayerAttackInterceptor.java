package com.linweiyun.genshin.mixin.mixins;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.combat.attack.HurtEntityHelper;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.enums.AttackType;
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

        // 只拦截 LivingEntity 目标
        if (!(target instanceof LivingEntity livingTarget)) return;

        // 获取玩家持有的原神角色
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        PGCharacter character = attachment.getCurrentCharacter();
        if (character == null) return;  // 非原神角色，放行原版

        // 取消原版攻击流程
        ci.cancel();

        // 构建伤害源 + 走管线
        ModDamageSource source = buildModDamageSource(player, character, livingTarget);
        HurtEntityHelper.hurtEntityForPlayer(source, character, livingTarget, 1.0f);
    }

    /**
     * 从 Player 获取 PGCharacter
     * TODO: 替换为你项目实际的关联方式
     */

    /**
     * 构建 ModDamageSource
     * 可根据玩家状态判断普攻/重击/战技/爆发
     */
    private ModDamageSource buildModDamageSource(Player player, PGCharacter character, LivingEntity target) {
        // 暂时默认普攻，后续细化
        ModDamageSpec spec = ModDamageSpec.physical(AttackType.NORMAL_ATTACK, 1.0f);
        return ModDamageSource.from(spec, player);
    }
}
