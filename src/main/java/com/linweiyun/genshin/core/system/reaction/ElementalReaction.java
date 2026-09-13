package com.linweiyun.genshin.core.system.reaction;

import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.status.StatusInstance;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.genshin.core.system.registry.ModRegistries;
import com.linweiyun.genshin.enums.ElementalReactionType;
import net.minecraft.resources.Identifier;

/**
 * 元素反应基类 —— 所有元素反应继承此类
 *
 * 注册参数说明：
 * - elementA / elementB：参与反应的两个元素（主元素）
 *   注册顺序有意义：消耗比 ratioA:ratioB 对应 elementA:elementB
 *   例：融化 elementA=PYRO elementB=CYRO ratioA=1 ratioB=2
 *       → 1 份 elementA 消耗 2 份 elementB（同时消耗）
 * - elementB 是被克制方（消耗更多的一方），先手是 elementB、后手是 elementA 时为克制反应（倍率 2.0）
 *
 * 类元素自动处理：反应查找时会用 getMainElement() 把类元素归并到主元素
 *   FROZEN -> CYRO, AGGRAVATE -> DENDRO, BURNING -> PYRO
 *
 * 消耗规则：双方同时按比例消耗，不是一方消耗另一方
 *   消耗比 1:2 → 扣 elementA 1 份，同时扣 elementB 2 份
 *   哪个先耗尽就停
 */
public abstract class ElementalReaction {

    protected final String elementAId;
    protected final String elementBId;
    private transient GenshinElement cachedElementA;
    private transient GenshinElement cachedElementB;
    protected final float ratioA;
    protected final float ratioB;
    protected final int basePriority;
    protected final ElementalReactionType reactionType;

    protected ElementalReaction(ElementalReactionType reactionType,
                                String elementAId, String elementBId,
                                float ratioA, float ratioB,
                                int basePriority) {
        this.reactionType = reactionType;
        this.elementAId = elementAId;
        this.elementBId = elementBId;
        this.ratioA = ratioA;
        this.ratioB = ratioB;
        this.basePriority = basePriority;
    }

    public GenshinElement getElementA() {
        if (cachedElementA == null) cachedElementA = resolveElement(elementAId);
        return cachedElementA;
    }
    public GenshinElement getElementB() {
        if (cachedElementB == null) cachedElementB = resolveElement(elementBId);
        return cachedElementB;
    }

    private static GenshinElement resolveElement(String id) {
        String[] parts = id.split(":", 2);
        Identifier identifier = Identifier.fromNamespaceAndPath(parts[0], parts[1]);
        return ModRegistries.ELEMENT_REGISTRY.get(identifier).map(r -> r.value()).orElse(null);
    }

    public float getRatioA() { return ratioA; }
    public float getRatioB() { return ratioB; }
    public int getBasePriority() { return basePriority; }
    public ElementalReactionType getReactionType() { return reactionType; }

    public boolean canMatch(GenshinElement attackerElement, GenshinElement defenderElement) {
        GenshinElement attackerMain = attackerElement.getMainElement();
        GenshinElement defenderMain = defenderElement.getMainElement();
        if (attackerMain == null || defenderMain == null) return false;
        GenshinElement a = getElementA();
        GenshinElement b = getElementB();
        return (attackerMain == a && defenderMain == b)
                || (attackerMain == b && defenderMain == a);
    }

    /**
     * 计算实际消耗 —— 给定先手元素和后手元素，按比例同时消耗
     *
     * 消耗逻辑：
     *   先判断方向（先手是 A 还是 B），确定双方的实际元素
     *   然后按 ratioA:ratioB 同时消耗，取能完整扣一轮的最大次数
     *
     * @return 消耗结果，包含 elementA/elementB 各消耗了多少
     */
    public float[] calculateConsumption(float unitA, float unitB) {
        // 按比例消耗，找 min( unitA/ratioA, unitB/ratioB )
        if (unitA <= 0 || unitB <= 0) return new float[]{0f, 0f};
        float rounds = Math.min(unitA / ratioA, unitB / ratioB);
        float consumedA = rounds * ratioA;
        float consumedB = rounds * ratioB;
        return new float[]{consumedA, consumedB};
    }

    /**
     * 执行反应 —— 消耗元素后执行效果
     *
     * @param context  反应上下文（目标、攻击者、伤害规格等）
     * @return 反应结果（消耗量、残留量、是否增幅、增幅倍率等）
     */
    public abstract ReactionResult execute(ReactionContext context);

    /**
     * 子类重写：这个先手元素实例能不能参与"slotElement 对应槽位"的消耗
     * 默认：主元素归并匹配（MAIN 策略，融化语义）
     * FreezeReaction 覆盖：排除 FROZEN（EXACT 策略语义）
     */
    public boolean canConsume(ElementalAttachmentInstance instance, GenshinElement slotElement) {
        return instance.getElement().getMainElement() == slotElement.getMainElement();
    }

    /**
     * 基类统一：求和容器中所有能参与 slotElement 消耗的实例的元素量
     * 内部调用 canConsume 做过滤
     */
    public float sumConsumable(StatusContainer container, GenshinElement slotElement) {
        float sum = 0f;
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            if (!canConsume(ea, slotElement)) continue;
            sum += ea.getUnit();
        }
        return sum;
    }

    /**
     * 基类统一：从容器中扣减 amount 量的、能参与 slotElement 消耗的实例
     * 返回实际扣了多少
     */
    public void consumeElementUnit(StatusContainer container, GenshinElement slotElement, float amount) {
        float remaining = amount;
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            if (!canConsume(ea, slotElement)) continue;
            float consumed = ea.consume(remaining);
            remaining -= consumed;
            if (remaining <= 0f) break;
        }
    }

    /**
     * 预留：聚变反应的"伤害冷却"和"公共冷却"接口
     * 非聚变反应返回 0 表示无冷却限制
     *
     * TODO: 聚变反应需要两种冷却——伤害冷却（同攻击者同反应）和公共冷却（同目标同反应）
     */
    public int getDamageCooldownMs() { return 0; }
    public int getReactionCooldownMs() { return 0; }

    /**
     * 子类可覆盖：判断该反应在当前目标状态下是否被禁止
     * 例：冻结状态下禁止感电/蒸发
     */
    public boolean isBlocked(ReactionContext context) { return false; }
}