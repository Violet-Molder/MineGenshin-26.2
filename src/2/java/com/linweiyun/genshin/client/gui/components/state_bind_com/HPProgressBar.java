package com.linweiyun.genshin.client.gui.components.state_bind_com;

import com.linweiyun.genshin.Minegenshin;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataProvider;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import dev.vfyjxf.taffy.style.TaffyPosition;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import org.appliedenergistics.yoga.YogaOverflow;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@KJSBindings
@LDLRegister(name = "hp-progress-bar", group = "minegenshin", registry = "ldlib2:ui_element")
public class HPProgressBar extends ProgressBar {
  protected final Map<IDataProvider<ItemStack>, ISubscription> characterSources =
      new LinkedHashMap<>();
  public final UIElement barIcon;

  public HPProgressBar() {
    this.barContainer.layout(
        layout -> {
          layout.paddingAll(0);
          layout.widthPercent(100);
          layout.heightPercent(100);
        });
    this.bar
        .addChild(barIcon = new UIElement())
        .layout(
            layout -> {
              layout.positionType(TaffyPosition.ABSOLUTE);
              layout.overflow(YogaOverflow.HIDDEN);
            })
        .style(s -> s.background(SpriteTexture.of(Minegenshin.id("textures/empty.png"))));
  }
}
