package com.linweiyun.genshin.client.gui.components.state_bind_com;

import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableUIElement;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@KJSBindings
@LDLRegister(name = "boolean-bind-uie", group = "minegenshin", registry = "ldlib2:ui_element")
public class BooleanDisplayBindUIElement extends BindableUIElement<Boolean> {
  public boolean display;

  @Override
  public Boolean getValue() {
    return this.display;
  }

  @Override
  public BindableUIElement<Boolean> setValue(@Nullable Boolean value, boolean notify) {
    if (Boolean.TRUE.equals(value) != this.display) {
      this.display = Boolean.TRUE.equals(value);
      if (Boolean.TRUE.equals(value)) {
        this.addClass("__selected__");
        this.removeClass("__unselected__");
      } else {
        this.addClass("__unselected__");
        this.removeClass("__selected__");
      }
    }

    return this;
  }
}
