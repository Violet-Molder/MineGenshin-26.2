package com.linweiyun.genshin.core.character.catalyst.vodyanitsa;

import com.linweiyun.genshin.content.skill_node.AreaEntityCollector;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.character.talent.TalentBase;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.enums.AttackType;
import com.linweiyun.genshin.enums.AttachmentType;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.List;

/**
 * 沃雅妮莎的天赋。
 *
 * <h2>倍率表（Lv1~Lv15）</h2>
 * 普通攻击 / 重击吃<b>攻击力</b>；战技与大招吃<b>生命值上限</b>（文案写的就是「%生命值上限」）。
 *
 * 「遥久之歌」的每 3 秒索敌攻击、1.5 秒回血、水/冰抗降低，
 * 以及突破天赋 1 的「流荡风旋」转化/创造与降风抗。
 */
public class VodyanitsaTalent extends TalentBase {
    public static final Logger LOGGER = LogUtils.getLogger();
    // ==================== 普通攻击「水色咏叹」四段 ====================

    public static final float[] NA_1 = {
            0.4338f, 0.4664f, 0.4989f, 0.5423f, 0.5748f, 0.6074f, 0.6508f, 0.6942f, 0.7375f, 0.7809f,
            0.8243f, 0.8677f, 0.9219f, 0.9762f, 1.0304f
    };
    public static final float[] NA_2 = {
            0.4023f, 0.4325f, 0.4627f, 0.5029f, 0.5331f, 0.5632f, 0.6035f, 0.6437f, 0.6839f, 0.7242f,
            0.7644f, 0.8046f, 0.8549f, 0.9052f, 0.9555f
    };
    public static final float[] NA_3 = {
            0.493f, 0.53f, 0.567f, 0.6163f, 0.6532f, 0.6902f, 0.7395f, 0.7888f, 0.8381f, 0.8874f,
            0.9367f, 0.986f, 1.0477f, 1.1093f, 1.1709f
    };
    public static final float[] NA_4 = {
            0.6556f, 0.7048f, 0.754f, 0.8195f, 0.8687f, 0.9179f, 0.9834f, 1.049f, 1.1146f, 1.1801f,
            1.2457f, 1.3113f, 1.3932f, 1.4752f, 1.5571f
    };

    /** 重击「向前掷出水球」。 */
    public static final float[] CHARGE = {
            1.2376f, 1.3304f, 1.4232f, 1.547f, 1.6398f, 1.7326f, 1.8564f, 1.9802f, 2.1039f, 2.2277f,
            2.3514f, 2.4752f, 2.6299f, 2.7846f, 2.9393f
    };

    // ==================== 元素战技「宣叙·晨声纷流」====================

    /** 技能伤害 / 唤春角笛伤害（同一个表，都是生命值上限百分比）。 */
    public static final float[] SKILL_DAMAGE = {
            0.0327f, 0.0352f, 0.0376f, 0.0409f, 0.0434f, 0.0458f, 0.0491f, 0.0524f, 0.0556f, 0.0589f,
            0.0622f, 0.0654f, 0.0695f, 0.0736f, 0.0777f
    };

    // ==================== 元素爆发「终奏·伴尔沉沦」====================

    public static final float[] BURST_DAMAGE = {
            0.4568f, 0.491f, 0.5253f, 0.571f, 0.6052f, 0.6395f, 0.6852f, 0.7308f, 0.7765f, 0.8222f,
            0.8679f, 0.9135f, 0.9706f, 1.0277f, 1.0848f
    };

    /** 处于「遥久之歌」时大招的加伤（生命值上限百分比）。 */
    public static final float[] BURST_SONG_BONUS = {
            0.48f, 0.516f, 0.552f, 0.60f, 0.636f, 0.672f, 0.72f, 0.768f, 0.816f, 0.864f,
            0.912f, 0.96f, 1.02f, 1.08f, 1.14f
    };

    @Override
    public int getMaxCombo() {
        return 4;
    }

    /** 表里只有 15 档，等级超过就按最后一档取。 */
    private static float at(float[] table, int level) {
        if (table == null || table.length == 0) return 0f;
        int index = Math.max(0, Math.min(table.length - 1, level - 1));
        return table[index];
    }

    // ==================== 普通攻击 ====================

    /** 普攻 / 重击的索敌与生效距离：<b>13 格</b>（她是法器，不贴脸）。 */
    public static final int ATTACK_RANGE = 13;

    /**
     * 普攻 / 重击的索敌：优先玩家当前锁定的目标，没锁就按 {@value #ATTACK_RANGE} 格半径找最近的。
     *
     * <p>法器角色是**远程**：索敌到目标后直接把伤害结算在目标身上
     * （{@code hurtServer}），不需要贴到脸上 —— 原来那段「沿视线 2.5 格」的近身判定框
     * 在 13 格外什么都打不到，看起来就像「普攻是近战」。
     */
    private static LivingEntity resolveAttackTarget(Player player) {
        return com.linweiyun.genshin.core.system.combat.targeting.SummonTargeting.defaultMode()
                .resolve(player, null, ATTACK_RANGE,
                        () -> new com.linweiyun.genshin.content.skill_node.TargetSeeker(
                                player, ATTACK_RANGE,
                                com.linweiyun.genshin.content.skill_node.TargetSeeker.TargetingType.RADIUS)
                                .execute());
    }

    @Override
    public void attack(Player player, PGCharacter character, int comboStage) {
        Level level = player.level();
        if (level.isClientSide()) return;

        int naLevel = Math.max(1, character.getData().getNormalAttackLevel());
        float multiplier = switch (comboStage) {
            case 2 -> at(NA_2, naLevel);
            case 3 -> at(NA_3, naLevel);
            case 4 -> at(NA_4, naLevel);
            default -> at(NA_1, naLevel);
        };

        // ① 锁到目标（13 格内）→ 直接把伤害结算在目标身上
        LivingEntity locked = resolveAttackTarget(player);
        if (locked != null) {
            hurt(player, character, locked, multiplier,
                    AttackType.NORMAL_ATTACK, AttachmentType.WEAK.getInitialAmount());
            return;
        }

        // ② 没锁到目标（例如贴脸空挥、目标不在视线里）→ 保留原来的近身判定框
        Vec3 from = player.position();
        Vec3 to = from.add(player.getLookAngle().scale(2.5f));
        List<LivingEntity> targets = new AreaEntityCollector(level, from, to, 1.0f).execute();

        for (LivingEntity target : targets) {
            if (target == player) continue;
            hurt(player, character, target, multiplier,
                    AttackType.NORMAL_ATTACK, AttachmentType.WEAK.getInitialAmount());
        }
    }

    @Override
    public void chargeAttack(Player player, PGCharacter character) {
        Level level = player.level();
        if (level.isClientSide()) return;

        int naLevel = Math.max(1, character.getData().getNormalAttackLevel());
        float multiplier = at(CHARGE, naLevel);

        // 同普攻：先按 13 格索敌直接结算，锁不到才退化成原来的水球判定框
        LivingEntity locked = resolveAttackTarget(player);
        if (locked != null) {
            hurt(player, character, locked, multiplier,
                    AttackType.CHARGED_ATTACK, AttachmentType.WEAK.getInitialAmount());
            return;
        }

        // 向前方掷出水球：打视线前方 4 格、半径 1.5 格的范围
        Vec3 center = player.position().add(player.getLookAngle().scale(4.0));
        List<LivingEntity> targets = new AreaEntityCollector(level,
                center.add(-1.5, -1.5, -1.5), center.add(1.5, 1.5, 1.5), 1.5f).execute();

        for (LivingEntity target : targets) {
            if (target == player) continue;
            hurt(player, character, target, multiplier,
                    AttackType.CHARGED_ATTACK, AttachmentType.WEAK.getInitialAmount());
        }
    }

    // ==================== 元素战技 / 元素爆发 ====================

    @Override
    public void elementalSkill(Player player, PGCharacter character, int skillTime) {
        Level level = player.level();
        if (level.isClientSide()) return;
        if (!(character instanceof Vodyanitsa vodyanitsa)) return;

        int level15 = character.getData().getElementalSkillLevel();
        float multiplier = at(SKILL_DAMAGE, level15);

        // ① 13 格内索敌到的目标 → 直接结算（法器是远程，不用贴脸）
        LivingEntity locked = resolveAttackTarget(player);
        if (locked != null) {
            hurtHpScaling(player, character, locked, multiplier,
                    AttackType.ELEMENTAL_SKILL, AttachmentType.STRONG.getInitialAmount());
        }

        // ② 自身周围一圈的水元素范围伤害（贴脸时的溅射，和上面可能是同一个目标，会各打一次）
        boolean hornHit = locked != null;
        Vec3 center = player.position();
        float radius = 4.0f;
        for (LivingEntity target : areaTargets(level, center, radius)) {
            hornHit = true;
            if (target == locked) continue;
            hurtHpScaling(player, character, target, multiplier,
                    AttackType.ELEMENTAL_SKILL, AttachmentType.STRONG.getInitialAmount());
        }

        // 获得「遥久之歌」（16 秒）—— 后续的定时攻击/回血/减抗由 Vodyanitsa.tick 负责
        vodyanitsa.startSong();

        // 2 命「穿彻风雪的余响」：唤春角笛**命中敌人**这一刻就给「黑与白的双音」
        //（遥久之歌每次跳动也会再给一次并刷新，见 songAttack）
        if (hornHit) {
            grantDuetBuff(player, vodyanitsa);
        }

        // 突破天赋 2（突破 ≥ 4）：25 层「领唱」给当前场上角色、10 层「重唱」给其余队伍角色，
        // 各持续 30 秒，施放战技时刷新层数（效果类内部按「取更多」处理）。
        int phase = vodyanitsa.getData().getAscensionPhase();
        if (phase >= 4) {
            grantSongStacks(player, vodyanitsa);
        } else {
            LOGGER.info("[沃雅妮莎] 突破 {} 级 < 4，突破天赋 2（领唱/重唱）不发放", phase);
        }

        // 6 命：遥久之歌持续期间，队伍附近角色的星扩散反应伤害擢升 30%、水/冰伤害 +60%
        //（buff 时长给成遥久之歌的时长，效果到期自动摘）
        if (vodyanitsa.hasConstellation(6)) {
            var glimmer = com.linweiyun.genshin.core.system.registry.register.ModCharacterEffects
                    .VODYANITSA_GLIMMER_EFFECT.get();
            var attachment6 = player.getData(
                    com.linweiyun.genshin.core.attachment.AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            if (glimmer != null && attachment6 != null) {
                for (int i = 0; i < 4; i++) {
                    PGCharacter member = attachment6.getPartyCharacter(i);
                    if (member == null) continue;
                    com.linweiyun.genshin.content.effect.character.CharacterEffectHelper.addEffect(
                            player, member,
                            new com.linweiyun.genshin.content.effect.character.CharacterEffectInstance(
                                    glimmer, vodyanitsa.songTicksRemaining(), 0, false));
                }
            }
        }

        // 突破天赋 1（突破 ≥ 1）：把当前场上的星辉风旋转化为「流荡风旋」。
        // 转化之后它们在引爆时也会降风抗（创造那一次在 SwirlReaction 里判定）。
        if (vodyanitsa.getData().getAscensionPhase() >= 1) {
            var box = new net.minecraft.world.phys.AABB(player.position(), player.position())
                    .inflate(SONG_RANGE * 2.0);
            for (var vortex : level.getEntitiesOfClass(
                    com.linweiyun.genshin.content.entities.area.StellarVortexEntity.class, box)) {
                vortex.markFlowingSwirl();
            }
        }
    }

    @Override
    public void elementalBurst(Player player, PGCharacter character) {
        Level level = player.level();
        if (level.isClientSide()) return;
        if (!(character instanceof Vodyanitsa vodyanitsa)) return;

        int burstLevel = character.getData().getElementalBurstLevel();
        // 一段生命值上限倍率的范围水伤
        float multiplier = at(BURST_DAMAGE, burstLevel);
        // 处于「遥久之歌」时额外一档 —— 落在**增伤区**（不是倍率区！）
        float songBonus = vodyanitsa.isSongActive() ? at(BURST_SONG_BONUS, burstLevel) : 0f;

        // ① 13 格内索敌到的目标 → 直接结算
        LivingEntity locked = resolveAttackTarget(player);
        if (locked != null) {
            hurtHpScaling(player, character, locked, multiplier,
                    AttackType.ELEMENTAL_BURST, AttachmentType.STRONG.getInitialAmount(), songBonus);
        }

        // ② 自身周围一圈（贴脸时的溅射）
        Vec3 center = player.position();
        float radius = 6.0f;
        for (LivingEntity target : areaTargets(level, center, radius)) {
            if (target == locked) continue;
            hurtHpScaling(player, character, target, multiplier,
                    AttackType.ELEMENTAL_BURST, AttachmentType.STRONG.getInitialAmount(), songBonus);
        }
    }

    // ==================== 伤害工具 ====================

    private static List<LivingEntity> areaTargets(Level level, Vec3 center, float radius) {
        return new AreaEntityCollector(level,
                center.add(-radius, -radius, -radius),
                center.add(radius, radius, radius), radius).execute();
    }

    /** 攻击力倍率的水伤（普攻 / 重击）。 */
    private static void hurt(Player player, PGCharacter character, LivingEntity target,
                             float multiplier, AttackType attackType, float elementAmount) {
        ModDamageSpec spec = ModDamageSpec.builder(attackType, ModElements.HYDRO.get())
                .multiplier(multiplier)
                .elementAmount(elementAmount)
                .attackerCharacter(character)
                .build();
        apply(player, target, spec);
    }

    /**
     * 生命值上限倍率的水伤（战技 / 大招）。
     *
     * <p>用 {@code hpMultiplier} 表达「%生命值上限」——基础区会算成
     * {@code 生命值 × 生命值倍率}（见 {@code DamageZones.baseZoneText}）。
     */
    private static void hurtHpScaling(Player player, PGCharacter character, LivingEntity target,
                                      float multiplier, AttackType attackType, float elementAmount) {
        hurtHpScaling(player, character, target, multiplier, attackType, elementAmount, 0f);
    }

    /**
     * 同上，外加一档<b>增伤区</b>加成（{@code damageBonus}）。
     *
     * <p>注意是<b>增伤区</b>（{@code 1 + 元素伤害加成 + 效果加成 + 招式增伤}），不是倍率区 ——
     * 倍率区是 {@code skillMultiplierBonus}。沃雅妮莎大招「处于遥久之歌时额外一档」就落在这里。
     */
    private static void hurtHpScaling(Player player, PGCharacter character, LivingEntity target,
                                      float multiplier, AttackType attackType, float elementAmount,
                                      float damageBonus) {
        ModDamageSpec spec = ModDamageSpec.builder(attackType, ModElements.HYDRO.get())
                .hpMultiplier(multiplier)
                .elementAmount(elementAmount)
                .attackerCharacter(character)
                .build();
        if (damageBonus != 0f) {
            spec.withDamageBonus(damageBonus);
        }
        apply(player, target, spec);
    }

    private static void apply(Player player, LivingEntity target, ModDamageSpec spec) {
        ModDamageSource source = ModDamageSource.from(spec, player);
        if (target.level() instanceof ServerLevel serverLevel) {
            target.hurtServer(serverLevel, source, 0f);
        }
    }

    // ==================== 「遥久之歌」的定时效果 ====================

    /** 索敌 13 格。 */
    public static final int SONG_RANGE = 13;
    /** 水/冰抗降低的修饰符来源（同一个来源只保留最新值）。 */
    public static final String SONG_RES_SOURCE = "vodyanitsa_song_res";
    /** 抗性降低持续时间 6 秒。 */
    public static final int SONG_RES_DURATION_TICKS = 6 * 20;

    /** 水/冰抗降低：Lv1 16.5% → Lv15 39%。 */
    public static final float[] SONG_RES_SHRED = {
            0.165f, 0.18f, 0.195f, 0.21f, 0.225f, 0.24f, 0.255f, 0.27f, 0.285f, 0.30f,
            0.318f, 0.336f, 0.354f, 0.372f, 0.39f
    };

    /** 治疗量 = 基础 + 百分比 × 生命值上限。 */
    public static final float[] HEAL_BASE = {
            269.63f, 296.6f, 325.81f, 357.27f, 390.98f, 426.93f, 465.14f, 505.59f, 548.28f, 593.23f,
            640.42f, 689.86f, 741.54f, 795.48f, 851.66f
    };
    public static final float[] HEAL_PCT = {
            0.028f, 0.0301f, 0.0322f, 0.035f, 0.0371f, 0.0392f, 0.042f, 0.0448f, 0.0476f, 0.0504f,
            0.0532f, 0.056f, 0.0595f, 0.063f, 0.0665f
    };

    /** 「遥久之歌」的覆盖范围（突破天赋 1 用）。 */
    public static final double SONG_AURA_RANGE = 13.0;

    /** 突破天赋 1：遥久之歌期间进入辉映·星扩散，持续时间 +4 秒。 */
    public static final int RADIANCE_EXTEND_TICKS = 4 * 20;

    /**
     * 这个位置有没有被「遥久之歌」覆盖 —— 队伍里任一沃雅妮莎正在唱、且离该位置够近。
     *
     * <p>突破天赋 1 用：遥久之歌期间队伍附近的角色进入辉映·星扩散时，持续时间 +4 秒。
     */
    public static boolean songCovers(ServerLevel level, double x, double y, double z) {
        double rangeSqr = SONG_AURA_RANGE * SONG_AURA_RANGE;
        for (var serverPlayer : level.players()) {
            var attachment = serverPlayer.getData(
                    com.linweiyun.genshin.core.attachment.AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            if (attachment == null) continue;
            for (int i = 0; i < 4; i++) {
                PGCharacter member = attachment.getPartyCharacter(i);
                if (member instanceof Vodyanitsa vodyanitsa && vodyanitsa.isSongActive()
                        && serverPlayer.position().distanceToSqr(x, y, z) <= rangeSqr) {
                    return true;
                }
            }
        }
        return false;
    }

    // ==================== 流荡风旋：降风抗 ====================

    /** 附近敌人风元素抗性 −35%，持续 6 秒。 */
    public static final float FLOWING_WIND_SHRED = 0.35f;
    public static final int FLOWING_WIND_SHRED_TICKS = 6 * 20;
    /** 「附近」的判定半径（格）。 */
    public static final double FLOWING_WIND_SHRED_RADIUS = 8.0;
    /** 风抗降低的修饰符来源。 */
    public static final String WIND_SHRED_SOURCE = "vodyanitsa_flowing_wind";

    /** 实体 UUID → 风抗降低到期时刻（跨反应，与角色实例解耦）。 */
    private static final java.util.Map<java.util.UUID, Long> WIND_SHRED_UNTIL = new java.util.HashMap<>();

    /**
     * 创造 / 引爆「流荡风旋」时调用：把周围敌人的风抗降低 {@value #FLOWING_WIND_SHRED}（35%），
     * 持续 {@link #FLOWING_WIND_SHRED_TICKS} 刻（6 秒）。
     */
    public static void shredWindAround(ServerLevel level, double x, double y, double z) {
        var box = new net.minecraft.world.phys.AABB(x, y, z, x, y, z)
                .inflate(FLOWING_WIND_SHRED_RADIUS);
        long until = level.getGameTime() + FLOWING_WIND_SHRED_TICKS;

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (target instanceof Player) continue;
            var stats = target.getData(
                    com.linweiyun.genshin.core.attachment.AttachmentRegistration.ENTITY_STATS);
            // 减抗是「基于目标自身抗性」的**乘算**：抗性 ×(1 - 降低比例)。
            // 属性系统里正好就是百分比修饰符：total = base × (1 + percent) + flat
            //   → 0.1 抗性 + percent(-0.165) = 0.1 × 0.835 = 0.0835 ✔
            // （写 flat 才是加算，那是错的；另外读取端必须读 total，见 TeyvatEntityStats）
            stats.attributes().setPercentModifier(
                    com.linweiyun.genshin.core.system.registry.register.ModAttributes.ANEMO_RES.value(),
                    WIND_SHRED_SOURCE, -FLOWING_WIND_SHRED);
            WIND_SHRED_UNTIL.put(target.getUUID(), until);
        }
    }

    /** 摘掉已到期的风抗降低（由在唱的沃雅妮莎每刻调一次即可）。 */
    public static void clearExpiredWindShred(ServerLevel level) {
        if (WIND_SHRED_UNTIL.isEmpty()) return;
        long now = level.getGameTime();
        var iterator = WIND_SHRED_UNTIL.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now < entry.getValue()) continue;
            iterator.remove();
            net.minecraft.world.entity.Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living)) continue;
            living.getData(com.linweiyun.genshin.core.attachment.AttachmentRegistration.ENTITY_STATS)
                    .attributes().removeModifier(
                            com.linweiyun.genshin.core.system.registry.register.ModAttributes.ANEMO_RES.value(),
                            WIND_SHRED_SOURCE);
        }
    }

    /**
     * 突破天赋 2：给当前场上角色 25 层「领唱」、其余队伍角色 10 层「重唱」（各 30 秒）。
     */
    private static void grantSongStacks(Player player, Vodyanitsa vodyanitsa) {
        var attachment = player.getData(
                com.linweiyun.genshin.core.attachment.AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        if (attachment == null) return;
        PGCharacter onField = attachment.getCurrentCharacter();

        for (int i = 0; i < 4; i++) {
            PGCharacter member = attachment.getPartyCharacter(i);
            if (member == null) continue;

            boolean isOnField = member == onField;
            com.linweiyun.genshin.content.effect.character.ICharacterEffect effect;
            int stacks;
            if (isOnField) {
                effect = com.linweiyun.genshin.core.system.registry.register.ModCharacterEffects
                        .VODYANITSA_ANTIPHON_EFFECT.get();
                stacks = com.linweiyun.genshin.content.effect.character.vodyanitsa
                        .VodyanitsaSongEffects.ANTIPHON_STACKS;
            } else {
                effect = com.linweiyun.genshin.core.system.registry.register.ModCharacterEffects
                        .VODYANITSA_REFRAIN_EFFECT.get();
                stacks = com.linweiyun.genshin.content.effect.character.vodyanitsa
                        .VodyanitsaSongEffects.REFRAIN_STACKS;
            }
            if (effect == null) continue;

            var instance = new com.linweiyun.genshin.content.effect.character.CharacterEffectInstance(
                    effect,
                    com.linweiyun.genshin.content.effect.character.vodyanitsa
                            .VodyanitsaSongEffects.DURATION_TICKS,
                    0, false);
            instance.setIntData(com.linweiyun.genshin.content.effect.character.vodyanitsa
                    .VodyanitsaSongEffects.STACK_KEY, stacks);
            com.linweiyun.genshin.content.effect.character.CharacterEffectHelper
                    .addEffect(player, member, instance);
            LOGGER.info("[沃雅妮莎] 突破天赋 2 → {} {} 层（{} 刻）",
                    member.getName().getString(), isOnField ? "领唱" : "重唱", stacks);
        }
    }

    /**
     * 2 命「穿彻风雪的余响」：<b>唤春角笛命中敌人</b>时，给队伍中当前场上角色挂「黑与白的双音」（5 秒）。
     *
     * <ul>
     *   <li>平时 → 水/冰伤害暴击伤害 +50%（{@code DuetElement}）；</li>
     *   <li>场上有流荡风旋、或处于引爆后 5 秒内 → 改为星扩散反应伤害暴击伤害 +60%（{@code DuetStellar}）。</li>
     * </ul>
     *
     * <p>6 命时改为对队伍附近所有角色生效（2 命只给当前场上角色）。
     * 施放战技那一刻（唤春角笛那一下）就会调用一次，遥久之歌每次跳动也会再调，所以触发时机和文案一致。
     */
    private static void grantDuetBuff(Player player, Vodyanitsa vodyanitsa) {
        if (!vodyanitsa.hasConstellation(2)) return;

        var duet = (com.linweiyun.genshin.content.effect.character.vodyanitsa.VodyanitsaSongEffects
                .flowingSwirlActive(player)
                ? com.linweiyun.genshin.core.system.registry.register.ModCharacterEffects
                        .VODYANITSA_DUET_STELLAR_EFFECT
                : com.linweiyun.genshin.core.system.registry.register.ModCharacterEffects
                        .VODYANITSA_DUET_ELEMENT_EFFECT).get();
        var attachment = player.getData(
                com.linweiyun.genshin.core.attachment.AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        if (duet == null || attachment == null) return;

        PGCharacter onField = attachment.getCurrentCharacter();

        // 2 命：只给当前场上角色；6 命：改为对队伍附近所有角色生效
        for (int i = 0; i < 4; i++) {
            PGCharacter member = attachment.getPartyCharacter(i);
            if (member == null) continue;
            if (!vodyanitsa.hasConstellation(6) && member != onField) continue;
            com.linweiyun.genshin.content.effect.character.CharacterEffectHelper.addEffect(
                    player, member,
                    new com.linweiyun.genshin.content.effect.character.CharacterEffectInstance(
                            duet, com.linweiyun.genshin.content.effect.character.vodyanitsa
                                    .VodyanitsaBuffs.DURATION_TICKS, 0, false));
        }
    }

    /** 索敌：优先玩家当前锁定的目标，没锁就按 13 格半径找最近的。 */    private static com.linweiyun.genshin.core.system.combat.targeting.SummonTargeting songTargeting() {
        return com.linweiyun.genshin.core.system.combat.targeting.SummonTargeting.defaultMode();
    }

    /** 每 3 秒一次：对一名敌人打生命值上限倍率的水伤，并降低它的水/冰抗（6 秒）。 */
    public static void songAttack(Player player, Vodyanitsa vodyanitsa) {
        Level level = player.level();
        if (level.isClientSide()) return;

        LivingEntity target = songTargeting().resolve(player, null, SONG_RANGE,
                () -> new com.linweiyun.genshin.content.skill_node.TargetSeeker(
                        player, SONG_RANGE,
                        com.linweiyun.genshin.content.skill_node.TargetSeeker.TargetingType.RADIUS).execute());
        if (target == null) return;

        int skillLevel = vodyanitsa.getData().getElementalSkillLevel();
        hurtHpScaling(player, vodyanitsa, target, at(SKILL_DAMAGE, skillLevel),
                AttackType.ELEMENTAL_SKILL, AttachmentType.WEAK.getInitialAmount());

        // 2 命：唤春角笛命中敌人 → 给当前场上角色挂「黑与白的双音」（5 秒）
        grantDuetBuff(player, vodyanitsa);

        // 命中时降低水/冰抗（同一来源覆盖；6 秒后由 Vodyanitsa.tick 摘掉）
        float shred = at(SONG_RES_SHRED, skillLevel);
        var stats = target.getData(com.linweiyun.genshin.core.attachment.AttachmentRegistration.ENTITY_STATS);
        stats.attributes().setPercentModifier(
                com.linweiyun.genshin.core.system.registry.register.ModAttributes.HYDRO_RES.value(),
                SONG_RES_SOURCE, -shred);
        stats.attributes().setPercentModifier(
                com.linweiyun.genshin.core.system.registry.register.ModAttributes.CYRO_RES.value(),
                SONG_RES_SOURCE, -shred);
        vodyanitsa.markSongShred(target, player.level().getGameTime() + SONG_RES_DURATION_TICKS);
    }

    /** 每 1.5 秒一次：给<b>全队所有角色</b>按表回血（受益于沃雅妮莎的生命值上限）。 */
    public static void songHeal(Player player, Vodyanitsa vodyanitsa) {
        if (player.level().isClientSide()) return;

        var attachment = player.getData(
                com.linweiyun.genshin.core.attachment.AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        if (attachment == null) return;

        int skillLevel = vodyanitsa.getData().getElementalSkillLevel();
        double maxHp = vodyanitsa.getData().getAttributeTotalValue(
                com.linweiyun.genshin.core.system.registry.register.ModAttributes.MAX_HP.value());
        double baseAmount = at(HEAL_BASE, skillLevel) + at(HEAL_PCT, skillLevel) * maxHp;
        baseAmount *= 1.0 + vodyanitsa.getHealingBonus();

        for (int i = 0; i < 4; i++) {
            PGCharacter member = attachment.getPartyCharacter(i);
            if (member == null) continue;

            double amount = baseAmount;
            var data = member.getData();
            double healMax = data.getAttributeTotalValue(
                    com.linweiyun.genshin.core.system.registry.register.ModAttributes.MAX_HP.value());
            if (vodyanitsa.hasConstellation(4)) {
                double ratio = healMax <= 0 ? 1.0 : (data.getCurrentHP() / healMax);
                if (ratio < 0.40) {
                    amount *= 1.5;
                } else {
                    vodyanitsa.addC4HpStack();
                }
            }
            data.setCurrentHP(Math.min(healMax, data.getCurrentHP() + amount));
        }

        com.linweiyun.genshin.content.items.weapon.WeaponItem.notifyHeal(
                player, vodyanitsa, player, (float) baseAmount);

        if (vodyanitsa.hasConstellation(1)) {
            var spotlight = com.linweiyun.genshin.core.system.registry.register.ModCharacterEffects
                    .VODYANITSA_SPOTLIGHT_EFFECT.get();
            if (spotlight != null) {
                for (int i = 0; i < 4; i++) {
                    PGCharacter member = attachment.getPartyCharacter(i);
                    if (member == null) continue;
                    com.linweiyun.genshin.content.effect.character.CharacterEffectHelper.addEffect(
                            player, member,
                            new com.linweiyun.genshin.content.effect.character.CharacterEffectInstance(
                                    spotlight, com.linweiyun.genshin.content.effect.character.vodyanitsa
                                            .VodyanitsaBuffs.DURATION_TICKS, 0, false));
                }
            }
        }
    }
}