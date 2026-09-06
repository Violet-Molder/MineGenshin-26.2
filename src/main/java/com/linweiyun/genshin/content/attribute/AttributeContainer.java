package com.linweiyun.genshin.content.attribute;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import java.util.HashMap;
import java.util.Map;

public class AttributeContainer implements IPersistedSerializable {

    @Persisted(key = "attributes")
    private final Map<String, AttributeInstance> attributes = new HashMap<>();

    public AttributeInstance getOrCreate(AttributeType type) {
        return attributes.computeIfAbsent(type.id().toString(), k -> new AttributeInstance(type));
    }

    public AttributeInstance get(AttributeType type) {
        return attributes.get(type.id().toString());
    }

    public boolean has(AttributeType type) {
        return attributes.containsKey(type.id().toString());
    }

    public double getTotalValue(AttributeType type) {
        AttributeInstance instance = get(type);
        return instance != null ? instance.getTotalValue() : type.defaultValue();
    }

    public double getBaseValue(AttributeType type) {
        AttributeInstance instance = get(type);
        return instance != null ? instance.getBaseValue() : type.defaultValue();
    }
    public double getFlatModifier(AttributeType type) {
        AttributeInstance instance = get(type);
        return instance != null ? instance.getTotalFlatModifier() : 0;
    }
    public double getTempFlatModifier(AttributeType type) {
        AttributeInstance instance = get(type);
        return instance != null ? instance.getTotalTempFlatModifier() : 0;
    }
    public double getPercentModifier(AttributeType type) {
        AttributeInstance instance = get(type);
        return instance != null ? instance.getTotalPercentModifier() : 0;
    }
    public double getTempPercentModifier(AttributeType type) {
        AttributeInstance instance = get(type);
        return instance != null ? instance.getTotalTempPercentModifier() : 0;
    }

    public void setBaseValue(AttributeType type, double value) {
        getOrCreate(type).setBaseValue(value);
    }

    public void addFlatModifier(AttributeType type, String source, double value) {
        getOrCreate(type).addFlatModifier(source, value);
    }

    public void addPercentModifier(AttributeType type, String source, double value) {
        getOrCreate(type).addPercentModifier(source, value);
    }

    public void setFlatModifier(AttributeType type, String source, double value) {
        getOrCreate(type).setFlatModifier(source, value);
    }

    public void setPercentModifier(AttributeType type, String source, double value) {
        getOrCreate(type).setPercentModifier(source, value);
    }

    public void addTempFlatModifier(AttributeType type, String source, double value) {
        getOrCreate(type).addTempFlatModifier(source, value);
    }

    public void addTempPercentModifier(AttributeType type, String source, double value) {
        getOrCreate(type).addTempPercentModifier(source, value);
    }

    public void removeModifier(AttributeType type, String source) {
        AttributeInstance instance = get(type);
        if (instance != null) {
            instance.removeFlatModifier(source);
            instance.removePercentModifier(source);
            instance.removeTempFlatModifier(source);
            instance.removeTempPercentModifier(source);
        }
    }

    public void clearTemporaryModifiers() {
        attributes.values().forEach(AttributeInstance::clearTemporaryModifiers);
    }

    public Map<String, AttributeInstance> getAllAttributes() {
        return attributes;
    }

    public AttributeContainer copy() {
        AttributeContainer copy = new AttributeContainer();
        for (var entry : attributes.entrySet()) {
            copy.attributes.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }
}