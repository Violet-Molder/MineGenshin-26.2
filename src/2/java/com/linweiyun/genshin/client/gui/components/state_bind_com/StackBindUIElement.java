package com.linweiyun.genshin.client.gui.components.state_bind_com;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableUIElement;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@KJSBindings
@LDLRegister(name = "stack-bind-uie", group = "minegenshin", registry = "ldlib2:ui_element")
public class StackBindUIElement extends BindableUIElement<ItemStack> {
  public ItemStack character;

  @Override
  public ItemStack getValue() {
    return this.character;
  }

  @Override
  public BindableUIElement<ItemStack> setValue(@Nullable ItemStack value, boolean notify) {
    if (value != this.character) {
      this.character = value;
      String characterID;
      if (this.character != null) {
        characterID = BuiltInRegistries.ITEM.getKey(this.character.getItem()).getPath() + ".png";
      } else {
        characterID = null;
      }
      this.style(
          s ->
              s.background(
                  SpriteTexture.of("minegenshin:textures/character_avatar/hud/" + characterID)));
    }

    return this;
  }
}
