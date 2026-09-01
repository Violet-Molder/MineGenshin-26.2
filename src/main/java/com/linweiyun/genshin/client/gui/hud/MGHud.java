package com.linweiyun.genshin.client.gui.hud;

import com.google.common.base.Suppliers;
import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.client.gui.components.state_bind_com.BooleanDisplayBindUIElement;
import com.linweiyun.genshin.client.gui.components.state_bind_com.HPProgressBar;
import com.linweiyun.genshin.client.gui.components.state_bind_com.SkillProgressBar;
import com.linweiyun.genshin.client.gui.components.state_bind_com.StackBindUIElement;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attribute.ModAttributes;
import com.linweiyun.genshin.core.character.PGCharacterData;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class MGHud {

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        var hudUICache =
                Suppliers.memoize(
                        () -> {
                            var stylesheet =
                                    StylesheetManager.INSTANCE.getStylesheetSafe(
                                            Minegenshin.id("lss/hud/character_party.lss"));
                            var ui = UI.of(buildCharacterPartyHud(), stylesheet);
                            return ModularUI.of(ui);
                        });
        event.registerAboveAll(Minegenshin.id("simple_hud"), (MyModularHudLayer) hudUICache::get);
    }

    private static UIElement buildCharacterPartyHud() {
        var root =
                new BooleanDisplayBindUIElement()
                        .bindDataSource(
                                SupplierDataSource.of(
                                        () -> {
                                            var player = Minecraft.getInstance().player;
                                            if (player == null) return false;
                                            return player.getData(AttachmentRegistration.GENSHIN_MODE_ATTACHMENT);
                                        }))
                        .setId("root")
                        .layout(l -> l.widthPercent(100).heightPercent(100))
                        .lss("position", "absolute");

        var characterList = new UIElement();
        var currentContent =
                (BooleanDisplayBindUIElement)
                        new BooleanDisplayBindUIElement().setId("current-content").addClass("__unselected__");
        var characterLevel = new Label();
        var currentCharacterHP = new HPProgressBar();
        var characterSkillIcon = new SkillProgressBar(40, 40);
        var characterBurstIcon = new SkillProgressBar(40, 40);
        characterList.setId("character_list");

        for (int i = 0; i < 4; i++) {
            int slotIndex = i;
            var characterSate =
                    (BooleanDisplayBindUIElement)
                            new BooleanDisplayBindUIElement().setId("character_state").addClass("__unselected__");
            var characterIcon = (StackBindUIElement) new StackBindUIElement().setId("character_icon");
            var iconLeftUIElement = new UIElement().setId("icon-left");
            var characterName = (Label) new Label().setId("character_name");
            var selectedGroup =
                    (BooleanDisplayBindUIElement)
                            new BooleanDisplayBindUIElement()
                                    .setId("selected_group")
                                    .addClass("__unselected__")
                                    .addClass("display");
            var notSelectedGroup =
                    (BooleanDisplayBindUIElement)
                            new BooleanDisplayBindUIElement()
                                    .setId("not_selected_group")
                                    .addClass("__unselected__")
                                    .addClass("display");
            var characterHP = (HPProgressBar) new HPProgressBar().setId("character_party_hp");

            characterSate
                    .bindDataSource(
                            SupplierDataSource.of(
                                    () -> {
                                        var player = Minecraft.getInstance().player;
                                        if (player != null) {
                                            var attachment =
                                                    player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
                                            return attachment.getPartyCharacter(slotIndex) != null;
                                        }
                                        return false;
                                    }))
                    .layout(
                            layout -> {
                                layout.flexDirection(FlexDirection.ROW);
                                layout.gapAll(6);
                                layout.display(TaffyDisplay.FLEX);
                            });

            characterIcon.bindDataSource(
                    SupplierDataSource.of(
                            () -> {
                                var player = Minecraft.getInstance().player;
                                if (player != null) {
                                    var attachment =
                                            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
                                    return attachment.getPartyCharacter(slotIndex);
                                }
                                return null;
                            }));

            selectedGroup.bindDataSource(
                    SupplierDataSource.of(
                            () -> {
                                var player = Minecraft.getInstance().player;
                                if (player != null) {
                                    var attachment =
                                            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
                                    return attachment.getCurrentCharacterIndex() == slotIndex;
                                }
                                return false;
                            }));

            notSelectedGroup.bindDataSource(SupplierDataSource.of(() -> !selectedGroup.getValue()));

            characterName.bindDataSource(
                    SupplierDataSource.of(
                            () -> {
                                PGCharacter character = characterIcon.getValue();
                                if (character == null) return Component.literal("");
                                return character.getName();
                            }));

            characterHP
                    .bindDataSource(
                            SupplierDataSource.of(
                                    () -> {
                                        PGCharacter character = characterIcon.getValue();
                                        if (character == null) return 1f;
                                        PGCharacterData data = character.getData();
                                        if (data != null) {
                                            return (float) (data.getCurrentHP() / data.getAttributeTotalValue(ModAttributes.MAX_HP.value()));
                                        }
                                        return 1f;
                                    }))
                    .label
                    .setText("")
                    .setId("character_party_hp");
            characterHP.layout(
                    layout -> {
                        layout.width(80);
                        layout.height(4);
                        layout.aspectRatio(20);
                    });
            characterHP
                    .barIcon
                    .style(
                            s ->
                                    s.background(
                                            SpriteTexture.of(
                                                    Minegenshin.id("textures/gui/short_character_hp_bar_green.png"))))
                    .layout(
                            l -> {
                                l.width(80);
                                l.aspectRatio(20);
                            });
            characterHP.barContainer(
                    c ->
                            c.style(
                                    s ->
                                            s.background(
                                                    SpriteTexture.of(
                                                            Minegenshin.id("textures/gui/short_character_hp_green.png")))));

            characterList.addChild(
                    characterSate.addChildren(
                            characterIcon,
                            iconLeftUIElement.addChildren(
                                    characterName,
                                    selectedGroup.addChildren(),
                                    notSelectedGroup.addChildren(characterHP))));
        }

        characterSkillIcon
                .bindDataSource(
                        SupplierDataSource.of(
                                () -> {
                                    var player = Minecraft.getInstance().player;
                                    if (player == null) return 0f;
                                    var attachment =
                                            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
                                    var character = attachment.getCurrentCharacter();
                                    if (character != null) {
                                        var data = character.getData();
                                        if (data != null) {
                                            return data.getElementalSkillCooldownTick() / character.getSkillShortMaxCooldownTick();
                                        }
                                    }
                                    return 0f;
                                }))
                .layout(
                        layout -> {
                            layout.width(40);
                            layout.height(40);
                        })
                .setId("character_skill_icon");
        characterSkillIcon.bindCharacterSource(
                SupplierDataSource.of(
                        () -> {
                            var player = Minecraft.getInstance().player;
                            if (player != null) {
                                var attachment =
                                        player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
                                return attachment.getCurrentCharacter();
                            }
                            return null;
                        }),
                0);
        characterSkillIcon.label.bindDataSource(
                SupplierDataSource.of(
                        () -> {
                            characterSkillIcon.label.textStyle(style -> style.fontSize(14));
                            var player = Minecraft.getInstance().player;
                            if (player == null) return Component.literal("");
                            var attachment =
                                    player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
                            var character = attachment.getCurrentCharacter();
                            if (character != null) {
                                var data = character.getData();
                                if (data != null) {
                                    float currentSkillCD =
                                            Math.round(data.getElementalSkillCooldownTick() / 20 * 10f) / 10f;
                                    if (currentSkillCD <= 0) return Component.literal("");
                                    return Component.literal(String.valueOf(currentSkillCD));
                                }

                            }
                            return Component.literal("");
                        }));

        characterBurstIcon
                .bindDataSource(
                        SupplierDataSource.of(
                                () -> {
                                    var player = Minecraft.getInstance().player;
                                    if (player == null) return 0f;
                                    var attachment =
                                            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
                                    var character = attachment.getCurrentCharacter();
                                    if (character != null) {
                                        var data = character.getData();
                                        if (data != null) {
                                            return data.getElementalBurstCooldownTick() / character.getBurstMaxCooldownTick();
                                        }
                                    }
                                    return 0f;
                                }))
                .layout(
                        layout -> {
                            layout.width(40);
                            layout.height(40);
                        })
                .setId("character_burst_icon");
        characterBurstIcon.bindCharacterSource(
                SupplierDataSource.of(
                        () -> {
                            var player = Minecraft.getInstance().player;
                            if (player != null) {
                                var attachment =
                                        player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
                                return attachment.getCurrentCharacter();
                            }
                            return null;
                        }),
                0);
        characterBurstIcon.label.bindDataSource(
                SupplierDataSource.of(
                        () -> {
                            characterBurstIcon.label.textStyle(style -> style.fontSize(14));
                            var player = Minecraft.getInstance().player;
                            if (player == null) return Component.literal("");
                            var attachment =
                                    player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
                            var character = attachment.getCurrentCharacter();
                            if (character != null) {
                                var data = character.getData();
                                float currentBurstCD =
                                        Math.round(data.getElementalBurstCooldownTick() / 20 * 10f) / 10f;
                                if (currentBurstCD <= 0) return Component.literal("");
                                return Component.literal(String.valueOf(currentBurstCD));
                            }
                            return Component.literal("");
                        }));

        currentCharacterHP
                .bindDataSource(
                        SupplierDataSource.of(
                                () -> {
                                    var player = Minecraft.getInstance().player;
                                    if (player == null) return 0f;
                                    var attachment =
                                            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
                                    var character = attachment.getCurrentCharacter();
                                    if (character != null) {
                                        var data = character.getData();
                                        return (float) (data.getCurrentHP() / data.getAttributeTotalValue(ModAttributes.MAX_HP.value()));
                                    }
                                    return 0f;
                                }))
                .setId("current_character_hp")
                .layout(
                        l -> {
                            l.width(210);
                            l.heightAuto();
                            l.aspectRatio(35);
                        });
        currentCharacterHP
                .barIcon
                .style(
                        s ->
                                s.background(
                                        SpriteTexture.of(
                                                Minegenshin.id("textures/gui/long_character_hp_bar_green.png"))))
                .layout(
                        l -> {
                            l.width(210);
                            l.aspectRatio(35);
                        });
        currentCharacterHP.barContainer(
                c ->
                        c.style(
                                s ->
                                        s.background(
                                                SpriteTexture.of(
                                                        Minegenshin.id("textures/gui/long_character_hp_green.png")))));
        currentCharacterHP.label.bindDataSource(
                SupplierDataSource.of(
                        () -> {
                            var player = Minecraft.getInstance().player;
                            if (player == null) return Component.empty();
                            var attachment =
                                    player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
                            var character = attachment.getCurrentCharacter();
                            if (character != null) {
                                var data = character.getData();
                                if (data != null) {
                                    return Component.literal(
                                            Math.round(data.getCurrentHP() * 10.0f) / 10.0f
                                                    + "/"
                                                    + data.getAttributeTotalValue(ModAttributes.MAX_HP.value()));
                                }

                            }
                            return Component.empty();
                        }));

        characterLevel.bindDataSource(
                        SupplierDataSource.of(
                                () -> {
                                    var player = Minecraft.getInstance().player;
                                    if (player == null) return Component.empty();
                                    var attachment =
                                            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
                                    var character = attachment.getCurrentCharacter();
                                    if (character != null) {
                                        var data = character.getData();
                                        if (data != null) {
                                            return Component.literal("Lv." + data.getLevel());
                                        }
                                    }
                                    return Component.empty();
                                }))
                .setId("character-level");

        // 属性调试
        var attributeDebug = new Label();
        attributeDebug.bindDataSource(
                        SupplierDataSource.of(
                                () -> {
                                    var player = Minecraft.getInstance().player;
                                    if (player == null) return Component.empty();
                                    var attachment =
                                            player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
                                    var character = attachment.getCurrentCharacter();
                                    if (character != null) {
                                        var data = character.getData();
                                        if (data != null) {
                                            return Component.literal(
                                                    "当前攻击力：" + (int) data.getAttributeTotalValue(ModAttributes.ATK.value())
                                                            + "\n当前防御力：" + (int) data.getAttributeTotalValue(ModAttributes.DEF.value())
                                                            + "\n当前生命值：" + (int) data.getAttributeTotalValue(ModAttributes.MAX_HP.value()));
                                        }
                                    }
                                    return Component.empty();
                                }))
                .setId("character-attribute");

        currentContent.bindDataSource(
                SupplierDataSource.of(
                        () -> {
                            var player = Minecraft.getInstance().player;
                            if (player != null) {
                                var attachment =
                                        player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
                                return attachment.getCurrentCharacter() != null;
                            }
                            return false;
                        }));

        var energy =
                new Label()
                        .bindDataSource(
                                SupplierDataSource.of(
                                        () -> {
                                            var player = Minecraft.getInstance().player;
                                            if (player == null) return Component.literal("");
                                            var attachment =
                                                    player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
                                            var character = attachment.getCurrentCharacter();
                                            if (character != null) {
                                                var data = character.getData();
                                                if (data != null) {
                                                    return Component.literal(
                                                            Math.round(data.getCurrentObtainingEnergy() * 10f) / 10f
                                                                    + "/"
                                                                    + character.getMaxObtainingEnergy());
                                                }
                                            }
                                            return Component.literal("");
                                        }));

        root.addChildren(
                characterList,
                currentContent
                        .addChildren(
                                attributeDebug,
                                characterLevel,
                                currentCharacterHP,
                                new UIElement()
                                        .setId("skill-content")
                                        .addChildren(characterSkillIcon, characterBurstIcon, energy))
                        .layout(
                                l -> {
                                    l.widthPercent(100);
                                    l.heightPercent(100);
                                }));

        return root;
    }
}
