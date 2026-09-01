package com.linweiyun.genshin.content.effects.itemstacks.registries;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.effects.itemstacks.IItemStackEffect;
import com.linweiyun.genshin.content.effects.itemstacks.character.skill.RegenerationEffect;
import com.linweiyun.genshin.content.effects.itemstacks.character.skill.columbina.GravityRippleEffect;
import com.linweiyun.genshin.content.effects.itemstacks.character.skill.shenhe.IcyQuillEffect;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemStackEffects {
  public static final DeferredRegister<IItemStackEffect> REGISTRY =
      DeferredRegister.create(
          ItemStackEffectRegistries.ITEM_STACK_EFFECT_REGISTRY_KEY, Minegenshin.MOD_ID);
  public static final Supplier<RegenerationEffect> REGENERATION_EFFECT =
      REGISTRY.register("regeneration", RegenerationEffect::new);
  public static final Supplier<IcyQuillEffect> ICE_QUILL_EFFECT =
      REGISTRY.register("ice_quill", IcyQuillEffect::new);
  public static final Supplier<IItemStackEffect> GRAVITY_RIPPLE_EFFECT =
      REGISTRY.register("gravity_ripple", GravityRippleEffect::new);

  public static void register(IEventBus bus) {
    REGISTRY.register(bus);
  }
}
