package com.linweiyun.genshin.client.gui.hud;

import com.lowdragmc.lowdraglib2.gui.hud.ModularHudLayer;
import com.lowdragmc.lowdraglib2.math.Size;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface MyModularHudLayer extends ModularHudLayer {
  @Override
  default Size getScreenSize() {
    return Size.of(
        Minecraft.getInstance().getWindow().getGuiScaledWidth(),
        Minecraft.getInstance().getWindow().getGuiScaledHeight());
  }
}
