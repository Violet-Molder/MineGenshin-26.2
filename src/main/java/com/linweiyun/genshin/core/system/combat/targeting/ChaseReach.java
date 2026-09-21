package com.linweiyun.genshin.core.system.combat.targeting;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 「这一次追击能不能把目标带进攻击距离」—— 索敌的可行性判定。
 *
 * <h2>要解决的两个现象</h2>
 * <ol>
 *   <li><b>隔着方块还去追</b>：目标在墙后 / 楼上 / 地板下，距离在索敌范围内，
 *       于是玩家一路撞墙撞到超时，伤害也没打出来。</li>
 *   <li><b>高度差</b>：飞到别人头上够不着；更常见的是目标就在<b>地板下面</b>——
 *       人冲过去只会停在地板上方干等他，因为高度差永远补不平。</li>
 * </ol>
 *
 * <h2>判定方式：把「身体能不能过去」夹出来</h2>
 * 从玩家身上取两根采样线（脚、腰）射向目标。任一条被方块挡住，就说明身体过不去；
 * 这时再看<b>被拦下的位置离目标多远</b>：
 *
 * <pre>
 * 一路通畅            → 追得上（能贴到目标身前）
 * 被拦下，但拦停点     → 也追得上：冲过去撞墙就停在那里，那个位置本来就够得着
 *   离目标 ≤ 攻击距离
 * 被拦下且够不着       → 追不上，不锁、不追（这才是「隔了两三个方块就别追」）
 * </pre>
 *
 * <p>第二条是刻意的：<b>「被墙拦停」不等于「追不上」</b>，
 * 只要拦停的地方已经在攻击距离内，那一次冲撞本身就是有效的接近。
 *
 * <h2>纵向也要卡一道</h2>
 * 「足够近」是用直线距离算的，而直线距离对<b>垂直方向</b>特别宽松：
 * 你站在地板上、目标在地板下 2 格，直线距离只有 2 格 —— 看着够得着，其实中间隔着地板。
 * 所以拦停点还要过一道高度检查：<b>拦停点到目标的垂直落差 &gt; 攻击距离 × {@value #VERTICAL_REACH_FACTOR}</b>
 * 就不算追得上。地板下、天花板上的目标都会被这一条挡掉。
 *
 * <p>另外有一条<b>一票否决</b>：目标在你脚下、而且真正拦住你的是它<b>头顶那层方块</b>
 * （房子 1L 地板 / -1L 刷怪那种）→ 完整隔断，连锁都不该锁。
 * 判据见 {@link #isSealedBelow}，它只看「目标正上方到拦停点之间有没有实心方块」，
 * 所以站在悬崖上打崖下的怪不会被误伤。
 *
 * <p>只做<b>方块</b>判定（{@code Block.COLLIDER}）：草、水、火把这些不挡路的东西不参与，
 * 其他生物也不参与 —— 怪挡怪不该让人放弃追击。
 *
 * <h2>什么时候才该问它</h2>
 * 只有<b>会突进的招式</b>才需要（{@link CombatTargeting.Params#chase()}）。
 * 远程招式的索敌距离 = 攻击距离，本来就不追人，隔着墙也照样能锁（那是射击的事）。
 */
public final class ChaseReach {

    /**
     * 拦停点到目标的高度落差容差 = 攻击距离 × 这个系数。
     *
     * <p>0.75 而不是 1.0：直线距离会低估纵向的阻隔（见类注释），
     * 卡紧一点才能把「地板下面 2~3 格」这种典型情况挡在索敌之外。
     */
    public static final double VERTICAL_REACH_FACTOR = 0.75;

    /** 拦停点到目标的距离允许比攻击距离多这么多格（身体有宽度，射线有误差）。 */
    public static final double STOP_SLACK = 0.5;

    /**
     * 身体采样高度（相对脚底，格）：脚 + 腰。
     *
     * <p>只打一条「脚 → 脚」的射线会从 1 格高的缝里穿过去，而身体其实过不去；
     * 加一条腰的高度能挡住绝大多数「看着通、其实钻不过」的情况。
     */
    private static final double[] BODY_SAMPLES = {0.2, 1.2};

    private ChaseReach() {
    }

    /**
     * 这一次追击能不能把目标带进攻击距离。
     *
     * @param attackRange 这一招的生效攻击距离（格）
     * @return {@code true} = 值得追（含「撞墙停下也够得着」）；{@code false} = 别锁、别追
     */
    public static boolean canCloseIn(Player player, LivingEntity target, double attackRange) {
        double range = Math.max(0.5, attackRange);

        Vec3 feet = player.position();
        Vec3 goal = target.position();

        // 已经在攻击距离内：连突进都不用，挡不挡墙都无所谓
        if (feet.distanceTo(goal) <= range) {
            return true;
        }

        for (double offset : BODY_SAMPLES) {
            Vec3 from = feet.add(0, offset, 0);
            Vec3 to = goal.add(0, offset, 0);

            BlockHitResult hit = player.level().clip(new ClipContext(
                    from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

            if (hit.getType() == HitResult.Type.MISS) {
                continue; // 这一层通得过
            }

            // ★ 目标在脚下、而且「头顶那层实心方块」才是拦停它的东西 → 完整隔断。
            //   典型：房子 1L 地板、-1L 两格高的小间里刷了怪 —— 它就在你正下方，
            //   中间隔着一整层地板。这种连锁都不该锁（打不到，追过去也只是站在地板上方干等）。
            if (isSealedBelow(player, target, hit.getLocation())) {
                return false;
            }

            if (isReachableFrom(hit.getLocation(), goal, range)) {
                continue; // 被拦下了，但拦停的位置本来就够得着 → 照追
            }

            return false;
        }

        return true;
    }

    /**
     * 目标是不是被「盖」在你脚下 —— 拦停点明显高于目标，且目标头顶到拦停点之间是实心方块。
     *
     * <p>判据分两步，故意都很保守，避免误杀：
     * <ol>
     *   <li>拦停点不比目标高多少（≤ 0.5 格）→ 那只是「旁边有堵墙」，不是「头上有盖子」，直接放过；</li>
     *   <li>从目标头顶往上打到拦停点下方：打不到方块 → 放过（说明它只是贴着墙，绕一下就能打）。</li>
     * </ol>
     *
     * <p>「站在悬崖上、目标在崖下」这种不会被误判：崖壁在它<b>旁边</b>，
     * 它头顶到崖顶那条竖线是空的 → 第 2 步打不到方块 → 照常走后面的「拦停点够不够得着」判断。
     */
    private static boolean isSealedBelow(Player player, LivingEntity target, Vec3 stopPoint) {
        if (stopPoint.y <= target.getY() + 0.5) {
            return false; // 拦停点就在目标这一层 → 是墙不是盖子
        }

        Vec3 from = target.position().add(0, target.getBbHeight() + 0.05, 0);
        Vec3 to = new Vec3(target.getX(), stopPoint.y - 0.05, target.getZ());
        if (from.y >= to.y) {
            return false; // 目标比拦停点还高（基本不会发生），不判
        }

        BlockHitResult hit = player.level().clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() != HitResult.Type.MISS;
    }

    /** 从被拦下的位置能不能打到目标：距离够近，而且高度落差没超过容差。 */
    private static boolean isReachableFrom(Vec3 stopPoint, Vec3 target, double attackRange) {
        if (stopPoint.distanceTo(target) > attackRange + STOP_SLACK) {
            return false;
        }
        return Math.abs(stopPoint.y - target.y) <= attackRange * VERTICAL_REACH_FACTOR;
    }
}
