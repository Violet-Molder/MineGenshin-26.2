package com.linweiyun.genshin.core.system.registry.register;

import com.linweiyun.genshin.Minegenshin;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class DamageTypeRegister {
    public static final DeferredRegister<DamageType> DAMAGE_TYPES =
            DeferredRegister.create(Registries.DAMAGE_TYPE, Minegenshin.MOD_ID);
    public static final DeferredHolder<DamageType, DamageType> NORMAL_ATTACK_TYPE =
            DAMAGE_TYPES.register("normal_attack",
                    () -> new DamageType("normal_attack", 0));

    public static final DeferredHolder<DamageType, DamageType> CHARGED_ATTACK_TYPE =
            DAMAGE_TYPES.register("charged_attack",
                    () -> new DamageType("charged_attack", 0));

    public static final DeferredHolder<DamageType, DamageType> PLUNGING_ATTACK_TYPE =
            DAMAGE_TYPES.register("plunging_attack",
                    () -> new DamageType("plunging_attack", 0));

    public static final DeferredHolder<DamageType, DamageType> ELEMENTAL_SKILL_TYPE =
            DAMAGE_TYPES.register("elemental_skill",
                    () -> new DamageType("elemental_skill", 0));

    public static final DeferredHolder<DamageType, DamageType> ELEMENTAL_BURST_TYPE =
            DAMAGE_TYPES.register("elemental_burst",
                    () -> new DamageType("elemental_burst", 0));

    public static final DeferredHolder<DamageType, DamageType> SPECIAL_TYPE =
            DAMAGE_TYPES.register("special",
                    () -> new DamageType("special", 0));

    public static final DeferredHolder<DamageType, DamageType> MONSTER_TYPE =
            DAMAGE_TYPES.register("monster",
                    () -> new DamageType("monster", 0));
    // ========== 注册方法 ==========

    /**
     * 注册所有伤害类型
     * 必须在 Minegenshin 构造函数中调用
     */
    public static void register(IEventBus bus) {
        DAMAGE_TYPES.register(bus);
    }
}
