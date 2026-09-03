package com.linweiyun.genshin.core.system.reaction.builtin;

import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.status.StatusAccessor;
import com.linweiyun.genshin.core.status.StatusInstance;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.genshin.core.system.reaction.ElementalReaction;
import com.linweiyun.genshin.core.system.reaction.ReactionContext;
import com.linweiyun.genshin.core.system.reaction.ReactionResult;
import com.linweiyun.genshin.enums.ElementalsGIM;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

/**
 * 冻结反应 —— 特殊反应（非增幅非聚变）
 *
 * 消耗比：水:冰 = 1:1
 *   1水 + 1冰 同时消耗 → 生成 2 倍消耗量的 FROZEN（冻）元素在目标身上
 *
 * 关键规则：
 *   - 不能与 FROZEN 本身发生冻结反应（已经冻住了不能再冻）
 *   - 冻结反应不是增幅反应，不影响伤害
 *   - 冻结反应产生 FROZEN 元素（通过 ElementalAttachmentHelper.attach 添加）
 *
 * TODO: 完整冻结效果（减速、冻结实体等）暂不实现，待后续补充
 * TODO: 冻结藏冰/藏水逻辑 —— 冻结反应发生后还能残留额外的冰或水
 */
public class FreezeReaction extends ElementalReaction {
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 冻结反应的生成物倍率
     * 消耗掉的(水+冰)总量 × 2 = 生成的 FROZEN 元素量
     * 例：消耗 0.8水 + 0.8冰 = 1.6总量 → 生成 3.2 FROZEN
     */
    private static final float FROZEN_MULTIPLIER = 2.0f;

    /**
     * 注册时的参数约定（遵循 ElementalReaction 基类规范）：
     *   elementA = HYDRO（水，消耗 ratioA=1 份）
     *   elementB = CYRO （冰，消耗 ratioB=1 份）
     *   消耗比 ratioA:ratioB = 1:1 → 同时消耗，无克制方
     */
    public FreezeReaction(ElementalReactionType type,
                          ElementalsGIM elementA, ElementalsGIM elementB,
                          float ratioA, float ratioB, int basePriority) {
        super(type, elementA, elementB, ratioA, ratioB, basePriority);
    }

    /**
     * canMatch —— 反应选择阶段的粗筛
     *
     * 覆盖基类默认：精确元素前置过滤，冻元素不参与冻结反应的任何一侧配对
     * 基类默认是主元素归并（FROZEN→CYRO 会被算进冰侧），这里在归并之前就把 FROZEN 拦掉
     *
     * 和 canConsume 的区别：
     *   canMatch     = 要不要选中我这个反应类？（反应选择阶段，看精确元素）
     *   canConsume   = 选中之后，这个实例能不能被我消耗？（执行消耗阶段，看实例）
     */
    @Override
    public boolean canMatch(ElementalsGIM attackerElement, ElementalsGIM defenderElement) {
        // 精确元素前置过滤：冻元素不参与冻结反应的任何一侧
        if (attackerElement == ElementalsGIM.FROZEN) return false;
        if (defenderElement == ElementalsGIM.FROZEN) return false;
        // 主元素归并配对（交给基类默认逻辑）
        return super.canMatch(attackerElement, defenderElement);
    }

    /**
     * canConsume —— 执行消耗阶段的细筛（覆盖基类）
     *
     * 基类默认是 MAIN 策略（主元素归并匹配），会把 FROZEN（主元素CYRO）算进冰侧一起消耗
     * 这里覆盖成 EXACT 语义：排除 FROZEN 实例，只让精确的 CYRO / HYDRO 参与消耗
     * 原因：冻是冻结反应的产物，不是反应物，不能被冻结反应本身消耗
     *
     * 例：目标有 1.0 CYRO + 2.0 FROZEN，FreezeReaction 下：
     *   sumConsumable(container, CYRO) → 只统计 1.0（FROZEN 被 canConsume 排除）
     * 如果是 MeltReaction（不覆盖 canConsume，用基类默认）：
     *   sumConsumable(container, CYRO) → 统计 3.0（CYRO + FROZEN 都能被融化）
     */
    @Override
    public boolean canConsume(ElementalAttachmentInstance instance, ElementalsGIM slotElement) {
        // 冻元素不参与冻结反应的任何槽位消耗
        if (instance.getElement() == ElementalsGIM.FROZEN) return false;
        // 其他情况（比如水槽用 HYDRO 实例、冰槽用 CYRO 实例）交给基类主元素归并匹配
        return super.canConsume(instance, slotElement);
    }

    /**
     * isBlocked —— 判断这个反应是否应该被禁止
     *
     * 规则：后手元素是 FROZEN（冻）时，不允许再发生冻结反应
     * 原因：冻元素本身就是冻结的产物，不能"再冻一次"
     * 但后手水 + 先手冻 是允许的（藏水逻辑的一部分），因为这是正常消耗
     */
    @Override
    public boolean isBlocked(ReactionContext context) {
        if (context.attackerElement() == ElementalsGIM.FROZEN) return true;
        return false;
    }

    /**
     * execute —— 冻结反应核心执行逻辑
     *
     * 整体流程分 4 步：
     *   ① 定向 —— 判断后手对应注册参数的 A 还是 B，确定先手要找哪种元素
     *   ② 算量 —— 遍历目标容器算出先手元素的总量（调用基类 sumConsumable，内部走 canConsume 过滤），按消耗比算双方各扣多少
     *   ③ 扣减 —— 后手元素从全局消耗，先手元素调用基类 consumeFromContainer（内部走 canConsume 过滤）
     *   ④ 生成 —— (消耗总量 × 2) 份 FROZEN 附加到目标身上
     *
     * ===== 变量名说明 =====
     *   attacker     = 后手（本次附着的那一方，来自 attacker 攻击）
     *   defender     = 先手（目标身上已有的那一方，来自 defender 身上的附着）
     *   elementA/B   = 注册时声明的两个元素，A 和 B 只是"槽位"，不代表后手或先手
     *   attackerIsA  = 判断后手是不是"注册时放在 A 槽位的那个元素"（布尔标志，后续所有分支的根基）
     *
     * ===== 为什么要分 A/B 槽位？ =====
     *   ElementalReaction 基类要求每个反应在注册时固定消耗比 ratioA:ratioB
     *   但实际触发时，后手可能是 A 也可能是 B（例：水冻冰 / 冰冻水）
     *   所以 attackerIsA 这个布尔变量就是用来把"随机先后手"映射回"固定 A/B 槽位"
     */
    @Override
    public ReactionResult execute(ReactionContext ctx) {

        // ===== 第 ① 步：定向 —— 把"运行时先后手"映射回"注册时的 A/B 槽位" =====

        // attackerMain：后手元素的"主元素"
        // getMainElement() 会把类元素归并到主元素（FROZEN→CYRO, AGGRAVATE→DENDRO, BURNING→PYRO）
        // 这样后手即使是 FROZEN，也能被识别成"主元素是 CYRO 的一方"
        ElementalsGIM attackerMain = ctx.attackerElement().getMainElement();

        // attackerIsA = 后手的主元素 == 注册时放在 A 槽位的元素？
        // 注册时 FreezeReaction: elementA=HYDRO, elementB=CYRO
        //   如果后手是水 → attackerIsA = true  → 后手是 A 槽 → 先手必须是 B 槽（冰）
        //   如果后手是冰 → attackerIsA = false → 后手是 B 槽 → 先手必须是 A 槽（水）
        // 这一行决定了后面所有 consume 调用该传 elementA 还是 elementB
        boolean attackerIsA = (attackerMain == elementA);

        // defenderTarget：我要从先手身上找哪种元素来和后手反应？
        // 后手是 A → 先手必须是 B（attackerIsA ? elementB : elementA）
        // 后手是 B → 先手必须是 A
        // 这里用三元运算：条件 ? 真值 : 假值
        // 例：attackerIsA=true → elementB（冰）；attackerIsA=false → elementA（水）
        ElementalsGIM defenderTarget = attackerIsA ? elementB : elementA;

        // totalDefenderUnit：目标身上所有"能参与 defenderTarget 槽位消耗"的附着实例的元素量之和
        // 调用基类 sumConsumable（内部走 canConsume 过滤）：
        //   - FreezeReaction 的 canConsume 排除 FROZEN → 只统计精确的 CYRO / HYDRO（不含冻）
        //   - MeltReaction 用基类默认 canConsume → 会把 CYRO + FROZEN 都统计（主元素归并）
        // 例：目标有 1.0 CYRO + 2.0 FROZEN，FreezeReaction 下 totalDefenderUnit=1.0
        //     同场景 MeltReaction 下 totalDefenderUnit=3.0
        float totalDefenderUnit = sumConsumable(ctx.targetContainer(), defenderTarget);

        // 如果目标身上完全没有能反应的先手元素 → 直接返回空结果（没反应发生）
        if (totalDefenderUnit <= 0f) {
            return ReactionResult.builder(reactionType).build();
        }

        // attackerQty：后手元素本次附着的总量（全额参与反应，不衰减）
        float attackerQty = ctx.attackerUnit();

        // ===== 第 ② 步：算量 —— 按消耗比算出双方各要扣多少 =====

        // consumedA / consumedB：A 槽元素和 B 槽元素各自的消耗数量
        // 注意：这里只算数值，还没真正扣减
        float consumedA, consumedB;

        // calculateConsumption(qtyA, qtyB, defenderIsA)
        //   前两个参数 = A槽数量、B槽数量（顺序固定！）
        //   返回 float[]{A槽消耗, B槽消耗}
        //
        // 所以调用时必须确保：
        //   第一个参数 = A槽当前的数量
        //   第二个参数 = B槽当前的数量
        //
        // 情况1: 后手是 A 槽（attackerIsA=true）
        //   attackerQty = A槽的数量，totalDefenderUnit = B槽的数量
        //   → calculateConsumption(attackerQty, totalDefenderUnit, ...)
        //   → consumed[0] = A消耗, consumed[1] = B消耗
        //
        // 情况2: 后手是 B 槽（attackerIsA=false）
        //   attackerQty = B槽的数量，totalDefenderUnit = A槽的数量
        //   → calculateConsumption(totalDefenderUnit, attackerQty, ...)
        //   → consumed[0] = A消耗, consumed[1] = B消耗
        if (attackerIsA) {
            float[] consumed = calculateConsumption(attackerQty, totalDefenderUnit);
            consumedA = consumed[0]; consumedB = consumed[1];
        } else {
            float[] consumed = calculateConsumption(totalDefenderUnit, attackerQty);
            consumedA = consumed[0]; consumedB = consumed[1];
        }

        // totalConsumed：A+B 总共消耗了多少（用于生成 FROZEN 的量）
        float totalConsumed = consumedA + consumedB;

        // consumedAttacker：后手这一侧消耗了多少（返回给 ReactionResult 用）
        // 又是 attackerIsA 分支：后手是 A 槽 → 消耗值在 consumedA；后手是 B 槽 → 消耗值在 consumedB
        float consumedAttacker = attackerIsA ? consumedA : consumedB;

        // ===== 第 ③ 步：扣减 —— 真正从目标身上扣元素 =====
            // 后手是 A 槽 → 扣 A 槽元素（从目标身上消耗）+ 扣 B 槽元素（从先手容器扣，用基类统一方法）
            consumeElementUnit(ctx.targetContainer(), elementB, consumedB);
            consumeElementUnit(ctx.targetContainer(), elementA, consumedA);
        // 两个消耗方法的区别：
        //   consumeAttacker：直接按元素类型消耗（调用 ElementalAttachmentHelper.consume）
        //     → 针对"后手元素"，它只消耗本次附着的那一批
        //   consumeFromContainer（基类）：遍历容器，内部走 canConsume 过滤后逐个实例扣
        //     → 针对"先手元素"，可能分散在多个实例里
        //     → FreezeReaction.canConsume 排除 FROZEN，MeltReaction 用默认（主元素归并）

        // ===== 第 ④ 步：生成 —— 附加 FROZEN 到目标 =====

        // frozenBefore：反应前目标身上已有的 FROZEN 元素总量
        // 为什么不用基类 sumConsumable？因为 FreezeReaction.canConsume 会排除 FROZEN，
        // 我们要的是【所有冻元素】（不管能不能被消耗），直接精确匹配更准确
        float frozenBefore = 0f;
        for (StatusInstance inst : ctx.targetContainer().getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            if (ea.getElement() == ElementalsGIM.FROZEN) {
                frozenBefore += ea.getUnit();
            }
        }

        if (totalConsumed > 0f) {
            // frozenQty：生成的冻元素量 = (消耗总量) × 2
            // 例: 消耗 0.5 水 + 0.5 冰 = 1.0 总量 → 生成 2.0 FROZEN
            float frozenQty = totalConsumed * FROZEN_MULTIPLIER;

            // 用 SPECIAL 来源附加 FROZEN（不是 NORMAL_ATTACK，因为这是反应生成物，不是直接攻击附着）
            // TODO: 完整冻结效果（减速、冻结实体）待实现
            ElementalAttachmentHelper.attach(
                    ctx.targetContainer(),
                    ElementalsGIM.FROZEN,
                    AttachmentSource.SPECIAL,
                    new AttachmentProfile(frozenQty, 1.0f, 0.0f, 999.0f)
            );
            StatusContainer container = ctx.targetContainer();
            if (container.getFrozenDecayState() != null) {
                container.getFrozenDecayState().activate();
            }
        }

        // 计算日志用的残余先手元素量
        float defenderConsumedQty = attackerIsA ? consumedB : consumedA;
        float defenderResidual = totalDefenderUnit - defenderConsumedQty;
        ElementalsGIM defenderElementConsumed = attackerIsA ? elementB : elementA;
        // 反应后冻元素总量 = 反应前已有的冻 + 新生成的冻
        float frozenAfter = frozenBefore + (totalConsumed > 0f ? totalConsumed * FROZEN_MULTIPLIER : 0f);

        LOGGER.info("冻结反应触发 | 总消耗={}U | 后手={} {}U | 先手={} {}U | 残余先手={} {}U | 反应后冻={}U",
                totalConsumed,
                ctx.attackerElement(), consumedAttacker,
                defenderElementConsumed, defenderConsumedQty,
                defenderElementConsumed, defenderResidual,
                frozenAfter);

        // 构建返回结果
        // reacted() 标记成功触发
        // consumedAttacker 记录后手消耗量
        // consumedDefender 记录先手消耗量 —— 又需要 attackerIsA 分支判断先手对应的是哪个槽
        return ReactionResult.builder(reactionType)
                .reacted()
                .consumedAttacker(consumedAttacker)
                .consumedDefender(defenderConsumedQty)
                .build();
    }

}