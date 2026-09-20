package com.linweiyun.genshin.core.system.combat.action.data;

import net.minecraft.world.item.ItemStack;

/**
 * 一个骨骼挂点 —— 「把某个模型画到角色模型的某根骨骼上」。
 *
 * <p>典型用法：模型里剑鞘和剑身是分开的骨骼，只把剑身（如 {@code blade_right}）换成
 * 实际装备的武器模型。{@link #boneName} 就是那根骨骼的名字。
 *
 * <h2>工作方式</h2>
 * <ol>
 *   <li>渲染这个角色时，按 {@link #source} 取一个 {@link ItemStack}；</li>
 *   <li>取到空 → 什么都不做，骨骼照原样渲染（模型自带的剑身在）；</li>
 *   <li>取到东西 → 隐藏这根骨骼自身的几何体，把物品模型画在它的位姿上。</li>
 * </ol>
 *
 * <h2>为什么要偏移 / 缩放 / 旋转</h2>
 * 物品模型是照「物品展示」的尺寸做的，直接放到手部骨骼上通常大小和朝向都不对，
 * 所以留了三个可调量。骨骼的名字与层级由模型决定，代码不猜。
 *
 * @param boneName  挂点骨骼名（模型 geo.json 里的名字）
 * @param source    内容来源，默认 {@link BoneMountSource#WEAPON_SLOT}
 * @param scale     统一缩放
 * @param offsetX   骨骼局部坐标 X 偏移（像素，1 格 = 16）
 * @param offsetY   骨骼局部坐标 Y 偏移
 * @param offsetZ   骨骼局部坐标 Z 偏移
 * @param rotationX 绕 X 轴旋转（度）
 * @param rotationY 绕 Y 轴旋转（度）
 * @param rotationZ 绕 Z 轴旋转（度）
 */
public record CharacterBoneMount(
        String boneName,
        BoneMountSource source,
        float scale,
        float offsetX,
        float offsetY,
        float offsetZ,
        float rotationX,
        float rotationY,
        float rotationZ) {

    /** 只指定骨骼，内容取武器槽，其余用默认值。 */
    public static CharacterBoneMount of(String boneName) {
        return new CharacterBoneMount(boneName, BoneMountSource.WEAPON_SLOT,
                1.0f, 0, 0, 0, 0, 0, 0);
    }

    /** 指定骨骼与内容来源。 */
    public static CharacterBoneMount of(String boneName, BoneMountSource source) {
        return new CharacterBoneMount(boneName, source, 1.0f, 0, 0, 0, 0, 0, 0);
    }

    public CharacterBoneMount withSource(BoneMountSource value) {
        return new CharacterBoneMount(boneName, value, scale, offsetX, offsetY, offsetZ,
                rotationX, rotationY, rotationZ);
    }

    public CharacterBoneMount withScale(float value) {
        return new CharacterBoneMount(boneName, source, value, offsetX, offsetY, offsetZ,
                rotationX, rotationY, rotationZ);
    }

    /** 偏移按「像素」给（模型坐标系），渲染时转成格。 */
    public CharacterBoneMount withOffset(float x, float y, float z) {
        return new CharacterBoneMount(boneName, source, scale, x, y, z,
                rotationX, rotationY, rotationZ);
    }

    public CharacterBoneMount withRotation(float x, float y, float z) {
        return new CharacterBoneMount(boneName, source, scale, offsetX, offsetY, offsetZ, x, y, z);
    }

    public boolean isValid() {
        return boneName != null && !boneName.isEmpty() && source != null;
    }
}
