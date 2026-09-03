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
 *
 * @param attackerElement  后手附着的元素（触发反应的那一个）
 * @param attackerUnit 后手附着的元素量
 * @param attackerSource   后手附着的来源
 * @param attackerProfile  后手附着的附着参数（用于残留判断）
 * @param damageSpec       本次附着对应的伤害规格（增幅反应要用到）
 * @param attackerEntity   伤害源实体（攻击者）
 * @param targetContainer  目标身上的状态容器（反应要消耗里面的元素）
 */
public record ReactionContext(ElementalsGIM attackerElement, float attackerUnit,
                              AttachmentSource attackerSource, AttachmentProfile attackerProfile,
                              ModDamageSpec damageSpec, Entity attackerEntity, StatusContainer targetContainer) {

    /**
     * 后手元素是否遵循"后手不残留"规则
     */
    public boolean attackerFollowsNoResidualRule() {
        return attackerSource == AttachmentSource.NORMAL_ATTACK;
    }
}
