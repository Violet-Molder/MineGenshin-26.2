package com.linweiyun.genshin.core.system.about;

import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.system.reaction.ElementalReactionType;
import net.minecraft.world.entity.LivingEntity;

/**
 * 生物侧的元素规则接口 —— 由 mixin 注入到 {@code LivingEntity}，怪物可覆盖。
 *
 * <p>真正的宿主抽象是 {@link com.linweiyun.genshin.core.system.about.host.ElementalHost}：
 * {@code EntityHost} 会把两段筛查委托到这里，所以生物只需要在本接口上表达规则，
 * 方块那套则实现在 {@code BlockHost} 上 —— 两边的规则最终汇进同一条附着入口。
 *
 * <p>本接口回答三个互相独立的问题：<b>这次附着收不收</b>（{@link #onAttachElement}）、
 * <b>这个反应能不能发生</b>（{@link #onReactElement}）、<b>这个元素伤害吃不吃</b>
 * （{@link #isImmuneToElementDamage}）。拆开的意义：拒收附着不该顺带拒收伤害侧之外的东西，
 * 免疫伤害也不该顺带吞掉附着。
 *
 * <p>默认实现（mixin）恒 {@code true}，即普通生物什么都收、什么都反应。
 */
public interface ElementalAttachable {

    /**
     * 第一段筛查：这次附着收不收。语义同 {@code ElementalHost#acceptsElement}。
     * @return true 允许附着，false 拒绝
     */
    boolean onAttachElement(GenshinElement element, AttachmentSource source, AttachmentProfile profile);

    /**
     * 第二段筛查：某个反应能不能发生在这个生物身上。
     *
     * <p>用于表达「允许挂水、但不接受冻结」这类规则：返回 {@code false} 时该反应被跳过，
     * 先手元素保留（共存），不会生成冻元素。默认全允许。
     */
    default boolean onReactElement(GenshinElement attackerElement,
                                   GenshinElement defenderElement,
                                   ElementalReactionType reactionType) {
        return true;
    }

    /**
     * <b>伤害侧</b>（不是筛查）：吃不吃这个元素造成的伤害。
     *
     * <p>它和「收不收附着」是两件事，必须分开：免疫的单位照样被挂上元素、照样参与反应，
     * 只是这一下伤害按 0 结算。元素生物（{@code ElementalCreature}）用它表达
     * 「挂冰但完全不受冰影响」。
     *
     * <p>以前这份免疫写在 {@code TeyvatMonster/TeyvatSlime.hurtServer} 的提前 {@code return false} 里，
     * 而附着恰好是在伤害管线<b>内部</b>做的 —— 于是「免疫伤害」被扩大成了「连附着都不会发生」，
     * 想表达「允许挂水、但不接受冻结」也没有地方可写。
     */
    default boolean isImmuneToElementDamage(GenshinElement element) {
        return false;
    }

    /** 便捷判定：这个实体（可能没有实现本接口）免疫这个元素的伤害吗。 */
    static boolean isImmuneToDamage(LivingEntity entity, GenshinElement element) {
        return entity instanceof ElementalAttachable attachable
                && attachable.isImmuneToElementDamage(element);
    }
}
