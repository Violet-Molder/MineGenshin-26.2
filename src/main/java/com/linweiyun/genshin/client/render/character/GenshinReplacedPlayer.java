package com.linweiyun.genshin.client.render.character;

import com.geckolib.animatable.GeoReplacedEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.linweiyun.genshin.core.system.combat.animation.animatable.IPlayerAnimatableProxy;
import com.linweiyun.genshin.client.combat.state.PlayerAnimationController;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * 玩家变身角色的 GeckoLib 动画代理。
 *
 * <p>动画决策全部交给 {@link PlayerAnimationController}（照搬参考2），
 * 这个类只负责「注册控制器」和「把玩家引用递给控制器」两件事 ——
 * 以前内嵌的那份 {@code resolveState()} 运动判定已经删除，避免两套状态机互相打架。
 *
 * <p>{@link GeoReplacedEntity} 是单例动画对象：动画状态由 GeckoLib 按实体 ID 分开存，
 * 所以这里的 {@code playerEntity} 只是「本帧渲染的是谁」的临时引用，
 * 由 {@link CharacterRenderDispatcher} 在每次绘制前刷新。
 */
public class GenshinReplacedPlayer implements GeoReplacedEntity, IPlayerAnimatableProxy {

    /** 每个玩家一份实例，所以用 instanced 缓存；单例缓存会让多个玩家共用一套动画状态。 */
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this, false);

    @Nullable
    private Player playerEntity;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(PlayerAnimationController.create(this));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public @Nullable Player getPlayerEntity() {
        return this.playerEntity;
    }

    @Override
    public void setPlayerEntity(@Nullable Player player) {
        this.playerEntity = player;
    }
}
