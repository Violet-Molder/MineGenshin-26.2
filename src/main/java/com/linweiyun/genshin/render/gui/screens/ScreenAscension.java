package com.linweiyun.genshin.render.gui.screens;

import com.linweiyun.genshin.core.attachment.AdventurerInfoAttachment;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class ScreenAscension extends Screen {
    final ModularUI modularUI;
    final Player player;

    private Label advRankLabel;
    private Label advExpLabel;
    private Label worldLevelLabel;
    private Button worldLevelBtn;
    private Label charExpLabel;
    private Label charLevelLabel;

    public ScreenAscension(Player player) {
        super(Component.empty());
        this.player = player;
        this.modularUI = createModularUI();
    }

    @Override
    public void init() {
        super.init();
        ModularUIClientAccess.setScreenAndInit(this.modularUI, this);
        this.addRenderableWidget(ModularUIClientAccess.getWidget(modularUI));
    }

    @Override
    public void tick() {
        super.tick();
        refreshLabels();
    }

    private void refreshLabels() {
        AdventurerInfoAttachment advInfo = player.getData(AttachmentRegistration.ADVENTURER_INFO_ATTACHMENT);
        advRankLabel.setText(Component.literal("冒险等级: " + advInfo.getAdventureRank()));
        if (advInfo.isMaxRank()) {
            advExpLabel.setText(Component.literal("冒险阅历: MAX"));
        } else {
            advExpLabel.setText(Component.literal("冒险阅历: " + advInfo.getCurrentExp() + " / " + advInfo.getExpToNextRank()));
        }
        worldLevelLabel.setText(Component.literal("世界等级: " + advInfo.getWorldLevel() + " / " + advInfo.getBreakthroughLevel()));

        if (advInfo.canDowngradeWorldLevel()) {
            worldLevelBtn.setText("降低世界等级");
            worldLevelBtn.setVisible(true);
        } else if (advInfo.canRestoreWorldLevel()) {
            worldLevelBtn.setText("还原世界等级");
            worldLevelBtn.setVisible(true);
        } else {
            worldLevelBtn.setVisible(false);
        }

        PlayerCharactersAttachment charAttachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        PGCharacter currentChar = charAttachment.getCurrentCharacter();
        if (currentChar != null && currentChar.getData() != null) {
            charExpLabel.setText(Component.literal("角色经验: " + currentChar.getData().getCurrentExp() + " / " + currentChar.getData().getMaxExp()));
            charLevelLabel.setText(Component.literal("角色等级: " + currentChar.getData().getLevel()
                    + " (突破" + currentChar.getData().getAscensionPhase() + "阶)"));
            charExpLabel.setVisible(true);
            charLevelLabel.setVisible(true);
        } else {
            charExpLabel.setVisible(false);
            charLevelLabel.setVisible(false);
        }
    }

    private ModularUI createModularUI() {
        var stylesheet = StylesheetManager.INSTANCE.getStylesheetSafe(
                Identifier.parse("minegenshin:lss/ascension.lss"));

        AdventurerInfoAttachment advInfo = player.getData(AttachmentRegistration.ADVENTURER_INFO_ATTACHMENT);
        PlayerCharactersAttachment charAttachment = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        PGCharacter currentChar = charAttachment.getCurrentCharacter();

        var root = new UIElement().setId("root");
        var window = new UIElement().setId("window");

        var titleLabel = new Label();
        titleLabel.setId("ascension-title");
        titleLabel.setText(Component.literal("突破"));

        var advInfoContainer = new UIElement().setId("adv-info-container");

        advRankLabel = new Label();
        advRankLabel.setId("adv-rank-label");
        advRankLabel.setText(Component.literal("冒险等级: " + advInfo.getAdventureRank()));

        advExpLabel = new Label();
        advExpLabel.setId("adv-exp-label");
        if (advInfo.isMaxRank()) {
            advExpLabel.setText(Component.literal("冒险阅历: MAX"));
        } else {
            advExpLabel.setText(Component.literal("冒险阅历: " + advInfo.getCurrentExp() + " / " + advInfo.getExpToNextRank()));
        }

        worldLevelLabel = new Label();
        worldLevelLabel.setId("world-level-label");
        worldLevelLabel.setText(Component.literal("世界等级: " + advInfo.getWorldLevel() + " / " + advInfo.getBreakthroughLevel()));

        var advBreakthroughBtn = new Button();
        advBreakthroughBtn.setId("adv-breakthrough-btn");
        advBreakthroughBtn.setText("冒险等级突破");
        advBreakthroughBtn.setOnClick(e -> {
            if (advInfo.canBreakthroughWorldLevel()) {
                NetworkManager.sendAscendAdventureRankToServer();
            } else {
                if (player.isLocalPlayer()) {
                    player.sendSystemMessage(Component.literal("不满足冒险等级突破条件"));
                }
            }
        });

        worldLevelBtn = new Button();
        worldLevelBtn.setId("world-level-btn");
        worldLevelBtn.setOnClick(e -> {
            if (advInfo.canDowngradeWorldLevel()) {
                NetworkManager.sendDowngradeWorldLevelToServer();
            } else if (advInfo.canRestoreWorldLevel()) {
                NetworkManager.sendRestoreWorldLevelToServer();
            }
        });
        if (advInfo.canDowngradeWorldLevel()) {
            worldLevelBtn.setText("降低世界等级");
        } else if (advInfo.canRestoreWorldLevel()) {
            worldLevelBtn.setText("还原世界等级");
        } else {
            worldLevelBtn.setVisible(false);
        }

        advInfoContainer.addChildren(advRankLabel, advExpLabel, worldLevelLabel, advBreakthroughBtn, worldLevelBtn);

        var charInfoContainer = new UIElement().setId("char-info-container");
        if (currentChar != null && currentChar.getData() != null) {
            charExpLabel = new Label();
            charExpLabel.setId("char-exp-label");
            charExpLabel.setText(Component.literal("角色经验: " + currentChar.getData().getCurrentExp() + " / " + currentChar.getData().getMaxExp()));

            charLevelLabel = new Label();
            charLevelLabel.setId("char-level-label");
            charLevelLabel.setText(Component.literal("角色等级: " + currentChar.getData().getLevel()
                    + " (突破" + currentChar.getData().getAscensionPhase() + "阶)"));

            var charBreakthroughBtn = new Button();
            charBreakthroughBtn.setId("char-breakthrough-btn");
            charBreakthroughBtn.setText("角色突破");

            final int maxLevelForPhase = currentChar.getData().getAscensionPhase() == 0
                    ? 20
                    : Math.min((currentChar.getData().getAscensionPhase() + 3) * 10, 90);

            charBreakthroughBtn.setOnClick(e -> {
                boolean canAscend = currentChar.getData().getLevel() >= maxLevelForPhase
                        && currentChar.getData().getLevel() < 90;
                if (canAscend) {
                    NetworkManager.sendAscendCharacterToServer();
                } else {
                    if (player.isLocalPlayer()) {
                        player.sendSystemMessage(Component.literal("角色不满足突破条件（需达到" + maxLevelForPhase + "级）"));
                    }
                }
            });

            charInfoContainer.addChildren(charExpLabel, charLevelLabel, charBreakthroughBtn);
        } else {
            charExpLabel = new Label();
            charExpLabel.setId("char-exp-label");
            charExpLabel.setText(Component.literal("角色经验: - / -"));
            charExpLabel.setVisible(false);

            charLevelLabel = new Label();
            charLevelLabel.setId("char-level-label");
            charLevelLabel.setText(Component.literal("角色等级: -"));

            var noCharLabel = new Label();
            noCharLabel.setId("no-char-label");
            noCharLabel.setText(Component.literal("当前没有选中角色"));
            charInfoContainer.addChildren(charExpLabel, charLevelLabel, noCharLabel);
        }

        window.addChildren(titleLabel, advInfoContainer, charInfoContainer);

        root.layout(layout -> {
            layout.widthPercent(100f);
            layout.heightPercent(100f);
        });

        root.addChildren(window);

        var ui = UI.of(root, stylesheet);
        return ModularUI.of(ui, player);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}