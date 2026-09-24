package com.linweiyun.genshin.core.system.combat.action;
import com.linweiyun.genshin.core.system.combat.action.data.ActionStep;

import com.linweiyun.genshin.core.network.ActionServer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 服务端侧的"外部打断"监听器。
 * <p>
 * 客户端能主动发的打断（跳跃）已经通过 RPC 走 {@link ActionServer} 处理，
 * 但"受击 / 击退"这类由服务端物理或战斗系统触发的事件，客户端根本不知道，
 * 必须由服务端在这里监听并调用 {@link ActionManager#interrupt}。
 * <p>
 * 生效范围：
 * <ul>
 *   <li>执行期（{@code [prepareTicks, protectDuration)} 内）→ <b>忽略</b>，不打断</li>
 *   <li>其余时间（准备阶段 / 后摇）→ 打断，当前动作取消，连招窗口消失</li>
 * </ul>
 * <p>
 * 「执行期不可打断」这条是刻意的：位移和伤害点都在执行期里，
 * 放行的话就是「CD 扣了、能量没了、效果没出来」。
 * 准备阶段（{@code prepareTicks} 那段吟唱）不属于执行期，照样能被打断 ——
 * 这是「前摇分两种」的另一半，见 {@code ActionStep#protectDuration}。
 * <p>
 * 想额外加"击退打断"？复制一份 onKnockback，把事件换成
 * {@code net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent}，
 * 打断原因换成 {@link InterruptReason#KNOCKBACK} 即可。
 * 想加"其它自定义来源"？直接调用
 * {@code ActionManager.get(player).interrupt(InterruptReason.MANUAL)}。
 */
@EventBusSubscriber
public final class ActionInterruptHandler {

    private ActionInterruptHandler() {}

    @SubscribeEvent
    public static void onPlayerHurt(LivingIncomingDamageEvent event) {
        // 只处理玩家
        if (!(event.getEntity() instanceof Player player)) return;
        // 只在服务端处理（客户端由 @DescSynced 同步状态，不需要打断自己）
        if (player.level().isClientSide()) return;
        // 交给 ActionManager，内部会根据当前是否在执行期判断是否允许打断
        ActionManager.get(player).interrupt(InterruptReason.DAMAGE);
    }

    // ========================================================================
    // 【可选 · 需要时取消注释】击退打断
    // 想区分"被击退"和"普通受击"时打开。
    // 注意：LivingKnockBackEvent 有时会被伤害事件连带触发，
    //       两个都开的话同一帧会 interrupt 两次，第二次会因为 current 已 finished 而直接返回，
    //       无副作用。
    // ========================================================================
    //
    // @SubscribeEvent
    // public static void onKnockback(LivingKnockBackEvent event) {
    //     if (!(event.getEntity() instanceof Player player)) return;
    //     if (player.level().isClientSide()) return;
    //     ActionManager.get(player).interrupt(InterruptReason.KNOCKBACK);
    // }
}