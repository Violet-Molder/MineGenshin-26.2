package com.linweiyun.genshin.content.entities.area;

import com.linweiyun.genshin.config.reaction.ReactionConfig;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.*;

public class StellarVortexEntity extends AreaEntity {
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final int EXPLODE_TIMER = 60;
    private static final int MAX_LEVEL = 6;
    private static final float DEFAULT_HORIZONTAL_RANGE = 5.0f;
    private static final float LEVEL3_HORIZONTAL_RANGE = 7.0f;
    private static final int LEVEL3_THRESHOLD = 3;
    private static final float ICE_ATTACH_QUANTITY = 1.0f;

    @Persisted(key = "sv_level")
    private int vortexLevel = 1;

    @Persisted(key = "sv_explode_timer")
    private int explodeTimer = EXPLODE_TIMER;

    @Persisted(key = "sv_exploded")
    private boolean exploded;

    // 星璇存活期间所有触发过星扩散的角色（只增不减，用于冰伤统计）
    private final List<PGCharacter> accumulatedContributors = new ArrayList<>();

    // 最后一次星扩散的触发者（作为星扩散-冰的伤害源）
    private transient PGCharacter lastStellarTriggerCharacter;

    // 伤害最高的贡献者（用于视觉效果等）
    private transient PGCharacter lastTopContributor;

    public StellarVortexEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.duration = EXPLODE_TIMER + 10;
        this.shapeType = AreaShapeType.CYLINDER;
        this.horizontalRadius = DEFAULT_HORIZONTAL_RANGE;
        this.verticalRadius = 3.0f;
    }

    public PGCharacter getOwnerCharacter() {
        return lastTopContributor;
    }

    /**
     * 从玩家队伍中查找指定 PGCharacter 所属的 Player
     */
    public Player resolveOwnerPlayer(ServerLevel level, PGCharacter character) {
        if (character == null) return null;
        Player directOwner = character.getData().getOwnerPlayer();
        if (directOwner != null) return directOwner;

        int targetUuid = character.getCharacterUUID();
        for (Player p : level.players()) {
            PlayerCharactersAttachment att = p.getData(
                    AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            for (int i = 0; i < 4; i++) {
                PGCharacter c = att.getPartyCharacter(i);
                if (c != null && c.getCharacterUUID() == targetUuid) {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * 向累积贡献者列表添加角色（用于星扩散-冰爆炸时统计）
     */
    public void addContributor(PGCharacter character) {
        if (character != null && !accumulatedContributors.contains(character)) {
            accumulatedContributors.add(character);
            LOGGER.info("[星辉风旋] 添加累积贡献者 char={} total={}",
                    character.getName(), accumulatedContributors.size());
        }
    }

    public void addAllContributors(Collection<PGCharacter> chars) {
        for (PGCharacter c : chars) {
            addContributor(c);
        }
    }

    public List<PGCharacter> getAccumulatedContributors() {
        return Collections.unmodifiableList(accumulatedContributors);
    }

    public void setLevel(int level) {
        this.vortexLevel = level;
    }

    public int getVortexLevel() {
        return vortexLevel;
    }

    /**
     * 每次星扩散触发时调用，升级并记录最后触发者
     * @param triggerCharacter 本次触发星扩散的角色
     */
    public void incrementLevel(PGCharacter triggerCharacter) {
        if (triggerCharacter != null) {
            this.lastStellarTriggerCharacter = triggerCharacter;
        }
        this.vortexLevel = Math.min(MAX_LEVEL, vortexLevel + 1);
        if (this.vortexLevel >= LEVEL3_THRESHOLD) {
            this.horizontalRadius = LEVEL3_HORIZONTAL_RANGE;
            this.setHorizontalRadius(LEVEL3_HORIZONTAL_RANGE);
        }
        if (this.vortexLevel >= MAX_LEVEL) {
            explode();
        }
        LOGGER.info("[星辉风旋] 等级提升至 {} trigger={} pos={}",
                vortexLevel,
                triggerCharacter != null ? triggerCharacter.getName() : "none",
                this.position());
    }

    private void explode() {
        if (exploded) return;
        exploded = true;

        if (!(this.level() instanceof ServerLevel level)) return;

        double iceCoefficient = vortexLevel <= 2
                ? ReactionConfig.STELLAR_SWIRL_ICE_COEFFICIENT_LOW.get()
                : ReactionConfig.STELLAR_SWIRL_ICE_COEFFICIENT_HIGH.get();

        AABB area = getAreaOfInfluence();
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, area, e -> e.isAlive() && !e.equals(this));

        List<PGCharacter> contributorList = new ArrayList<>(accumulatedContributors);
        if (contributorList.isEmpty()) {
            LOGGER.warn("[星辉风旋] 爆炸时无贡献者，跳过");
            this.discard();
            return;
        }
        this.lastTopContributor = contributorList.get(0);

        // 星扩散-冰的伤害源 = 最后一次星扩散的触发者
        PGCharacter iceSource = lastStellarTriggerCharacter;
        Player iceSourcePlayer = resolveOwnerPlayer(level, iceSource);
        if (iceSourcePlayer == null) {
            iceSourcePlayer = resolveOwnerPlayer(level, contributorList.get(0));
        }
        if (iceSourcePlayer == null) {
            LOGGER.warn("[星辉风旋] 爆炸时无法解析伤害源玩家，跳过");
            this.discard();
            return;
        }

        for (LivingEntity target : targets) {
            ModDamageSpec spec = buildIceSpec(iceCoefficient, contributorList);
            ModDamageSource source = ModDamageSource.from(spec, iceSourcePlayer);
            target.hurtServer(level, source, 0f);

            StatusContainer container = target.getData(AttachmentRegistration.CONTAINER);
            ElementalAttachmentHelper.attach(
                    target, container, ModElements.CYRO.get(),
                    AttachmentSource.SPECIAL,
                    createIceAttachmentProfile(),
                    null, level.getGameTime());
        }


        this.discard();
    }

    /**
     * 触发星扩散-风的区域伤害
     * @param triggerCharacter 本次触发星扩散的角色（作为伤害源）
     * @param windContributors  本次触发时目标身上的冰+风附着角色（作为贡献者）
     */
    public void triggerWindDamage(PGCharacter triggerCharacter, List<PGCharacter> windContributors) {
        if (!(this.level() instanceof ServerLevel level)) return;

        double windCoefficient = ReactionConfig.STELLAR_SWIRL_WIND_COEFFICIENT.get();

        AABB area = getAreaOfInfluence();
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, area, e -> e.isAlive() && !e.equals(this));

        List<PGCharacter> contributorList = new ArrayList<>(windContributors);
        if (contributorList.isEmpty()) {
            LOGGER.warn("[星辉风旋] 风伤时无贡献者，跳过");
            return;
        }

        // 星扩散-风的伤害源 = 触发者本人
        Player windSourcePlayer = resolveOwnerPlayer(level, triggerCharacter);
        if (windSourcePlayer == null) {
            windSourcePlayer = resolveOwnerPlayer(level, contributorList.get(0));
        }
        if (windSourcePlayer == null) {
            LOGGER.warn("[星辉风旋] 风伤时无法解析伤害源玩家，跳过");
            return;
        }

        for (LivingEntity target : targets) {
            ModDamageSpec spec = buildWindSpec(windCoefficient, contributorList);
            ModDamageSource source = ModDamageSource.from(spec, windSourcePlayer);
            target.hurtServer(level, source, 0f);
        }

        this.lastTopContributor = contributorList.isEmpty() ? null : contributorList.get(0);

        LOGGER.info("[星辉风旋] 星扩散-风伤害 level={} targets={} trigger={} windContributors={}",
                vortexLevel, targets.size(),
                triggerCharacter != null ? triggerCharacter.getName() : "none",
                contributorList.size());
    }

    private static ModDamageSpec buildWindSpec(double coefficient, List<PGCharacter> contributors) {
        ModDamageSpec spec = ModDamageSpec.stellar(ElementalReactionType.STELLAR_SWIRL_WIND, ModElements.ANEMO.get());
        setSpecFields(spec, (float) coefficient, 0f, 0f);
        spec.setStellarContributors(contributors);
        return spec;
    }

    private static ModDamageSpec buildIceSpec(double coefficient, List<PGCharacter> contributors) {
        ModDamageSpec spec = ModDamageSpec.stellar(ElementalReactionType.STELLAR_SWIRL_ICE, ModElements.CYRO.get());
        setSpecFields(spec, (float) coefficient, 0f, 0f);
        spec.setStellarContributors(contributors);
        return spec;
    }

    private static void setSpecFields(ModDamageSpec spec, float coefficient, float baseBonusMult, float baseBonusFlat) {
        try {
            java.lang.reflect.Field cField = ModDamageSpec.class.getDeclaredField("stellarCoefficient");
            cField.setAccessible(true);
            cField.set(spec, coefficient);

            java.lang.reflect.Field bmField = ModDamageSpec.class.getDeclaredField("stellarBaseBonusMult");
            bmField.setAccessible(true);
            bmField.set(spec, baseBonusMult);

            java.lang.reflect.Field bfField = ModDamageSpec.class.getDeclaredField("stellarBaseBonusFlat");
            bfField.setAccessible(true);
            bfField.set(spec, baseBonusFlat);
        } catch (Exception e) {
            LOGGER.error("[星辉风旋] 反射设置spec参数失败", e);
        }
    }

    @Override
    protected void serverTick() {
        super.serverTick();
        if (this.isRemoved() || exploded) return;

        explodeTimer--;
        if (explodeTimer <= 0) {
            explode();
        }
    }

    public AABB getAreaOfInfluence() {
        double hw = horizontalRadius;
        double hh = verticalRadius;
        return new AABB(
                position().x - hw, position().y - hh, position().z - hw,
                position().x + hw, position().y + hh, position().z + hw);
    }

    public boolean containsPos(double x, double y, double z) {
        double hw = horizontalRadius;
        double hh = verticalRadius;
        return x >= position().x - hw && x <= position().x + hw
                && y >= position().y - hh && y <= position().y + hh
                && z >= position().z - hw && z <= position().z + hw;
    }

    public static StellarVortexEntity findExisting(ServerLevel level, double x, double y, double z, float reuseRange) {
        List<StellarVortexEntity> all = level.getEntitiesOfClass(
                StellarVortexEntity.class,
                new AABB(x - reuseRange, y - reuseRange, z - reuseRange, x + reuseRange, y + reuseRange, z + reuseRange),
                e -> e.isAlive() && !e.exploded);
        for (StellarVortexEntity v : all) {
            double dx = x - v.getX();
            double dy = y - v.getY();
            double dz = z - v.getZ();
            if (dx * dx + dy * dy + dz * dz <= reuseRange * reuseRange) return v;
        }
        return null;
    }

    private static AttachmentProfile createIceAttachmentProfile() {
        return new AttachmentProfile(ICE_ATTACH_QUANTITY, 1.0f,
                ICE_ATTACH_QUANTITY / 9.5f, 9.5f);
    }
}