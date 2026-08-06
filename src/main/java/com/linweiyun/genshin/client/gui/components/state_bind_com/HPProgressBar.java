package com.linweiyun.genshin.client.gui.components.state_bind_com;

import com.linweiyun.genshin.core.character.PGCharacterData;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataProvider;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.resources.Identifier;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
@KJSBindings
@LDLRegister(name = "hp-progress-bar", group = "minegenshin", registry = "ldlib2:ui_element")
public class HPProgressBar extends ProgressBar {
    protected final Map<IDataProvider<PGCharacterData>, ISubscription> characterSources =
            new LinkedHashMap<>();
    public final UIElement barIcon;

    public HPProgressBar() {
        this.barContainer.layout(layout -> {
            layout.paddingAll(0);
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        this.bar
                .addChild(barIcon = new UIElement())
                .layout(layout -> {
                    layout.positionType(TaffyPosition.ABSOLUTE);
                })
                .style(s -> s.background(SpriteTexture.of(
                        Identifier.fromNamespaceAndPath("minegenshin", "textures/empty.png"))));
    }
}
