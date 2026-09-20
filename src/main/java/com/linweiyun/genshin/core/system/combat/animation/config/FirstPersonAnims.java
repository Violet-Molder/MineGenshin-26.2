package com.linweiyun.genshin.core.system.combat.animation.config;

import org.jetbrains.annotations.Nullable;

/**
 * 第一人称动画配置 —— <b>每个角色一个开关</b>。
 *
 * <h2>两套做法，靠这一个配置切换</h2>
 * <table border="1">
 *   <caption>两种模式</caption>
 *   <tr><th>模式</th><th>怎么配</th><th>效果</th></tr>
 *   <tr>
 *     <td><b>A · 写一套第一人称动画</b></td>
 *     <td>{@code enabled = true}，并在动画文件里真的写了 {@code fp_*} 动画</td>
 *     <td>第一人称播 {@code fp_<状态名>}，找不到才回落普通动画</td>
 *   </tr>
 *   <tr>
 *     <td><b>B · 复用现有动画 + 转角度</b></td>
 *     <td>{@code enabled = true}，但没有写 {@code fp_*} 动画</td>
 *     <td>照播普通动画，由 {@link #camera()} 里的机位/角度把它摆成第一人称视角</td>
 *   </tr>
 *   <tr>
 *     <td><b>关</b></td>
 *     <td>{@code enabled = false}（默认）</td>
 *     <td>第一人称不做任何特殊处理</td>
 *   </tr>
 * </table>
 *
 * <p>所以「两套」不需要两套代码路径 —— <b>写了 fp_ 动画就是 A，没写就是 B</b>，
 * 运行时按动画名是否存在自动选。
 *
 * <h2>第一人称真正要的是「武器挥动」</h2>
 * 第三人称要照顾全身，第一人称镜头只看得到手臂和武器，所以第一人称动画的重点不是
 * 走跑跳，而是<b>攻击/技能那几段的挥动轨迹</b>。日常移动（待机/走/跑）通常直接复用，
 * 最多调一下机位。这也是为什么 {@link #prefix()} 只对「动作动画」生效、
 * 常态动画默认不查 {@code fp_}。
 *
 * @param enabled 这个角色要不要走第一人称动画
 * @param prefix  第一人称动画名的前缀，默认 {@code "fp_"}（即 {@code attack_1} → {@code fp_attack_1}）
 * @param camera  复用模式下的机位微调
 */
public record FirstPersonAnims(boolean enabled, String prefix, FirstPersonCamera camera) {

    /** 关：这个角色第一人称不做特殊处理。 */
    public static final FirstPersonAnims DISABLED =
            new FirstPersonAnims(false, "fp_", FirstPersonCamera.DEFAULT);

    /** 开：默认前缀 {@code fp_}，机位用默认值。 */
    public static FirstPersonAnims on() {
        return new FirstPersonAnims(true, "fp_", FirstPersonCamera.DEFAULT);
    }

    /** 开：自定义前缀（比如 {@code "fp1_"}）。 */
    public static FirstPersonAnims on(String prefix) {
        return new FirstPersonAnims(true, prefix == null || prefix.isEmpty() ? "fp_" : prefix,
                FirstPersonCamera.DEFAULT);
    }

    /** 开：自定义机位（复用模式调角度用这个）。 */
    public static FirstPersonAnims on(FirstPersonCamera camera) {
        return new FirstPersonAnims(true, "fp_", camera);
    }

    /** 这个状态有没有对应的第一人称动画名可试。常态动画默认不试（第一人称看的是武器挥动）。 */
    @Nullable
    public String resolve(String stateName, boolean isActionState) {
        if (!enabled || stateName == null || stateName.isEmpty()) {
            return null;
        }
        if (!isActionState) {
            return null;
        }
        return prefix + stateName;
    }

    /**
     * 第一人称机位 —— <b>相机相对玩家眼睛的偏移</b>。
     *
     * <p>渲染时模型不是被搬到相机上，而是相机从眼睛位置挪开，
     * 于是模型（连同手里的武器）就相对地进了画面。所有偏移都是「格」，旋转是「度」：
     *
     * <pre>
     * 模型脚底（相机空间）= ( -offsetX, -(眼高 + offsetY), +offsetZ )
     * </pre>
     *
     * 所以 {@code offsetZ} 为正 = 相机沿视线往<b>前</b>移 = 模型相对往后退 = 看到更多手臂。
     *
     * @param offsetX 相机相对眼睛的左右偏移（正 = 往右，模型看起来往左）
     * @param offsetY 相机相对眼睛的高低偏移（正 = 抬高，模型看起来往下；下蹲/游泳时不用管，眼高是实时取的）
     * @param offsetZ 相机沿视线方向的前移量（正 = 前移，让手臂和武器进画面）
     * @param pitch   模型相对相机的俯仰微调（正 = 和玩家低头同向）
     * @param yaw     水平微调
     * @param roll    侧倾
     * @param scale   整体缩放（第一人称离得近，通常不用改）
     * @param fovPadding 预留字段，<b>当前未使用</b>：第一人称是直接在相机空间画的，
     *                   不像「另开一个相机」那样需要为近裁剪面留视野余量
     */
    public record FirstPersonCamera(float offsetX, float offsetY, float offsetZ,
                                    float pitch, float yaw, float roll,
                                    float scale, float fovPadding) {

        /**
         * 默认机位：贴着眼镜位置、沿视线前移 0.25 格。
         *
         * <p>前移是必要的：相机就落在头壳里，往前挪一点才能把「脸」甩到身后，
         * 看到自己的手臂和武器（头部模型本身会在第一人称被藏掉，见
         * {@code FirstPersonCharacterRenderer}，但机位还是要让开一点，
         * 免得贴着手臂看）。
         */
        public static final FirstPersonCamera DEFAULT =
                new FirstPersonCamera(0f, 0f, 0.25f, 0f, 0f, 0f, 1f, 0f);

        public FirstPersonCamera withOffset(float x, float y, float z) {
            return new FirstPersonCamera(x, y, z, pitch, yaw, roll, scale, fovPadding);
        }

        public FirstPersonCamera withRotation(float newPitch, float newYaw, float newRoll) {
            return new FirstPersonCamera(offsetX, offsetY, offsetZ, newPitch, newYaw, newRoll,
                    scale, fovPadding);
        }

        public FirstPersonCamera withScale(float value) {
            return new FirstPersonCamera(offsetX, offsetY, offsetZ, pitch, yaw, roll, value, fovPadding);
        }
    }
}
