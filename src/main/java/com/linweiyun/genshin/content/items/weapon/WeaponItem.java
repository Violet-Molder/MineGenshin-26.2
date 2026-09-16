package com.linweiyun.genshin.content.items.weapon;

import com.linweiyun.genshin.content.attribute.AttributeType;
import com.linweiyun.genshin.content.items.TeyvatItem;
import com.linweiyun.genshin.content.items.component.WeaponStatsComponent;
import com.linweiyun.genshin.content.items.weapon.bow.Bow;
import com.linweiyun.genshin.content.items.weapon.catalyst.Catalyst;
import com.linweiyun.genshin.content.items.weapon.claymore.Claymore;
import com.linweiyun.genshin.content.items.weapon.polearm.Polearm;
import com.linweiyun.genshin.content.items.weapon.sword.Sword;
import com.linweiyun.genshin.content.stat.TeyvatItemStat;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.genshin.core.system.registry.register.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Consumer;

public class WeaponItem extends TeyvatItem {

    protected int tier = 1;
    protected DeferredHolder<AttributeType, AttributeType> subStatAttribute;
    protected boolean canRefine = true;

    public WeaponItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public int getTier() {
        return tier;
    }

    public DeferredHolder<AttributeType, AttributeType> getSubStatAttribute() {
        return subStatAttribute;
    }

    public WeaponStatsComponent getStats(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.WEAPON_STATS.get(), WeaponStatsComponent.DEFAULT);
    }

    public WeaponStatsComponent getOrInitStats(ItemStack stack) {
        WeaponStatsComponent stats = getStats(stack);
        if (stats.uid == 0 && stack.getItem() instanceof WeaponItem weapon) {
            stats = new WeaponStatsComponent();
            stats.tier = weapon.tier;
            stats.subStatType = weapon.subStatAttribute != null ? weapon.subStatAttribute.get() : new AttributeType();
            stats.uid = weapon.getUID();
            stats.initStats(star);
            stack.set(ModDataComponents.WEAPON_STATS.get(), stats);
        }
        return stats;
    }

    public void addExp(ItemStack stack, int amount) {
        WeaponStatsComponent stats = getOrInitStats(stack);
        stats.addExp(amount, star);
        stack.set(ModDataComponents.WEAPON_STATS.get(), stats);
    }

    public boolean ascend(ItemStack stack) {
        WeaponStatsComponent stats = getOrInitStats(stack);
        boolean result = stats.ascend(star);
        if (result) {
            stack.set(ModDataComponents.WEAPON_STATS.get(), stats);
        }
        return result;
    }

    public boolean canAscend(ItemStack stack) {
        WeaponStatsComponent stats = getOrInitStats(stack);
        return stats.canAscend();
    }

    public boolean canRefine() {
        return canRefine;
    }

    public void setCanRefine(boolean canRefine) {
        this.canRefine = canRefine;
    }

    public boolean increaseRefinement(ItemStack stack) {
        if (!canRefine) return false;
        WeaponStatsComponent stats = getOrInitStats(stack);
        boolean result = stats.increaseRefinement(star);
        if (result) {
            stack.set(ModDataComponents.WEAPON_STATS.get(), stats);
        }
        return result;
    }

    public void attach(LivingEntity target, StatusContainer container,
                       GenshinElement element,
                       AttachmentSource source,
                       AttachmentProfile profile) {
        ElementalAttachmentHelper.attach(target, container, element, source, profile);
    }

    public int getUID() {
        if (star <= 0) return 0;
        int typeDigit = getWeaponTypeDigit();
        int uid = 100000 + star * 10000 + typeDigit * 1000 + tier * 100;
        return uid;
    }

    private int getWeaponTypeDigit() {
        if (this instanceof Polearm) return 1;
        if (this instanceof Sword) return 2;
        if (this instanceof Claymore) return 3;
        if (this instanceof Bow) return 4;
        if (this instanceof Catalyst) return 5;
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        WeaponStatsComponent stats = getOrInitStats(stack);

        builder.accept(Component.literal("★".repeat(star)).withStyle(ChatFormatting.GOLD));

        if (stats.refinementRank > 1) {
            builder.accept(Component.translatable("item.minegenshin.weapon.refinement_rank", stats.refinementRank).withStyle(ChatFormatting.AQUA));
        }

        if (stats.mainStat != null && stats.mainStat.isInitialized()) {
            ChatFormatting starColor = getStarColor();
            builder.accept(Component.literal(buildStatText(stats.mainStat)).withStyle(starColor, ChatFormatting.BOLD));
        }

        if (stats.subStat != null && stats.subStat.isInitialized()) {
            builder.accept(Component.empty());
            builder.accept(Component.literal(buildStatText(stats.subStat)).withStyle(ChatFormatting.GRAY));
        }

        if (stats.uid != 0) {
            builder.accept(Component.literal("UID: " + stats.uid).withStyle(ChatFormatting.DARK_GRAY));
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
}