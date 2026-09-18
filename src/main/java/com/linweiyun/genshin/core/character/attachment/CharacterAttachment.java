package com.linweiyun.genshin.core.character.attachment;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

/**
 * 角色附件基类 —— 可以挂到 CharacterAttachmentContainer 里的数据包的统一规格
 * <p>
 * 使用方式（以 Vesna 特殊能量条为例）：
 * <ol>
 *   <li>继承本类，所有需要序列化的字段加 @Persisted</li>
 *   <li>在无参构造中调用 super("你的typeId")</li>
 *   <li>在 ModCharacterAttachmentTypes 中注册 typeId → 构造器</li>
 *   <li>通过 {@code character.getAttachments().get("vesna_energy", VesnaEnergyAttachment.class)} 读写</li>
 * </ol>
 * <p>
 * 与实体附着 StatusInstance 的关键区别：
 * 本类不参与 tick / isFinished 生命周期，纯粹是数据容器。
 */
public abstract class CharacterAttachment implements IPersistedSerializable {

    @Persisted(key = "type_id")
    protected String typeId;

    public CharacterAttachment() {
        this.typeId = "";
    }

    public CharacterAttachment(String typeId) {
        this.typeId = typeId;
    }

    public String getTypeId() {
        return typeId;
    }

    /**
     * 深拷贝（需要多路复用时覆写，默认抛异常提醒子类实现）
     */
    public CharacterAttachment copy() {
        return this;
    }
}