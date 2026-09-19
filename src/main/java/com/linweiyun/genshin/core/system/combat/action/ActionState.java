package com.linweiyun.genshin.core.system.combat.action;

import com.mojang.logging.LogUtils;
import lombok.Getter;
import org.slf4j.Logger;

import java.util.function.Consumer;

public class ActionState {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Getter private final ActionDefinition definition;
    @Getter private final ActionContext context;
    @Getter private ActionPhase phase;
    private int phaseTicks;
    @Getter private boolean finished;

    public ActionState(ActionDefinition definition, ActionContext context) {
        this.definition = definition;
        this.context = context;
        this.phase = ActionPhase.PRECAST;
        this.phaseTicks = 0;
        this.finished = false;
        context.setPhase(ActionPhase.PRECAST);
        LOGGER.info("[ActionState] START kind={} comboIndex={} precast={} active={} postcast={} side={}",
                definition.kind, definition.comboIndex,
                definition.precastTicks, definition.activeTicks, definition.postcastTicks,
                context.player.level().isClientSide() ? "CLIENT" : "SERVER");
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
                if (phaseTicks >= definition.precastTicks) transitionTo(ActionPhase.ACTIVE);
            }
            case ACTIVE -> {
                fire(definition.getOnActiveTick());
                if (phaseTicks >= definition.activeTicks) transitionTo(ActionPhase.POSTCAST);
            }
            case POSTCAST -> {
                if (phaseTicks >= definition.postcastTicks) transitionTo(ActionPhase.IDLE);
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

        LOGGER.info("[ActionState] PHASE -> {} kind={} side={}",
                next, definition.kind,
                context.player.level().isClientSide() ? "CLIENT" : "SERVER");

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
        LOGGER.info("[ActionState] INTERRUPT reason={} kind={} side={}",
                reason, definition.kind,
                context.player.level().isClientSide() ? "CLIENT" : "SERVER");
        context.markInterrupted(reason);
        fire(definition.getOnInterrupt());
        finished = true;
    }

    /**
     * 触发 hook。
     * null hook = 该阶段没有回调，静默跳过（不是错误）。
     */
    private void fire(Consumer<ActionContext> hook) {
        if (hook == null) return;
        try {
            hook.accept(context);
        } catch (Exception e) {
            LOGGER.error("[ActionState] hook threw kind={} phase={}", definition.kind, phase, e);
        }
    }
}