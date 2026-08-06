package com.linweiyun.genshin.registry.register;

import com.linweiyun.genshin.core.skill.CharacterSkillExecutor;
import com.linweiyun.genshin.core.skill.ShenHeSkillExecutor;
import com.linweiyun.genshin.registry.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SkillExecutorRegister {
    public static final DeferredRegister<CharacterSkillExecutor> SKILL_EXECUTORS =
            ModRegistries.SKILL_EXECUTORS;

    // ========== 角色技能执行器 ==========
    public static final DeferredHolder<CharacterSkillExecutor, CharacterSkillExecutor> SHENHE =
            SKILL_EXECUTORS.register("shenhe", ShenHeSkillExecutor::new);

    public static void register(IEventBus bus) {
        SKILL_EXECUTORS.register(bus);
    }
}
