package com.linweiyun.genshin.client.gui.components.state_bind_com;

import com.linweiyun.genshin.Minegenshin;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataProvider;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.util.ITickable;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import dev.vfyjxf.taffy.style.*;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.appliedenergistics.yoga.YogaOverflow;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@KJSBindings
@LDLRegister(name = "skill-progress-bar", group = "minegenshin", registry = "ldlib2:ui_element")
public class SkillProgressBar extends ProgressBar {
  private ItemStack character = ItemStack.EMPTY;
  public final UIElement barIcon;
  protected final Map<IDataProvider<ItemStack>, ISubscription> characterSources =
      new LinkedHashMap<>();

  private static final int SKILL_ICON_SIZE = 40;

  public SkillProgressBar() {
    super();
    this.barContainer.layout(
        layout -> {
          layout.paddingAll(0);
          layout.positionType(TaffyPosition.ABSOLUTE);
        });
    bar.layout(
            layout -> {
              layout.positionType(TaffyPosition.ABSOLUTE);
              layout.overflow(YogaOverflow.HIDDEN);
              layout.width(SKILL_ICON_SIZE);
              layout.height(SKILL_ICON_SIZE);
              layout.positionType(TaffyPosition.ABSOLUTE);
            })
        .style(s -> s.opacity(1));
    bar.addChild(
        barIcon =
            new UIElement()
                .layout(
                    layout -> {
                      layout.width(SKILL_ICON_SIZE);
                      layout.height(SKILL_ICON_SIZE);
                      layout.positionType(TaffyPosition.ABSOLUTE);
                    }));
    this.label.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE));
  }

  public SkillProgressBar(int width, int height) {
    super();
    this.barContainer.layout(layout -> layout.paddingAll(0));

    bar.layout(
            layout -> {
              layout.positionType(TaffyPosition.ABSOLUTE);
              layout.overflow(YogaOverflow.HIDDEN);
              layout.width(width);
              layout.height(height);
              layout.positionType(TaffyPosition.ABSOLUTE);
            })
        .style(s -> s.background(SpriteTexture.of(Minegenshin.id("textures/empty.png"))));

    bar.addChild(
        barIcon =
            new UIElement()
                .layout(
                    layout -> {
                      layout.width(width);
                      layout.height(height);
                      layout.positionType(TaffyPosition.ABSOLUTE);
                    }));
    this.label.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE));
  }

  public SkillProgressBar bindCharacterSource(
      IDataProvider<ItemStack> characterProvider, int flagProvider) {

    if (characterProvider instanceof ITickable tickable) {
      UIEventListener tickableListener = e -> tickable.tick();
      addEventListener(UIEvents.TICK, tickableListener);
    }
    ISubscription subscription = null;
    if (flagProvider == 0) {
      subscription = characterProvider.registerListener(this::setCharacterSkill, true);
    } else if (flagProvider == 1) {
      subscription = characterProvider.registerListener(this::setCharacterBurst, true);
    }

    if (subscription != null) {
      this.characterSources.put(characterProvider, subscription);
    }

    return this;
  }

  private void setCharacterBurst(ItemStack stack) {
    this.character = stack;
    String characterID = BuiltInRegistries.ITEM.getKey(this.character.getItem()).getPath();
    this.barContainer.style(
        s ->
            s.background(
                SpriteTexture.of(Minegenshin.id("textures/skill/" + characterID + "_burst.png"))));
    this.barIcon.style(
        s -> s.background(SpriteTexture.of(Minegenshin.id("textures/skill/cd.png"))));
  }

  private void setCharacterSkill(ItemStack stack) {
    this.character = stack;
    String characterID = BuiltInRegistries.ITEM.getKey(this.character.getItem()).getPath();
    this.barContainer.style(
        s ->
            s.background(
                SpriteTexture.of(Minegenshin.id("textures/skill/" + characterID + "_skill.png"))));
    this.barIcon.style(
        s -> s.background(SpriteTexture.of(Minegenshin.id("textures/skill/cd.png"))));
  }

  public SkillProgressBar unbindCharacterSource(IDataProvider<ItemStack> dataProvider) {
    var removed = this.characterSources.remove(dataProvider);
    if (removed != null) {
      removed.unsubscribe();
    }
    return this;
  }
}
