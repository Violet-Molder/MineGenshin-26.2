package com.linweiyun.genshin.core.system.about;

import com.linweiyun.genshin.core.element.GenshinElement;

/**
 * 元素附着入口接口 —— 所有可接受元素附着的宿主都实现此接口。
 *
 * 以后所有元素附着的入口不是直接调 ElementalAttachmentHelper.attach，
 * 而是先调用目标身上的 onAttachElement 判断是否允许。
 *
 * LivingEntity / BlockEntity → Mixin 实现（默认返回 true）
 * 方块（水/冰等）          → BlockElementHelper 内静态判断
 * Item                    → 暂不处理
 */
public interface ElementalAttachable {

    /**
     * 元素附着前回调。
     * @return true 允许附着，false 拒绝
     */
    boolean onAttachElement(GenshinElement element, AttachmentSource source, AttachmentProfile profile);
}