package com.linweiyun.genshin.config.util;

import net.neoforged.neoforge.common.ModConfigSpec;

public class StringDoubleValue {

    private final ModConfigSpec.ConfigValue<String> raw;
    private final double defaultValue;
    private final double min;
    private final double max;

    public StringDoubleValue(ModConfigSpec.ConfigValue<String> raw,
                             double defaultValue,
                             double min,
                             double max) {
        this.raw = raw;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
    }

    public double get() {
        try {
            String s = raw.get();
            if (s == null || s.isBlank()) return defaultValue;
            double v = Double.parseDouble(s.trim());
            return Math.max(min, Math.min(max, v));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // 新增：直接拿到 float
    public float getFloat() {
        return (float) get();
    }

    // 新增：直接拿到 int
    public int getInt() {
        return (int) get();
    }

    public void set(double value) {
        double v = Math.max(min, Math.min(max, value));
        raw.set(Double.toString(v));
    }

    public ModConfigSpec.ConfigValue<String> raw() {
        return raw;
    }

    public static StringDoubleValue defineInRange(ModConfigSpec.Builder builder,
                                                  String key,
                                                  double defaultValue,
                                                  double min,
                                                  double max) {
        ModConfigSpec.ConfigValue<String> raw = builder.define(key, Double.toString(defaultValue));
        return new StringDoubleValue(raw, defaultValue, min, max);
    }
}