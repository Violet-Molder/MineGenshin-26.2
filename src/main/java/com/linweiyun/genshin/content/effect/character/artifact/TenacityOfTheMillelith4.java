package com.linweiyun.genshin.content.effect.character.artifact;

import com.linweiyun.genshin.content.effect.character.CharacterEffectHelper;
import com.linweiyun.genshin.content.effect.character.CharacterEffectInstance;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.registry.register.ModCharacterEffects;
import com.linweiyun.genshin.core.system.combat.attack.AttackType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 千岩牢固 · 四件套：元素战技命中敌人后，队伍中附近的<span>所有</span>角色
 * 攻击力提升 <b>20%</b>，持续 <b>3 秒</b>；每 <b>0.5 秒</b>至多触发一次；装备者处于后台时依然能触发。
 *
 * <p>这个类本身不改属性 —— 它只是「穿着四件套」的<b>标记</b>
 * （和 {@link ScarletProof4} 同一形态）：真正改属性的是触发出来的
 * {@link TenacityOfTheMillelithBuff}（3 秒后自然掉）。
 *
 * <h2>触发点</h2>
 * 唯一的入口是 {@link #notifySkillHit}，由伤害统一入口
 * {@code HurtEntityHelper#calculateFinalModDamage} 在直伤结算处调用一次：
 * 那里同时拿得到「攻击类型」（{@code spec.getAttackType()}）、攻击者角色、被命中的实体，
 * 是「元素战技命中敌人」最窄、也最不会漏的一处（后台打的战技伤害走同一条管线）。
 *
 * <h2>0.5 秒闸门与「后台也能触发」</h2>
 * 闸门是<b>角色</b>身上的持久化绝对游戏刻（{@code PGCharacterData.tenacity4GateTick}），
 * 和蝶变的 {@code weaponPassiveGateTick} 同一形态 —— 换人、存档、重登都不会串。
 * 触发链只依赖角色数据与队伍数据，不依赖「装备者是不是当前场上角色」，
 * 所以后台照常触发。
 *
 * <h2>「队伍中附近的所有角色」</h2>
 * 本 MOD 的角色是<b>数据</b>（{@code PGCharacter}）而不是实体，队里 4 个角色共用玩家本人的坐标，
 * 没有各自的实体位置可比距离，所以「附近」在这里等同于「同一玩家的队伍全员」
 * （沃雅妮莎 6 命、漩流颂歌 4 件套都是同一个口径）。
 */
public class TenacityOfTheMillelith4 extends ArtifactSetEffect {

    /** 触发后 buff 持续 3 秒。 */
    public static final int BUFF_DURATION_TICKS = 3 * 20;

    /** 触发间隔下限：每 0.5 秒至多触发一次。 */
    public static final int TRIGGER_GATE_TICKS = 10;

    /** 队伍里最多 4 个角色（和仓库其它地方一致）。 */
    private static final int PARTY_SIZE = 4;

    /**
     * 「元素战技命中敌人」的触发入口（服务端）。
     *
     * <p>由伤害统一入口调用；不是元素战技、攻击者不是角色、没穿满四件套、或者还在 0.5 秒闸门里，
     * 都会直接返回。
     */
    public static void notifySkillHit(ModDamageSource damageSource, PGCharacter attacker, LivingEntity target) {
        if (attacker == null || target == null || damageSource == null) return;
        if (target.level().isClientSide()) return;
        if (damageSource.getSpec().getAttackType() != AttackType.ELEMENTAL_SKILL) return;
        if (!(damageSource.getEntity() instanceof Player holder)) return;

        // 穿着四件套才在（四件套是常驻标记，触发出来的才是那 3 秒 buff）
        if (!attacker.getData().getEffectContainer().hasEffectOfType(TenacityOfTheMillelith4.class)) return;

        long now = target.level().getGameTime();
        if (now < attacker.getData().getTenacity4GateTick()) return;
        attacker.getData().setTenacity4GateTick(now + TRIGGER_GATE_TICKS);

        PlayerCharactersAttachment attachment =
                holder.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        if (attachment == null) return;

        for (int i = 0; i < PARTY_SIZE; i++) {
            PGCharacter member = attachment.getPartyCharacter(i);
            if (member == null) continue;
            CharacterEffectHelper.addEffect(holder, member, new CharacterEffectInstance(
                    ModCharacterEffects.TENACITY_OF_THE_MILLELITH_BUFF_EFFECT.get(),
                    BUFF_DURATION_TICKS, 0, false));
        }
    }
}
