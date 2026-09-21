package com.linweiyun.genshin.core.system.combat.action;

import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData.ActionStep;
import lombok.Getter;

import java.util.function.Consumer;

/**
 * 一次动作的完整定义。不可变，用 Builder 构建。
 * <p>
 * 时序来自 {@link ActionStep}（CharacterActionData），不再自行定义前摇/执行/后摇。
 * 回调链：onCastStart（触发那一刻）→ onActiveStart（伤害点）→ onComplete（收尾）→ onInterrupt（打断清理）。
 */
public class ActionDefinition {
    public final ActionKind kind;
    public final int comboIndex;

    /** 数据驱动的时序配置（动画名/时长/攻击点/执行期/连击窗口） */
    public final ActionStep step;

    @Getter
    private final Consumer<ActionContext> onCastStart;
    @Getter
    private final Consumer<ActionContext> onActiveStart;
    @Getter
    private final Consumer<ActionContext> onComplete;
    @Getter
    private final Consumer<ActionContext> onInterrupt;

    private ActionDefinition(Builder b) {
        this.kind = b.kind;
        this.comboIndex = b.comboIndex >= 0 ? b.comboIndex : -1;
        this.step = b.step;
        this.onCastStart = b.onCastStart;
        this.onActiveStart = b.onActiveStart;
        this.onComplete = b.onComplete;
        this.onInterrupt = b.onInterrupt;
    }

    public boolean isCombo() { return kind.isCombo(); }

    public String animationName() { return step != null ? step.animation : "default"; }

    public int totalDuration() { return step != null ? step.duration : 1; }

    public int protectDuration() { return step != null ? step.protectDuration : 0; }

    /** 准备阶段长度（刻）—— 见 {@link ActionStep#prepareTicks}。 */
    public int prepareTicks() { return step != null ? step.prepareTicks : 0; }

    public int comboWindow() { return step != null ? step.comboWindow : 0; }

    public static Builder builder(ActionKind kind) { return new Builder(kind); }

    public static final class Builder {
        private final ActionKind kind;
        private int comboIndex = -1;
        private ActionStep step;

        private Consumer<ActionContext> onCastStart;
        private Consumer<ActionContext> onActiveStart;
        private Consumer<ActionContext> onComplete;
        private Consumer<ActionContext> onInterrupt;

        private Builder(ActionKind kind) { this.kind = kind; }

        public Builder comboIndex(int i)          { this.comboIndex = i; return this; }
        public Builder step(ActionStep s)         { this.step = s; return this; }

        /**
         * <b>触发那一刻</b>（动作刚开始，早于任何伤害点）回调一次。
         *
         * <p>和 {@link #onActiveStart} 的分工：那个是「伤害/特效在动画的哪一帧」，
         * 这个是「这一招<b>已经放出来了</b>」—— 换姿态、开模式、扣资源这类
         * <b>触发即生效</b>的东西放这里，否则会出现
         * 「CD 已经转了，但前摇被打断，模式没进去」。
         */
        public Builder onCastStart(Consumer<ActionContext> h)    { this.onCastStart = h; return this; }
        public Builder onActiveStart(Consumer<ActionContext> h)  { this.onActiveStart = h; return this; }
        public Builder onComplete(Consumer<ActionContext> h)     { this.onComplete = h; return this; }
        public Builder onInterrupt(Consumer<ActionContext> h)    { this.onInterrupt = h; return this; }

        public ActionDefinition build() { return new ActionDefinition(this); }
    }
}