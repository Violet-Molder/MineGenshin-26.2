package com.linweiyun.genshin.core.element;

import com.linweiyun.genshin.content.entities.teyvat.TeyvatLiving;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

/**
 * 冻元素 —— 附着在非玩家生物身上时禁用AI（NoAI=true）
 */
public class FrozenElement extends GenshinElement {

    private static final Logger LOGGER = LogUtils.getLogger();

    protected FrozenElement(String translationKey) {
        super(false, false, translationKey);
    }

    @Override
    public void onAttach(LivingEntity entity) {
        if (!isNonPlayerLiving(entity)) return;
        if (entity instanceof TeyvatLiving teyvat) {
            teyvat.setAiEnabled(false);
            LOGGER.debug("FrozenElement.onAttach: entity={} AI disabled", entity.getName().getString());
        }
    }

    @Override
    public void onDetach(LivingEntity entity) {
        if (!isNonPlayerLiving(entity)) return;
        if (entity instanceof TeyvatLiving teyvat) {
            teyvat.setAiEnabled(true);
            LOGGER.debug("FrozenElement.onDetach: entity={} AI enabled", entity.getName().getString());
        }
    }
}