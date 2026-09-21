package com.linweiyun.genshin.core.character.catalyst.vodyanitsa;

import com.linweiyun.genshin.config.character.VodyanitsaAttributeConfig;
import com.linweiyun.genshin.core.character.catalyst.CatalystCharacter;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 沃雅妮莎（Vodyanitsa）—— 水系五星法器，突破属性<b>生命值</b>。
 *
 * <p>数值表见 {@link VodyanitsaAttributeConfig}；技能见 {@link VodyanitsaTalent}。
 */
public class Vodyanitsa extends CatalystCharacter {

    public static final String ID = "vodyanitsa";
    public static final int UID = 145002;

    /** 战技冷却 16 秒。 */
    public static final int SKILL_COOLDOWN_TICKS = 16 * 20;
    /** 大招冷却 15 秒 / 能量 60。 */
    public static final int BURST_COOLDOWN_TICKS = 15 * 20;
    public static final float BURST_ENERGY_COST = 60f;

    public Vodyanitsa() {
        super(UID, 5, Component.translatable("character.name.vodyanitsa"),
                "minegenshin:hydro", CharacterAscendAttribute.HP,
                SKILL_COOLDOWN_TICKS, BURST_COOLDOWN_TICKS, BURST_ENERGY_COST, ID,
                Map.of(
                        ModAttributes.MAX_HP.getId(), VodyanitsaAttributeConfig::getAllHp,
                        ModAttributes.ATK.getId(), VodyanitsaAttributeConfig::getAllAtk,
                        ModAttributes.DEF.getId(), VodyanitsaAttributeConfig::getAllDef
                ));
        this.talent = new VodyanitsaTalent();
    }

    @Override
    public com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData getActionData() {
        return VodyanitsaResources.ACTION_DATA;
    }

    @Override
    public Map<Identifier, Supplier<List<? extends Integer>>> getStatGrowthMap() {
        return Map.of(
                ModAttributes.MAX_HP.getId(), VodyanitsaAttributeConfig::getAllHp,
                ModAttributes.ATK.getId(), VodyanitsaAttributeConfig::getAllAtk,
                ModAttributes.DEF.getId(), VodyanitsaAttributeConfig::getAllDef
        );
    }

    // ==================== 「遥久之歌」====================

    /** 持续 16 秒。 */
    public static final int SONG_DURATION_TICKS = 16 * 20;

    /** 2 命额外延长 9 秒。 */
    public static final int SONG_C2_EXTRA_TICKS = 9 * 20;
    /** 每隔 3 秒打一次（索敌 13 格、默认索敌模式、优先玩家锁定的目标）。 */
    public static final int SONG_ATTACK_INTERVAL_TICKS = 3 * 20;
    /** 每 1.5 秒给当前场上角色回一次血。 */
    public static final int SONG_HEAL_INTERVAL_TICKS = 30;

    /** 剩余刻数（0 = 没在唱）。 */
    @com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced
    @com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted(key = "song_ticks")
    protected int songTicks;

    /** 距离下一次定时攻击/回血还有多少刻。 */
    protected int songAttackTimer;
    protected int songHealTimer;

    public void startSong() {
        // 2 命「穿彻风雪的余响」：遥久之歌持续时间 +9 秒
        this.songTicks = SONG_DURATION_TICKS
                + (hasConstellation(2) ? SONG_C2_EXTRA_TICKS : 0);
        this.songAttackTimer = SONG_ATTACK_INTERVAL_TICKS;
        this.songHealTimer = SONG_HEAL_INTERVAL_TICKS;
        syncRealtimeState();
    }

    public boolean isSongActive() {
        return songTicks > 0;
    }

    /** 遥久之歌还剩多少刻（6 命用这个时长发给队友）。 */
    public int songTicksRemaining() {
        return Math.max(0, songTicks);
    }

    @Override
    public void tick(Player player) {
        super.tick(player);
        if (player.level().isClientSide()) return;

        // 4 命的层数即使在遥久之歌结束后也要继续独立倒计时
        tickC4Hp();

        if (songTicks <= 0) return;

        songTicks--;
        if (songTicks <= 0) {
            syncRealtimeState();
            return;
        }

        // 定时攻击 / 回血 / 减抗
        if (--songAttackTimer <= 0) {
            songAttackTimer = SONG_ATTACK_INTERVAL_TICKS;
            VodyanitsaTalent.songAttack(player, this);
        }
        if (--songHealTimer <= 0) {
            songHealTimer = SONG_HEAL_INTERVAL_TICKS;
            VodyanitsaTalent.songHeal(player, this);
        }

        // 水/冰抗降低到期 → 摘掉修饰符（可能有好几个目标）
        clearExpiredSongShred(player);

        // 流荡风旋的风抗降低到期 → 一并清理
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            VodyanitsaTalent.clearExpiredWindShred(serverLevel);
        }
    }

    // ==================== 4 命：生命值上限 +20%（最多 3 层、每层独立计时）====================

    public static final int C4_HP_STACK_TICKS = 6 * 20;
    private static final String C4_HP_SOURCE = "vodyanitsa_c4_hp";
    /** 每层自己的剩余刻数（0 = 这层不存在）——和整肃 decreeTicks 同形态。 */
    protected final int[] c4HpTicks = new int[3];

    /** 加一层：有空位用空位，满了挤掉最早那层；然后重算属性。 */
    public void addC4HpStack() {
        int slot = 0;
        int lowest = Integer.MAX_VALUE;
        for (int i = 0; i < c4HpTicks.length; i++) {
            if (c4HpTicks[i] <= 0) {
                slot = i;
                break;
            }
            if (c4HpTicks[i] < lowest) {
                lowest = c4HpTicks[i];
                slot = i;
            }
        }
        c4HpTicks[slot] = C4_HP_STACK_TICKS;
        refreshC4HpBonus();
    }

    /** 每层独立倒计时；有层掉了就重算。 */
    private void tickC4Hp() {
        boolean changed = false;
        for (int i = 0; i < c4HpTicks.length; i++) {
            if (c4HpTicks[i] > 0 && --c4HpTicks[i] == 0) {
                changed = true;
            }
        }
        if (changed) {
            refreshC4HpBonus();
        }
    }

    private void refreshC4HpBonus() {
        int stacks = 0;
        for (int ticks : c4HpTicks) {
            if (ticks > 0) stacks++;
        }
        getData().removeAttributeModifier(
                com.linweiyun.genshin.core.system.registry.register.ModAttributes.MAX_HP.value(), C4_HP_SOURCE);
        if (stacks > 0) {
            getData().addAttributeTempPercentModifier(
                    com.linweiyun.genshin.core.system.registry.register.ModAttributes.MAX_HP.value(),
                    C4_HP_SOURCE, 0.20f * stacks);
        }
    }

    // ==================== 水/冰抗降低的记账 ====================
    //
    // ⚠️ 必须记账「每一个」被减抗的敌人：歌每 3 秒挑一个目标，
    //    16 秒里可能打好几个不同敌人，只记最后一个的话，先被打的那些减抗会摘不掉。

    /** 实体 UUID → 减抗到期时刻。 */
    protected final java.util.Map<java.util.UUID, Long> songShredUntil = new java.util.HashMap<>();

    /** 记下这一次减抗的实体与到期时刻。 */
    public void markSongShred(net.minecraft.world.entity.LivingEntity target, long expireTick) {
        songShredUntil.put(target.getUUID(), expireTick);
    }

    /** 摘掉所有已到期的减抗修饰符。 */
    public void clearExpiredSongShred(Player player) {
        if (songShredUntil.isEmpty()) return;
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        long now = player.level().getGameTime();
        var iterator = songShredUntil.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now < entry.getValue()) continue;
            iterator.remove();

            net.minecraft.world.entity.Entity entity = serverLevel.getEntity(entry.getKey());
            if (!(entity instanceof net.minecraft.world.entity.LivingEntity living)) continue;
            var stats = living.getData(
                    com.linweiyun.genshin.core.attachment.AttachmentRegistration.ENTITY_STATS);
            stats.attributes().removeModifier(
                    com.linweiyun.genshin.core.system.registry.register.ModAttributes.HYDRO_RES.value(),
                    VodyanitsaTalent.SONG_RES_SOURCE);
            stats.attributes().removeModifier(
                    com.linweiyun.genshin.core.system.registry.register.ModAttributes.CYRO_RES.value(),
                    VodyanitsaTalent.SONG_RES_SOURCE);
        }
    }
}
