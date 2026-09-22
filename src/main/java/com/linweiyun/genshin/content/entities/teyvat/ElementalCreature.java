package com.linweiyun.genshin.content.entities.teyvat;

import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import net.minecraft.world.damagesource.DamageSource;
import org.jetbrains.annotations.Nullable;

/**
 * <b>元素生物</b> —— 史莱姆、无相之冰这类「本身就是某个元素」的敌人。
 *
 * <h2>统一规则</h2>
 * <ul>
 *   <li>生物记录自己的元素（{@link #getCreatureElement()}）；</li>
 *   <li><b>永久免疫同元素伤害</b>：大型冰史莱姆吃冰元素伤害永远是 0，
 *       不管这个伤害来自角色技能、反应还是环境。</li>
 * </ul>
 *
 * <p>免疫判定走 {@link #isImmuneTo(GenshinElement)}，用<b>主元素</b>比较 ——
 * 所以冰史莱姆同时也免疫「冻」元素（{@code FROZEN} 的主元素是 {@code CYRO}）。
 *
 * <p>这个接口只负责「记录 + 免疫」。它不负责套盾、不负责移动，
 * 那些是 {@link TeyvatLiving} 和实体自己的事。
 */
public interface ElementalCreature extends TeyvatLiving {

    /** 这个生物的元素。 */
    //TEMP
    GenshinElement getCreatureElement();

    /**
     * 是不是免疫这个元素。
     *
     * <p>默认实现：同主元素即免疫。子类可以放宽（比如某个精英怪只免疫 50%，
     * 那就别用这个接口，自己在 {@code hurtServer} 里削伤害）。
     */
    //TEMP
    default boolean isImmuneTo(@Nullable GenshinElement element) {
        if (element == null) {
            return false;
        }
        GenshinElement own = getCreatureElement();
        return own != null && element.getMainElement() == own.getMainElement();
    }

    /** 从伤害源上取元素再判断；取不到元素的（原版物理伤害）一律不免疫。 */
    //TEMP
    default boolean isImmuneToSource(DamageSource source) {
        if (source instanceof ModDamageSource modSource && modSource.getSpec() != null) {
            return isImmuneTo(modSource.getSpec().getElement());
        }
        return false;
    }
}
