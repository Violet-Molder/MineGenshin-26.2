package com.linweiyun.genshin.data.loot.modifier;

import com.linweiyun.genshin.content.items.ModItems;
import com.linweiyun.genshin.content.items.artifact.ArtifactItem;
import com.linweiyun.genshin.content.items.artifact.ArtifactSet;
import com.linweiyun.genshin.core.world.TeyvatWorldInvasion;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class InvasionLootModifier extends LootModifier {

    public static final MapCodec<InvasionLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(
                    inst -> codecStart(inst).apply(inst, InvasionLootModifier::new));

    protected InvasionLootModifier(LootItemCondition[] conditionsIn, int priority) {
        super(conditionsIn, priority);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(
            @NotNull ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ServerLevel level = context.getLevel();
        if (!TeyvatWorldInvasion.get(level).isInvaded()) {
            return generatedLoot;
        }

        if (context.hasParameter(LootContextParams.BLOCK_ENTITY)) {
            handleChestLoot(generatedLoot, context.getRandom());
        }

        return generatedLoot;
    }

    private void handleChestLoot(ObjectArrayList<ItemStack> generatedLoot,
                                  RandomSource random) {
        if (random.nextFloat() < 0.25f) {
            addRandomArtifactPiece(generatedLoot, random);
        }
    }

    private void addRandomArtifactPiece(ObjectArrayList<ItemStack> generatedLoot,
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
        generatedLoot.add(stack);
    }

    @Override
    public @NotNull MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}