package com.linweiyun.genshin.content.attribute;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import java.util.HashMap;
import java.util.Map;

public class AttributeInstance implements IPersistedSerializable {
    private AttributeType type;

    @Persisted(key = "base_value")
    private double baseValue;

    @Persisted(key = "flat_modifiers")
    private final Map<String, Double> flatModifiers = new HashMap<>();

    @Persisted(key = "percent_modifiers")
    private final Map<String, Double> percentModifiers = new HashMap<>();

    @Persisted(key = "temp_flat_modifiers")
    private final Map<String, Double> tempFlatModifiers = new HashMap<>();

    @Persisted(key = "temp_percent_modifiers")
    private final Map<String, Double> tempPercentModifiers = new HashMap<>();
    public AttributeInstance() {}
    public AttributeInstance(AttributeType type) {
        this.type = type;
        this.baseValue = type.defaultValue();
    }

    public AttributeType getType() { return type; }

    // ========== 基础值 ==========
    public double getBaseValue() { return baseValue; }

    public void setBaseValue(double value) {
        this.baseValue = roundTo4(value);
    }

    public void addBaseValue(double value) {
        this.baseValue = roundTo4(this.baseValue + value);
    }

    // ========== 常驻固定值加成 ==========
    public void setFlatModifier(String source, double value) {
        flatModifiers.put(source, roundTo4(value));
    }

    public void addFlatModifier(String source, double value) {
        flatModifiers.merge(source, roundTo4(value), Double::sum);
    }

    public void removeFlatModifier(String source) {
        flatModifiers.remove(source);
    }

    public double getTotalFlatModifier() {
        return flatModifiers.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    // ========== 常驻百分比加成 ==========
    public void setPercentModifier(String source, double value) {percentModifiers.put(source, roundTo4(value));}
    public void addPercentModifier(String source, double value) {percentModifiers.merge(source, roundTo4(value), Double::sum);}
    public void removePercentModifier(String source) {
        percentModifiers.remove(source);
    }
    public double getTotalPercentModifier() {return percentModifiers.values().stream().mapToDouble(Double::doubleValue).sum();}
    public double getTotalPercentModifierDisplay() {return percentModifiers.values().stream().mapToDouble(Double::doubleValue).sum() * 100.0;}

    // ========== 临时固定值加成 ==========
    public void setTempFlatModifier(String source, double value) {tempFlatModifiers.put(source, roundTo4(value));}
    public void addTempFlatModifier(String source, double value) {tempFlatModifiers.merge(source, roundTo4(value), Double::sum);}

    public void removeTempFlatModifier(String source) {
        tempFlatModifiers.remove(source);
    }

    public double getTotalTempFlatModifier() {
        return tempFlatModifiers.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    // ========== 临时百分比加成 ==========
    public void setTempPercentModifier(String source, double value) {tempPercentModifiers.put(source, roundTo4(value));}
    public void addTempPercentModifier(String source, double value) {tempPercentModifiers.merge(source, roundTo4(value), Double::sum);}
    public void removeTempPercentModifier(String source) {tempPercentModifiers.remove(source);}
    public double getTotalTempPercentModifier() {return tempPercentModifiers.values().stream().mapToDouble(Double::doubleValue).sum();}
    public double getTotalTempPercentModifierDisplay() {return tempPercentModifiers.values().stream().mapToDouble(Double::doubleValue).sum() * 100.0;}

    // ========== 计算总值 ==========
    // 公式: 总值 = 基础值 × (1 + 常驻百分比 + 临时百分比) + 常驻固定值 + 临时固定值
    public double getTotalValue() {
        double totalPercent = getTotalPercentModifier() + getTotalTempPercentModifier();
        double totalFlat = getTotalFlatModifier() + getTotalTempFlatModifier();
        return roundTo3(baseValue * (1.0 + totalPercent) + totalFlat);
    }

    // 永久值（不含临时）
    public double getPermanentValue() {
        return roundTo3(baseValue * (1.0 + getTotalPercentModifier()) + getTotalFlatModifier());
    }

    // ========== 清除临时 ==========
    public void clearTemporaryModifiers() {
        tempFlatModifiers.clear();
        tempPercentModifiers.clear();
    }

    // ========== 工具方法 ==========
    private static double roundTo4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private static double roundTo3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public AttributeInstance copy() {
        AttributeInstance copy = new AttributeInstance(this.type);
        copy.baseValue = this.baseValue;
        copy.flatModifiers.putAll(this.flatModifiers);
        copy.percentModifiers.putAll(this.percentModifiers);
        copy.tempFlatModifiers.putAll(this.tempFlatModifiers);
        copy.tempPercentModifiers.putAll(this.tempPercentModifiers);
        return copy;
    }
}