package com.linweiyun.genshin.core.character.attachment;

import com.linweiyun.genshin.core.system.registry.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 角色附件类型注册入口
 * <p>
 * 如果附件是角色专属的，在 {@code ModCharacters} 的 register() 中通过 varargs 自动注册:<br>
 * <pre>{@code register("vesna", 115005, Vesna::new, VesnaEnergy.class)}</pre>
 * <p>
 * 如果附件是全局的（不绑定特定角色），在本类中手动注册。
 */
public class ModCharacterAttachmentTypes {

    public static final DeferredRegister<CharacterAttachmentType<?>> CHARACTER_ATTACHMENT_TYPES_REGISTER =
            ModRegistries.CHARACTER_ATTACHMENT_TYPES;

    public static void register(IEventBus bus) {
        CHARACTER_ATTACHMENT_TYPES_REGISTER.register(bus);
    }
}