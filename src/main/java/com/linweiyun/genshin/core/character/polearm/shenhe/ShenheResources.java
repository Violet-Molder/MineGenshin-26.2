package com.linweiyun.genshin.core.character.polearm.shenhe;

import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData.ActionStep;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData.Engagement;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData.Hit;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData.SkillData;

import java.util.List;

/**
 * 申鹤的动作数据表。
 *
 * <h2>为什么她必须有一份自己的表</h2>
 * 没有 {@code getActionData()} 覆写的角色会走
 * {@link CharacterActionData#fallback(int) 通用兜底表}，而兜底表给<b>每一段</b>
 * 都塞了一个 {@code Move(0, 1.0)} 的服务端冲量。对申鹤来说这是错的：
 * <b>她的位移完全由自己的 {@code DashSystem} 负责</b>
 * （{@code RushesForward} 算方向 → 客户端 {@code startDash} 每刻 {@code move}
 * → 服务端 {@code startDamageDash} 沿途扫伤害）。
 *
 * <p>两者同帧打架的结果就是「E 不放突刺了、只剩一下小幅前移」——
 * 服务端的冲量盖掉了客户端的突刺。所以这里的战技两段 {@code moves} 必须是空的。
 *
 * <h2>长按为什么也要在这里写</h2>
 * {@code SkillBase.buildDefaultActionSet} 只在 {@code skill.hold() != null} 时才注册
 * {@code ELEMENTAL_SKILL_HOLD}；兜底表的 {@code SkillData} 是 {@code (step, null)}，
 * 于是客户端的短/长按判定（短 CD ≠ 长 CD 才等长按）通过之后，
 * {@code set.getElementalSkillHold()} 是 {@code null} → 长按什么都不发生，
 * 而且松手时也不会补一个短按（长按已经触发过了）。申鹤的短 CD 200 / 长 CD 300
 * 本来就是两套，所以这里补上 hold 那一段就能直接把她的长按效果接回来。
 *
 * <p>普攻 / 大招 / 闪避仍沿用兜底表：那几段现在没出问题，等有动画了再逐段替换。
 */
public final class ShenheResources {

    private ShenheResources() {
    }

    public static final CharacterActionData ACTION_DATA = build();

    /** 短按 E：突刺 + 一次冰伤（伤害本身在 {@code ShenheSkill.elementalSkill} 里算）。 */
    private static final int TAP_DURATION = 25;
    /** 长按 E：站桩，给全队冰翎与普攻/重击/下落加成。 */
    private static final int HOLD_DURATION = 30;
    /** 伤害点（刻）—— 和 {@code DashSystem} 的突刺刻数（10 刻）对齐，冲出去之后结算。 */
    private static final int SKILL_HIT_DELAY = 8;

    /**
     * 战技的交战形态：<b>通用突进与吸附都关掉</b>，只保留「转向目标」。
     *
     * <p>她的位移是自己的 {@code DashSystem}（服务端推速度 + 沿途扫伤害），
     * 通用那套（{@code AttackApproach} 的突进 / 出手瞬间的吸附小步）会和她抢位移：
     * <ul>
     *   <li>{@code withDash(false)}：不要框架的近战突进；</li>
     *   <li>{@code withAdhesion(0, 0)}：不要「出手瞬间朝目标推一小步」
     *       —— 就是玩家说的「通用的小幅度移动」。</li>
     * </ul>
     *
     * <p>转向保留（默认）：{@code RushesForward} 是按<b>视线方向</b>算位移的，
     * 先转向目标才是「朝目标突刺」。
     */
    private static final Engagement NO_GENERIC_MOVE =
            Engagement.melee().withDash(false).withAdhesion(0, 0);

    private static CharacterActionData build() {
        CharacterActionData base = CharacterActionData.fallback(5);

        // 短按：没有准备阶段（按下去就是突刺，位移本身就是执行期），执行期盖住伤害点
        ActionStep tap = new ActionStep(
                "skill", TAP_DURATION, SKILL_HIT_DELAY + 6, 3,
                List.of(),                                   // ← 位移交给 DashSystem，这里必须为空
                List.of(new Hit(SKILL_HIT_DELAY, 0, 1.5, 1.0, 0.0, 3.0, false)),
                List.of(),
                0, 0, 0, 0
        ).withEngagement(NO_GENERIC_MOVE);

        // 长按：站桩不位移
        ActionStep hold = new ActionStep(
                "skill_hold", HOLD_DURATION, SKILL_HIT_DELAY + 6, 3,
                List.of(),
                List.of(new Hit(SKILL_HIT_DELAY, 0, 1.5, 1.0, 0.0, 3.0, false)),
                List.of(),
                0, 0, 0, 0
        ).withEngagement(NO_GENERIC_MOVE);

        return new CharacterActionData(
                base.combo(),
                new SkillData(tap, hold),
                base.burst(),
                base.dodge());
    }
}
