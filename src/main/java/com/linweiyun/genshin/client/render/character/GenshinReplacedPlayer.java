package com.linweiyun.genshin.client.render.character;

import com.geckolib.animatable.GeoReplacedEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.linweiyun.genshin.client.animation.animatable.IPlayerAnimatableProxy;
import com.linweiyun.genshin.client.animation.state.PlayerAnimationController;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * 玩家变身角色的 GeckoLib 动画代理。
 *
 * <p>动画决策全部交给 {@link PlayerAnimationController}（照搬参考2），
 * 这个类只负责「把玩家引用递给控制器」和「注册控制器」两件事 ——
 * 以前内嵌的那份 {@code resolveState()} 运动判定已经删除，避免两套状态机互相打架。
 */
public class GenshinReplacedPlayer implements GeoReplacedEntity, IPlayerAnimatableProxy {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private Player playerEntity;

    /** 当前角色 ID，仅用于日志与调试（渲染数据由模型自己拿）。 */
    @Nullable
    private String characterId;

    private boolean registeredControllers;
    private boolean loggedNullPlayer;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        registeredControllers = true;
        controllers.add(PlayerAnimationController.create(this));
        LOGGER.debug("[GenshinReplacedPlayer] AnimationController 已注册");
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
        if (player == null && !loggedNullPlayer) {
            loggedNullPlayer = true;
            LOGGER.debug("[GenshinReplacedPlayer] playerEntity 置空（退出原神模式或玩家卸载）");
        }
    }

    @Nullable
    public String getCharacterId() {
        return characterId;
    }

    public void setCharacterId(@Nullable String characterId) {
        this.characterId = characterId;
    }

    public boolean hasRegisteredControllers() {
        return registeredControllers;
    }
}
