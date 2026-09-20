package com.linweiyun.genshin.core.system.combat.action;

import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData.ActionStep;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData.Hit;
import com.mojang.logging.LogUtils;
import lombok.Getter;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * 动作运行时状态机。
 * <p>
 * 不再使用 PRECAST/ACTIVE/POSTCAST 阶段模型。
 * 改为基于 {@link ActionStep} 的 tick 计数 + hitDelay 驱动：
 * <ul>
 *     <li>每 tick 递增计数器</li>
 *     <li>在 hitDelay 指定的 tick 触发 onActiveStart 回调</li>
 *     <li>达到 totalDuration 则完成</li>
 * </ul>
 */
public class ActionState {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Getter private final ActionDefinition definition;
    @Getter private final ActionContext context;

    private final int totalDuration;
    private final int protectDuration;
    private final int[] hitDelays;
    private final BitSet hitsFired;

    /** 这个动作没有配 hits：{@code onActiveStart} 只在 tick 0 触发一次。 */
    private final boolean firesWithoutHits;

    private int tickCount;
    @Getter private boolean finished;

    public ActionState(ActionDefinition definition, ActionContext context) {
        this.definition = definition;
        this.context = context;
        this.totalDuration = definition.totalDuration();
        this.protectDuration = definition.protectDuration();

        ActionStep step = definition.step;
        if (step != null && step.hits != null) {
            List<Integer> delays = new ArrayList<>();
            for (Hit hit : step.hits) {
                delays.add(hit.delay);
            }
            this.hitDelays = delays.stream().mapToInt(Integer::intValue).toArray();
        } else {
            this.hitDelays = new int[0];
        }

        // 没有配 hits 的动作（例如闪避）没有触发点，补一个 tick 0 —— 否则 onActiveStart 永远不会被调用
        this.firesWithoutHits = this.hitDelays.length == 0;
        this.hitsFired = new BitSet(Math.max(1, hitDelays.length));

        this.tickCount = 0;
        this.finished = false;

        LOGGER.info("[ActionState] START kind={} comboIndex={} anim={} duration={} hits={} protect={} side={}",
                definition.kind, definition.comboIndex,
                definition.animationName(), totalDuration, hitDelays.length, protectDuration,
                context.player.level().isClientSide() ? "CLIENT" : "SERVER");

        // tick 0 触发伤害（delay=0 的 hit）
        checkHits();
    }

    public void tick() {
        if (finished) return;
        tickCount++;
        context.tickTotal();

        // 检查本 tick 是否有命中
        checkHits();

        // 时长到期 → 完成
        if (tickCount >= totalDuration) {
            finish();
            return;
        }

        // 保护期内不接新动作，由 ActionManager 检查
    }

    private void checkHits() {
        // 没有 hits 的动作：只在 tick 0 触发一次
        if (firesWithoutHits) {
            if (tickCount == 0 && !hitsFired.get(0)) {
                hitsFired.set(0);
                fire(definition.getOnActiveStart());
            }
            return;
        }

        for (int i = 0; i < hitDelays.length; i++) {
            if (!hitsFired.get(i) && tickCount >= hitDelays[i]) {
                hitsFired.set(i);
                fire(definition.getOnActiveStart());
            }
        }
    }

    private void finish() {
        LOGGER.info("[ActionState] FINISH kind={} comboIndex={} totalTicks={} side={}",
                definition.kind, definition.comboIndex, tickCount,
                context.player.level().isClientSide() ? "CLIENT" : "SERVER");
        fire(definition.getOnComplete());
        finished = true;
    }

    private void syncAnimationToClient() {
        // 已移除：动画由客户端状态机自己驱动，服务端不再回推动画名。
        //
        // 旧实现会在第一段伤害命中时给施法者自己发一个 syncAnimationRPCPacket，
        // 客户端收到后走 forceRequest(priority=99, 30 刻, 硬直 15, 定身 15)，
        // 把本地刚按下的动作整个覆盖掉 —— 表现就是「按下去没反应、必须等动画播完」。
        // 远端表现现在由 ActionStateMachine → NetworkManager.animationStateRPCPacket →
        // ANIMATION_STATE_ATTACHMENT 同步，见 ServerAnimationTicker。
    }

    public void interrupt(InterruptReason reason) {
        if (finished) return;
        LOGGER.info("[ActionState] INTERRUPT reason={} kind={} side={}",
                reason, definition.kind,
                context.player.level().isClientSide() ? "CLIENT" : "SERVER");
        context.markInterrupted(reason);
        fire(definition.getOnInterrupt());
        finished = true;
    }

    /** 当前保护期剩余 tick */
    public int protectRemaining() {
        return Math.max(0, protectDuration - tickCount);
    }

    /** 当前是否在保护期内 */
    public boolean isProtected() {
        return protectDuration > 0 && tickCount < protectDuration;
    }

    private void fire(Consumer<ActionContext> hook) {
        if (hook == null) return;
        try {
            hook.accept(context);
        } catch (Exception e) {
            LOGGER.error("[ActionState] hook threw kind={} tick={}", definition.kind, tickCount, e);
        }
    }
}