package com.linweiyun.genshin.content.effect.character;

import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.system.registry.ModRegistries;
import com.linweiyun.genshin.enums.AttackType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CharacterEffectContainer {

    // 存储所有效果实例的列表
    private List<CharacterEffectInstance> effects;

    /** 默认构造函数 —— 创建空效果容器 */
    public CharacterEffectContainer() {
        this.effects = new ArrayList<>(); // 初始化空列表
    }

    /** 从已有列表创建效果容器 */
    public CharacterEffectContainer(List<CharacterEffectInstance> effects) {
        this.effects = new ArrayList<>(effects); // 拷贝传入列表
    }

    // 空容器常量
    public static final CharacterEffectContainer EMPTY = new CharacterEffectContainer(List.of());

    /** 获取所有效果实例列表 */
    public List<CharacterEffectInstance> getEffects() { return effects; }

    /**
     * 检查是否包含指定ID的效果
     * @param effectId 效果注册ID字符串
     * @return 是否存在该效果
     */
    public boolean hasEffect(String effectId) {
        return effects.stream()                                              // 转换为流
                .anyMatch(e -> e.getEffectIdString().equals(effectId));      // 匹配效果ID
    }

    /**
     * 检查是否包含指定效果 —— 通过引用相等判断（适用于参数化实例）
     * @param effect 效果实例
     * @return 是否存在该效果
     */
    public boolean hasEffect(ICharacterEffect effect) {
        return effects.stream()
                .anyMatch(e -> e.getEffect() == effect);
    }

    /**
     * 根据ID查找效果实例
     * @param effectId 效果注册ID字符串
     * @return 匹配的效果实例，未找到返回null
     */
    public @Nullable CharacterEffectInstance getEffectInstance(String effectId) {
        return effects.stream()                                              // 转换为流
                .filter(e -> e.getEffectIdString().equals(effectId))         // 过滤匹配的ID
                .findFirst()                                                 // 取第一个匹配项
                .orElse(null);                                               // 未找到返回null
    }

    /**
     * 根据效果对象查找效果实例 —— 通过引用相等判断（适用于参数化实例）
     * @param effect 效果实例
     * @return 匹配的效果实例，未找到返回null
     */
    public @Nullable CharacterEffectInstance getEffectInstance(ICharacterEffect effect) {
        return effects.stream()
                .filter(e -> e.getEffect() == effect)
                .findFirst()
                .orElse(null);
    }

    /**
     * 添加效果到容器 —— 如果已有相同ID的效果则替换
     * 注意：覆盖逻辑由CharacterEffectHelper处理，这里只做数据层面的替换
     * @param instance 要添加的效果实例
     */
    public void addEffect(CharacterEffectInstance instance) {
        effects.removeIf(e -> e.getEffectIdString().equals(instance.getEffectIdString())); // 移除同ID旧效果
        effects.add(instance);                                               // 添加新效果
    }

    /**
     * 根据ID移除效果
     * @param effectId 效果注册ID字符串
     * @return 是否成功移除
     */
    public boolean removeEffect(String effectId) {
        return effects.removeIf(e -> e.getEffectIdString().equals(effectId)); // 移除匹配ID的效果
    }

    /**
     * 根据效果对象移除效果 —— 通过引用相等判断（适用于参数化实例）
     * @param effect 效果实例
     * @return 是否成功移除
     */
    public boolean removeEffect(ICharacterEffect effect) {
        return effects.removeIf(e -> e.getEffect() == effect);
    }

    /** 清空所有效果 */
    public void clear() {
        effects.clear();                                                     // 清空列表
    }

    /** 检查容器是否为空 */
    public boolean isEmpty() {
        return effects.isEmpty();                                            // 返回列表是否为空
    }

    /** 获取效果数量 */
    public int size() {
        return effects.size();                                               // 返回列表大小
    }

    /**
     * 获取所有效果对指定攻击类型的伤害加成总和
     * @param attackType 攻击类型
     * @return 总加成值（小数，如 0.12 = 12%）
     */
    public float getTotalDamageBonus(AttackType attackType) {
        float total = 0f;
        for (CharacterEffectInstance instance : effects) {
            total += instance.getEffect().getDamageBonus(attackType);
        }
        return total;
    }

    /**
     * 获取所有效果对指定攻击类型和元素的伤害加成总和 —— 带元素校验的重载版本
     * @param attackType 攻击类型
     * @param element 攻击元素
     * @return 总加成值（小数，如 0.12 = 12%）
     */
    public float getTotalDamageBonus(AttackType attackType, GenshinElement element) {
        float total = 0f;
        for (CharacterEffectInstance instance : effects) {
            total += instance.getEffect().getDamageBonus(attackType, element);
        }
        return total;
    }

    // ========== 序列化/反序列化 ==========

    /**
     * 将效果容器序列化为ListTag
     * 每个效果实例序列化为一个CompoundTag，所有CompoundTag组成ListTag
     * @return 序列化后的ListTag
     */
    public ListTag toListTag() {
        ListTag listTag = new ListTag();                                     // 创建空ListTag
        for (CharacterEffectInstance instance : effects) {                   // 遍历所有效果实例
            listTag.add(instance.toTag());                                   // 将每个实例序列化并添加
        }
        return listTag;                                                      // 返回ListTag
    }

    /**
     * 从ListTag反序列化效果容器
     * @param listTag 包含效果数据的ListTag
     * @return 反序列化后的效果容器
     */
    public static CharacterEffectContainer fromListTag(ListTag listTag) {
        List<CharacterEffectInstance> effects = new ArrayList<>();           // 创建效果列表
        for (int i = 0; i < listTag.size(); i++) {                          // 遍历ListTag中每个元素
            CompoundTag tag = listTag.getCompoundOrEmpty(i);                 // 26.2安全获取方法，获取CompoundTag
            effects.add(CharacterEffectInstance.fromTag(tag));               // 反序列化每个效果实例
        }
        return new CharacterEffectContainer(effects);                        // 构建容器
    }

    /**
     * 深拷贝效果容器
     * @return 包含所有效果副本的新容器
     */
    public CharacterEffectContainer copy() {
        List<CharacterEffectInstance> copied = new ArrayList<>();            // 创建新列表
        for (CharacterEffectInstance instance : effects) {                   // 遍历所有效果
            copied.add(instance.copy());                                     // 深拷贝每个实例
        }
        return new CharacterEffectContainer(copied);                         // 返回新容器
    }

    // ========== 工具方法 ==========

    /**
     * 获取效果对象的注册ID字符串
     * @param effect 注册表中的效果实例
     * @return 效果ID字符串，未注册返回"unknown"
     */
    public static String getEffectIdString(ICharacterEffect effect) {
        var key = ModRegistries.CHARACTER_EFFECT_REGISTRY.getKey(effect);    // 从注册表获取Key
        return key != null ? key.toString() : "unknown";                     // 转为字符串，未注册返回unknown
    }
}