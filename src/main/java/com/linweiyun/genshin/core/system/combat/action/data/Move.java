package com.linweiyun.genshin.core.system.combat.action.data;



    public final class Move {
        public final int delay;      // 延时（刻）
        public final double speed;   // 速度

        public Move(int delay, double speed) {
            this.delay = delay;
            this.speed = speed;
        }
    }
