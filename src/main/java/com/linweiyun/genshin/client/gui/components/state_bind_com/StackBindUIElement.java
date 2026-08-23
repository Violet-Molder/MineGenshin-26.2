package com.linweiyun.genshin.client.gui.components.state_bind_com;

import com.linweiyun.genshin.core.character.PGCharacterData;
import com.linweiyun.genshin.core.character.PGCharacterDefine;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableUIElement;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@KJSBindings
@LDLRegister(name = "stack-bind-uie", group = "minegenshin", registry = "ldlib2:ui_element")
public class StackBindUIElement extends BindableUIElement<PGCharacterData> {
  private PGCharacterData character;

  @Override
  public PGCharacterData getValue() {
    return this.character;
  }

  @Override
  public BindableUIElement<PGCharacterData> setValue(@Nullable PGCharacterData value, boolean notify) {
    if (value != this.character) {
      this.character = value;
      if (this.character != null) {
        PGCharacterDefine def = this.character.getDefinition();
        if (def != null) {
          String textureId = def.getTextureId();
          this.style(s -> s.background(
                  SpriteTexture.of("minegenshin:textures/character_avatar/hud/" + textureId + ".png")));
        }
      } else {
        this.style(s -> s.background(null));
      }
    }
    return this;
  }
}
