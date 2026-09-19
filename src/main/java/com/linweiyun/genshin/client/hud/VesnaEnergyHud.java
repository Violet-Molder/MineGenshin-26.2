package com.linweiyun.genshin.client.hud;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.character.sword.vesna.Vesna;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class VesnaEnergyHud {

    private static final int RECT_WIDTH = 12;
    private static final int RECT_HEIGHT = 24;
    private static final int RECT_GAP = 3;
    private static final int MAX_STACKS = 3;
    private static final int ENERGY_PER_STACK = 6;

    private static final String CLASS_STACK = "vesna-energy-stack";
    private static final String CLASS_FILLED = "filled";
    private static final String CLASS_EMPTY = "empty";
    private static final String CLASS_PARTIAL = "partial";

    private static UIElement hudRoot;
    private static final UIElement[] stacks = new UIElement[MAX_STACKS];

    public static UIElement buildHudRoot() {
        hudRoot = new UIElement()
                .setId("vesna-energy-root")
                .layout(l -> l.widthPercent(100).heightPercent(100));

        for (int i = 0; i < MAX_STACKS; i++) {
            stacks[i] = new UIElement()
                    .addClass(CLASS_STACK)
                    .addClass(CLASS_EMPTY)
                    .layout(l -> l
                            .positionType(TaffyPosition.ABSOLUTE)
                            .width(RECT_WIDTH).height(RECT_HEIGHT));
            hudRoot.addChild(stacks[i]);
        }

        return hudRoot;
    }

    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        updateHud();
    }

    private static void updateHud() {
        if (hudRoot == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        PGCharacter character = getCurrentCharacter(mc);
        if (!(character instanceof Vesna vesna) || !vesna.isWindriderActive()) {
            hudRoot.style(s -> s.opacity(0f));
            return;
        }

        hudRoot.style(s -> s.opacity(1f));

        int x, y;
        if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            x = screenWidth / 2 - 91 - RECT_WIDTH - 8;
            y = screenHeight - 39 - (MAX_STACKS * (RECT_HEIGHT + RECT_GAP));
        } else {
            Vec3 worldPos = mc.player.position().add(0, 1.2, 0);
            Vec3 ndc = mc.gameRenderer.projectPointToScreen(worldPos);
            if (ndc == null || ndc.z <= 0) {
                hudRoot.style(s -> s.opacity(0f));
                return;
            }

            double windowWidth = mc.getWindow().getWidth();
            double windowHeight = mc.getWindow().getHeight();
            double screenX = (ndc.x + 1.0) * 0.5 * windowWidth;
            double screenY = (1.0 - (ndc.y + 1.0) * 0.5) * windowHeight;
            x = (int) (screenX / mc.getWindow().getGuiScale()) - RECT_WIDTH - 20;
            y = (int) (screenY / mc.getWindow().getGuiScale()) - (MAX_STACKS * (RECT_HEIGHT + RECT_GAP)) / 2;
        }

        float energy = vesna.getVesnaEnergy();
        int filledStacks = (int) (energy / ENERGY_PER_STACK);
        float partialFill = (energy % ENERGY_PER_STACK) / ENERGY_PER_STACK;

        for (int i = 0; i < MAX_STACKS; i++) {
            int rectY = y + (MAX_STACKS - 1 - i) * (RECT_HEIGHT + RECT_GAP);
            stacks[i].layout(l -> l
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(x).top(rectY)
                    .width(RECT_WIDTH).height(RECT_HEIGHT));

            stacks[i].removeClass(CLASS_FILLED);
            stacks[i].removeClass(CLASS_PARTIAL);
            stacks[i].removeClass(CLASS_EMPTY);

            if (i < filledStacks) {
                stacks[i].addClass(CLASS_FILLED);
            } else if (i == filledStacks && partialFill > 0) {
                stacks[i].addClass(CLASS_PARTIAL);
                int fillHeight = Math.max(1, (int) (RECT_HEIGHT * partialFill));
                stacks[i].layout(l -> l.height(fillHeight));
            } else {
                stacks[i].addClass(CLASS_EMPTY);
            }
        }
    }

    private static PGCharacter getCurrentCharacter(Minecraft mc) {
        if (mc.player == null) return null;
        var attachment = mc.player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        return attachment.getCurrentCharacter();
    }
}