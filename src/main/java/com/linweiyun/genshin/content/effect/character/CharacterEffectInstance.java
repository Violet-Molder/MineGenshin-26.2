package com.linweiyun.genshin.content.effect.character;

import com.linweiyun.genshin.registry.ModRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;

public class CharacterEffectInstance {

    // 无限持续时间常量
    public static final int INFINITE = -1;

    // 效果注册ID（字符串格式，如 "minegenshin:regeneration"）
    private String effectId;
    // 效果剩余持续时间（单位：tick），-1表示无限持续
    private int duration;
    // 效果等级/强度
    private int amplifier;
    // 是否在HUD上隐藏该效果
    private boolean hidden;
    // 效果特有额外数据（如冰凌数量、血契数值等）
    private CompoundTag data;

    /**
     * 完整构造函数
     * @param effectId 效果注册ID（字符串格式）
     * @param duration 持续时间（tick）
     * @param amplifier 效果等级
     * @param hidden 是否隐藏
     * @param data 额外数据标签
     */
    public CharacterEffectInstance(String effectId, int duration, int amplifier, boolean hidden, CompoundTag data) {
        this.effectId = effectId;           // 设置效果ID
        this.duration = duration;           // 设置持续时间
        this.amplifier = amplifier;         // 设置效果等级
        this.hidden = hidden;               // 设置是否隐藏
        this.data = data != null ? data : new CompoundTag(); // 设置额外数据，防止null
    }

    /** 不带额外数据的构造函数 */
    public CharacterEffectInstance(String effectId, int duration, int amplifier, boolean hidden) {
        this(effectId, duration, amplifier, hidden, new CompoundTag()); // 默认空数据标签
    }

    /** 不带隐藏和额外数据的构造函数 */
    public CharacterEffectInstance(String effectId, int duration, int amplifier) {
        this(effectId, duration, amplifier, false, new CompoundTag()); // 默认不隐藏、空数据
    }

    /** 使用Identifier作为效果ID的构造函数 */
    public CharacterEffectInstance(Identifier effectId, int duration, int amplifier, boolean hidden, CompoundTag data) {
        this(effectId.toString(), duration, amplifier, hidden, data); // 将Identifier转为字符串存储
    }

    /** 使用Identifier、不带额外数据的构造函数 */
    public CharacterEffectInstance(Identifier effectId, int duration, int amplifier, boolean hidden) {
        this(effectId.toString(), duration, amplifier, hidden, new CompoundTag()); // Identifier转字符串
    }

    // ========== Getter方法 ==========

    /** 获取效果ID的字符串形式 */
    public String getEffectIdString() { return effectId; }

    /** 获取效果ID的Identifier形式 */
    public Identifier getEffectId() { return Identifier.parse(effectId); } // 从字符串解析为Identifier

    /** 获取效果剩余持续时间 */
    public int getDuration() { return duration; }

    /** 设置效果剩余持续时间 */
    public void setDuration(int duration) { this.duration = duration; }

    /** 获取效果等级 */
    public int getAmplifier() { return amplifier; }

    /** 设置效果等级 */
    public void setAmplifier(int amplifier) { this.amplifier = amplifier; }

    /** 获取是否隐藏 */
    public boolean isHidden() { return hidden; }

    /** 设置是否隐藏 */
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    /** 获取额外数据标签 */
    public CompoundTag getData() { return data; }

    /** 设置额外数据标签 */
    public void setData(CompoundTag data) { this.data = data != null ? data : new CompoundTag(); } // 防止null

    // ========== 额外数据快捷访问方法 ==========

    /**
     * 从额外数据中读取整数值
     * 注意：26.2中CompoundTag.getInt()返回Optional<Integer>，需要用orElse提供默认值
     */
    public int getIntData(String key) {
        return data.getInt(key).orElse(0); // 26.2返回Optional<Integer>，不存在时返回0
    }

    /**
     * 从额外数据中读取字符串值
     * 注意：26.2中CompoundTag.getString()返回Optional<String>，需要用orElse提供默认值
     */
    public String getStringData(String key) {
        return data.getString(key).orElse(""); // 26.2返回Optional<String>，不存在时返回空字符串
    }

    /**
     * 从额外数据中读取布尔值
     * 注意：26.2中CompoundTag.getBoolean()返回Optional<Boolean>，需要用orElse提供默认值
     */
    public boolean getBooleanData(String key) {
        return data.getBoolean(key).orElse(false); // 26.2返回Optional<Boolean>，不存在时返回false
    }

    /**
     * 从额外数据中读取浮点数值
     * 注意：26.2中CompoundTag.getFloat()返回Optional<Float>，需要用orElse提供默认值
     */
    public float getFloatData(String key) {
        return data.getFloat(key).orElse(0.0f); // 26.2返回Optional<Float>，不存在时返回0.0f
    }

    /**
     * 从额外数据中读取双精度浮点数值
     * 注意：26.2中CompoundTag.getDouble()返回Optional<Double>，需要用orElse提供默认值
     */
    public double getDoubleData(String key) {
        return data.getDouble(key).orElse(0.0); // 26.2返回Optional<Double>，不存在时返回0.0
    }

    /**
     * 从额外数据中读取嵌套CompoundTag
     * 注意：26.2中CompoundTag.getCompound()返回Optional<CompoundTag>，需要用orElse提供默认值
     */
    public CompoundTag getCompoundData(String key) {
        return data.getCompound(key).orElse(new CompoundTag()); // 26.2返回Optional<CompoundTag>，不存在时返回空标签
    }

    /** 向额外数据中写入整数值 */
    public void setIntData(String key, int value) { data.putInt(key, value); }

    /** 向额外数据中写入字符串值 */
    public void setStringData(String key, String value) { data.putString(key, value); }

    /** 向额外数据中写入布尔值 */
    public void setBooleanData(String key, boolean value) { data.putBoolean(key, value); }

    /** 向额外数据中写入浮点数值 */
    public void setFloatData(String key, float value) { data.putFloat(key, value); }

    /** 向额外数据中写入双精度浮点数值 */
    public void setDoubleData(String key, double value) { data.putDouble(key, value); }

    /** 向额外数据中写入嵌套CompoundTag */
    public void setCompoundData(String key, CompoundTag value) { data.put(key, value); }

    // ========== 效果注册表查找 ==========

    /**
     * 通过效果ID从注册表中查找对应的ICharacterEffect实例
     * @return 注册表中的效果实例，如果未找到则返回null
     */
    public @Nullable ICharacterEffect getEffect() {
        return ModRegistries.CHARACTER_EFFECT_REGISTRY.getValue(Identifier.parse(effectId)); // 从注册表查找
    }

    // ========== 数据副本 ==========

    /**
     * 创建一个带有新额外数据的效果实例副本
     * @param newData 新的额外数据标签
     * @return 新的效果实例
     */
    public CharacterEffectInstance withData(CompoundTag newData) {
        return new CharacterEffectInstance(effectId, duration, amplifier, hidden, newData); // 创建新实例
    }

    /**
     * 深拷贝当前效果实例
     * @return 包含所有数据副本的新实例
     */
    public CharacterEffectInstance copy() {
        return new CharacterEffectInstance(effectId, duration, amplifier, hidden, data.copy()); // 深拷贝data
    }

    // ========== 序列化/反序列化 ==========

    /**
     * 将效果实例序列化为CompoundTag
     * 用于存储到CharacterEffectContainer的ListTag中
     * @return 包含所有效果数据的CompoundTag
     */
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();      // 创建新的NBT标签
        tag.putString("effect_id", effectId);     // 写入效果ID
        tag.putInt("duration", duration);         // 写入持续时间
        tag.putInt("amplifier", amplifier);       // 写入效果等级
        tag.putBoolean("hidden", hidden);         // 写入是否隐藏
        tag.put("data", data.copy());             // 写入额外数据（深拷贝防止外部修改）
        return tag;                               // 返回序列化后的标签
    }

    /**
     * 从CompoundTag反序列化效果实例
     * 注意：26.2中所有getter返回Optional，必须用orElse()提供默认值
     * @param tag 包含效果数据的CompoundTag
     * @return 反序列化后的效果实例
     */
    public static CharacterEffectInstance fromTag(CompoundTag tag) {
        // 26.2 API变更：getString/getInt/getBoolean/getCompound 均返回 Optional<T>
        String effectId = tag.getString("effect_id").orElse("unknown");       // 读取效果ID，默认"unknown"
        int duration = tag.getInt("duration").orElse(0);                      // 读取持续时间，默认0
        int amplifier = tag.getInt("amplifier").orElse(0);                    // 读取效果等级，默认0
        boolean hidden = tag.getBoolean("hidden").orElse(false);              // 读取是否隐藏，默认false
        CompoundTag data = tag.getCompound("data").orElse(new CompoundTag()); // 读取额外数据，默认空标签
        return new CharacterEffectInstance(effectId, duration, amplifier, hidden, data); // 构建实例
    }
}