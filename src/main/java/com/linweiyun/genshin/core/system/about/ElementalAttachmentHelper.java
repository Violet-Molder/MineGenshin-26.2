package com.linweiyun.genshin.core.system.about;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.status.StatusInstance;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
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
    public static void attach(LivingEntity target, StatusContainer container,
                              GenshinElement element,
                              AttachmentSource source,
                              AttachmentProfile profile) {
        doAttach(target, container, element, source, profile, null, 0L);
    }

    /** 带角色信息的附着（用于反应贡献者追踪） */
    public static void attach(LivingEntity target, StatusContainer container,
                              GenshinElement element,
                              AttachmentSource source,
                              AttachmentProfile profile,
                              PGCharacter character, long gameTime) {
        doAttach(target, container, element, source, profile, character, gameTime);
    }

    // ========== 消耗（元素反应调用）==========

    /**
     * 从容器里消耗指定元素的附着量
     */
    public static float consume(StatusContainer container, GenshinElement element, float amount) {
        if (container == null) return 0f;
        return consumeInternal(container, element, amount);
    }

    private static float consumeInternal(StatusContainer container, GenshinElement element, float amount) {
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

    private static void doAttach(LivingEntity target, StatusContainer container,
                                 GenshinElement element,
                                 AttachmentSource source,
                                 AttachmentProfile profile,
                                 PGCharacter character, long gameTime) {

        // 1. 实际附着量 = baseQuantity × lossMultiplier
        float actualQuantity = profile.actualQuantity();

        // 2. 查找容器里"同元素 + 同 source"的已有实例
        ElementalAttachmentInstance existing = findMatching(container, element, source, character);

        if (existing == null) {
            // 无匹配实例 → 新建
            ElementalAttachmentInstance newInst =
                    new ElementalAttachmentInstance(element, source, profile, actualQuantity);
            if (character != null) {
                newInst.setSourceCharacter(character, gameTime);
            } else if (gameTime > 0L) {
                newInst.setAttachTick(gameTime);
            }
            if (target != null && GenshinElement.isNonPlayerLiving(target)) {
                element.onAttach(target);
            }
            newInst.setOwner(target);
            container.add(newInst);
            return;
        }

        // 3. 量多则覆盖判断
        // 主线/支线逻辑：同角色重复附着时，新持续时间超过当前剩余时间则延长
        if (character != null && existing.hasSourceCharacter()) {
            long currentTick = gameTime;
            long existingEnd = existing.getDecayEndTick();
            float newDurationTicks = profile.getDurationSeconds() * 20f;
            long newEnd = currentTick + (long) newDurationTicks;
            if (newEnd > existingEnd) {
                // 支线超过主线：刷新全量并延长到期时间
                existing.refreshQuantity(actualQuantity);
                existing.overrideDecayRate(profile.getDecayPerSecond());
                existing.setAttachTick(currentTick);
                return;
            }
            // 支线未超过主线：仅刷新角色信息，不改变到期时间
            existing.setSourceCharacter(character, gameTime);
            return;
        }

        if (actualQuantity <= existing.getUnit()) {
            // 后手段量 ≤ 先手段量 → 不覆盖
            // 但仍需记录角色信息
            if (character != null) {
                existing.setSourceCharacter(character, gameTime);
            }
            return;
        }

        // 4. 发生覆盖
        existing.refreshQuantity(actualQuantity);
        if (character != null) {
            existing.setSourceCharacter(character, gameTime);
        }
        // 衰减速率分支
        if (element.canOverrideDecay()) {
            // 火/激/燃：直接替换为新的衰减速率
            existing.overrideDecayRate(profile.getDecayPerSecond());
        }
        // 其他元素：继承原先的衰减速率（什么都不做）
    }

    // ========== 查找工具 ==========

    private static ElementalAttachmentInstance findMatching(
            StatusContainer container, GenshinElement element, AttachmentSource source,
            PGCharacter character) {
        return (ElementalAttachmentInstance) container.find(inst -> {
            if (inst.isFinished()) return false;
            if (!(inst instanceof ElementalAttachmentInstance ea)) return false;
            if (ea.getElement() != element || ea.getSource() != source) return false;
            if (character != null && ea.hasSourceCharacter()
                    && ea.getSourceCharacter() != character) {
                return false;
            }
            return true;
        });
    }
}