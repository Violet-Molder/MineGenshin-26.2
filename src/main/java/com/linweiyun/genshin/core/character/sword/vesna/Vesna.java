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
    public static final int SPECIAL_SKILL_ENERGY_COST = 6;

    @DescSynced
    @Persisted(key = "vesnaEnergy")
    protected float vesnaEnergy;
    @DescSynced
    @Persisted(key = "vesnaMaxEnergy")
    protected float vesnaMaxEnergy;
    @DescSynced
    @Persisted(key = "windriderActive")
    protected boolean windriderActive;
    @DescSynced
    @Persisted(key = "windriderRemainingTicks")
    protected int windriderRemainingTicks;

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

    public void addEnergy(float value) {
        this.vesnaEnergy = Math.min(vesnaEnergy + value, vesnaMaxEnergy);
        LOGGER.info("Vesna energy: {}", vesnaEnergy);
        syncRealtimeState();
    }

    public void consumeEnergy(float value) {
        this.vesnaEnergy = Math.max(vesnaEnergy - value, 0);
        syncRealtimeState();
    }

    @Override
    public void tick(Player player) {
        super.tick(player);
        if (player.level().isClientSide()) return;

        if (windriderActive && windriderRemainingTicks > 0) {
            windriderRemainingTicks--;
            if (windriderRemainingTicks <= 0) {
                windriderActive = false;
                windriderRemainingTicks = 0;
            }
        }

        if (windriderActive) {
            syncRealtimeState();
        }
    }

    public void activateWindriderMode() {
        this.windriderActive = true;
        this.windriderRemainingTicks = WIND_RIDER_DURATION_TICKS;
    }

    @Override
    public String getActionStateKey(Player player) {
        return windriderActive ? "windrider" : "default";
    }

    /** 风骑模式下特殊战技无 CD，HUD 显示 0 */
    @Override
    public float getSkillDisplayCooldown() {
        if (windriderActive) return 0f;
        return super.getSkillDisplayCooldown();
    }

    // ==================== E 钩子覆写 ====================

    /**
     * 风骑内：只要能量够就能放，不看 CD。
     * 非风骑：走父类默认（CD == 0）。
     */
    @Override
    public boolean canUseElementalSkill(Player player, int skillTime) {
        if (windriderActive) {
            return vesnaEnergy >= SPECIAL_SKILL_ENERGY_COST;
        }
        return super.canUseElementalSkill(player, skillTime);
    }

    /**
     * 风骑内：扣能量，不设 CD。
     * 非风骑：进风骑模式 + 设 18s CD（前摇还没结束，CD 已经在跑）。
     */
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
}