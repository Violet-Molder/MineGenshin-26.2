package com.linweiyun.genshin.client.render;

import com.linweiyun.genshin.client.model.entity.monster.SlimeCyroModel;
import com.linweiyun.genshin.content.entities.entity.teyvat.monster.SlimeCyro;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SlimeCyroRenderer extends GeoEntityRenderer<SlimeCyro> {
  public SlimeCyroRenderer(EntityRendererProvider.Context context) {
    super(context, new SlimeCyroModel());
  }
}
