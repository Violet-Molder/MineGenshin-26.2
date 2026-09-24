package com.linweiyun.genshin.core.system.about;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * 一次附着的来源上下文 —— 附着入口内部触发反应时要用的「谁挂的、算多少、按什么规格」。
 *
 * <p>大多数来源只需要默认值（环境、自身、反应内的二次写入）。攻击管线会带上攻击者实体、
 * 伤害规格，以及一个元素量覆盖值：衰减系数会削减这次附着的「反应用量」，
 * 而附着本身仍然按 profile 算，这是既有语义，必须显式带过来而不是让入口自己猜。
 *
 * @param character       挂上这条附着的角色（用于反应贡献者追踪，可空）
 * @param gameTime        附着时刻（可空：传 0 表示不记录）
 * @param attackerEntity  攻击者实体（反应要用它算来源/月感电等，可空）
 * @param damageSpec      本次伤害规格（增幅/月体系反应要用，可空）
 * @param reactionUnit    触发反应时使用的元素量；{@code null} 表示用实际附着量
 */
public record AttachContext(@Nullable PGCharacter character,
                           long gameTime,
                           @Nullable Entity attackerEntity,
                           @Nullable ModDamageSpec damageSpec,
                           @Nullable Float reactionUnit) {

    /** 环境/自身附着：没有攻击者、没有伤害规格。 */
    public static final AttachContext ENVIRONMENT = new AttachContext(null, 0L, null, null, null);

    /** 攻击型附着：带角色、时刻、攻击者，并按给定元素量触发反应。 */
    public static AttachContext attack(@Nullable PGCharacter character, long gameTime,
                                       @Nullable Entity attackerEntity,
                                       @Nullable ModDamageSpec damageSpec,
                                       float reactionUnit) {
        return new AttachContext(character, gameTime, attackerEntity, damageSpec, reactionUnit);
    }

    /** 反应内的二次写入：带攻击者用于后续伤害来源，但不覆盖元素量。 */
    public static AttachContext reactionWrite(@Nullable Entity attackerEntity,
                                              @Nullable ModDamageSpec damageSpec) {
        return new AttachContext(null, 0L, attackerEntity, damageSpec, null);
    }
}
