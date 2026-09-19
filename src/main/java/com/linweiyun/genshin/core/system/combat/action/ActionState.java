package com.linweiyun.genshin.core.system.combat.action;

import com.mojang.logging.LogUtils;
import lombok.Getter;
import org.slf4j.Logger;

import java.util.function.Consumer;

/**
 * 运行时状态机 —— 管理一次动作的阶段推进。
 * <p>
 * 时序示例（precast=3, active=1, postcast=2）：
 * <pre>
 * tick 0 : PRECAST (onPrecastStart 已调)  phaseTicks=1
 * tick 1 : PRECAST                        phaseTicks=2
 * tick 2 : PRECAST → ACTIVE (onActiveStart) phaseTicks=0
 * tick 3 : ACTIVE → POSTCAST (onActiveTick, onActiveEnd, onPostcastStart)
 * tick 4 : POSTCAST                       phaseTicks=1
 * tick 5 : POSTCAST → IDLE (onComplete)   结束
 * </pre>
 */
public class ActionState {
    public static final Logger LOGGER = LogUtils.getLogger();
    @Getter
    private final ActionDefinition definition;
    @Getter
    private final ActionContext context;
    @Getter
    private ActionPhase phase;
    private int phaseTicks;
    @Getter
    private boolean finished;

    public ActionState(ActionDefinition definition, ActionContext context) {
        this.definition = definition;
        this.context = context;
        this.phase = ActionPhase.PRECAST;
        this.phaseTicks = 0;
        this.finished = false;
        context.setPhase(ActionPhase.PRECAST);
        fire(definition.getOnPrecastStart());

        if (definition.precastTicks <= 0) {
            transitionTo(ActionPhase.ACTIVE);
        }
    }

    public void tick() {
        if (finished) return;
        context.tickTotal();
        context.tickPhase();
        phaseTicks++;

        switch (phase) {
            case PRECAST -> {
                if (phaseTicks >= definition.precastTicks) {
                    transitionTo(ActionPhase.ACTIVE);
                }
            }
            case ACTIVE -> {
                fire(definition.getOnActiveTick());
                if (phaseTicks >= definition.activeTicks) {
                    transitionTo(ActionPhase.POSTCAST);
                }
            }
            case POSTCAST -> {
                if (phaseTicks >= definition.postcastTicks) {
                    transitionTo(ActionPhase.IDLE);
                }
            }
            default -> {}
        }
    }

    private void transitionTo(ActionPhase next) {
        if (phase == ActionPhase.ACTIVE) fire(definition.getOnActiveEnd());

        phase = next;
        phaseTicks = 0;
        context.setPhase(next);
        context.resetPhaseElapsed();

        switch (next) {
            case ACTIVE -> {
                fire(definition.getOnActiveStart());
                if (definition.activeTicks <= 0) transitionTo(ActionPhase.POSTCAST);
            }
            case POSTCAST -> {
                fire(definition.getOnPostcastStart());
                if (definition.postcastTicks <= 0) transitionTo(ActionPhase.IDLE);
            }
            case IDLE -> {
                fire(definition.getOnComplete());
                finished = true;
            }
            default -> {}
        }
    }

    public void interrupt(InterruptReason reason) {
        if (finished) return;
        context.markInterrupted(reason);
        fire(definition.getOnInterrupt());
        finished = true;
    }

    private void fire(Consumer<ActionContext> hook) {
        if (hook == null) return;
        try {
            hook.accept(context);
        } catch (Exception ignored) {
            // TODO: log
        }
    }

}