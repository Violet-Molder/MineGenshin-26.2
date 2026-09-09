package com.linweiyun.genshin.render.gui.components.state_bind_com;

import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@KJSBindings
@LDLRegister(name = "custom-item-slot", group = "minegenshin", registry = "ldlib2:ui_element")
public class CustomItemSlot extends ItemSlot {
    public CustomItemSlot() {
        super();
    }
}

