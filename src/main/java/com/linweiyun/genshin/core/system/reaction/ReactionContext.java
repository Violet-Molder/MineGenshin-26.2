package com.linweiyun.genshin.core.system.reaction;

import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 反应执行上下文 —— 携带一次元素反应需要的全部环境数据
 */
public class ReactionContext {

    /** 被反应的目标（先手元素持有者） */
    public final LivingEntity target;

    /** 后手附着的元素（触发反应的那一个） */
    public final ElementalsGIM attackerElement;

    /** 后手附着的元素量 */
    public final float attackerQuantity;

    /** 后手附着的来源 */
    public final AttachmentSource attackerSource;

    /** 后手附着的附着参数（用于残留判断） */
    public final AttachmentProfile attackerProfile;

    /** 本次附着对应的伤害规格（增幅反应要用到） */
    public final ModDamageSpec damageSpec;

    /** 伤害源实体（攻击者） */
    public final Entity attackerEntity;

    /** 目标身上的状态容器（反应要消耗里面的元素） */
    public final StatusContainer targetContainer;

    public ReactionContext(LivingEntity target,
                           ElementalsGIM attackerElement, float attackerQuantity,
                           AttachmentSource attackerSource, AttachmentProfile attackerProfile,
                           ModDamageSpec damageSpec, Entity attackerEntity,
                           StatusContainer targetContainer) {
        this.target = target;
        this.attackerElement = attackerElement;
        this.attackerQuantity = attackerQuantity;
        this.attackerSource = attackerSource;
        this.attackerProfile = attackerProfile;
        this.damageSpec = damageSpec;
        this.attackerEntity = attackerEntity;
        this.targetContainer = targetContainer;
    }

    /** 后手元素是否遵循"后手不残留"规则 */
    public boolean attackerFollowsNoResidualRule() {
        return attackerSource == AttachmentSource.NORMAL_ATTACK;
    }
}
