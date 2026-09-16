package com.linweiyun.genshin.core.system.wish;

import com.google.gson.JsonParser;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.core.system.registry.register.ModCharacters;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;

import java.util.List;
import java.util.Random;

public class WishSystem {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();
    private static final int WISH_COST = 160;
    private static final int DUPLICATE_COMPENSATION = 75;
    private static WishConfig cachedConfig;

    public static void performWish(ServerPlayer serverPlayer) {
        int primogem = serverPlayer.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT);
        if (primogem < WISH_COST) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.minegenshin.wish.not_enough_primogem"));
            return;
        }

        serverPlayer.setData(AttachmentRegistration.PRIMOGEM_ATTACHMENT, primogem - WISH_COST);
        NetworkManager.setPrimogemToPlayer(serverPlayer, primogem - WISH_COST);

        WishConfig config = getConfig(serverPlayer);
        if (config == null || config.pools().isEmpty()) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.minegenshin.wish.no_reward"));
            return;
        }

        PlayerCharactersAttachment charactersAttachment =
                serverPlayer.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);

        boolean gotSomething = false;
        for (WishPool pool : config.pools()) {
            for (int i = 0; i < pool.rolls(); i++) {
                WishEntry entry = rollEntry(pool.entries());
                if (entry == null) continue;

                if ("character".equals(entry.type())) {
                    gotSomething |= handleCharacterDrop(serverPlayer, charactersAttachment, entry);
                } else if ("item".equals(entry.type())) {
                    gotSomething |= handleItemDrop(serverPlayer, entry);
                }
            }
        }

        if (!gotSomething) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.minegenshin.wish.no_reward"));
        }

        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, serverPlayer.registryAccess());
        charactersAttachment.serialize(output);
        NetworkManager.setPlayerCharactersToPlayer(serverPlayer, output.buildResult());
    }

    private static boolean handleCharacterDrop(ServerPlayer serverPlayer,
                                               PlayerCharactersAttachment charactersAttachment,
                                               WishEntry entry) {
        Identifier charId = Identifier.parse(entry.id());
        PGCharacter character = ModCharacters.getById(charId);
        if (character == null) {
            LOGGER.warn("Unknown character in wish: {}", entry.id());
            return false;
        }

        if (charactersAttachment.hasCharacter(character.getCharacterUUID())) {
            int newPrimogem = serverPlayer.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT);
            serverPlayer.setData(AttachmentRegistration.PRIMOGEM_ATTACHMENT,
                    newPrimogem + DUPLICATE_COMPENSATION);
            NetworkManager.setPrimogemToPlayer(serverPlayer, newPrimogem + DUPLICATE_COMPENSATION);
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.minegenshin.wish.owned_character",
                            character.getName().copy().withStyle(ChatFormatting.GOLD),
                            Component.literal(String.valueOf(DUPLICATE_COMPENSATION))
                                    .withStyle(ChatFormatting.AQUA)));
        } else {
            charactersAttachment.addCharacter(character, serverPlayer);
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.minegenshin.wish.draw_character",
                            character.getName().copy().withStyle(ChatFormatting.GOLD)));
        }
        return true;
    }

    private static boolean handleItemDrop(ServerPlayer serverPlayer, WishEntry entry) {
        Identifier itemId = Identifier.parse(entry.id());
        Item item = BuiltInRegistries.ITEM.getValue(
                ResourceKey.create(Registries.ITEM, itemId));
        if (item == null) {
            LOGGER.warn("Unknown item in wish: {}", entry.id());
            return false;
        }

        ItemStack stack = new ItemStack(item, entry.count());
        NetworkManager.giveItemToPlayer(serverPlayer, stack);
        serverPlayer.sendSystemMessage(
                Component.translatable("message.minegenshin.wish.draw_item",
                        stack.getDisplayName().copy().withStyle(ChatFormatting.GOLD),
                        stack.getCount()));
        return true;
    }

    private static WishEntry rollEntry(List<WishEntry> entries) {
        int totalWeight = 0;
        for (WishEntry entry : entries) {
            totalWeight += entry.weight();
        }
        if (totalWeight <= 0) return null;

        int roll = RANDOM.nextInt(totalWeight);
        int cumulative = 0;
        for (WishEntry entry : entries) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                return entry;
            }
        }
        return entries.get(entries.size() - 1);
    }

    private static WishConfig getConfig(ServerPlayer serverPlayer) {
        if (cachedConfig != null) return cachedConfig;

        MinecraftServer server = ((ServerLevel) serverPlayer.level()).getServer();
        if (server == null) return null;

        ResourceManager rm = server.getResourceManager();
        Identifier resourceId = Identifier.fromNamespaceAndPath("minegenshin", "wish/standard.json");

        try {
            var resource = rm.getResourceOrThrow(resourceId);
            try (var reader = resource.openAsReader()) {
                var json = JsonParser.parseReader(reader);
                var result = WishConfig.CODEC.parse(JsonOps.INSTANCE, json);
                cachedConfig = result.getOrThrow();
                LOGGER.info("Loaded wish config: {} pools", cachedConfig.pools().size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load wish config from {}", resourceId, e);
        }

        return cachedConfig;
    }

    public static void clearCache() {
        cachedConfig = null;
    }
}