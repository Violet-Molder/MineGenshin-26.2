package com.linweiyun.genshin.content.items.artifact;

import com.linweiyun.genshin.content.effect.character.artifact.ArtifactSetEffect;
import com.linweiyun.genshin.content.items.TeyvatItem;
import com.linweiyun.genshin.content.items.component.ArtifactStatsComponent;
import com.linweiyun.genshin.content.stat.TeyvatItemStat;
import com.linweiyun.genshin.core.system.registry.ModRegistries;
import com.linweiyun.genshin.core.system.registry.register.ModDataComponents;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class ArtifactItem extends TeyvatItem {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected ArtifactType type;
    protected DeferredHolder<ArtifactSet, ArtifactSet> set;

    public ArtifactItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack artifactStack = player.getItemInHand(hand);
        ArtifactItem.initializeArtifactStackIfNeeded(artifactStack);
        return super.use(level, player, hand);
    }

    public static void initializeArtifactStackIfNeeded(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArtifactItem artifact)) return;
        ArtifactStatsComponent stats = stack.getOrDefault(
                ModDataComponents.ARTIFACT_STATS.get(),
                ArtifactStatsComponent.DEFAULT
        );
        //TEMP mainStat 为 null 说明是未初始化的默认值，才生成随机属性
        if (!stats.mainStat.isInitialized()) {
            ArtifactStatsComponent generated = buildInitialStats(artifact.getStar(), artifact.getType());
            stack.set(ModDataComponents.ARTIFACT_STATS.get(), generated);
            LOGGER.info("Initialized artifact stats for stack: {}", stack);
                   }
    }
    protected static ArtifactStatsComponent buildInitialStats(int star, ArtifactType type) {
        Random random = new Random();
        TeyvatItemStat mainStat = ArtifactMainStatGenerator.generate(type, star, random);
        List<TeyvatItemStat> subStats = ArtifactSubStatGenerator.generateAll(star, type, mainStat.getAttribute(), mainStat.getKind(), random);
        return new ArtifactStatsComponent(0, 0, mainStat, subStats);
    }
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        ArtifactStatsComponent stats = stack.getOrDefault(ModDataComponents.ARTIFACT_STATS.get(), ArtifactStatsComponent.DEFAULT);

        // 第一行：小字，圣遗物类型
        builder.accept(Component.translatable("artifact.type." + type.name().toLowerCase()).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.literal("★".repeat(star)).withStyle(ChatFormatting.GOLD));
        builder.accept(Component.literal("+" + stats.level).withStyle(ChatFormatting.GRAY));

        // 主词条（大字 + 星级颜色）
        //TEMP mainStat.getAttribute() 为 null 说明未初始化（创造栏默认组件），跳过不显示
        if (stats.mainStat != null && stats.mainStat.isInitialized()) {
            ChatFormatting starColor = getStarColor();
            builder.accept(Component.literal(buildStatText(stats.mainStat)).withStyle(starColor, ChatFormatting.BOLD));
        }

        // 副词条
        if (stats.subStats != null && !stats.subStats.isEmpty()) {
            boolean hasUnlocked = false;
            for (var stat : stats.subStats) {
                //TEMP attribute 为 null 跳过这个空壳子属性
                if (!stat.isInitialized()) continue;
                if (!hasUnlocked) {
                    builder.accept(Component.empty());
                    hasUnlocked = true;
                }
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
            builder.accept(Component.translatable(setNameKey).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

            var twoPc = artifactSet.twoPcEffect().get();
            if (twoPc instanceof ArtifactSetEffect setEffect) {
                builder.accept(Component.translatable("artifact_set.effect.2pc").withStyle(ChatFormatting.YELLOW).append(": ").append(setEffect.getDescription()));
            }
            if (star >= 4 && artifactSet.hasFourPcEffect()) {
                var fourPc = artifactSet.fourPcEffect().get();
                if (fourPc instanceof ArtifactSetEffect setEffect) {
                    builder.accept(Component.translatable("artifact_set.effect.4pc").withStyle(ChatFormatting.YELLOW).append(": ").append(setEffect.getDescription()));
                }
            }
            // 圣遗物文本
            if (setKey != null) {
                builder.accept(Component.translatable("artifact.desc."+ setKey.getPath() +"." + type.name().toLowerCase()).withStyle(ChatFormatting.WHITE));
            }
        }

    }
    private String buildStatText(TeyvatItemStat stat) {
        if (!stat.isInitialized()) return "";
        String attrName = Component.translatable(stat.getAttribute().translationKey()).getString();
        if (stat.getKind() == TeyvatItemStat.StatKind.PERCENT) {
            return attrName + " +" + String.format("%.1f%%", stat.getValue() * 100);
        } else {
            return attrName + " +" + String.format("%.0f", stat.getValue());
        }
    }

    private ChatFormatting getStarColor() {
        return switch (star) {
            case 5 -> ChatFormatting.YELLOW;
            case 4 -> ChatFormatting.LIGHT_PURPLE;
            case 3 -> ChatFormatting.AQUA;
            case 2 -> ChatFormatting.WHITE;
            default -> ChatFormatting.GRAY;
        };
    }

    public ArtifactType getType() {return type;}
    public DeferredHolder<ArtifactSet, ArtifactSet> getSet() {return set;}
}