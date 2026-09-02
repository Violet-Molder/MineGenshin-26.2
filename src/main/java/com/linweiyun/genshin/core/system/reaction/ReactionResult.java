package com.linweiyun.genshin.core.system.reaction;

import com.linweiyun.genshin.enums.ElementalReactionType;

/**
 * 元素反应执行结果
 */
public class ReactionResult {

    private final ElementalReactionType reactionType;
    private final boolean reacted;

    // 消耗信息
    private final float consumedAttacker;
    private final float consumedDefender;

    // 后手残留（来自 SPECIAL/SELF_ATTACH 等不遵循后手不残留规则的来源）
    private final float attackerResidual;

    // 增幅反应专用
    private final boolean isAmplified;
    private final float amplifyMultiplier;

    private ReactionResult(Builder builder) {
        this.reactionType = builder.reactionType;
        this.reacted = builder.reacted;
        this.consumedAttacker = builder.consumedAttacker;
        this.consumedDefender = builder.consumedDefender;
        this.attackerResidual = builder.attackerResidual;
        this.isAmplified = builder.isAmplified;
        this.amplifyMultiplier = builder.amplifyMultiplier;
    }

    // ========== Getter ==========

    public ElementalReactionType getReactionType() { return reactionType; }
    public boolean isReacted() { return reacted; }
    public float getConsumedAttacker() { return consumedAttacker; }
    public float getConsumedDefender() { return consumedDefender; }
    public float getAttackerResidual() { return attackerResidual; }
    public boolean isAmplified() { return isAmplified; }
    public float getAmplifyMultiplier() { return amplifyMultiplier; }

    public static Builder builder(ElementalReactionType type) {
        return new Builder(type);
    }

    public static class Builder {
        private final ElementalReactionType reactionType;
        private boolean reacted = false;
        private float consumedAttacker = 0f;
        private float consumedDefender = 0f;
        private float attackerResidual = 0f;
        private boolean isAmplified = false;
        private float amplifyMultiplier = 1.0f;

        public Builder(ElementalReactionType type) { this.reactionType = type; }

        public Builder reacted() { this.reacted = true; return this; }
        public Builder consumedAttacker(float v) { this.consumedAttacker = v; return this; }
        public Builder consumedDefender(float v) { this.consumedDefender = v; return this; }
        public Builder attackerResidual(float v) { this.attackerResidual = v; return this; }
        public Builder amplified(float multiplier) {
            this.isAmplified = true;
            this.amplifyMultiplier = multiplier;
            return this;
        }

        public ReactionResult build() { return new ReactionResult(this); }
    }
}