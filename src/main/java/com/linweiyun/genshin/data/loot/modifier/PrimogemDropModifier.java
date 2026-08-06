//package com.linweiyun.genshin.data.loot.modifier;
//
//import com.mojang.serialization.Codec;
//import com.mojang.serialization.MapCodec;
//import com.mojang.serialization.codecs.RecordCodecBuilder;
//import it.unimi.dsi.fastutil.objects.ObjectArrayList;
//import net.minecraft.core.registries.BuiltInRegistries;
//import net.minecraft.world.entity.EntityType;
//import net.minecraft.world.entity.LivingEntity;
//import net.minecraft.world.entity.MobCategory;
//import net.minecraft.world.item.Item;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.level.storage.loot.LootContext;
//import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
//import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
//import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
//import net.neoforged.neoforge.common.loot.LootModifier;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.Collections;
//import java.util.Set;
//import java.util.WeakHashMap;
//
//public class PrimogemDropModifier extends LootModifier {
//  public static final MapCodec<PrimogemDropModifier> CODEC =
//      RecordCodecBuilder.mapCodec(
//          inst ->
//              codecStart(inst)
//                  .and(Codec.list(DropEntry.CODEC).fieldOf("drops").forGetter(m -> m.drops))
//                  .apply(inst, PrimogemDropModifier::new));
//
//  private final java.util.List<DropEntry> drops;
//
//  protected PrimogemDropModifier(
//      LootItemCondition[] conditionsIn, java.util.List<DropEntry> drops) {
//    super(conditionsIn);
//    this.drops = drops;
//  }
//
//  private static final Set<Integer> processedContexts =
//      Collections.newSetFromMap(new WeakHashMap<>());
//
//  @Override
//  protected @NotNull ObjectArrayList<ItemStack> doApply(
//      @NotNull ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
//    if (context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof LivingEntity livingEntity
//        && isMonster(livingEntity.getType())) {
//      var damageSource = context.getParamOrNull(LootContextParams.DAMAGE_SOURCE);
//      if (damageSource == null) {
//        return generatedLoot;
//      }
//
//      int contextHash = System.identityHashCode(context);
//      if (!processedContexts.add(contextHash)) {
//        return generatedLoot;
//      }
//      float roll = context.getRandom().nextFloat();
//      float weightSum = 0;
//      for (DropEntry entry : drops) {
//        weightSum += entry.weight;
//      }
//      float weightedRoll = roll * weightSum;
//      float current = 0;
//      for (DropEntry entry : drops) {
//        current += entry.weight;
//        if (weightedRoll < current) {
//          generatedLoot.add(new ItemStack(entry.item, entry.count));
//          break;
//        }
//      }
//    }
//    return generatedLoot;
//  }
//
//  private boolean isMonster(EntityType<?> type) {
//    return type.getCategory() == MobCategory.MONSTER;
//  }
//
//  public @NotNull MapCodec<? extends IGlobalLootModifier> codec() {
//    return CODEC;
//  }
//
//  public record DropEntry(int count, float weight, Item item) {
//    public static final Codec<DropEntry> CODEC =
//        RecordCodecBuilder.create(
//            inst ->
//                inst.group(
//                        Codec.INT.fieldOf("count").forGetter(e -> e.count),
//                        Codec.FLOAT.fieldOf("weight").forGetter(e -> e.weight),
//                        BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(e -> e.item))
//                    .apply(inst, DropEntry::new));
//  }
//}
