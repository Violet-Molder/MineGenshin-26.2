package com.linweiyun.genshin.core.system.combat.action;

import lombok.Getter;

import java.util.function.Consumer;

/**
 * 一次动作的完整定义。不可变，用 Builder 构建。
 * <p>
 * 3 个时长：
 * <ul>
 *     <li>precastTicks 前摇 —— 可为 0（立即进入 ACTIVE）</li>
 *     <li>activeTicks 执行期 —— 至少 1</li>
 *     <li>postcastTicks 后摇 —— 可为 0；后摇期间是连招窗口</li>
 * </ul>
 */
public class ActionDefinition {
    public final ActionKind kind;
    public final int comboIndex;      // 连招索引，非连招为 -1
    public final int precastTicks;
    public final int activeTicks;
    public final int postcastTicks;

    @Getter
    private final Consumer<ActionContext> onPrecastStart;
    @Getter
    private final Consumer<ActionContext> onActiveStart;
    @Getter
    private final Consumer<ActionContext> onActiveTick;
    @Getter
    private final Consumer<ActionContext> onActiveEnd;
    @Getter
    private final Consumer<ActionContext> onPostcastStart;
    @Getter
    private final Consumer<ActionContext> onComplete;
    @Getter
    private final Consumer<ActionContext> onInterrupt;

    private ActionDefinition(Builder b) {
        this.kind = b.kind;
        this.comboIndex = b.comboIndex;
        this.precastTicks = Math.max(0, b.precastTicks);
        this.activeTicks = Math.max(1, b.activeTicks);
        this.postcastTicks = Math.max(0, b.postcastTicks);
        this.onPrecastStart = b.onPrecastStart;
        this.onActiveStart = b.onActiveStart;
        this.onActiveTick = b.onActiveTick;
        this.onActiveEnd = b.onActiveEnd;
        this.onPostcastStart = b.onPostcastStart;
        this.onComplete = b.onComplete;
        this.onInterrupt = b.onInterrupt;
    }

    public boolean isCombo() { return kind.isCombo(); }
    public int getTotalDurationTicks() { return precastTicks + activeTicks + postcastTicks; }

    public static Builder builder(ActionKind kind) { return new Builder(kind); }

    public static final class Builder {
        private final ActionKind kind;
        private int comboIndex = -1;
        private int precastTicks = 0;
        private int activeTicks = 1;
        private int postcastTicks = 0;

        private Consumer<ActionContext> onPrecastStart;
        private Consumer<ActionContext> onActiveStart;
        private Consumer<ActionContext> onActiveTick;
        private Consumer<ActionContext> onActiveEnd;
        private Consumer<ActionContext> onPostcastStart;
        private Consumer<ActionContext> onComplete;
        private Consumer<ActionContext> onInterrupt;

        private Builder(ActionKind kind) { this.kind = kind; }

        public Builder comboIndex(int i)      { this.comboIndex = i; return this; }
        public Builder precast(int ticks)     { this.precastTicks = ticks; return this; }
        public Builder active(int ticks)      { this.activeTicks = ticks; return this; }
        public Builder postcast(int ticks)    { this.postcastTicks = ticks; return this; }

        public Builder onPrecastStart(Consumer<ActionContext> h)  { this.onPrecastStart = h; return this; }
        public Builder onActiveStart(Consumer<ActionContext> h)   { this.onActiveStart = h; return this; }
        public Builder onActiveTick(Consumer<ActionContext> h)    { this.onActiveTick = h; return this; }
        public Builder onActiveEnd(Consumer<ActionContext> h)     { this.onActiveEnd = h; return this; }
        public Builder onPostcastStart(Consumer<ActionContext> h) { this.onPostcastStart = h; return this; }
        public Builder onComplete(Consumer<ActionContext> h)      { this.onComplete = h; return this; }
        public Builder onInterrupt(Consumer<ActionContext> h)     { this.onInterrupt = h; return this; }

        public ActionDefinition build() { return new ActionDefinition(this); }
    }
}