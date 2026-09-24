package com.linweiyun.genshin.core.character.polearm.raiden_shogun;

import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData;
import com.linweiyun.genshin.core.system.combat.action.data.ActionStep;
import com.linweiyun.genshin.core.system.combat.action.data.BurstData;
import com.linweiyun.genshin.core.system.combat.action.data.ComboData;
import com.linweiyun.genshin.core.system.combat.action.data.DodgeData;
import com.linweiyun.genshin.core.system.combat.action.data.Engagement;
import com.linweiyun.genshin.core.system.combat.action.data.SkillData;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 雷电将军的动作数据表。
 *
 * <h2>为什么要有这一份</h2>
 * 不覆写 {@code PGCharacter.getActionData()} 的角色会走
 * {@link CharacterActionData#fallback(int) 通用兜底表}：兜底表能跑，但它不知道
 * 这个角色是近战还是远程、也不带角色自己的动画名。接上本表之后动作时序与索敌形态
 * 都归角色自己管，客户端动画与服务端伤害仍读同一份数据（见 {@code ResourceDrivenActionHandler}）。
 *
 * <h2>数值口径</h2>
 * <b>时序完全复用兜底表</b>（每一段的 duration / protectDuration / hits / comboWindow
 * 都取自 {@link CharacterActionData#fallback(int)}），本表只做两件事：
 * <ol>
 *   <li>把连段数固定成 {@code RaidenShogunSkill.getMaxCombo()} 声明的 {@value #MAX_COMBO} 段；</li>
 *   <li>把每一段标成<b>长柄近战</b>形态（{@link Engagement#melee()} + 3 格攻击距离）。</li>
 * </ol>
 * 伤害倍率仍然只在 {@link RaidenShogunSkill} 里，本表不管数值。
 */
public final class RaidenShogunResources {

    private RaidenShogunResources() {
    }

    /**
     * 连段数 —— 必须和 {@link RaidenShogunSkill#getMaxCombo()} 一致
     * （{@code SkillBase.buildDefaultActionSet} 取两者的较小值）。
     */
    public static final int MAX_COMBO = 5;

    /**
     * 生效攻击距离 <b>3 格</b> = 框架的近战默认值（{@link ActionStep#DEFAULT_MELEE_RANGE}）。
     *
     * <p>长柄是<b>近战</b>：普攻本身就是 {@code AreaEntityCollector} 扫面前 2.5 格、半径 1.0 的盒，
     * 和 3 格「够得着就停下出手」是同一量级；不需要 {@code withAcquireRange}
     * （默认的索敌 6 格 / 保持 9 格、差额交给突进补才是长柄的手感）。
     */
    public static final float ATTACK_RANGE = ActionStep.DEFAULT_MELEE_RANGE;

    /** 近战形态：允许框架突进 + 出手瞬间吸附一小步（{@link Engagement#melee()} 的默认值）。 */
    private static final Engagement MELEE = Engagement.melee();

    public static final CharacterActionData ACTION_DATA = build();

    private static CharacterActionData build() {
        // 时序的唯一来源：兜底表（不新造平衡数值）
        CharacterActionData base = CharacterActionData.fallback(MAX_COMBO);

        Map<Integer, ActionStep> combo = new LinkedHashMap<>();
        for (int stage = 1; stage <= MAX_COMBO; stage++) {
            ActionStep step = base.combo().getStep(stage);
            if (step != null) {
                combo.put(stage, melee(step));
            }
        }

        SkillData baseSkill = base.skill();
        BurstData baseBurst = base.burst();
        DodgeData baseDodge = base.dodge();

        return new CharacterActionData(
                new ComboData(MAX_COMBO, combo),
                // 战技只有点按：他的短 CD == 长 CD（构造器用的两冷却重载），
                // 客户端 {@code pressSkill} 的短/长按判定恒为「点按」，
                // 所以 {@code RaidenShogunSkill.elementalSkill(..., 1000)} 那条长按分支走不到；
                // 想让长按也生效要改角色构造器的短/长冷却，那属于数值调整，不在本表范围内。
                new SkillData(melee(baseSkill == null ? null : baseSkill.tap()), null),
                new BurstData(melee(baseBurst == null ? null : baseBurst.step()),
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
    private static ActionStep melee(ActionStep step) {
        return step == null ? null : step.withEngagement(MELEE).withAttackRange(ATTACK_RANGE);
    }
}
