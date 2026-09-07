package com.linweiyun.genshin.render.gui.components.state_bind_com;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableUIElement;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@KJSBindings
@LDLRegister(name = "stack-bind-uie", group = "minegenshin", registry = "ldlib2:ui_element")
public class StackBindUIElement extends BindableUIElement<PGCharacter> {
  private PGCharacter character;

  @Override
  public PGCharacter getValue() {
    return this.character;
  }

  @Override
  public BindableUIElement<PGCharacter> setValue(@Nullable PGCharacter value, boolean notify) {
    if (value != this.character) {
      this.character = value;
      if (this.character != null) {
          String textureId = character.getTextureId();
          this.style(s -> s.background(
                  SpriteTexture.of("minegenshin:textures/character_avatar/hud/" + textureId + ".png")));
      } else {
        this.style(s -> s.background(null));
      }
    }
    return this;
  }
}
