package com.linweiyun.genshin.event.server;

import com.linweiyun.genshin.content.entities.teyvat.monster.slime.LargeCryoSlime;
import com.linweiyun.genshin.content.items.ModItems;
import com.linweiyun.genshin.content.items.artifact.ArtifactItem;
import com.linweiyun.genshin.content.items.artifact.ArtifactSet;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.core.world.TeyvatWorldInvasion;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@EventBusSubscriber
public class EntityDeathDropEvent {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide()) return;

        ServerLevel serverLevel = (ServerLevel) level;
        if (!TeyvatWorldInvasion.get(serverLevel).isInvaded()) return;

        DamageSource source = event.getSource();
        ServerPlayer killer = source.getEntity() instanceof ServerPlayer sp ? sp : null;
        RandomSource random = entity.getRandom();

        if (entity instanceof EnderDragon
                || entity instanceof WitherBoss
                || entity instanceof Warden) {
            handleBossDrop(event, killer, random);
        } else if (entity instanceof LargeCryoSlime) {
            handleCryoSlimeDrop(event, random);
        } else if (entity.getType().getCategory() == MobCategory.MONSTER) {
            handleMonsterDrop(event, random);
        }
    }

    private static void handleBossDrop(LivingDropsEvent event, ServerPlayer killer,
                                        RandomSource random) {
        if (killer != null) {
            int current = killer.getData(AttachmentRegistration.PRIMOGEM_ATTACHMENT.get());
            int total = current + 1600;
            killer.setData(AttachmentRegistration.PRIMOGEM_ATTACHMENT.get(), total);
            NetworkManager.setPrimogemToPlayer(killer, total);
            killer.sendSystemMessage(
                    Component.literal("Boss击杀奖励：获得 1600 原石（当前：" + total + "）"));
        }
        addRandomFullArtifactSet(event, random);
    }

    private static void handleMonsterDrop(LivingDropsEvent event, RandomSource random) {
        if (random.nextFloat() < 0.5f) {
            int count = 2 + random.nextInt(9);
            event.getDrops().add(createItemEntity(event.getEntity(),
                    new ItemStack(ModItems.PRIMOGEM.get(), count)));
        }
        addBooks(event, random, 0, 4, 0, 3, 0, 2);
    }

    private static void handleCryoSlimeDrop(LivingDropsEvent event, RandomSource random) {
        int primoCount = 2 + random.nextInt(9);
        event.getDrops().add(createItemEntity(event.getEntity(),
                new ItemStack(ModItems.PRIMOGEM.get(), primoCount)));

        if (random.nextFloat() < 0.7f) {
            addRandomArtifactPiece(event, random);
        }

        addBooks(event, random, 1, 8, 0, 6, 0, 4);
    }

    private static void addBooks(LivingDropsEvent event, RandomSource random,
                                  int wMin, int wMax,
                                  int aMin, int aMax,
                                  int hMin, int hMax) {
        LivingEntity entity = event.getEntity();
        int w = wMin + random.nextInt(wMax - wMin + 1);
        if (w > 0) {
            event.getDrops().add(createItemEntity(entity,
                    new ItemStack(ModItems.WANDERERS_ADVICE.get(), w)));
        }
        int a = aMin + random.nextInt(aMax - aMin + 1);
        if (a > 0) {
            event.getDrops().add(createItemEntity(entity,
                    new ItemStack(ModItems.ADVENTURERS_EXPERIENCE.get(), a)));
        }
        int h = hMin + random.nextInt(hMax - hMin + 1);
        if (h > 0) {
            event.getDrops().add(createItemEntity(entity,
                    new ItemStack(ModItems.HEROS_WIT.get(), h)));
        }
    }

    private static ItemEntity createItemEntity(LivingEntity entity, ItemStack stack) {
        return new ItemEntity(entity.level(),
                entity.getX(), entity.getY(), entity.getZ(), stack);
    }

    private static void addRandomFullArtifactSet(LivingDropsEvent event,
                                                  RandomSource random) {
        Map<DeferredHolder<ArtifactSet, ArtifactSet>,
                List<Supplier<? extends ArtifactItem>>> artifactMap =
                ModItems.getArtifactSetItemsMap();
        if (artifactMap.isEmpty()) return;

        List<DeferredHolder<ArtifactSet, ArtifactSet>> keys =
                new ArrayList<>(artifactMap.keySet());
        DeferredHolder<ArtifactSet, ArtifactSet> chosenSet =
                keys.get(random.nextInt(keys.size()));

        List<Supplier<? extends ArtifactItem>> items = artifactMap.get(chosenSet);
        if (items == null) return;

        for (Supplier<? extends ArtifactItem> supplier : items) {
            ItemStack stack = new ItemStack(supplier.get());
            ArtifactItem.initializeArtifactStackIfNeeded(stack);
            event.getDrops().add(createItemEntity(event.getEntity(), stack));
        }
    }

    private static void addRandomArtifactPiece(LivingDropsEvent event,
                                                RandomSource random) {
        Map<DeferredHolder<ArtifactSet, ArtifactSet>,
                List<Supplier<? extends ArtifactItem>>> artifactMap =
                ModItems.getArtifactSetItemsMap();
        if (artifactMap.isEmpty()) return;

        List<Supplier<? extends ArtifactItem>> allItems = new ArrayList<>();
        for (List<Supplier<? extends ArtifactItem>> setItems : artifactMap.values()) {
            allItems.addAll(setItems);
        }
        if (allItems.isEmpty()) return;

        Supplier<? extends ArtifactItem> chosenItem =
                allItems.get(random.nextInt(allItems.size()));
        ItemStack stack = new ItemStack(chosenItem.get());
        ArtifactItem.initializeArtifactStackIfNeeded(stack);
        event.getDrops().add(createItemEntity(event.getEntity(), stack));
    }
}