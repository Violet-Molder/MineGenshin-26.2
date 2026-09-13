package com.linweiyun.genshin.core.system.about;

/**
 * 附着来源 —— 决定"谁和谁能相互覆盖"
 *
 * 只有同元素（GenshinElement 匹配）+ 同来源（本枚举匹配）的两份附着才能发生覆盖。
 * 不同来源的同元素是容器里的两个独立 StatusInstance，互不干扰。
 *
 * 例子：可莉普攻火（NORMAL_ATTACK）和元素试炼仪火（SPECIAL）同时挂在一个目标身上，
 * 它们不会互相覆盖——因为 source 不同。
 *
 * 除了 NORMAL_ATTACK 以外的所有来源都是"直接附着"：
 *   - 无附着损耗（lossMultiplier = 1.0）
 *   - 不遵循后手不残留规则
 */
public enum AttachmentSource {

    /**
     * 角色攻击造成的常规附着
     * - 常规附着作为先手时有 20% 损耗（×0.8）
     * - 触发不共存反应时遵循后手不残留规则
     */
    NORMAL_ATTACK,

    /**
     * 环境附着（水渊、荆棘方块、元素方碑等）
     * - 无损耗
     * - 通常是恒定附着（不衰减，被消耗后周期补充）
     */
    ENVIRONMENTAL,

    /**
     * 武器元素附魔（重云E冰附魔、班尼特Q火附魔等）
     * - 无损耗
     * - 参数由天赋等级决定
     */
    WEAPON_ENCHANT,

    /**
     * 角色自附着（班尼特Q给队友的火等）
     * - 无损耗
     * - 参数完全由天赋决定
     */
    SELF_ATTACH,

    /**
     * 特殊附着（激元素、冻元素、燃元素、元素试炼仪等）
     * - 无损耗
     * - 有各自专属的衰减规律
     * - 不遵循后手不残留规则
     */
    SPECIAL;
}