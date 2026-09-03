package com.linweiyun.genshin.core.character;


import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.content.attribute.AttributeContainer;
import com.linweiyun.genshin.content.attribute.AttributeType;
import net.minecraft.world.entity.player.Player;

public class CharacterHelper {

    public static PGCharacter getCurrentCharacter(Player player) {
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        return attachment.getCurrentCharacter();
    }

    public static PGCharacter getCharacterByUUID(Player player, int uuid) {
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        return attachment.getCharacterByUUID(uuid);
    }







    // ========== 核心变更：统一的属性基础值更新方法 ==========
    // 从 Config 列表中读取指定等级的属性值，设置到 AttributeContainer
    private static void updateBaseStatsFromConfig(
            AttributeContainer attrs, PGCharacter def, int statIndex) {
        for (AttributeType type : def.getStatGrowthTypes()) {
            int value = def.getStatAtLevel(type, statIndex);
            attrs.setBaseValue(type, value);
        }
    }

    // 根据突破属性类型获取对应的 AttributeType

}
