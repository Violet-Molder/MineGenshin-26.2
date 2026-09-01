package com.linweiyun.genshin.content.entities.entity.teyvat.monster;

import com.linweiyun.genshin.content.entities.TeyvatLivingEntity;
import com.linweiyun.genshin.content.entities.damagesource.ElementalDamageSourceGIM;
import com.linweiyun.genshin.enums.ElementalsGIM;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class ElementalSlime extends Monster implements TeyvatLivingEntity, GeoEntity {
    protected static final RawAnimation JUMPING_START = RawAnimation.begin().thenLoop("move.jump");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private static final EntityDataAccessor<String> DATA_ELEMENT = SynchedEntityData.defineId(ElementalSlime.class, EntityDataSerializers.STRING);
    protected ElementalSlime(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new SlimeMoveControl(this);
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ELEMENT, ElementalsGIM.FYSIKOS.getId());
    }
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new ElementalSlime.SlimeFloatGoal(this));
        this.goalSelector.addGoal(2, new ElementalSlime.SlimeAttackGoal(this));
        this.goalSelector.addGoal(3, new ElementalSlime.SlimeRandomDirectionGoal(this));
        this.goalSelector.addGoal(5, new ElementalSlime.SlimeKeepOnJumpingGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "jump", 0, this::jumpingStart));
    }


    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source instanceof ElementalDamageSourceGIM elementalDamageSourceGIM) {
            if (elementalDamageSourceGIM.getElement() == this.getElement()) {
                return false;
            }
        }
        return super.hurt(source, amount);
    }
    public ElementalsGIM getElement() {
        return ElementalsGIM.getElementById(this.entityData.get(DATA_ELEMENT));
    }

    public void setElement(ElementalsGIM element) {
        this.entityData.set(DATA_ELEMENT, element.getId());
    }
    private <E extends ElementalSlime> PlayState jumpingStart(AnimationState<E> event) {
        if (!this.onGround()) {
            return event.setAndContinue(JUMPING_START);
        }
        return PlayState.STOP;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 35.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4f)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    protected boolean isDealsDamage() {
        return this.isEffectiveAi();
    }

    protected int getJumpDelay() {
        return this.random.nextInt(20) + 10;
    }

    static class SlimeMoveControl extends MoveControl {
        private float yRot;
        private int jumpDelay;
        private final ElementalSlime slime;
        private boolean isAggressive;

        public SlimeMoveControl(ElementalSlime slime) {
            super(slime);
            this.slime = slime;
            this.yRot = 180.0F * slime.getYRot() / (float) Math.PI;
        }

        public void setDirection(float yRot, boolean aggressive) {
            this.yRot = yRot;
            this.isAggressive = aggressive;
        }

        public void setWantedMovement(double speed) {
            this.speedModifier = speed;
            this.operation = Operation.MOVE_TO;
        }

        public void tick() {
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), this.yRot, 90.0F));
            this.mob.yHeadRot = this.mob.getYRot();
            this.mob.yBodyRot = this.mob.getYRot();
            if (this.operation != Operation.MOVE_TO) {
                this.mob.setZza(0.0F);
            } else {
                this.operation = Operation.WAIT;
                if (this.mob.onGround()) {
                    this.mob.setSpeed(
                            (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                    if (this.jumpDelay-- <= 0) {
                        this.jumpDelay = this.slime.getJumpDelay();
                        if (this.isAggressive) {
                            this.jumpDelay /= 3;
                        }

                        this.slime.getJumpControl().jump();
                    } else {
                        this.slime.xxa = 0.0F;
                        this.slime.zza = 0.0F;
                        this.mob.setSpeed(0.0F);
                    }
                } else {
                    this.mob.setSpeed(
                            (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                }
            }
        }
    }


    public void playerTouch(@NotNull Player entity) {
        if (this.isDealsDamage()) {
            this.dealDamage(entity);
        }
    }

    protected void dealDamage(LivingEntity livingEntity) {
        if (this.isAlive()
                && this.isWithinMeleeAttackRange(livingEntity)
                && this.hasLineOfSight(livingEntity)) {
            DamageSource damagesource = this.damageSources().mobAttack(this);
            if (livingEntity.hurt(damagesource, this.getAttackDamage())) {
                this.playSound(
                        SoundEvents.SLIME_ATTACK,
                        1.0F,
                        (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                Level var4 = this.level();
                if (var4 instanceof ServerLevel serverlevel) {
                    EnchantmentHelper.doPostAttackEffects(serverlevel, livingEntity, damagesource);
                }
            }
        }
    }

    protected float getAttackDamage() {
        return (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    public void jumpFromGround() {
        Vec3 vec3 = this.getDeltaMovement();
        this.setDeltaMovement(vec3.x, this.getJumpPower(), vec3.z);
        this.hasImpulse = true;
        CommonHooks.onLivingJump(this);
    }

    // 攻击目标
    static class SlimeAttackGoal extends Goal {
        private final ElementalSlime slime;
        private int growTiredTimer;

        public SlimeAttackGoal(ElementalSlime slime) {
            this.slime = slime;
            this.setFlags(EnumSet.of(Flag.LOOK));
        }

        public boolean canUse() {
            LivingEntity livingentity = this.slime.getTarget();
            if (livingentity == null) {
                return false;
            } else {
                return this.slime.canAttack(livingentity)
                        && this.slime.getMoveControl() instanceof SlimeMoveControl;
            }
        }

        public void start() {
            this.growTiredTimer = reducedTickDelay(300);
            super.start();
        }

        public boolean canContinueToUse() {
            LivingEntity livingentity = this.slime.getTarget();
            if (livingentity == null) {
                return false;
            } else {
                return !this.slime.canAttack(livingentity) ? false : --this.growTiredTimer > 0;
            }
        }

        public boolean requiresUpdateEveryTick() {
            return true;
        }

        public void tick() {
            LivingEntity livingentity = this.slime.getTarget();
            if (livingentity != null) {
                this.slime.lookAt(livingentity, 10.0F, 10.0F);
            }

            MoveControl var3 = this.slime.getMoveControl();
            if (var3 instanceof SlimeMoveControl slime$slimemovecontrol) {
                slime$slimemovecontrol.setDirection(this.slime.getYRot(), this.slime.isDealsDamage());
            }
        }
    }

    static class SlimeFloatGoal extends Goal {
        private final ElementalSlime slime;

        public SlimeFloatGoal(ElementalSlime slime) {
            this.slime = slime;
            this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
            slime.getNavigation().setCanFloat(true);
        }

        public boolean canUse() {
            return (this.slime.isInWater() || this.slime.isInLava())
                    && this.slime.getMoveControl() instanceof SlimeMoveControl;
        }

        public boolean requiresUpdateEveryTick() {
            return true;
        }

        public void tick() {
            if (this.slime.getRandom().nextFloat() < 0.8F) {
                this.slime.getJumpControl().jump();
            }

            MoveControl var2 = this.slime.getMoveControl();
            if (var2 instanceof SlimeMoveControl slime$slimemovecontrol) {
                slime$slimemovecontrol.setWantedMovement(1.2);
            }
        }
    }

    static class SlimeKeepOnJumpingGoal extends Goal {
        private final ElementalSlime slime;

        public SlimeKeepOnJumpingGoal(ElementalSlime slime) {
            this.slime = slime;
            this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
        }

        public boolean canUse() {
            return !this.slime.isPassenger();
        }

        public void tick() {
            MoveControl var2 = this.slime.getMoveControl();
            if (var2 instanceof SlimeMoveControl slime$slimemovecontrol) {
                slime$slimemovecontrol.setWantedMovement(1.0F);
            }
        }
    }



    static class SlimeRandomDirectionGoal extends Goal {
        private final ElementalSlime slime;
        private float chosenDegrees;
        private int nextRandomizeTime;

        public SlimeRandomDirectionGoal(ElementalSlime slime) {
            this.slime = slime;
            this.setFlags(EnumSet.of(Flag.LOOK));
        }

        public boolean canUse() {
            return this.slime.getTarget() == null
                    && (this.slime.onGround()
                    || this.slime.isInWater()
                    || this.slime.isInLava()
                    || this.slime.hasEffect(MobEffects.LEVITATION))
                    && this.slime.getMoveControl() instanceof SlimeMoveControl;
        }

        public void tick() {
            if (--this.nextRandomizeTime <= 0) {
                this.nextRandomizeTime = this.adjustedTickDelay(40 + this.slime.getRandom().nextInt(60));
                this.chosenDegrees = (float) this.slime.getRandom().nextInt(360);
            }

            MoveControl var2 = this.slime.getMoveControl();
            if (var2 instanceof SlimeMoveControl slime$slimemovecontrol) {
                slime$slimemovecontrol.setDirection(this.chosenDegrees, false);
            }
        }
    }
}