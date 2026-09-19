package com.linweiyun.genshin.core.character.sword.vesna;

import com.linweiyun.genshin.config.character.ShenheAttributeConfig;
import com.linweiyun.genshin.core.character.PGCharacter;
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

    // ==================== HUD 显示 CD 覆写 ====================

    /**
     * 风骑模式下特殊战技无 CD，HUD 显示 0。
     * 内部 elementalSkillCooldownTick 照常计时，只是不显示。
     */
    @Override
    public float getSkillDisplayCooldown() {
        if (windriderActive) {
            return 0f;
        }
        return super.getSkillDisplayCooldown();
    }

    @Override
    public void performElementalSkill(Player player, int skillTime) {
        if (player.level().isClientSide()) return;

        if (windriderActive) {
            if (talent != null) {
                talent.elementalSkill(player, this, skillTime);
            }
        } else {
            super.performElementalSkill(player, skillTime);
        }
        syncRealtimeState();
    }
}