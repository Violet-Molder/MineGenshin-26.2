package com.linweiyun.genshin.content.effect.character;

import com.linweiyun.genshin.core.system.registry.ModRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

public class CharacterEffectInstance {

    // 无限持续时间常量
    public static final int INFINITE = -1;

    // ========== 核心字段 ==========

    // 效果实例（直接持有，类型安全）
    private final ICharacterEffect effect;
    // 效果注册名（用于序列化/反序列化）
    private final Identifier effectId;
    // 效果剩余持续时间（单位：tick），-1表示无限持续
    private int duration;
    // 效果等级/强度
    private int amplifier;
    // 是否在HUD上隐藏该效果
    private boolean hidden;
    // 效果特有额外数据（如冰凌数量、血契数值等）
    private CompoundTag data;

    // ========== 构造函数 ==========

    /**
     * 完整构造函数 —— 直接传 ICharacterEffect 实例
     *
     * @param effect 效果实例（如 IcyQuillEffect.INSTANCE）
     * @param duration 持续时间（tick）
     * @param amplifier 效果等级
     * @param hidden 是否隐藏
     * @param data 额外数据标签
     */
    public CharacterEffectInstance(ICharacterEffect effect, int duration, int amplifier, boolean hidden, CompoundTag data) {
        this.effect = effect;             // 直接存储效果实例
        // 从注册表反查 effect 的 Identifier
        this.effectId = ModRegistries.CHARACTER_EFFECT_REGISTRY.getKey(effect);
        this.duration = duration;
        this.amplifier = amplifier;
        this.hidden = hidden;
        this.data = data != null ? data : new CompoundTag();
    }

    /** 不带额外数据的构造函数 */
    public CharacterEffectInstance(ICharacterEffect effect, int duration, int amplifier, boolean hidden) {
        this(effect, duration, amplifier, hidden, new CompoundTag());
    }

    /** 不带隐藏和额外数据的构造函数 */
    public CharacterEffectInstance(ICharacterEffect effect, int duration, int amplifier) {
        this(effect, duration, amplifier, false, new CompoundTag());
    }

    // ========== Getter 方法 ==========

    public ICharacterEffect getEffect() {
        return effect;
    }

    /** 获取效果的注册名 Identifier */
    public Identifier getEffectId() {
        return effectId;
    }

    /** 获取效果ID的字符串形式 */
    public String getEffectIdString() {
        return effectId.toString();
    }

    /** 获取效果剩余持续时间 */
    public int getDuration() {
        return duration;
    }

    /** 设置效果剩余持续时间 */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /** 获取效果等级 */
    public int getAmplifier() {
        return amplifier;
    }

    /** 设置效果等级 */
    public void setAmplifier(int amplifier) {
        this.amplifier = amplifier;
    }

    /** 获取是否隐藏 */
    public boolean isHidden() {
        return hidden;
    }

    /** 设置是否隐藏 */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /** 获取额外数据标签 */
    public CompoundTag getData() {
        return data;
    }

    /** 设置额外数据标签 */
    public void setData(CompoundTag data) {
        this.data = data != null ? data : new CompoundTag();
    }

    // ========== 额外数据快捷访问方法 ==========

    /** 从额外数据中读取整数值 */
    public int getIntData(String key) {
        return data.getInt(key).orElse(0);
    }

    /** 从额外数据中读取字符串值 */
    public String getStringData(String key) {
        return data.getString(key).orElse("");
    }

    /** 从额外数据中读取布尔值 */
    public boolean getBooleanData(String key) {
        return data.getBoolean(key).orElse(false);
    }

    /** 从额外数据中读取浮点数值 */
    public float getFloatData(String key) {
        return data.getFloat(key).orElse(0.0f);
    }

    /** 从额外数据中读取双精度浮点数值 */
    public double getDoubleData(String key) {
        return data.getDouble(key).orElse(0.0);
    }

    /** 从额外数据中读取嵌套CompoundTag */
    public CompoundTag getCompoundData(String key) {
        return data.getCompound(key).orElse(new CompoundTag());
    }

    /** 向额外数据中写入整数值 */
    public void setIntData(String key, int value) {
        data.putInt(key, value);
    }

    /** 向额外数据中写入字符串值 */
    public void setStringData(String key, String value) {
        data.putString(key, value);
    }

    /** 向额外数据中写入布尔值 */
    public void setBooleanData(String key, boolean value) {
        data.putBoolean(key, value);
    }

    /** 向额外数据中写入浮点数值 */
    public void setFloatData(String key, float value) {
        data.putFloat(key, value);
    }

    /** 向额外数据中写入双精度浮点数值 */
    public void setDoubleData(String key, double value) {
        data.putDouble(key, value);
    }

    /** 向额外数据中写入嵌套CompoundTag */
    public void setCompoundData(String key, CompoundTag value) {
        data.put(key, value);
    }

    // ========== 数据副本 ==========

    /**
     * 创建一个带有新额外数据的效果实例副本
     */
    public CharacterEffectInstance withData(CompoundTag newData) {
        return new CharacterEffectInstance(effect, duration, amplifier, hidden, newData);
    }

    /**
     * 深拷贝当前效果实例
     */
    public CharacterEffectInstance copy() {
        return new CharacterEffectInstance(effect, duration, amplifier, hidden, data.copy());
    }

    // ========== 序列化/反序列化 ==========

    /**
     * 将效果实例序列化为CompoundTag
     */
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("effect_id", effectId.toString()); // 存注册名
        tag.putInt("duration", duration);
        tag.putInt("amplifier", amplifier);
        tag.putBoolean("hidden", hidden);
        tag.put("data", data.copy());
        return tag;
    }

    /**
     * 从CompoundTag反序列化效果实例
     * 通过存的 registry name 从注册表查找 ICharacterEffect 实例
     */
    public static CharacterEffectInstance fromTag(CompoundTag tag) {
        String effectIdStr = tag.getString("effect_id").orElse("unknown");
        Identifier id = Identifier.parse(effectIdStr);

        // 通过注册表查找 ICharacterEffect 实例
        ICharacterEffect effect = ModRegistries.CHARACTER_EFFECT_REGISTRY.getValue(id);
        if (effect == null) {
            effect = DummyEffect.INSTANCE; // 回退：未注册的效果用DummyEffect
        }

        int duration = tag.getInt("duration").orElse(0);
        int amplifier = tag.getInt("amplifier").orElse(0);
        boolean hidden = tag.getBoolean("hidden").orElse(false);
        CompoundTag data = tag.getCompound("data").orElse(new CompoundTag());

        return new CharacterEffectInstance(effect, duration, amplifier, hidden, data);
    }

    // ========== 调试输出 ==========

    @Override
    public String toString() {
        return "EffectInstance{" +
                "effect=" + effectId +
                ", duration=" + duration +
                ", amplifier=" + amplifier +
                ", hidden=" + hidden +
                '}';
    }

    /**
     * 占位效果 —— 当反序列化时找不到注册的效果时使用
     * 防止NPE，标记为unknown状态
     */
    public static class DummyEffect implements ICharacterEffect {
        public static final DummyEffect INSTANCE = new DummyEffect();

        @Override
        public String toString() {
            return "DummyEffect(unknown)";
        }
    }
}