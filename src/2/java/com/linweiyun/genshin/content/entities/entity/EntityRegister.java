package com.linweiyun.genshin.content.entities.entity;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.entities.entity.summon.field.player.FieldTalismanSpirit;
import com.linweiyun.genshin.content.entities.entity.teyvat.monster.SlimeCyro;
import java.util.function.Supplier;

import com.linweiyun.genshin.content.entities.entity.teyvat.spectial.ElementalOrb;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EntityRegister {
  public static final DeferredRegister<EntityType<?>> ENTITIES =
      DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Minegenshin.MOD_ID);
  public static final Supplier<EntityType<ElementalOrb>> ELEMENTAL_ORB =
      ENTITIES.register(
          "elemental_orb",
          () ->
              EntityType.Builder.of(ElementalOrb::new, MobCategory.CREATURE)
                  .sized(0.6F, 1.8F)
                  .build("entity_genshin"));
  public static final Supplier<EntityType<FieldTalismanSpirit>> FIELD_TALISMAN_SPIRIT =
      ENTITIES.register(
          "field_talisman_spirit",
          () ->
              EntityType.Builder.of(FieldTalismanSpirit::new, MobCategory.CREATURE)
                  .build("entity_genshin"));
  public static final Supplier<EntityType<SlimeCyro>> SLIME_CYRO =
      ENTITIES.register(
          "slime_cyro",
          () ->
              EntityType.Builder.of(SlimeCyro::new, MobCategory.MONSTER)
                  .sized(1.3964844F, 1.6F)
                  .eyeHeight(1.52F)
                  .passengerAttachments(1.31875F)
                  .clientTrackingRange(10)
                  .build("entity_genshin"));

  public static void register(IEventBus eventBus) {
    ENTITIES.register(eventBus);
    eventBus.addListener(EntityRegister::registerEntityAttributes);
  }

  private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
    event.put(SLIME_CYRO.get(), SlimeCyro.createAttributes().build());
  }
}
