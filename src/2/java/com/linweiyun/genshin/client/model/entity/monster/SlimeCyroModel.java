package com.linweiyun.genshin.client.model.entity.monster;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.entities.entity.teyvat.monster.SlimeCyro;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SlimeCyroModel extends GeoModel<SlimeCyro> {
  private final ResourceLocation model = Minegenshin.id("geo/slime_cyro.geo.json");
  private final ResourceLocation texture = Minegenshin.id("textures/entity/monster/slime_cyro.png");
  private final ResourceLocation animations =
      Minegenshin.id("animations/slime_cyro.animation.json");

  @Override
  public ResourceLocation getModelResource(SlimeCyro animatable) {
    return model;
  }

  @Override
  public ResourceLocation getTextureResource(SlimeCyro animatable) {
    return texture;
  }

  @Override
  public ResourceLocation getAnimationResource(SlimeCyro animatable) {
    return animations;
  }
}
