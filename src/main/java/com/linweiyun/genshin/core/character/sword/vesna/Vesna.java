package com.linweiyun.genshin.core.character.sword.vesna;

import com.linweiyun.genshin.config.character.ShenheAttributeConfig;
import com.linweiyun.genshin.core.character.IStellarSwirlParticipant;
import com.linweiyun.genshin.core.character.sword.SwordCharacter;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.mojang.logging.LogUtils;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Getter
public class Vesna extends SwordCharacter implements IStellarSwirlParticipant {
    public static final String ID = "vesna";
    public static final int UID = 115001;
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int WIND_RIDER_DURATION_TICKS = 15 * 20;
    /** 每层剑气 = 6 点能量 */
    public static final float ENERGY_PER_QIQI = 6f;
    /** 翔风剑一次消耗 1 层剑气 */
    public static final float SPECIAL_SKILL_ENERGY_COST = ENERGY_PER_QIQI;
    /** 开 E 获得两层剑气 */
    public static final float WIND_RIDER_ENTER_ENERGY = ENERGY_PER_QIQI * 2;

    @DescSynced @Persisted(key = "vesnaEnergy")
    protected float vesnaEnergy;
    @DescSynced @Persisted(key = "vesnaMaxEnergy")
    protected float vesnaMaxEnergy;
    @DescSynced @Persisted(key = "windriderActive")
    protected boolean windriderActive;
    @DescSynced @Persisted(key = "windriderRemainingTicks")
    protected int windriderRemainingTicks;

    /** 下一次施放的翔风剑阶级（1~3）。talent 读取它当本次施放等级。 */
    @DescSynced @Persisted(key = "xiangfengJianLevel")
    protected int xiangfengJianLevel = 1;

    /** 本次巡风列装已施放的三阶次数，达到 3 次退出模式。 */
    @DescSynced @Persisted(key = "lv3UsesInWindrider")
    protected int lv3UsesInWindrider = 0;

    public Vesna() {
        super(UID, 5, Component.translatable("character.name.vesna"),
                ModElements.ANEMO.getId().toString(), CharacterAscendAttribute.ATK,
                18 * 20, 10 * 20, 80f, ID,
                Map.of(
                        ModAttributes.MAX_HP.getId(), ShenheAttributeConfig::getAllHp,
                        ModAttributes.ATK.getId(), ShenheAttributeConfig::getAllAtk,
                        ModAttributes.DEF.getId(), ShenheAttributeConfig::getAllDef
                ));
        this.talent = new VesnaTalent();
        this.vesnaEnergy = 0;
        this.vesnaMaxEnergy = 18;
    }

    @Override
    public Map<Identifier, Supplier<List<? extends Integer>>> getStatGrowthMap() {
        return Map.of(
                ModAttributes.MAX_HP.getId(), ShenheAttributeConfig::getAllHp,
                ModAttributes.ATK.getId(), ShenheAttributeConfig::getAllAtk,
                ModAttributes.DEF.getId(), ShenheAttributeConfig::getAllDef
        );
    }

    // ==================== 能量 ====================

    public void addEnergy(float value) {
        this.vesnaEnergy = Math.min(vesnaEnergy + value, vesnaMaxEnergy);
        syncRealtimeState();
    }

    public void consumeEnergy(float value) {
        this.vesnaEnergy = Math.max(vesnaEnergy - value, 0);
        syncRealtimeState();
    }

    // ==================== 模式切换 ====================

    public void activateWindriderMode() {
        this.windriderActive = true;
        this.windriderRemainingTicks = WIND_RIDER_DURATION_TICKS;
        this.vesnaEnergy = WIND_RIDER_ENTER_ENERGY;
        this.xiangfengJianLevel = 1;
        this.lv3UsesInWindrider = 0;
    }

    public void exitWindriderMode() {
        this.windriderActive = false;
        this.windriderRemainingTicks = 0;
        this.vesnaEnergy = 0f;
        this.xiangfengJianLevel = 1;
        this.lv3UsesInWindrider = 0;
    }

    // ==================== 字段 setter（供 talent 修改） ====================

    public void setXiangfengJianLevel(int level) {
        this.xiangfengJianLevel = Math.max(1, Math.min(3, level));
        syncRealtimeState();
    }

    public void setLv3UsesInWindrider(int n) {
        this.lv3UsesInWindrider = Math.max(0, n);
        syncRealtimeState();
    }

    // ==================== action state ====================

    @Override
    public String getActionStateKey(Player player) {
        return windriderActive ? "windrider" : "default";
    }

    @Override
    public float getSkillDisplayCooldown() {
        if (windriderActive) return 0f;
        return super.getSkillDisplayCooldown();
    }

    // ==================== E 钩子覆写 ====================

    @Override
    public boolean canUseElementalSkill(Player player, int skillTime) {
        if (windriderActive) return vesnaEnergy >= SPECIAL_SKILL_ENERGY_COST;
        return super.canUseElementalSkill(player, skillTime);
    }

    @Override
    public void applyElementalSkillCooldown(Player player, int skillTime) {
        if (windriderActive) {
            consumeEnergy(SPECIAL_SKILL_ENERGY_COST);
        } else {
            activateWindriderMode();
            getData().setElementalSkillCooldownTick(18 * 20);
            syncRealtimeState();
        }
    }

    @Override
    public void sendSkillCooldownMessage(Player player) {
        if (windriderActive) {
            player.sendSystemMessage(Component.translatable("message.minegenshin.not_enough_energy"));
        } else {
            super.sendSkillCooldownMessage(player);
        }
    }

    // ==================== tick ====================

    @Override
    public void tick(Player player) {
        super.tick(player);

        if (player.level().isClientSide()) return;

        if (windriderActive && windriderRemainingTicks > 0) {
            windriderRemainingTicks--;
            if (windriderRemainingTicks <= 0) {
                exitWindriderMode();
            }
        }

        if (windriderActive) {
            syncRealtimeState();
        }
    }
}