package com.linweiyun.genshin.content.items.artifact;

import com.linweiyun.genshin.config.ArtifactConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class ArtifactLevelData {

    public static long getExpToNextLevel(int star, int currentLevel) {
        if (currentLevel >= getMaxLevel(star)) return 0;
        return switch (Math.min(star, 5)) {
            case 1, 2, 3 -> getIntAt(ArtifactConfig.EXP_3_STAR, currentLevel);
            case 4 -> getIntAt(ArtifactConfig.EXP_4_STAR, currentLevel);
            case 5 -> getIntAt(ArtifactConfig.EXP_5_STAR, currentLevel);
            default -> 0;
        };
    }

    public static int getMaxLevel(int star) {
        return star <= 2 ? 4 : 4 * star;
    }

    private static long getIntAt(ModConfigSpec.ConfigValue<List<? extends Integer>> config, int index) {
        List<? extends Integer> list = config.get();
        if (index < 0 || index >= list.size()) return 0;
        return list.get(index);
    }
}
