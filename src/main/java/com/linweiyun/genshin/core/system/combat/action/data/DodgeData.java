package com.linweiyun.genshin.core.system.combat.action.data;



    public final class DodgeData {
        private final ActionStep step;

        public DodgeData(ActionStep step) {
            this.step = step;
        }

        public ActionStep step() { return step; }
    }
