package com.linweiyun.genshin.content.entities.test;

import com.linweiyun.genshin.content.entities.ai.control.WriggleMoveControl;
import com.linweiyun.genshin.content.entities.ai.goal.ApproachTargetGoal;
import com.linweiyun.genshin.content.entities.teyvat.monster.TeyvatMonster;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * 1 号测试实体 {@code minegenshin:test1} —— 最简的一只提瓦特敌对生物。
 *
 * <h2>行为规格</h2>
 * <ul>
 *   <li><b>不能飞、不能游泳</b>：用地面寻路，且把水的寻路代价设为不可通行
 *       （{@link PathType#WATER} 的寻路惩罚改成 -1），也不注册
 *       {@link net.minecraft.world.entity.ai.goal.FloatGoal} —— 掉进水里会往下沉，
 *       而不是浮起来游。</li>
 *   <li><b>常态蠕动位移</b>：由 {@link WriggleMoveControl} 负责「一顿一冲 + 左右摆」。</li>
 *   <li><b>有目标开始跳</b>：同一个 MoveControl 在 {@code getTarget() != null} 时切成小跳推进。</li>
 *   <li><b>攻击距离 2 格</b>：与目标水平距离 ≤ {@link #SMASH_DISTANCE} 时
 *       {@link Test1LeapSmashGoal} 起跳，空中撞上目标，然后落回起跳点。</li>
 *   <li><b>会主动找玩家</b>：{@link NearestAttackableTargetGoal} + {@link HurtByTargetGoal}。</li>
 * </ul>
 *
 * <p>资源：{@code assets/minegenshin/entity/test1/} 下的
 * {@code test1.json} / {@code test1.animation.json} / {@code test1.png}，
 * 由 {@link TestMonster#assetSet()} 推出来。
 */
public class Test1Entity extends TestMonster {

    /** 资源 id，同时也是 {@code assets/minegenshin/entity/<id>/} 的目录名。 */
    //TEMP
    public static final String ASSET_ID = "test1";

    /**
     * 攻击距离：与目标的<b>水平</b>距离 ≤ 2 格就起跳。
     *
     * <p>是「以内」不是「正好 2 格」—— 1.2 格、1.8 格都算，像投篮一样可近可远。
     */
    //TEMP
    public static final double SMASH_DISTANCE = 2.0D;

    /** 撞击倍率：在 {@code ATTACK_DAMAGE} 上再乘这么多数，体现「重击」。 */
    //TEMP
    public static final float SMASH_MULTIPLIER = 1.5F;

    //TEMP
    public Test1Entity(EntityType<? extends TeyvatMonster> type, Level level) {
        super(type, level);
        this.moveControl = new WriggleMoveControl<Test1Entity>(this);

        // 不能游泳：寻路不再把水面当可漂浮区域，而是明确避开
        this.navigation.setCanFloat(false);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
    }

    //TEMP
    @Override
    public String assetId() {
        return ASSET_ID;
    }

    //TEMP
    public static AttributeSupplier.Builder createAttributes() {
        return createTestAttributes();
    }

    // ==================== AI ====================

    /**
     * 目标选择在前、动作在后 —— 和原版怪物同序：先决定「打谁」，再决定「怎么打」。
     *
     * <p>优先级 1 的出招 Goal 与优先级 2 的接近 Goal 是一对：接近在
     * {@code 距离 > 2 格} 时运行、出招在 {@code 距离 ≤ 2 格} 时运行，刚好互补。
     *
     * <p>{@link WriggleMoveControl} 的小跳推进不占优先级：它是移动控制层，
     * 由「谁拿到了 MOVE 标志位」间接决定跑不跑，所以这里只要排好 Goal 就够了。
     */
    //TEMP
    @Override
    protected void registerGoals() {
        // 1：进入 2 格就大跳扑击
        this.goalSelector.addGoal(1, new Test1LeapSmashGoal(this));
        // 2：还差得远就蠕动过去（只走不打）
        this.goalSelector.addGoal(2, new ApproachTargetGoal(this, 0.6D, SMASH_DISTANCE));
        // 4：没目标时随便蠕动（避水，所以不会主动往水里走）
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        // 8：看玩家 / 东张西望，最低优先级
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        // 挨打就还手
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // 主动找最近的玩家
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    //TEMP
    @Override
    protected TestAction defaultAction() {
        boolean moving = isMovingHorizontally();
        if (this.getTarget() != null) {
            // 有目标：移动就是小跳，站着就是待机
            return moving ? TestAction.HOP : TestAction.IDLE;
        }
        return moving ? TestAction.WRIGGLE : TestAction.IDLE;
    }

    // ==================== 撞击伤害 ====================

    /**
     * 撞击伤害。
     *
     * <h2>为什么用原版伤害源，而不是 {@code ModDamageSource}</h2>
     * 本 MOD 的伤害管线是
     * {@code LivingEntityHurtMixin → HurtEntityHelper → DirectDamagePipeline}，
     * 它的<b>基础伤害区</b>是 {@code ATK×攻击力倍率 + …}，而 ATK 只有<b>角色</b>
     * （{@code PGCharacter}）身上才有 —— 怪物进来的时候
     * {@code AttackerResolver.resolveCharacter(mob)} 返回 null，
     * 管线里的 {@code hasAttacker == false}，基础区直接取 {@code 0f}：
     *
     * <pre>
     * float baseDamage = hasAttacker ? DamageZones.baseDamage(attacker, spec) : 0f;
     * </pre>
     *
     * <p>也就是说：<b>非角色攻击者塞 {@code ModDamageSource} 必然打出 0 伤害</b>
     * （传入 {@code hurtServer} 的那个 float 会被管线整个忽略）。
     * 所以怪物一律走原版伤害源，数值取自 {@code ATTACK_DAMAGE}。
     *
     * <p>将来要接元素/抗性，正确做法是让管线支持「非角色基础伤害」
     * （比如给怪物一个攻击者替身），而不是在这里塞 spec。
     */
    //TEMP
    public void smashAttack(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * SMASH_MULTIPLIER;
        target.hurtServer(serverLevel, this.damageSources().mobAttack(this), damage);
        // 打中别人 = 进入战斗，交给 CombatTimerHandler 计时
        this.resetCombat();
    }

    @Override
    public boolean onAttachElement(GenshinElement element, AttachmentSource source, AttachmentProfile profile) {
        return true;
    }
}
