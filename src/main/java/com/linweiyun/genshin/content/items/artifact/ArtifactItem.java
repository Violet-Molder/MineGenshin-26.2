package com.linweiyun.genshin.content.items.artifact;

import com.linweiyun.genshin.content.effect.character.artifact.ArtifactSetEffect;
import com.linweiyun.genshin.content.items.TeyvatItem;
import com.linweiyun.genshin.content.items.component.ArtifactStatsComponent;
import com.linweiyun.genshin.content.stat.TeyvatItemStat;
import com.linweiyun.genshin.core.system.registry.ModRegistries;
import com.linweiyun.genshin.core.system.registry.register.ModDataComponents;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ArtifactItem extends TeyvatItem {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected ArtifactType type;
    protected DeferredHolder<ArtifactSet, ArtifactSet> set;

    public ArtifactItem(Properties properties) {
        super(properties);
    }

    public static void initializeArtifactStackIfNeeded(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArtifactItem artifact)) return;

        ArtifactStatsComponent stats = stack.getOrDefault(
                ModDataComponents.ARTIFACT_STATS.get(),
                ArtifactStatsComponent.DEFAULT
        );
        //TEMP mainStat为null说明是未初始化的默认空值，才生成随机属性
        if (stats.mainStat == null) {

            ArtifactStatsComponent generated = buildInitialStats(artifact.getStar(), artifact.getType());
            stack.set(ModDataComponents.ARTIFACT_STATS.get(), generated);
            LOGGER.info("Initializing artifact stack for {}", artifact);
        }
    }
    protected static ArtifactStatsComponent buildInitialStats(int star, ArtifactType type) {
        java.util.Random random = new java.util.Random();
        TeyvatItemStat mainStat = ArtifactMainStatGenerator.generate(type, star, random);
        java.util.List<TeyvatItemStat> subStats = mainStat != null
                ? ArtifactSubStatGenerator.generateAll(star, type, mainStat.getAttribute(), mainStat.getKind(), random)
                : java.util.List.of();
        return new ArtifactStatsComponent(1, 0, mainStat, subStats);
    }
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        ArtifactStatsComponent stats = stack.getOrDefault(ModDataComponents.ARTIFACT_STATS.get(), ArtifactStatsComponent.DEFAULT);

        // 第一行：小字，圣遗物类型
        builder.accept(Component.translatable("artifact.type." + type.name().toLowerCase()).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.empty());

        // 主词条（大字 + 星级颜色）
        if (stats.mainStat != null) {
            ChatFormatting starColor = getStarColor();
            builder.accept(Component.literal(buildStatText(stats.mainStat)).withStyle(starColor, ChatFormatting.BOLD));
        }

        // 副词条
        if (stats.subStats != null && !stats.subStats.isEmpty()) {
            builder.accept(Component.empty());
            for (var stat : stats.subStats) {
                String text = buildStatText(stat);
                if (stat.isUnlocked()) {
                    builder.accept(Component.literal(text).withStyle(ChatFormatting.GRAY));
                } else {
                    builder.accept(Component.literal(text).withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        }

        // 套装信息
        if (set != null) {
            ArtifactSet artifactSet = set.get();
            builder.accept(Component.empty());
            var setKey = ModRegistries.ARTIFACT_SET_REGISTRY.getKey(artifactSet);
            String setNameKey = setKey != null ? "artifact_set." + setKey.getPath() : "artifact_set.unknown";
            builder.accept(Component.translatable(setNameKey).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

            var twoPc = artifactSet.twoPcEffect().get();
            if (twoPc instanceof ArtifactSetEffect setEffect) {
                builder.accept(Component.literal("2  ").withStyle(ChatFormatting.YELLOW).append(setEffect.getDescription()));
            }
            if (star >= 4 && artifactSet.hasFourPcEffect()) {
                var fourPc = artifactSet.fourPcEffect().get();
                if (fourPc instanceof ArtifactSetEffect setEffect) {
                    builder.accept(Component.literal("4  ").withStyle(ChatFormatting.YELLOW).append(setEffect.getDescription()));
                }
            }
        }
    }
    private String buildStatText(TeyvatItemStat stat) {
        String attrName = Component.translatable(stat.getAttribute().translationKey()).getString();
        if (stat.getKind() == TeyvatItemStat.StatKind.PERCENT) {
            return attrName + " +" + String.format("%.1f%%", stat.getValue());
        } else {
            return attrName + " +" + String.format("%.0f", stat.getValue());
        }
    }

    private ChatFormatting getStarColor() {
        return switch (star) {
            case 5 -> ChatFormatting.GOLD;
            case 4 -> ChatFormatting.LIGHT_PURPLE;
            case 3 -> ChatFormatting.AQUA;
            case 2 -> ChatFormatting.WHITE;
            default -> ChatFormatting.GRAY;
        };
    }

    public ArtifactType getType() {return type;}
    public DeferredHolder<ArtifactSet, ArtifactSet> getSet() {return set;}
}
