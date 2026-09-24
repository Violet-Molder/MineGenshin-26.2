package com.linweiyun.genshin.client.combat.action;

import com.linweiyun.genshin.client.combat.state.ActionStateMachine;
import com.linweiyun.genshin.core.system.combat.action.data.ActionStep;
import com.linweiyun.genshin.core.system.combat.action.data.SoundCue;
import com.linweiyun.genshin.core.system.combat.action.data.SoundRef;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 招式音效排期 —— 把 {@link ActionStep} 里的音效编排翻译成一串「第 N 刻播某个音效」的客户端任务。
 *
 * <p>从 {@link ResourceDrivenActionHandler} 抽出：这一组方法只依赖 {@link ActionStep} 的资源数据与
 * {@link ActionStateMachine} 的排期能力，和招式逻辑、索敌、动画都无耦合。
 */
public final class ActionSoundScheduler {

    private ActionSoundScheduler() {
    }

    /**
     * 排期播放这段动作的音效。
     *
     * <p>两种来源叠加：
     * <ol>
     *   <li>旧的 {@code ActionStep.sounds} —— 每条当成「这一格固定播」；</li>
     *   <li>新的 {@code ActionStep.soundCues} —— 序列 + 随机可组合
     *       （{@link SoundCue.PickMode#PLAY_ALL} 全播 /
     *        {@link SoundCue.PickMode#PICK_ONE} 按权重抽一条，
     *        候选里放 {@link SoundRef#silent()} 就是「这一格不出声」）。</li>
     * </ol>
     *
     * <p><b>只有出手的本人听得到</b>：这些音效走 {@code playLocalSound}（本地播放、不发包），
     * 其他玩家那边只有 {@code AnimationStateSync} 按状态名补的那一条
     * （{@code CharacterAnimations.soundForState}）。想让别人也听到整套编排，
     * 得把解析结果发到服务端再广播 —— 目前没做，见附录 A.5。
     *
     * <p>音效名不带命名空间时按 {@code minegenshin:} 补全；音效文件不存在时
     * {@link ActionStateMachine#playLocalSound} 会静默跳过，不会报错。
     */
    public static void scheduleActionSounds(Player player, @Nullable ActionStep step) {
        if (step == null) {
            return;
        }

        // 1. 旧写法：每条固定播一格
        if (step.sounds != null) {
            for (SoundRef sound : step.sounds) {
                if (sound == null || sound.isSilent()) {
                    continue;
                }
                queueSound(player, sound.delay, sound.name, sound.volume, sound.pitch);
            }
        }

        // 2. 新写法：编排表
        if (step.soundCues == null || step.soundCues.isEmpty()) {
            return;
        }

        java.util.Random random = soundRandom(player);

        for (SoundCue cue : step.soundCues) {
            if (cue == null || cue.isEmpty()) {
                continue;
            }

            if (cue.mode == SoundCue.PickMode.PLAY_ALL) {
                for (SoundRef variant : cue.variants) {
                    if (variant != null && !variant.isSilent()) {
                        queueSound(player, cue.delay, variant.name, variant.volume, variant.pitch);
                    }
                }
                continue;
            }

            // PICK_ONE：按权重抽一条；抽到静音就这一格什么都不播
            SoundRef picked = weightedPick(cue.variants, random);
            if (picked != null && !picked.isSilent()) {
                queueSound(player, cue.delay, picked.name, picked.volume, picked.pitch);
            }
        }
    }

    private static void queueSound(Player player, int delay, String name, float volume, float pitch) {
        if (name == null || name.isEmpty()) {
            return;
        }
        String soundId = name.indexOf(':') >= 0 ? name : "minegenshin:" + name;
        ActionStateMachine.queueClientWork(Math.max(0, delay),
                () -> ActionStateMachine.playLocalSound(player, soundId, volume, pitch));
    }

    /** 按权重抽一条。 */
    @Nullable
    private static SoundRef weightedPick(List<SoundRef> variants,
                                                             java.util.Random random) {
        int total = 0;
        for (SoundRef variant : variants) {
            if (variant != null) {
                total += variant.weight;
            }
        }
        if (total <= 0) {
            return null;
        }

        int roll = random.nextInt(total);
        for (SoundRef variant : variants) {
            if (variant == null) {
                continue;
            }
            roll -= variant.weight;
            if (roll < 0) {
                return variant;
            }
        }
        return variants.getLast();
    }

    /**
     * 确定性随机源：种子 = 玩家 UUID + 本动作的序号。
     *
     * <p>「确定性」在这里的含义是<b>可复现</b>，不是「跨客户端一致」——
     * 音效只在出手者本人的客户端播（见 {@link #scheduleActionSounds}），
     * 所以这个种子的实际作用有两个：
     * <ul>
     *   <li>同一段连招里每一刀抽到不同变体（序号在变，不会连着四次都「哈！」）；</li>
     *   <li>出问题时能按「谁的哪一刀」精确复现，方便排查。</li>
     * </ul>
     */
    private static java.util.Random soundRandom(Player player) {
        long seed = player.getUUID().getLeastSignificantBits()
                ^ (long) ActionStateMachine.actionSequence() * 0x9E3779B97F4A7C15L;
        return new java.util.Random(seed);
    }
}
