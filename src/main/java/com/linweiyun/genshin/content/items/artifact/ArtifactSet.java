package com.linweiyun.genshin.content.items.artifact;

import com.linweiyun.genshin.content.effect.character.ICharacterEffect;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 圣遗物套装定义。
 *
 * setId           套装编号，3 位数字，001 起，同一套装下的所有部位共享该编号
 * twoPcEffect     2 件套效果
 * fourPcEffect    4 件套效果
 * hasTwoPcEffect  是否存在 2 件套效果
 * hasFourPcEffect 是否存在 4 件套效果
 */
public record ArtifactSet(int setId,
                          DeferredHolder<ICharacterEffect, ?> twoPcEffect,
                          DeferredHolder<ICharacterEffect, ?> fourPcEffect,
                          boolean hasTwoPcEffect,
                          boolean hasFourPcEffect) {

    /**
     * 获取套装 UID。
     *
     * 格式：32EIII
     *   3    固定前缀，表示属于“圣遗物类”UID
     *   2    表示这是“圣遗物套装”（区别于圣遗物物品的 1）
     *   E    套装效果位：
     *           0 —— 属于该系列的圣遗物是一组，但没有任何套装效果
     *           2 —— 只有 2 件套效果
     *           4 —— 同时拥有 2 件套和 4 件套效果
     *   III  套装编号，例如魔女套为 001
     *
     * 例如：魔女套（2+4 件套）= 324001，仅二件套 = 322001，无效果系列 = 320001。
     *
     * @return 套装 UID
     */
    public int getUID() {
        int effect;
        if (hasFourPcEffect) {
            effect = 4;
        } else if (hasTwoPcEffect) {
            effect = 2;
        } else {
            effect = 0;
        }
        return 320000 + effect * 1000 + setId;
    }
}