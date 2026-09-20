package com.linweiyun.genshin.client.render.character;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoObjectRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.world.entity.player.Player;

/**
 * 角色模型渲染器。
 *
 * <h2>为什么要把父类的原点平移「改成 0」</h2>
 * 基类 {@link GeoObjectRenderer}（给「摆件」用的渲染器）在 {@code adjustRenderPose} 里
 * 有一句 {@code translate(0.5, 0.51, 0.5)} —— 那是给<b>以方块角为原点</b>导出的
 * 摆件模型准备的补偿。
 *
 * <p>而我们这套角色模型是<b>以原点为中心</b>导出的：所有 cube 的 origin 都对称于 x=0
 * （头 −3.5~3.5、躯干 −3~3、腿 ±1.9 单位），模型自带
 * {@code visible_bounds_offset = [0, 1.75, 0]}。人也确实站在方块中心 ——
 * 原版实体渲染交给我们的 pose 就只有「实体相对相机的位置」（见
 * {@code EntityRenderDispatcher.submit}），没有任何半格偏移。
 *
 * <p>所以那句 +0.5 会把模型整体推到斜后方半格（约 0.71 格），和判定箱、影子对不上。
 * GeckoLib 自己的实体渲染器（{@code GeoEntityRenderer} / {@code GeoReplacedEntityRenderer}）
 * 就<b>没有</b>这句平移，只有摆件渲染器有 —— 这也说明它是「摆件约定」，不是实体的。
 *
 * <p>覆盖成空实现后：模型正好落在实体位置上，第一人称那边也不需要再做
 * 「反向补偿半格」的换算。
 */
public class CharacterRenderer extends GeoObjectRenderer<GenshinReplacedPlayer, Player, GeoRenderState> {

    public CharacterRenderer(GeoModel<GenshinReplacedPlayer> model) {
        super(model);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoRenderState> renderPassInfo) {
        // 故意什么都不做：模型以原点为中心，不需要摆件渲染器那半格补偿。
        // （要调模型相对实体的位置就改这里，别去动 adjustRenderPose 的父类默认值）
    }
}
