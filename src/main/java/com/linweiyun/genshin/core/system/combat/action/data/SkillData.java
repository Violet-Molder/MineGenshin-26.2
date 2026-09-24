package com.linweiyun.genshin.core.system.combat.action.data;



    public final class SkillData {
        private final ActionStep tap;
        private final ActionStep hold;

        public SkillData(ActionStep tap, ActionStep hold) {
            this.tap = tap;
            this.hold = hold;
        }

        public ActionStep tap() { return tap; }
        public ActionStep hold() { return hold; }
        public boolean hasHold() { return hold != null; }
    }
