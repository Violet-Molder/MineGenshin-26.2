package com.linweiyun.genshin.core.character.talent;

import java.util.List;

public class TalentBase {
    protected final int maxCombo;
    protected final List<Integer> precastDurations;
    protected final List<Integer> postcastDurations;
    protected final List<Integer> windowDurations;

    public TalentBase(int maxCombo, List<Integer> precastDurations,
                      List<Integer> postcastDurations, List<Integer> windowDurations) {
        this.maxCombo = maxCombo;
        this.precastDurations = precastDurations;
        this.postcastDurations = postcastDurations;
        this.windowDurations = windowDurations;
    }

    public int getMaxCombo() {
        return maxCombo;
    }

    public int getPrecastTicks(int stage) {
        if (stage >= 0 && stage < precastDurations.size()) {
            return precastDurations.get(stage);
        }
        return 0;
    }

    public int getPostcastTicks(int stage) {
        if (stage >= 0 && stage < postcastDurations.size()) {
            return postcastDurations.get(stage);
        }
        return 0;
    }

    public int getWindowTicks(int stage) {
        if (stage >= 0 && stage < windowDurations.size()) {
            return windowDurations.get(stage);
        }
        return 0;
    }
}