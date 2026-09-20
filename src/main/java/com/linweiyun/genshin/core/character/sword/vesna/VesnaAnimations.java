package com.linweiyun.genshin.core.character.sword.vesna;

import com.linweiyun.genshin.core.system.combat.animation.config.CharacterAnimations;
import com.linweiyun.genshin.core.system.combat.animation.config.FirstPersonAnims;
import com.linweiyun.genshin.core.system.combat.animation.config.LocomotionAnims;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Vesna 的动画目录 —— 纯数据类：常态动画名、特殊动画名单、过渡刻数、状态→音效映射。
 *
 * <p>动画名与 {@code assets/minegenshin/character/vesna/vesna.animation.json}
 * 里的 key 一一对应；控制器逻辑在 {@code PlayerAnimationController}，
 * 动作编排在 {@code ResourceDrivenActionHandler}（时序来自 {@link VesnaResources#ACTION_DATA}）。
 *
 * <p>对应参考2 的 {@code MiyabiAnimations}。
 */
public final class VesnaAnimations implements CharacterAnimations {

    public static final VesnaAnimations INSTANCE = new VesnaAnimations();

    private static final String SOUND_NS = "minegenshin:";

    /** 常态动画名。 */
    public static final LocomotionAnims LOCOMOTION = LocomotionAnims.of(
            "idle", "walk", "run", "walk_back",
            "crouch", "crouch_walk", "sleep", "climb",
            "water", "water_walk", "water_walk_back", "swim",
            "jump", "jump_down");

    /**
     * 「动作动画」名单 —— <b>只用来决定过渡刻数</b>（名单内一律 0 刻硬切），不是存在性白名单。
     *
     * <p>动画是否存在由 {@code AnimationAvailability} 直接查 GeckoLib 的烘培缓存判断，
     * 所以这里漏写或多写都不会导致模型变成原始姿态，最多是过渡长短不理想。
     */
    public static final Set<String> SPECIAL_ANIMS = Set.of(
            // 普攻 5 段 + 第 6 段与它的收尾
            "attack_1", "attack_2", "attack_3", "attack_4", "attack_5",
            "air_attack_1", "air_attack_2", "air_attack_long", "air_attack_end",
            // 蓄力重击
            "heavy_1", "heavy_2", "heavy_3",
            // 战技
            "skill_energy", "skill_energy_continue", "skill_no_energy",
            // 闪避 4 向
            "dodge_front", "dodge_back", "dodge_left", "dodge_right",
            // 大招（json 里的名字是 final）
            "final");

    /** 常态动画互相切换的过渡刻数。 */
    public static final int EXIT_TRANSITION_TICKS = 5;

    /**
     * 状态 → 音效。切到这个状态时会自动播放，并跟着动画同步包一起广播给其他玩家。
     *
     * <p><b>Vesna 这张表留空是故意的</b>：她的普攻/战技/大招音效写在动作数据里
     * （{@code VesnaResources} 的 {@code SoundRef}，卡着每一段的 hit 时序播），
     * 两处都写会变成同一刀响两下。
     *
     * <p>要「一进状态就响」的音效（比如起手喊话）时才往这里加：
     * <pre>
     * Map.entry("attack_1", "minegenshin:vesna_attack_1")
     * </pre>
     * 声音文件放 {@code character/vesna/sounds/attack_1.ogg}，事件定义写在
     * {@code character/vesna/sounds.json}（见 {@code CharacterSounds}）。
     */
    public static final Map<String, String> STATE_SOUNDS = Map.ofEntries();

    private VesnaAnimations() {
    }

    @Override
    public LocomotionAnims locomotion() {
        return LOCOMOTION;
    }

    @Override
    public Set<String> specialAnims() {
        return SPECIAL_ANIMS;
    }

    @Override
    public int exitTransitionTicks() {
        return EXIT_TRANSITION_TICKS;
    }

    /**
     * 第一人称：<b>开</b>。
     *
     * <p>动画文件里写了 {@code fp_attack_1} … 就用它；没写就自动复用第三人称动画
     * （复用模式靠 {@link FirstPersonAnims.FirstPersonCamera} 把机位摆正）。
     * 现阶段文件里还没有 fp_ 动画，所以走的是复用模式。
     */
    @Override
    public FirstPersonAnims firstPerson() {
        return FirstPersonAnims.on();
    }

    @Nullable
    @Override
    public String soundForState(String stateName) {
        return STATE_SOUNDS.get(stateName);
    }
}
