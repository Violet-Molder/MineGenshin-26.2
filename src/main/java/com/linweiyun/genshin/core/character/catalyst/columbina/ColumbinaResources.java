package com.linweiyun.genshin.core.character.catalyst.columbina;

import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData.ActionStep;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData.BurstData;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData.ComboData;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData.DodgeData;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData.Engagement;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData.SkillData;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 哥伦比娅的动作数据表。
 *
 * <h2>为什么要有这一份</h2>
 * 不覆写 {@code PGCharacter.getActionData()} 的角色会走
 * {@link CharacterActionData#fallback(int) 通用兜底表}：兜底表的每一段都是<b>近战默认形态</b>
 * （够得着 3 格就停下出手），法器角色的射程就完全用不上 —— 按键看起来「没反应」，
 * 其实是框架认为目标太远、不让她出手。本表把她的普攻/战技/大招都标成<b>远程</b>，
 * 射程与 {@link ColumbinaSkill} 里 {@code TargetSeeker} 用的 10 格对齐。
 *
 * <h2>数值口径</h2>
 * <b>时序完全复用兜底表</b>（duration / protectDuration / hits / comboWindow 都取自
 * {@link CharacterActionData#fallback(int)}），本表只做两件事：
 * <ol>
 *   <li>把连段数固定成 {@code ColumbinaSkill.getMaxCombo()} 声明的 {@value #MAX_COMBO} 段；</li>
 *   <li>把每一段标成远程形态（{@link Engagement#ranged()} + {@value #ATTACK_RANGE} 格攻击距离）。</li>
 * </ol>
 * 伤害倍率仍然只在 {@link ColumbinaSkill} 里，本表不管数值。
 */
public final class ColumbinaResources {

    private ColumbinaResources() {
    }

    /**
     * 连段数 —— 必须和 {@link ColumbinaSkill#getMaxCombo()} 一致
     * （{@code SkillBase.buildDefaultActionSet} 取两者的较小值）。
     */
    public static final int MAX_COMBO = 3;

    /**
     * 生效攻击距离 <b>10 格</b> —— 对齐 {@link ColumbinaSkill#attack} 里
     * {@code new TargetSeeker(player, 10.0, ...)} 的索敌半径：
     * 伤害结算在 10 格内锁到的目标身上，那么「够得着就停下出手」的距离也该是 10 格。
     *
     * <p>⚠️ 和 {@code VodyanitsaResources.ATTACK_RANGE} 同一条口径：
     * <b>只设 {@link Engagement#withAcquireRange} 只能让她「选得到」目标</b>；
     * 框架判「够得着就停下出手」用的是 {@link ActionStep#effectiveAttackRange()} ——
     * 不显式 {@code withAttackRange} 的话它就是近战默认的 3 格，法器角色会被拉到脸上才打。
     * 所以本表<b>两个都设</b>。
     */
    public static final float ATTACK_RANGE = 10.0f;

    /** 远程形态：不突进、索敌距离 = 攻击距离、只转向（{@link Engagement#ranged()}）。 */
    private static final Engagement RANGED = Engagement.ranged()
            .withAcquireRange(ATTACK_RANGE)
            .withKeepRange(ATTACK_RANGE + 2.0);

    public static final CharacterActionData ACTION_DATA = build();

    private static CharacterActionData build() {
        // 时序的唯一来源：兜底表（不新造平衡数值）
        CharacterActionData base = CharacterActionData.fallback(MAX_COMBO);

        Map<Integer, ActionStep> combo = new LinkedHashMap<>();
        for (int stage = 1; stage <= MAX_COMBO; stage++) {
            ActionStep step = base.combo().getStep(stage);
            if (step != null) {
                combo.put(stage, ranged(step));
            }
        }

        SkillData baseSkill = base.skill();
        BurstData baseBurst = base.burst();
        DodgeData baseDodge = base.dodge();

        return new CharacterActionData(
                new ComboData(MAX_COMBO, combo),
                // 战技只有点按：她的短 CD == 长 CD（构造器用的两冷却重载），客户端不会走长按分支
                new SkillData(ranged(baseSkill == null ? null : baseSkill.tap()), null),
                new BurstData(ranged(baseBurst == null ? null : baseBurst.step()),
                        baseBurst == null ? 0f : baseBurst.energyCost()),
                // 闪避不索敌、不标交战形态（方向由玩家输入决定）
                new DodgeData(baseDodge == null ? null : baseDodge.step()));
    }

    /**
     * 把兜底步改成这个角色的交战形态。
     *
     * <p>{@code fallback(...)} 每次调用都返回全新对象，所以这里原地改它的字段是安全的，
     * 不会污染其它角色拿到的兜底表。
     */
    private static ActionStep ranged(ActionStep step) {
        return step == null ? null : step.withEngagement(RANGED).withAttackRange(ATTACK_RANGE);
    }
}
