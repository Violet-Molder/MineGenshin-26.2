package com.linweiyun.genshin.core.system.combat.action.data;



    public final class BurstData {
        private final ActionStep step;
        private final float energyCost;

        public BurstData(ActionStep step, float energyCost) {
            this.step = step;
            this.energyCost = energyCost;
        }

        public ActionStep step() { return step; }
        public float energyCost() { return energyCost; }
    }
