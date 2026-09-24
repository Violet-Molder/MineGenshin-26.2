package com.linweiyun.genshin.core.system.combat.action.data;



    public final class Hit {
        public final int delay;          // 延时（刻）
        public final double forward;     // 向前距离
        public final double yOffset;     // Y 偏移
        public final double damage;      // 基础伤害倍率
        public final double damageSp;    // 特殊伤害倍率
        public final double scope;       // 伤害范围
        public final boolean ignoreInvuln; // 是否无视无敌

        public Hit(int delay, double forward, double yOffset, double damage,
                   double damageSp, double scope, boolean ignoreInvuln) {
            this.delay = delay;
            this.forward = forward;
            this.yOffset = yOffset;
            this.damage = damage;
            this.damageSp = damageSp;
            this.scope = scope;
            this.ignoreInvuln = ignoreInvuln;
        }
    }
