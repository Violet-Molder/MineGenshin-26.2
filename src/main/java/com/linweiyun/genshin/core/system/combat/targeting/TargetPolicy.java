package com.linweiyun.genshin.core.system.combat.targeting;

import com.linweiyun.genshin.content.entities.teyvat.TeyvatLiving;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * 索敌筛选策略 —— 「这个东西能不能打」。
 *
 * <h2>默认策略（不给任何额外信息时用这个）</h2>
 * <ul>
 *   <li>✅ 敌对生物：{@link Enemy}（原版怪物）、{@link Mob} 里 {@code isAggressive()} 的、
 *       以及本 MOD 的 {@link TeyvatLiving} 里非友方的</li>
 *   <li>❌ 玩家（包括自己、队友、其它玩家）</li>
 *   <li>❌ 非生物实体（掉落物、船、盔甲架……）—— <b>除非</b>它被
 *       {@link #registerTargetableNonLiving} 登记过</li>
 *   <li>❌ 召唤物与弹射物自身</li>
 * </ul>
 *
 * <h2>特殊非生物目标</h2>
 * 元素方碑这类「不是生物但应该能锁定」的东西，等实现时注册一条就行：
 * <pre>
 * TargetPolicy.registerTargetableNonLiving(
 *         entity -&gt; entity instanceof ElementalMonument);
 * </pre>
 * 不需要改这里的任何逻辑。
 */
@FunctionalInterface
public interface TargetPolicy {

    /** 这个候选能不能被 {@code owner} 锁定。 */
    boolean isTargetable(Player owner, Entity candidate);

    /**
     * 打分：<b>越大越优先</b>。默认按「越近越好、正前方加分」算。
     *
     * @param distanceSq 候选到玩家的距离平方
     * @param facingDot  候选方向与玩家视线的点积（-1..1，1 = 正前方）
     */
    default double score(Player owner, Entity candidate, double distanceSq, double facingDot) {
        // 距离为主（归一化到 0..1），朝向给一点权重，避免锁到背后的东西
        double distanceScore = 1.0 / (1.0 + distanceSq);
        return distanceScore + Math.max(0.0, facingDot) * 0.5;
    }

    // ==================== 默认策略 ====================

    /** 「非生物但可被索敌」的登记表。 */
    List<Predicate<Entity>> TARGETABLE_NON_LIVING = new CopyOnWriteArrayList<>();

    /**
     * 登记一类「虽然是非生物、但应该能被锁定」的实体（元素方碑这类）。
     *
     * <p>默认策略会先放行这些，再走常规判断。
     */
    static void registerTargetableNonLiving(Predicate<Entity> predicate) {
        if (predicate != null) {
            TARGETABLE_NON_LIVING.add(predicate);
        }
    }

    /** 这个非生物实体是不是被登记过。 */
    static boolean isSpecialTargetableNonLiving(Entity entity) {
        for (Predicate<Entity> predicate : TARGETABLE_NON_LIVING) {
            if (predicate.test(entity)) {
                return true;
            }
        }
        return false;
    }

    /** 默认策略：敌对生物可以，玩家/非生物不行（登记过的除外）。 */
    TargetPolicy DEFAULT = (owner, candidate) -> {
        if (candidate == null || candidate == owner || !candidate.isAlive()) {
            return false;
        }
        if (candidate instanceof Player) {
            return false;
        }
        // 召唤物自身、弹射物不做目标
        if (candidate instanceof Projectile) {
            return false;
        }

        if (candidate instanceof LivingEntity living) {
            return isHostile(owner, living);
        }

        // 非生物：只有登记过的才算（元素方碑这类）
        return isSpecialTargetableNonLiving(candidate);
    };

    /** 敌对判定：原版 Enemy、主动攻击型 Mob、或本 MOD 的提瓦特生物。 */
    static boolean isHostile(Player owner, LivingEntity living) {
        if (living instanceof Enemy) {
            return true;
        }
        if (living instanceof Mob mob) {
            return mob.getTarget() == owner || mob.isAggressive();
        }
        // 本 MOD 的提瓦特实体都是怪物阵营；将来若有友方单位，在这里加白名单即可
        return living instanceof TeyvatLiving;
    }
}
