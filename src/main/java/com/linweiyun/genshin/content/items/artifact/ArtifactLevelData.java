package com.linweiyun.genshin.content.items.artifact;

import com.linweiyun.genshin.config.artifact.ArtifactLevelConfig;

public class ArtifactLevelData {

    public static long getExpToNextLevel(int star, int currentLevel) {
        if (currentLevel >= getMaxLevel(star)) return 0;
        return switch (Math.min(star, 5)) {
            case 1, 2, 3 -> ArtifactLevelConfig.get3Star(currentLevel);
            case 4 -> ArtifactLevelConfig.get4Star(currentLevel);
            case 5 -> ArtifactLevelConfig.get5Star(currentLevel);
            default -> 0;
        };
    }

    public static int getMaxLevel(int star) {
        return star <= 2 ? 4 : 4 * star;
    }
}