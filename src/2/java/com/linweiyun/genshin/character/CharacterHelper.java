package com.linweiyun.genshin.character;


import com.linweiyun.genshin.Config;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.attribute.AttributeContainer;
import com.linweiyun.genshin.core.attribute.AttributeType;
import com.linweiyun.genshin.core.attribute.ModAttributes;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class CharacterHelper {

    public static PGCharacterData getCurrentCharacter(Player player) {
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        return attachment.getCurrentCharacter();
    }

    public static PGCharacterData getCharacterByUUID(Player player, int uuid) {
        PlayerCharactersAttachment attachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        return attachment.getCharacterByUUID(uuid);
    }

    public static void addExperience(PGCharacterData character, Player player, int amount) {
        if (character == null) return;
        character.setCurrentExp(character.getCurrentExp() + amount);
        tryLevelUp(character, player);
    }

    // ========== 升级逻辑 ==========
    public static void tryLevelUp(PGCharacterData character, Player player) {
        PGCharacter def = character.getDefinition();
        if (def == null) return;

        var expList = Config.CHARACTER_UP_EXP.get();
        AttributeContainer attrs = character.getAttributes();

        // ===== 先计算阶段：遍历计算可升级级数，不修改状态 =====
        int totalExpConsumed = 0;
        int levelsToGain = 0;
        int targetLevel = character.getLevel();
        int targetAscensionPhase = character.getAscensionPhase();

        while (targetLevel < 90) {
            int expNeeded = expList.get(targetLevel - 1);
            if (character.getCurrentExp() - totalExpConsumed < expNeeded) break;

            int maxLevelForPhase = targetAscensionPhase == 0
                    ? 20
                    : Math.min((targetAscensionPhase + 3) * 10, 90);
            if (targetLevel >= maxLevelForPhase) break;

            totalExpConsumed += expNeeded;
            levelsToGain++;
            targetLevel++;
        }

        if (levelsToGain == 0) return;

        // ===== 后修改阶段：一次性更新所有状态 =====
        character.setCurrentExp(character.getCurrentExp() - totalExpConsumed);
        character.addLevel(levelsToGain);

        // ========== 核心变更：通过 AttributeType 设置基础值 ==========
        int statIndex = character.getLevel() - 1 + character.getAscensionPhase();
        updateBaseStatsFromConfig(attrs, def, statIndex);

        // 升级回满血
        character.setCurrentHP(attrs.getValue(ModAttributes.MAX_HP.value()));

        if (character.getLevel() < 90) {
            character.setMaxExp(expList.get(character.getLevel() - 1));
        }

        player.sendSystemMessage(Component.literal(
                "升级后等级：" + character.getLevel()
                        + " 当前经验值：" + character.getCurrentExp()));
    }

    // ========== 突破逻辑 ==========
    public static void ascend(PGCharacterData character, Player player) {
        if (character == null) return;
        PGCharacter def = character.getDefinition();
        if (def == null) return;

        AttributeContainer attrs = character.getAttributes();

        int maxLevelForPhase = character.getAscensionPhase() == 0
                ? 20
                : Math.min((character.getAscensionPhase() + 3) * 10, 90);

        if (character.getLevel() != maxLevelForPhase) return;

        int newPhase = character.getAscensionPhase() + 1;
        character.setAscensionPhase(newPhase);

        // ========== 核心变更：突破属性成长通过 AttributeType 设置 ==========
        int statIndex = maxLevelForPhase - 1 + newPhase;
        updateBaseStatsFromConfig(attrs, def, statIndex);

        // ========== 核心变更：突破百分比加成通过 source 标识添加 ==========
        // 替代原来的 character.getATK().addExtraPercent(0.012f * (start + 1))
        int starRating = def.getStarRating();
        CharacterAscendAttribute ascendAttr = def.getAscendAttribute();

        // 先移除旧的突破加成（如果有），再添加新的
        attrs.removeModifier(getAscendAttributeType(ascendAttr), "ascension_bonus");

        AttributeType targetAttrType = getAscendAttributeType(ascendAttr);
        switch (ascendAttr) {
            case ATK:
                attrs.addPercentModifier(targetAttrType, "ascension_bonus", 0.012f * (starRating + 1));
                break;
            case HP:
                attrs.addPercentModifier(targetAttrType, "ascension_bonus", 0.012f * (starRating + 1));
                break;
            case DEF:
                attrs.addPercentModifier(targetAttrType, "ascension_bonus", 0.015f * (starRating + 1));
                break;
        }

        // 突破后尝试继续升级
        tryLevelUp(character, player);
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
    private static AttributeType getAscendAttributeType(CharacterAscendAttribute ascendAttr) {
        return switch (ascendAttr) {
            case ATK -> ModAttributes.ATK.value();
            case HP -> ModAttributes.MAX_HP.value();
            case DEF -> ModAttributes.DEF.value();
        };
    }
}
