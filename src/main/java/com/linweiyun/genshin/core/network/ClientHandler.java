package com.linweiyun.genshin.core.network;

import com.linweiyun.genshin.content.items.artifact.ArtifactItem;
import com.linweiyun.genshin.core.attachment.AdventurerInfoAttachment;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.Backpack;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;

public class ClientHandler {

    // ========== 原石 ==========
    public static void primogemClientHandler(int amount) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.isRemoved()) {
            mc.player.setData(AttachmentRegistration.PRIMOGEM_ATTACHMENT, amount);
        }
    }

    // ========== 原神模式 ==========
    public static void genshinModeClientHandler(boolean isGenshinMode) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.isRemoved()) {
            mc.player.setData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT, isGenshinMode);
        }
    }

    // ========== 冒险者信息 ==========
    public static void adventurerInfoClientHandler(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.isRemoved()) {
            AdventurerInfoAttachment attachment =
                    mc.player.getData(AttachmentRegistration.ADVENTURER_INFO_ATTACHMENT);
            attachment.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, mc.player.registryAccess(), data));
        }
    }

    // ========== 角色整体同步 ==========
    public static void playerCharactersClientHandler(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.isRemoved()) {
            PlayerCharactersAttachment attachment =
                    mc.player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            attachment.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, mc.player.registryAccess(), data));
            attachment.fixCharacterTypes();
        }
    }

    // ========== 单个角色数据 ==========
    public static void characterDataClientHandler(int uuid, CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.isRemoved()) {
            PlayerCharactersAttachment attachment = mc.player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            PGCharacter character = attachment.getCharacterByUUID(uuid);
            if (character != null) {
                character.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, mc.player.registryAccess(), data));
            }
        }
    }

    // ========== 角色选择 ==========
    public static void characterSelectionClientHandler(int index) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.isRemoved()) {
            PlayerCharactersAttachment attachment =
                    mc.player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            attachment.setCurrentCharacterIndex(index);
        }
    }

    // ========== 设置队伍角色 ==========
    public static void setPartyCharacterClientHandler(int index, int characterUUID) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.isRemoved()) {
            PlayerCharactersAttachment attachment = mc.player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            attachment.setPartyCharacter(index, characterUUID);
        }
    }

    // ========== 移除队伍角色 ==========
    public static void removePartyCharacterClientHandler(int index) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.isRemoved()) {
            PlayerCharactersAttachment attachment = mc.player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            attachment.removePartyCharacter(index);
        }
    }

    // ========== 添加角色 ==========
    public static void addCharacterClientHandler(CompoundTag characterData) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.isRemoved()) {
            PlayerCharactersAttachment attachment = mc.player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            PGCharacter character = new PGCharacter();
            character.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, mc.player.registryAccess(), characterData));
            attachment.addCharacter(character, mc.player);
        }
    }

    // ========== 移除角色 ==========
    public static void removeCharacterClientHandler(int uuid) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.isRemoved()) {
            PlayerCharactersAttachment attachment = mc.player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            attachment.removeCharacter(uuid);
        }
    }

    // ========== 圣遗物穿戴/更换 ==========
    public static void equipOrSwapArtifactClientHandler(int artifactSlotIndex, int inventorySlotIndex) {
        // 客户端不需要额外处理，数据通过 PlayerCharactersAttachment 同步回来
    }

    // ========== 圣遗物卸下 ==========
    public static void unequipArtifactClientHandler(int artifactSlotIndex) {
        // 客户端不需要额外处理，数据通过 PlayerCharactersAttachment 同步回来
    }

    /**
     * 客户端处理服务端激活同步：
     * 更新本地背包中指定索引的圣遗物数据。
     */
    public static void activateArtifactClientHandler(int inventorySlotIndex) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return;
        Backpack backpack = player.getData(AttachmentRegistration.BACKPACK_ATTACHMENT);
        int globalSlot = 0;
        for (var category : Backpack.Category.values()) {
            if (category == Backpack.Category.ARTIFACTS) break;
            globalSlot += category.maxCapacity;
        }
        globalSlot += inventorySlotIndex;
        ItemStack stack = backpack.getItem(globalSlot);
        if (stack.isEmpty() || !(stack.getItem() instanceof ArtifactItem)) return;
        // 本地补一次激活（与服务端结果一致），UI 下次刷新时即显示激活后的数据
        ArtifactItem.initializeArtifactStackIfNeeded(stack);
        backpack.setItem(globalSlot, stack);
    }

//    // ========== 原神背包同步（服务端→客户端） ==========
//    public static void genshinBackpackClientHandler(CompoundTag data) {
//        Minecraft mc = Minecraft.getInstance();
//        if (mc.player != null && !mc.player.isRemoved()) {
//            GenshinBackpack backpack =
//                    mc.player.getData(AttachmentRegistration.GENSHIN_BACKPACK_ATTACHMENT);
//            backpack.setSuppressDirty(true);
//            backpack.deserialize(TagValueInput.create(
//                    ProblemReporter.DISCARDING, mc.player.registryAccess(), data));
//            backpack.setSuppressDirty(false);
//            backpack.clearDirty();
//        }
//    }
}