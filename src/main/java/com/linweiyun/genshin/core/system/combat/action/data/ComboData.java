package com.linweiyun.genshin.core.system.combat.action.data;

import java.util.Collections;

import java.util.Map;

    public final class ComboData {
        private final int maxCombo;
        private final Map<Integer, ActionStep> steps;

        public ComboData(int maxCombo, Map<Integer, ActionStep> steps) {
            this.maxCombo = maxCombo;
            this.steps = steps == null ? Collections.emptyMap() : steps;
        }

        public int maxCombo() { return maxCombo; }
        public ActionStep getStep(int stage) { return steps.get(stage); }
    }
