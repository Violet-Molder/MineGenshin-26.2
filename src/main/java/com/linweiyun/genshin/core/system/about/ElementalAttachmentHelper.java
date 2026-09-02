package com.linweiyun.genshin.core.system.about;

import com.linweiyun.genshin.core.status.StatusAccessor;
import com.linweiyun.genshin.core.status.StatusInstance;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.character.PGCharacterData;
import com.linweiyun.genshin.core.system.registry.register.StatusDataComponents;
import com.linweiyun.genshin.enums.ElementalsGIM;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

/**
 * 元素附着入口 —— 实现附着损耗和覆盖规则，内部调桥接层存实例
 *
 * 附着处理流程：
 * 1. 算出实际附着量 = profile.actualQuantity() （baseQuantity × lossMultiplier）
 * 2. 在容器里查找"同元素 + 同 source"的已有实例
 *    → 不同 source 的同元素是独立实例，互不干扰
 * 3. 三种情况：
 *    A. 无匹配实例 → 新建 ElementalAttachmentInstance
 *    B. 有实例 + 后手段量 ≤ 先手段量 → 不覆盖，什么都不做
 *    C. 有实例 + 后手段量 > 先手段量 → 量多则覆盖：
 *       - quantity = 后手段量
 *       - 元素.canOverrideDecay() == true（火/激/燃）→ 衰减速率替换为新值
 *       - 其他元素 → 衰减速率不变（继承原先的）
 * 4. 风/岩（instant=true）附着后立即消失，只能做后手反应
 *
 * 后手不残留规则：本 Helper 只管"附着"，不管"触发反应"。
 * 反应消耗后后手元素是否强制清除，由 ElementalReactionHandler 处理。
 */
public class ElementalAttachmentHelper {

    // ========== 附着入口 —— 四种宿主 ==========
    public static final Logger LOGGER = LogUtils.getLogger();
    public static void attach(LivingEntity target,
                              ElementalsGIM element,
                              AttachmentSource source,
                              AttachmentProfile profile) {
        doAttach(StatusAccessor.of(target), element, source, profile);
    }

    public static void attach(BlockEntity be,
                              ElementalsGIM element,
                              AttachmentSource source,
                              AttachmentProfile profile) {
        doAttach(StatusAccessor.of(be), element, source, profile);
    }

    public static void attach(PGCharacterData data,
                              ElementalsGIM element,
                              AttachmentSource source,
                              AttachmentProfile profile) {
        doAttach(StatusAccessor.of(data), element, source, profile);
    }

    public static void attach(ItemStack stack,
                              ElementalsGIM element,
                              AttachmentSource source,
                              AttachmentProfile profile) {
        // ItemStack 先 get DataComponent，没有就创建新的 StatusContainer
        StatusContainer container = stack.get(
                StatusDataComponents.CONTAINER);
        if (container == null) {
            container = new StatusContainer();
            stack.set(StatusDataComponents.CONTAINER, container);
        }
        doAttach(container, element, source, profile);
    }

    // ========== 消耗（元素反应调用）==========

    /**
     * 从容器里消耗指定元素的附着量
     * 遍历容器里所有同元素（ElementalsGIM 匹配）的实例，逐个扣量
     * 注意：类元素（FROZEN）和对应的主元素（CYRO）是不同的 ElementalsGIM 值，
     * 不会被一起消耗。元素反应需要根据实际消耗规则自行决定消耗哪些。
     */
    public static float consume(LivingEntity target, ElementalsGIM element, float amount) {
        return consumeInternal(StatusAccessor.of(target), element, amount);
    }
    public static float consume(BlockEntity be, ElementalsGIM element, float amount) {
        return consumeInternal(StatusAccessor.of(be), element, amount);
    }
    public static float consume(PGCharacterData data, ElementalsGIM element, float amount) {
        return consumeInternal(StatusAccessor.of(data), element, amount);
    }

    private static float consumeInternal(StatusContainer container, ElementalsGIM element, float amount) {
        float remaining = amount;
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            if (ea.getElement() != element) continue;
            float consumed = ea.consume(remaining);
            remaining -= consumed;
            if (remaining <= 0) break;
        }
        return amount - remaining;
    }

    // ========== 核心附着逻辑 ==========

    private static void doAttach(StatusContainer container,
                                 ElementalsGIM element,
                                 AttachmentSource source,
                                 AttachmentProfile profile) {

        // 1. 实际附着量 = baseQuantity × lossMultiplier
        float actualQuantity = profile.actualQuantity();

        // 2. 风/岩：瞬时附着。附着后立即 finished，只能做后手反应
        if (element.isInstant()) {
            // 先加到容器（让反应逻辑在同一 tick 内读到它）
            // 但设置 quantity 让它立即 finished → 容器下一个 tick 清
            // 不对——瞬时附着的"附着量"有意义（反应消耗时要读），
            // 所以 initial quantity 设为 actualQuantity，但让它 isFinished 在同 tick 结束后
            // 实现方式：加进去，然后反应逻辑处理完后，容器 tick 清掉。
            // 这里不做特殊处理，直接走到下面的"无匹配实例 → 新建"逻辑
        }

        // 3. 查找容器里"同元素 + 同 source + 未 finished"的已有实例
        ElementalAttachmentInstance existing = findMatching(container, element, source);

        if (existing == null) {
            // 无匹配实例 → 新建
            ElementalAttachmentInstance newInst =
                    new ElementalAttachmentInstance(element, source, profile, actualQuantity);
            container.add(newInst);
            return;
        }

        // 4. 量多则覆盖判断
        if (actualQuantity <= existing.getQuantity()) {
            // 后手段量 ≤ 先手段量 → 不覆盖
            return;
        }

        // 5. 发生覆盖
        existing.refreshQuantity(actualQuantity);
        // 衰减速率分支
        if (element.canOverrideDecay()) {
            // 火/激/燃：直接替换为新的衰减速率
            existing.overrideDecayRate(profile.getDecayPerSecond());
        }
        // 其他元素：继承原先的衰减速率（什么都不做）
    }

    // ========== 查找工具 ==========

    /**
     * 在容器里查找同元素 + 同来源 + 未 finished 的实例
     * 不同来源的同元素是独立的（比如可莉普攻火 vs 元素试炼仪火）
     */
    private static ElementalAttachmentInstance findMatching(
            StatusContainer container, ElementalsGIM element, AttachmentSource source) {
        return (ElementalAttachmentInstance) container.find(inst -> {
            if (inst.isFinished()) return false;
            if (!(inst instanceof ElementalAttachmentInstance ea)) return false;
            return ea.getElement() == element && ea.getSource() == source;
        });
    }
}