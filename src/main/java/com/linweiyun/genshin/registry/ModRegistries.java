package com.linweiyun.genshin.registry;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.effect.character.ICharacterEffect;
import com.linweiyun.genshin.core.attribute.AttributeType;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.skill.CharacterSkillExecutor;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

@EventBusSubscriber
public class ModRegistries {
    public static final ResourceKey<Registry<AttributeType>> ATTRIBUTE_TYPE_REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(Minegenshin.MOD_ID, "attribute_types"));
    public static final Registry<AttributeType> ATTRIBUTE_TYPE_REGISTRY =
            new RegistryBuilder<>(ATTRIBUTE_TYPE_REGISTRY_KEY)
                    .sync(true)
                    .create();

    public static final ResourceKey<Registry<PGCharacter>> CHARACTER_REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(Minegenshin.MOD_ID, "characters"));
    public static final Registry<PGCharacter> CHARACTER_REGISTRY =
            new RegistryBuilder<>(CHARACTER_REGISTRY_KEY)
                    .sync(true)
                    .create();

    public static final ResourceKey<Registry<ICharacterEffect>> CHARACTER_EFFECT_REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(Minegenshin.MOD_ID, "character_effects"));

    // 角色效果注册表实例 —— 存储所有注册的角色效果（buff/debuff）
    // sync(true)确保客户端和服务端注册表同步
    // defaultKey设置默认效果ID为"minegenshin:empty"
    // maxId(256)限制最多256种效果
    public static final Registry<ICharacterEffect> CHARACTER_EFFECT_REGISTRY =
            new RegistryBuilder<>(CHARACTER_EFFECT_REGISTRY_KEY)
                    .sync(true)
                    .defaultKey(Minegenshin.id("empty"))
                    .maxId(256)
                    .create();

    public static final ResourceKey<Registry<CharacterSkillExecutor>> SKILL_EXECUTOR_REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath(Minegenshin.MOD_ID, "skill_executors"));
    public static final Registry<CharacterSkillExecutor> SKILL_EXECUTOR_REGISTRY =
            new RegistryBuilder<>(SKILL_EXECUTOR_REGISTRY_KEY)
                    .sync(true)
                    .maxId(64)
                    .create();

    public static final DeferredRegister<AttributeType> ATTRIBUTE_TYPES =
            DeferredRegister.create(ATTRIBUTE_TYPE_REGISTRY, Minegenshin.MOD_ID);

    public static final DeferredRegister<PGCharacter> CHARACTERS =
            DeferredRegister.create(CHARACTER_REGISTRY, Minegenshin.MOD_ID);

    public static final DeferredRegister<ICharacterEffect> CHARACTER_EFFECTS =
            DeferredRegister.create(CHARACTER_EFFECT_REGISTRY, Minegenshin.MOD_ID);

    public static final DeferredRegister<CharacterSkillExecutor> SKILL_EXECUTORS =
            DeferredRegister.create(SKILL_EXECUTOR_REGISTRY, Minegenshin.MOD_ID);

    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        event.register(ATTRIBUTE_TYPE_REGISTRY);
        event.register(CHARACTER_REGISTRY);
        event.register(CHARACTER_EFFECT_REGISTRY);
        event.register(SKILL_EXECUTOR_REGISTRY);
    }
}
