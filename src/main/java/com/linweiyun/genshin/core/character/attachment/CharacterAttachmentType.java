package com.linweiyun.genshin.core.character.attachment;

import java.util.function.Supplier;

/**
 * 角色附件子类的注册表条目（record = 不可变数据载体）
 * <p>
 * 用法示例：
 * <pre>
 * new CharacterAttachmentType<>("vesna_energy", VesnaEnergyAttachment::new)
 * </pre>
 */
public record CharacterAttachmentType<T extends CharacterAttachment>(
        String typeId,
        Supplier<T> constructor
) {}