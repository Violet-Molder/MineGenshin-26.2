package com.linweiyun.genshin.content.effect.character;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.enums.AttackType;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public interface ICharacterEffect {
    public static final Logger LOGGER = LogUtils.getLogger();
    /**
     * 效果首次添加时调用
     * @param holder 效果持有者（玩家）
     * @param character 效果作用的角色数据
     * @param instance 效果实例（包含持续时间、等级、额外数据等）
     */
    default void onEffectAdded(Player holder, PGCharacter character, CharacterEffectInstance instance) {}

    /**
     * 效果被移除时调用
     * @param holder 效果持有者（玩家）
     * @param character 效果作用的角色数据
     * @param instance 被移除的效果实例
     */
    default void onEffectRemoved(Player holder, PGCharacter character, CharacterEffectInstance instance) {}

    /**
     * 效果覆盖时调用 —— 当角色已有相同效果且再次添加时触发
     * 由各个具体效果类实现，定义重复添加时的行为（如叠加数量、刷新持续时间等）
     * @param holder 效果持有者（玩家）
     * @param character 效果作用的角色数据
     * @param existingInstance 已存在的效果实例（即将被覆盖的旧实例）
     * @param newInstance 新添加的效果实例（即将替换旧实例的新实例）
     */
    default void onEffectOverride(Player holder, PGCharacter character, CharacterEffectInstance existingInstance, CharacterEffectInstance newInstance) {}

    /**
     * 效果前台tick —— 角色处于前台激活状态时每tick调用
     * @return 返回true表示效果继续生效，返回false表示效果应被移除
     */
    default boolean onEffectFrontTick(Player holder, PGCharacter character, CharacterEffectInstance instance) {
        return true;
    }

    /**
     * 效果后台tick —— 角色处于后台待机状态时每tick调用
     * @return 返回true表示效果继续生效，返回false表示效果应被移除
     */
    default boolean onEffectBackTick(Player holder, PGCharacter character, CharacterEffectInstance instance) {
        return true;
    }

    /**
     * 效果通用tick —— 无论角色前后台都会调用
     * @return 返回true表示效果继续生效，返回false表示效果应被移除
     */
    default boolean onEffectTick(Player holder, PGCharacter character, CharacterEffectInstance instance) {
        if (!holder.level().isClientSide()) {
            if (instance.getDuration() == CharacterEffectInstance.INFINITE) return true;
            instance.setDuration(instance.getDuration() - 1);
        }
        return instance.getDuration() > 0;
    }

    /**
     * 角色受到攻击时调用
     * @param holder 效果持有者（玩家）
     * @param character 效果作用的角色数据
     * @param target 攻击目标
     * @param instance 效果实例
     */
    default void onAttacked(Player holder, PGCharacter character, LivingEntity target, CharacterEffectInstance instance, ModDamageSource damageSource) {}

    /**
     * 是否为即时效果（一次性生效，不需要持续）
     * @return true表示即时效果，false表示持续效果
     */
    default boolean isInstantaneous() {
        return false;
    }

    /**
     * 获取对指定攻击类型的伤害加成（加成区）
     * @param attackType 攻击类型
     * @return 加成值（小数，如 0.12 = 12%），默认返回0
     */
    default float getDamageBonus(AttackType attackType) {
        return 0f;
    }

    /**
     * 获取对指定攻击类型和元素的伤害加成（加成区） —— 带元素校验的重载版本
     * @param attackType 攻击类型
     * @param element 攻击元素
     * @return 加成值（小数，如 0.12 = 12%），默认返回0
     */
    default float getDamageBonus(AttackType attackType, GenshinElement element) {
        return 0f;
    }
}