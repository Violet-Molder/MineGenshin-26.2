package com.linweiyun.genshin.core.system.combat.animation;

import com.linweiyun.genshin.core.system.combat.animation.action.CharacterActions;
import com.linweiyun.genshin.core.system.combat.animation.action.ResourceDrivenActionHandler;
import com.linweiyun.genshin.core.system.combat.animation.config.DefaultCharacterAnimations;
import com.linweiyun.genshin.core.character.sword.vesna.Vesna;
import com.linweiyun.genshin.core.character.sword.vesna.VesnaAnimations;

/**
 * 角色动作系统的登记表 —— <b>加角色就在这里加一行</b>。
 *
 * <p>对应参考2 的 {@code Characters.registerAll()}：渲染数据是数据驱动的（{@code XxxResources}），
 * 动画/动作是代码登记的（{@code XxxAnimations}）。
 *
 * <p>本项目所有角色共用同一个 {@link ResourceDrivenActionHandler}：动作时序来自
 * 各自的 {@code XxxResources.ACTION_DATA} → {@code XxxTalent.buildActionSet()}，
 * 所以只要角色有 {@code CharacterActionData}，动作就能跑起来，不需要每个角色写一套动作类。
 * 某个角色确实需要特殊编排时，把第二个参数换成自己的 {@code CharacterActionHandler} 实现即可。
 *
 * <p>由 {@code MinegenshinClient.onClientSetup} 调用（客户端专属）。
 */
public final class CharacterAnimationRegistry {

    private CharacterAnimationRegistry() {
    }

    public static void registerAll() {
        // 目前只有 Vesna 有完整资源和天赋；其余角色登记为「只有通用常态动画、没有动作」
        CharacterActions.register(Vesna.ID, ResourceDrivenActionHandler.INSTANCE, VesnaAnimations.INSTANCE);

        registerPlaceholder("shenhe");
        registerPlaceholder("arlecchino");
        registerPlaceholder("columbina");
        registerPlaceholder("raiden_shogun");
    }

    /** 还没有专属动画数据的角色：只保证模型能渲染，按键不产生动作。 */
    private static void registerPlaceholder(String characterId) {
        CharacterActions.register(characterId, ResourceDrivenActionHandler.INSTANCE,
                DefaultCharacterAnimations.INSTANCE);
    }
}
