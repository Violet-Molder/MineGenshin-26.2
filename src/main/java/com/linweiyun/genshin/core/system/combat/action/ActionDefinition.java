package com.linweiyun.genshin.core.system.combat.action;

import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData.ActionStep;
import lombok.Getter;

import java.util.function.Consumer;

/**
 * 一次动作的完整定义。不可变，用 Builder 构建。
 * <p>
 * 时序来自 {@link ActionStep}（CharacterActionData），不再自行定义前摇/执行/后摇。
 * 回调链：onActiveStart（伤害/特效）→ onComplete（收尾）→ onInterrupt（打断清理）。
 */
public class ActionDefinition {
    public final ActionKind kind;
    public final int comboIndex;

    /** 数据驱动的时序配置（动画名/时长/攻击点/保护期/连击窗口） */
    public final ActionStep step;

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
        this.onActiveStart = b.onActiveStart;
        this.onComplete = b.onComplete;
        this.onInterrupt = b.onInterrupt;
    }

    public boolean isCombo() { return kind.isCombo(); }

    public String animationName() { return step != null ? step.animation : "default"; }

    public int totalDuration() { return step != null ? step.duration : 1; }

    public int protectDuration() { return step != null ? step.protectDuration : 0; }

    public int comboWindow() { return step != null ? step.comboWindow : 0; }

    public static Builder builder(ActionKind kind) { return new Builder(kind); }

    public static final class Builder {
        private final ActionKind kind;
        private int comboIndex = -1;
        private ActionStep step;

        private Consumer<ActionContext> onActiveStart;
        private Consumer<ActionContext> onComplete;
        private Consumer<ActionContext> onInterrupt;

        private Builder(ActionKind kind) { this.kind = kind; }

        public Builder comboIndex(int i)          { this.comboIndex = i; return this; }
        public Builder step(ActionStep s)         { this.step = s; return this; }

        public Builder onActiveStart(Consumer<ActionContext> h)  { this.onActiveStart = h; return this; }
        public Builder onComplete(Consumer<ActionContext> h)     { this.onComplete = h; return this; }
        public Builder onInterrupt(Consumer<ActionContext> h)    { this.onInterrupt = h; return this; }

        public ActionDefinition build() { return new ActionDefinition(this); }
    }
}