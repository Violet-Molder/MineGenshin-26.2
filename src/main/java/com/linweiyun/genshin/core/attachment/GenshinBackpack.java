package com.linweiyun.genshin.core.attachment;

import com.linweiyun.genshin.content.items.TeyvatItem;
import com.linweiyun.genshin.content.items.artifact.ArtifactItem;
import com.linweiyun.genshin.content.items.development.CharacterDevelopmentItem;
import com.linweiyun.genshin.content.items.food.FoodItem;
import com.linweiyun.genshin.content.items.furnishing.FurnishingItem;
import com.linweiyun.genshin.content.items.gadget.GadgetItem;
import com.linweiyun.genshin.content.items.material.MaterialItem;
import com.linweiyun.genshin.content.items.preicous.PreciousItem;
import com.linweiyun.genshin.content.items.quest.QuestItem;
import com.linweiyun.genshin.content.items.weapon.WeaponItem;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import org.jspecify.annotations.NonNull;

public class GenshinBackpack implements Container, IPersistedSerializable {

    public enum Category {
        WEAPONS("武器", 2000, WeaponItem.class),
        ARTIFACTS("圣遗物", 2700, ArtifactItem.class),
        DEVELOPMENT("养成道具", 1000, CharacterDevelopmentItem.class),
        FOOD("食物", 1000, FoodItem.class),
        MATERIALS("材料", 1000, MaterialItem.class),
        GADGET("小道具", 500, GadgetItem.class),
        QUEST("任务", 200, QuestItem.class),
        PRECIOUS("贵重道具", 300, PreciousItem.class),
        FURNISHINGS("摆设", 2700, FurnishingItem.class);

        public final String displayName;
        public final int maxCapacity;
        public final Class<? extends TeyvatItem> itemClass;

        Category(String displayName, int maxCapacity, Class<? extends TeyvatItem> itemClass) {
            this.displayName = displayName;
            this.maxCapacity = maxCapacity;
            this.itemClass = itemClass;
        }
    }

    public static final int WEAPONS_SIZE = 2000;
    public static final int ARTIFACTS_SIZE = 2700;
    public static final int DEVELOPMENT_SIZE = 1000;
    public static final int FOOD_SIZE = 1000;
    public static final int MATERIALS_SIZE = 1000;
    public static final int GADGET_SIZE = 500;
    public static final int QUEST_SIZE = 200;
    public static final int PRECIOUS_SIZE = 300;
    public static final int FURNISHINGS_SIZE = 2700;

    @Persisted(key = "weapons")
    private ItemStack[] weapons;
    @Persisted(key = "artifacts")
    private ItemStack[] artifacts;
    @Persisted(key = "development")
    private ItemStack[] development;
    @Persisted(key = "food")
    private ItemStack[] food;
    @Persisted(key = "materials")
    private ItemStack[] materials;
    @Persisted(key = "gadget")
    private ItemStack[] gadget;
    @Persisted(key = "quest")
    private ItemStack[] quest;
    @Persisted(key = "precious")
    private ItemStack[] precious;
    @Persisted(key = "furnishings")
    private ItemStack[] furnishings;

    private Runnable onChange = () -> {};

    public GenshinBackpack() {
        weapons = new ItemStack[WEAPONS_SIZE];
        artifacts = new ItemStack[ARTIFACTS_SIZE];
        development = new ItemStack[DEVELOPMENT_SIZE];
        food = new ItemStack[FOOD_SIZE];
        materials = new ItemStack[MATERIALS_SIZE];
        gadget = new ItemStack[GADGET_SIZE];
        quest = new ItemStack[QUEST_SIZE];
        precious = new ItemStack[PRECIOUS_SIZE];
        furnishings = new ItemStack[FURNISHINGS_SIZE];
        clearContent();
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public ResourceHandler<ItemResource> asResourceHandler() {
        return VanillaContainerWrapper.of(this);
    }

    public ItemStack[] getCategoryArray(Category category) {
        return switch (category) {
            case WEAPONS -> weapons;
            case ARTIFACTS -> artifacts;
            case DEVELOPMENT -> development;
            case FOOD -> food;
            case MATERIALS -> materials;
            case GADGET -> gadget;
            case QUEST -> quest;
            case PRECIOUS -> precious;
            case FURNISHINGS -> furnishings;
        };
    }

    public int getCategoryCapacity(Category category) {
        return category.maxCapacity;
    }

    public static boolean isValidForCategory(Category category, ItemStack stack) {
        if (stack.isEmpty()) return true;
        return category.itemClass.isInstance(stack.getItem());
    }

    public int getCategoryFirstEmptySlot(Category category) {
        ItemStack[] arr = getCategoryArray(category);
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].isEmpty()) return i;
        }
        return -1;
    }

    public int getCategoryUsedSlots(Category category) {
        ItemStack[] arr = getCategoryArray(category);
        int count = 0;
        for (ItemStack stack : arr) {
            if (!stack.isEmpty()) count++;
        }
        return count;
    }

    public boolean addItemToCategory(Category category, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!isValidForCategory(category, stack)) return false;
        int slot = getCategoryFirstEmptySlot(category);
        if (slot < 0) return false;
        ItemStack[] arr = getCategoryArray(category);
        arr[slot] = stack.copy();
        setChanged();
        return true;
    }

    public ItemStack removeItemFromCategory(Category category, int index) {
        ItemStack[] arr = getCategoryArray(category);
        if (index < 0 || index >= arr.length) return ItemStack.EMPTY;
        ItemStack existing = arr[index];
        if (existing.isEmpty()) return ItemStack.EMPTY;
        arr[index] = ItemStack.EMPTY;
        setChanged();
        return existing;
    }

    public ItemStack getItemInCategory(Category category, int index) {
        ItemStack[] arr = getCategoryArray(category);
        if (index < 0 || index >= arr.length) return ItemStack.EMPTY;
        return arr[index];
    }

    public void setItemInCategory(Category category, int index, ItemStack stack) {
        ItemStack[] arr = getCategoryArray(category);
        if (index < 0 || index >= arr.length) return;
        if (!stack.isEmpty() && !isValidForCategory(category, stack)) return;
        arr[index] = stack;
        setChanged();
    }

    private static int getCategoryOffset(Category category) {
        int offset = 0;
        for (Category cat : Category.values()) {
            if (cat == category) return offset;
            offset += cat.maxCapacity;
        }
        return offset;
    }

    public static Category getCategoryFromFlatIndex(int flatIndex) {
        int offset = 0;
        for (Category cat : Category.values()) {
            if (flatIndex < offset + cat.maxCapacity) return cat;
            offset += cat.maxCapacity;
        }
        return Category.WEAPONS;
    }

    public static int getIndexInCategoryFromFlatIndex(int flatIndex) {
        int offset = 0;
        for (Category cat : Category.values()) {
            if (flatIndex < offset + cat.maxCapacity) return flatIndex - offset;
            offset += cat.maxCapacity;
        }
        return 0;
    }

    @Override
    public int getContainerSize() {
        return WEAPONS_SIZE + ARTIFACTS_SIZE + DEVELOPMENT_SIZE + FOOD_SIZE
                + MATERIALS_SIZE + GADGET_SIZE + QUEST_SIZE + PRECIOUS_SIZE + FURNISHINGS_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (Category cat : Category.values()) {
            ItemStack[] arr = getCategoryArray(cat);
            for (ItemStack stack : arr) {
                if (!stack.isEmpty()) return false;
            }
        }
        return true;
    }

    @Override
    public @NonNull ItemStack getItem(int flatIndex) {
        Category cat = getCategoryFromFlatIndex(flatIndex);
        int idx = getIndexInCategoryFromFlatIndex(flatIndex);
        return getItemInCategory(cat, idx);
    }

    @Override
    public @NonNull ItemStack removeItem(int flatIndex, int amount) {
        Category cat = getCategoryFromFlatIndex(flatIndex);
        int idx = getIndexInCategoryFromFlatIndex(flatIndex);
        ItemStack[] arr = getCategoryArray(cat);
        if (idx < 0 || idx >= arr.length) return ItemStack.EMPTY;
        ItemStack existing = arr[idx];
        if (existing.isEmpty()) return ItemStack.EMPTY;
        int toRemove = Math.min(amount, existing.getCount());
        ItemStack result = existing.copyWithCount(toRemove);
        if (toRemove >= existing.getCount()) {
            arr[idx] = ItemStack.EMPTY;
        } else {
            existing.shrink(toRemove);
        }
        setChanged();
        return result;
    }

    @Override
    public @NonNull ItemStack removeItemNoUpdate(int flatIndex) {
        Category cat = getCategoryFromFlatIndex(flatIndex);
        int idx = getIndexInCategoryFromFlatIndex(flatIndex);
        ItemStack[] arr = getCategoryArray(cat);
        if (idx < 0 || idx >= arr.length) return ItemStack.EMPTY;
        ItemStack existing = arr[idx];
        if (existing.isEmpty()) return ItemStack.EMPTY;
        arr[idx] = ItemStack.EMPTY;
        return existing;
    }

    @Override
    public void setItem(int flatIndex, ItemStack stack) {
        Category cat = getCategoryFromFlatIndex(flatIndex);
        int idx = getIndexInCategoryFromFlatIndex(flatIndex);
        setItemInCategory(cat, idx, stack);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean canPlaceItem(int flatIndex, ItemStack stack) {
        Category cat = getCategoryFromFlatIndex(flatIndex);
        return isValidForCategory(cat, stack);
    }

    @Override
    public void setChanged() {
        onChange.run();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        weapons = new ItemStack[WEAPONS_SIZE];
        artifacts = new ItemStack[ARTIFACTS_SIZE];
        development = new ItemStack[DEVELOPMENT_SIZE];
        food = new ItemStack[FOOD_SIZE];
        materials = new ItemStack[MATERIALS_SIZE];
        gadget = new ItemStack[GADGET_SIZE];
        quest = new ItemStack[QUEST_SIZE];
        precious = new ItemStack[PRECIOUS_SIZE];
        furnishings = new ItemStack[FURNISHINGS_SIZE];
        for (Category cat : Category.values()) {
            ItemStack[] arr = getCategoryArray(cat);
            for (int i = 0; i < arr.length; i++) {
                arr[i] = ItemStack.EMPTY;
            }
        }
        setChanged();
    }
}
