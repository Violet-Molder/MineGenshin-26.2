package com.linweiyun.genshin.content.entities.teyvat.monster.slime;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.linweiyun.genshin.config.entity.MobBehaviorConfig;
import com.linweiyun.genshin.content.entities.ai.control.WriggleMoveControl;
import com.linweiyun.genshin.content.entities.ai.goal.ApproachTargetGoal;
import com.linweiyun.genshin.content.entities.ai.goal.LeapSmashGoal;
import com.linweiyun.genshin.content.entities.teyvat.ElementalCreature;
import com.linweiyun.genshin.content.entities.teyvat.monster.TeyvatMonster;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.genshin.core.system.about.block.BlockElementHelper;
import com.linweiyun.genshin.core.system.shield.ShieldProfiles;
import com.linweiyun.genshin.core.system.shield.ShieldService;
import com.linweiyun.genshin.core.system.shield.ShieldState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.Nullable;

/**
 * <b>大型冰史莱姆</b>（Large Cryo Slime）—— 元素生物，带冰元素护盾。
 *
 * <h2>它由三块拼起来</h2>
 * <ol>
 *   <li><b>身体</b>：蠕动位移 + 有目标小跳（直接复用 test1/test2 那套
 *       {@link WriggleMoveControl} 与 {@link ApproachTargetGoal}）；</li>
 *   <li><b>技能</b>：撞击 / 冰刺三连 / 冰雾 / 跃起砸落 / 护盾恢复，见 {@code registerGoals}；</li>
 *   <li><b>护盾</b>：{@link ShieldProfiles#CRYO_ELEMENT}，
 *       伤害打不进本体、破盾只能靠元素与削韧。</li>
 * </ol>
 *
 * <h2>元素生物的统一规则</h2>
 * {@link ElementalCreature} 让它<b>永久免疫冰元素伤害</b>（含「冻」元素，
 * 因为冻的主元素是冰）。免疫判定在 {@code TeyvatMonster#hurtServer} 里统一做。
 *
 * <h2>环境交互</h2>
 * 实现 {@code ElementalAttachable.onAttachElement} 拒绝水元素附着。
 * 踩水 → 给水附着冰（冻结成浮冰），踩冰 → 给冰附着冻（永不融化）。
 *
 * <h2>数值</h2>
 * 生命 ×2、攻击 ×1.4，接在既有的「按等级查表」系统上
 * （{@code MonsterLevelSpawnHandler} 会读这两个系数 × 配置表的基础值）。
 */
public class LargeCryoSlime extends TeyvatMonster implements ElementalCreature, GeoEntity {

    /** 生命值系数（配置表基础值 × 它）。 */
    //TEMP
    public static final float HEALTH_MULTIPLIER = 2.0f;

    /** 攻击力系数（配置表基础值 × 它）。 */
    //TEMP
    public static final float ATTACK_MULTIPLIER = 1.4f;

    /** 只有一个跳跃动画，浮空时播。 */
    //TEMP
    private static final RawAnimation JUMPING = RawAnimation.begin().thenPlay("move.jump");

    /**
     * 破盾：把 {@code hat} 骨骼缩放成 0，等于不渲染它。播完停在最后一帧。
     *
     * <p>GeckoLib 5 没有 {@code GeoBone.setHidden}（骨骼显隐由动画/渲染趟的
     * {@code frameSnapshot} 决定），所以「隐藏一根骨骼」最省事、最稳的做法就是
     * 用一段动画把它缩成 0 —— 不碰渲染器、不用改模型文件。
     */
    //TEMP
    private static final RawAnimation SHIELD_DOWN = RawAnimation.begin().thenPlayAndHold("shield.down");

    /** 补盾：把 {@code hat} 骨骼缩回 1，帽子回来。 */
    //TEMP
    private static final RawAnimation SHIELD_UP = RawAnimation.begin().thenPlayAndHold("shield.up");

    //TEMP
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** 护盾是不是已经给过了（决定要不要在第一次 tick 补盾）。 */
    //TEMP
    private boolean shieldInitialized;

    /** 自挂冰附着的计时器（刻）。 */
    //TEMP
    private int cryoAuraTimer = SELF_AURA_INTERVAL;

    /** 自挂冰附着的间隔：每秒一次。 */
    //TEMP
    private static final int SELF_AURA_INTERVAL = 20;

    /** 帽子形态这条控制器是不是还没做过第一次判断（第一帧只记录、不播动画）。 */
    //TEMP
    private boolean shieldAnimInitialized;

    /** 上一帧的持盾状态。 */
    //TEMP
    private boolean lastShielded = true;

    /** 当前该播哪段帽子动画；null = 还没发生过变化，保持默认（帽子在）。 */
    //TEMP
    @Nullable
    private RawAnimation shieldAnim;

    //TEMP
    public LargeCryoSlime(EntityType<? extends TeyvatMonster> type, Level level) {
        super(type, level);
        this.moveControl = new WriggleMoveControl<LargeCryoSlime>(this);
        this.navigation.setCanFloat(false);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
    }

    // ==================== 元素附着控制 ====================

    /**
     * 冰史莱姆拒绝所有水元素附着。
     * 通过 Mixin 注入的 ElementalAttachable 接口实现。
     */
    @Override
    public boolean onAttachElement(GenshinElement element, AttachmentSource source, AttachmentProfile profile) {
        return element != ModElements.HYDRO.get();
    }

    // ==================== 元素生物 ====================

    //TEMP
    @Override
    public GenshinElement getCreatureElement() {
        return ModElements.CYRO.get();
    }

    //TEMP
    @Override
    public float getHealthMultiplier() {
        return HEALTH_MULTIPLIER;
    }

    //TEMP
    @Override
    public float getAttackMultiplier() {
        return ATTACK_MULTIPLIER;
    }

    //TEMP
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    // ==================== 护盾 ====================

    /** 套上（或补回）冰元素盾。 */
    //TEMP
    public void grantCryoShield() {
        ShieldService.grant(this, ShieldProfiles.cryoElementShield(),
                MobBehaviorConfig.shieldValue(), ShieldState.FOREVER);
    }

    //TEMP
    public boolean hasShield() {
        return ShieldService.has(this);
    }

    /** 最近一次挨打的时刻（护盾恢复逻辑用）。 */
    //TEMP
    public long lastHitGameTime() {
        return ShieldService.get(this).lastHitGameTime();
    }

    /**
     * 护盾是不是「破了」。
     *
     * <p>还没初始化过（{@code shieldInitialized == false}）不算破 ——
     * 否则刚生成的那一 tick 恢复 Goal 就会误判成「盾没了」。
     */
    //TEMP
    public boolean isShieldBroken() {
        return this.shieldInitialized && !hasShield();
    }

    // ==================== tick ====================

    //TEMP
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (!this.shieldInitialized) {
            this.shieldInitialized = true;
            this.grantCryoShield();
        }
        // 只要盾还在，就每秒给自己挂一层弱冰附着 —— 这就是「冰盾自挂元素」：
        // 火打上来会正常挂火、和这层冰反应触发融化、双方被消耗，同时盾按元素表掉量。
        // 盾没了就不再自挂（否则破盾后身上还永远带冰）。
        if (this.hasShield() && --this.cryoAuraTimer <= 0) {
            this.cryoAuraTimer = SELF_AURA_INTERVAL;
            applySelfCryoAura();
        }

        // 落水保护：冰史莱姆绝不允许在水中
        if (this.isInWater() && this.level() instanceof ServerLevel sl) {
            BlockPos escapePos = findEscapePos();
            if (escapePos != null) {
                this.teleportTo(escapePos.getX() + 0.5, escapePos.getY(), escapePos.getZ() + 0.5);
                BlockPos under = escapePos.below();
                if (this.level().getBlockState(under).is(Blocks.WATER)) {
                    BlockElementHelper.applyElement(sl, under, ModElements.CYRO.get(), 2.0f, 0.2f);
                }
            }
            return;
        }

        // 环境交互：踩水冻结 / 踩冰强化冻元素
        if (this.onGround() && this.level() instanceof ServerLevel sl) {
            BlockPos under = this.blockPosition().below();
            BlockState underState = this.level().getBlockState(under);
            if (underState.is(Blocks.WATER)) {
                // 踩水 → 给水附着冰 → 冻结反应 → 浮冰 + 冻元素
                BlockElementHelper.applyElement(sl, under, ModElements.CYRO.get(), 1.0f, 0.2f);
            } else if (underState.is(Blocks.FROSTED_ICE)) {
                // 踩冰 → 每 tick 刷新冻元素量，靠高频刷新抵消衰减实现永不化
                BlockElementHelper.applyElement(sl, under, ModElements.FROZEN.get(), 1.0f, 0.2f);
            }
        }
    }

    /** 给自己挂弱冰附着。 */
    //TEMP
    private void applySelfCryoAura() {
        StatusContainer container = this.getData(AttachmentRegistration.CONTAINER);
        if (container == null) {
            return;
        }
        // 自挂元素是「全额附着 + 后手残留」：不走 NORMAL_ATTACK（那条有 0.8 损耗、且反应后清残留），
        // 所以这里显式把 lossMultiplier 设成 1.0，来源用 SELF_ATTACH。
        AttachmentProfile weak = AttachmentProfile.WEAK;
        AttachmentProfile full = new AttachmentProfile(
                weak.getBaseQuantity(), 1.0f, weak.getDecayPerSecond(), weak.getDurationSeconds());
        ElementalAttachmentHelper.attach(this, container, ModElements.CYRO.get(),
                AttachmentSource.SELF_ATTACH, full);
    }

    /**
     * 从脚下逐层向上搜索非水空气方块作为安全传送点。
     */
    private BlockPos findEscapePos() {
        BlockPos pos = this.blockPosition();
        int maxSearch = 16;
        for (int dy = 0; dy < maxSearch; dy++) {
            BlockPos check = pos.above(dy);
            BlockState state = this.level().getBlockState(check);
            if (!state.is(Blocks.WATER) && state.isAir()) {
                return check;
            }
        }
        return pos.above(maxSearch);
    }

    // ==================== AI ====================

    /**
     * 优先级从「大招」到「小动作」排列。
     *
     * <p>注意几个技能 Goal <b>都不占 MOVE</b>：它们要么自己写位移，
     * 要么原地抬手，所以能和优先级 6 的接近 Goal 并行 ——
     * 一边挪一边放技能，不会互相抢占。
     */
    //TEMP
    @Override
    protected void registerGoals() {
        // 1：普通攻击 —— <b>向前大跳撞击，撞到之后落回起跳点</b>
        //    轨迹是一条弧线（起跳 → 空中撞上 → 掉头落回），也就是「1/4 圆」那一下。
        //    直接复用 test1 的 LeapSmashGoal：它天生就是「跳出去打一下再回原位」。
        this.goalSelector.addGoal(1, new LeapSmashGoal<>(this, MobBehaviorConfig.collideRange()));
        // 2~4：三个技能，冷却长、发动率低，只作为点缀
        this.goalSelector.addGoal(2, new CryoSlimeSlamGoal(this));
        this.goalSelector.addGoal(3, new CryoSlimeMistGoal(this));
        this.goalSelector.addGoal(4, new CryoSlimeShardGoal(this));
        // 5：护盾恢复
        this.goalSelector.addGoal(5, new CryoSlimeShieldRestoreGoal(this));
        // 6：只走不打。
        //    ⚠️ 停下距离必须和撞击的触发距离<b>一致</b>：停在 6 格而撞击要 4 格内的话，
        //    它会在 6 格站定、然后永远等不到可以起跳的距离（这个坑踩过）。
        this.goalSelector.addGoal(6, new ApproachTargetGoal(this, 0.6D, MobBehaviorConfig.collideRange()));
        // 8/9：看人与东张西望
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // 村民（含流浪商人：AbstractVillager 是 Villager 和 WanderingTrader 的父类）
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
    }

    // ==================== GeckoLib ====================

    //TEMP
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    /** 现有资源只有一条 {@code move.jump}，浮空时播它；盾没了再叠一层 {@code shield.down}。 */
    //TEMP
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("jump", 0, this::jumpingState));
        controllers.add(new AnimationController<>("shield", 0, this::shieldState));
    }

    //TEMP
    private PlayState jumpingState(AnimationTest<GeoAnimatable> test) {
        if (!this.onGround()) {
            return test.setAndContinue(JUMPING);
        }
        return PlayState.STOP;
    }

    /**
     * 帽子形态：<b>只在「盾在/盾破」发生变化的那一帧</b>播一次对应动画。
     *
     * <p>为什么不能按状态每帧 {@code setAndContinue}：
     * <ul>
     *   <li>生成的第一帧客户端还没收到盾的同步（{@code hasShield()} 是 false），
     *       按状态播就会立刻把帽子收掉 —— 表现是「刚生成就没帽子」；</li>
     *   <li>盾恢复后如果只是 {@code STOP} 这条控制器，上一帧留下的「缩成 0」
     *       不会自己还原，帽子就再也回不来了。</li>
     * </ul>
     * 所以：第一帧只记录、不播；之后只在状态翻转时切到 {@code shield.down} / {@code shield.up}，
     * 两段动画都 {@code thenPlayAndHold}（停在最后一帧），谁也不用循环。
     */
    //TEMP
    private PlayState shieldState(AnimationTest<GeoAnimatable> test) {
        boolean shielded = this.hasShield();
        if (!this.shieldAnimInitialized) {
            this.shieldAnimInitialized = true;
            this.lastShielded = shielded;
            return PlayState.STOP;
        }
        if (shielded != this.lastShielded) {
            this.lastShielded = shielded;
            this.shieldAnim = shielded ? SHIELD_UP : SHIELD_DOWN;
        }
        if (this.shieldAnim == null) {
            return PlayState.STOP;
        }
        return test.setAndContinue(this.shieldAnim);
    }

    // ==================== 工具 ====================

    /** 攻击力数值（技能伤害都按它的倍率算）。 */
    //TEMP
    public float attackDamageValue() {
        return (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    //TEMP
    @Nullable
    public LivingEntity currentTarget() {
        LivingEntity target = this.getTarget();
        return target != null && target.isAlive() ? target : null;
    }
}