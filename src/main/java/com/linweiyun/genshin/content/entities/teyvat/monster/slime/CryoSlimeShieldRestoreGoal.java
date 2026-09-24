package com.linweiyun.genshin.content.entities.teyvat.monster.slime;

import com.linweiyun.genshin.config.entity.MobBehaviorConfig;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * <b>护盾恢复</b> —— 盾破了之后找机会重新套上。
 *
 * <h2>两个触发条件（满足其一即可）</h2>
 * <ol>
 *   <li><b>失去护盾满 {@code shield-restore-seconds}</b>（默认 30 秒）；</li>
 *   <li><b>多久没挨打满 {@code shield-idle-restore-seconds}</b>（默认 10 秒）——
 *       这个更早生效，也就是「哪怕还在战斗状态，只要十分钟没人碰它，它就开始补盾」。</li>
 * </ol>
 *
 * <p>然后原地停顿 {@code shield-cast-seconds}（默认 2 秒）才真正补上 ——
 * 这 2 秒是玩家的输出窗口。
 *
 * <p>时间全部可配：深渊环境把两个秒数调小就是「机制加强」。
 */
public class CryoSlimeShieldRestoreGoal extends Goal {

    private final LargeCryoSlime slime;

    private int elapsed;

    /** 盾是什么时候破的（服务端刻）；-1 = 未知/还没破。 */
    private long shieldBrokenAt = -1L;

    public CryoSlimeShieldRestoreGoal(LargeCryoSlime slime) {
        this.slime = slime;
        // 占 MOVE：补盾时要停下来，不能被漫游抢走
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.slime.hasShield()) {
            this.shieldBrokenAt = -1L;
            return false;
        }
        if (!this.slime.isShieldBroken()) {
            // 还没初始化过盾，不算「破了」
            return false;
        }
        long now = this.slime.level().getGameTime();
        if (this.shieldBrokenAt < 0L) {
            this.shieldBrokenAt = now;
        }
        boolean brokenLongAgo = now - this.shieldBrokenAt >= MobBehaviorConfig.shieldRestoreTicks();
        boolean idleLongEnough = now - this.slime.lastHitGameTime() >= MobBehaviorConfig.shieldIdleRestoreTicks();
        return brokenLongAgo || idleLongEnough;
    }

    @Override
    public boolean canContinueToUse() {
        return this.elapsed <= MobBehaviorConfig.shieldCastTicks() + 5 && !this.slime.hasShield();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.elapsed = 0;
        this.slime.getNavigation().stop();
        this.slime.setAggressive(false);
    }

    @Override
    public void tick() {
        this.elapsed++;
        this.slime.getNavigation().stop();
        this.slime.setSpeed(0.0F);
        this.slime.setZza(0.0F);
        if (this.elapsed >= MobBehaviorConfig.shieldCastTicks()) {
            this.slime.grantCryoShield();
            this.shieldBrokenAt = -1L;
        }
    }

    @Override
    public void stop() {
        this.elapsed = 0;
    }
}
