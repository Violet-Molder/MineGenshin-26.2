package com.linweiyun.genshin.config.character;

import java.util.ArrayList;
import java.util.List;

/**
 * 沃雅妮莎的成长表（1~100 级）。
 *
 * <h2>为什么不是 96 个 TOML 项</h2>
 * 别的角色（申鹤/哥伦比娅）是每个等级一条 {@code defineInRange}，一共近 300 条。
 * 沃雅妮莎这边只拿到了<b>里程碑等级</b>的数值（1 / 20 / 20+ / 40 / 40+ / … / 90+ / 100），
 * 所以这里按里程碑做<b>线性插值</b>生成逐级数值：
 * 里程碑那一级的值就是表里的原值，中间的等级在相邻两个里程碑之间均分。
 *
 * <p>{@code 20+} 这种「突破后」的行记在 {@code 21} 级的位置上（突破后的第一级）——
 * 和游戏里「突破后基础属性会跳一档」的表现一致。
 *
 * <p>想要逐级精确值/可配置的话，照 {@code ShenheAttributeConfig} 那样展开成 TOML 即可，
 * 数值接口（{@link #getAllHp()} 等）不用改。
 */
public final class VodyanitsaAttributeConfig {

    private VodyanitsaAttributeConfig() {
    }

    /** 里程碑等级（{@code 20+} 记在 21 级、{@code 40+} 记在 41 级…）。 */
    private static final int[] LEVELS = {
            1, 20, 21, 40, 41, 50, 51, 60, 61, 70, 71, 80, 81, 90, 91, 100
    };

    private static final double[] HP = {
            1154, 2992, 3981, 5957, 6660, 7662, 8599, 9612, 10315, 11337, 12040, 13073, 13776, 14818, 14818, 15871
    };

    private static final double[] ATK = {
            8.38, 21.74, 28.92, 43.27, 48.38, 55.66, 62.46, 69.82, 74.92, 82.35, 87.45, 94.96, 100.06, 107.63,
            107.63, 131.85
    };

    private static final double[] DEF = {
            37.69, 97.78, 130.09, 194.66, 217.63, 250.38, 281.0, 314.09, 337.06, 370.45, 393.42, 427.19, 450.15,
            484.19, 484.19, 518.6
    };

    /** 等级上限（表和 {@code PGCharacter} 的等级系统一致是 90 级为常规上限，这里按表给到 100）。 */
    public static final int MAX_LEVEL = 100;

    public static List<Integer> getAllHp() {
        return build(HP);
    }

    public static List<Integer> getAllAtk() {
        return build(ATK);
    }

    public static List<Integer> getAllDef() {
        return build(DEF);
    }

    /** 按里程碑线性插值出 1~{@link #MAX_LEVEL} 级的一整条曲线。 */
    private static List<Integer> build(double[] milestones) {
        List<Integer> out = new ArrayList<>(MAX_LEVEL);
        for (int level = 1; level <= MAX_LEVEL; level++) {
            out.add((int) Math.floor(interpolate(level, milestones)));
        }
        return out;
    }

    private static double interpolate(int level, double[] milestones) {
        if (level <= LEVELS[0]) {
            return milestones[0];
        }
        for (int i = 1; i < LEVELS.length; i++) {
            if (level <= LEVELS[i]) {
                int fromLevel = LEVELS[i - 1];
                int toLevel = LEVELS[i];
                double from = milestones[i - 1];
                double to = milestones[i];
                double t = (double) (level - fromLevel) / (toLevel - fromLevel);
                return from + (to - from) * t;
            }
        }
        return milestones[milestones.length - 1];
    }
}
