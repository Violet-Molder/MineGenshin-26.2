package com.linweiyun.genshin.core.character.polearm.arlecchino;

import com.linweiyun.genshin.core.character.talent.TalentBase;

import java.util.List;

public class ArlecchinoTalent extends TalentBase {

    private static final float[] COMBO_MULTIPLIERS = {0.41f, 0.42f, 0.55f, 0.35f, 0.68f};

    public ArlecchinoTalent(int maxCombo, List<Integer> precastDurations, List<Integer> postcastDurations, List<Integer> windowDurations) {
        super(maxCombo, precastDurations, postcastDurations, windowDurations);
    }
}
