package com.linweiyun.genshin.core.system.combat.attack;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.character.PGCharacter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * 「这个实体对应哪个角色」—— 伤害管线里到处都要问这一句。
 *
 * <p>只有玩家身上才挂着角色（{@code PLAYER_CHARACTERS_ATTACHMENT} 的当前出战角色），
 * 怪物一律返回 {@code null} —— 于是所有「攻击者是不是角色」的判定都收敛成
 * {@code attacker != null}，不用在每条管线里各写一遍 instanceof。
 */
public final class AttackerResolver {

    private AttackerResolver() {
    }

    @Nullable
    public static PGCharacter resolveCharacter(@Nullable LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT).getCurrentCharacter();
        }
        return null;
    }
}
