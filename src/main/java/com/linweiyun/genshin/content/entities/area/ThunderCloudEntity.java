package com.linweiyun.genshin.content.entities.area;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSource;
import com.linweiyun.genshin.core.system.combat.damage.ModDamageSpec;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.genshin.core.system.reaction.builtin.ElectroChargedReaction;
import com.linweiyun.genshin.enums.ElementalReactionType;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ThunderCloudEntity extends AreaEntity {
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final int TICK_INTERVAL = 40;
    private static final int DEFAULT_DURATION = 121;
    private static final float CONSUME_PER_ATTACK = 0.4f;
    private static final float HORIZONTAL_RANGE = 7.0f;
    private static final float VERTICAL_RANGE = 7.0f;

    @Persisted(key = "tc_contributors")
    private Map<String, Long> contributorExpireTicks = new HashMap<>();

    @Persisted(key = "tc_tick_counter")
    private int tickCounter;

    private final List<Vec3> particlePositions = new ArrayList<>();
    private boolean particlesCalculated;

    public ThunderCloudEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.duration = DEFAULT_DURATION;
        this.shapeType = AreaShapeType.CYLINDER;
        this.horizontalRadius = HORIZONTAL_RANGE;
        this.verticalRadius = VERTICAL_RANGE;
        this.tickCounter = 0;
    }

    public void addContributor(String contributorKey, long expireTick) {
        contributorExpireTicks.put(contributorKey, expireTick);
        LOGGER.info("[雷暴云] 添加贡献者 key={} | expireTick={}", contributorKey, expireTick);
    }

    public void refreshDuration() {
        this.tickCount = 0;
        this.duration = DEFAULT_DURATION;
        LOGGER.info("[雷暴云] 刷新持续时间");
    }

    @Override
    protected void serverTick() {
        super.serverTick();

        if (this.isRemoved()) return;

        tickCounter++;
        if (tickCounter >= TICK_INTERVAL) {
            tickCounter = 0;

            if (!(this.level() instanceof ServerLevel level)) return;

            long currentTick = level.getGameTime();
            contributorExpireTicks.entrySet().removeIf(e -> e.getValue() <= currentTick);

            if (contributorExpireTicks.isEmpty()) return;

            AABB area = getAreaOfInfluence();
            List<LivingEntity> targets = level.getEntitiesOfClass(
                    LivingEntity.class, area, e -> e.isAlive() && !e.equals(this));

            for (LivingEntity target : targets) {
                if (!targetHasHydroAndElectro(target)) continue;
                if (isTargetInOtherCloud(target, level)) continue;

                dealLunarDamage(target, level);
            }
        }
    }

    private boolean isTargetInOtherCloud(LivingEntity target, ServerLevel level) {
        List<ThunderCloudEntity> allClouds = level.getEntitiesOfClass(
                ThunderCloudEntity.class,
                new AABB(target.getX() - 16, target.getY() - 16, target.getZ() - 16,
                        target.getX() + 16, target.getY() + 16, target.getZ() + 16),
                e -> e.isAlive() && !e.equals(this));
        for (ThunderCloudEntity other : allClouds) {
            if (other.getAreaOfInfluence().contains(target.getX(), target.getY(), target.getZ())) {
                if (other.tickCount < this.tickCount
                        || (other.tickCount == this.tickCount && other.getId() < this.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean targetHasHydroAndElectro(LivingEntity target) {
        StatusContainer container = target.getData(AttachmentRegistration.CONTAINER);
        if (container == null) return false;

        ElementalAttachmentInstance hydro = ElectroChargedReaction.findElement(container, ModElements.HYDRO.get());
        ElementalAttachmentInstance electro = ElectroChargedReaction.findElement(container, ModElements.ELECTRO.get());

        if (hydro == null || electro == null) return false;
        return hydro.getUnit() > 0 && electro.getUnit() > 0;
    }

    public void dealLunarDamage(LivingEntity target, ServerLevel level) {
        StatusContainer container = target.getData(AttachmentRegistration.CONTAINER);
        if (container == null) return;

        ElementalAttachmentInstance hydro = ElectroChargedReaction.findElement(container, ModElements.HYDRO.get());
        ElementalAttachmentInstance electro = ElectroChargedReaction.findElement(container, ModElements.ELECTRO.get());

        float consumedHydro = Math.min(CONSUME_PER_ATTACK, hydro.getUnit());
        float consumedElectro = Math.min(CONSUME_PER_ATTACK, electro.getUnit());
        hydro.consume(consumedHydro);
        electro.consume(consumedElectro);

        List<PGCharacter> contributors = new ArrayList<>();
        for (Map.Entry<String, Long> entry : contributorExpireTicks.entrySet()) {
            String key = entry.getKey();
            String[] parts = key.split("::", 2);
            if (parts.length != 2) continue;
            UUID playerUUID = UUID.fromString(parts[0]);
            String className = parts[1];

            Player player = level.getPlayerByUUID(playerUUID);
            if (player == null) continue;

            PlayerCharactersAttachment attachment = player.getData(
                    AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            PGCharacter character = null;
            for (int i = 0; i < 4; i++) {
                PGCharacter c = attachment.getPartyCharacter(i);
                if (c != null && c.getClass().getSimpleName().equals(className)) {
                    character = c;
                    break;
                }
            }
            if (character == null) continue;
            contributors.add(character);
        }

        if (contributors.isEmpty()) return;

        LivingEntity attacker = resolveAttacker(level);
        if (attacker == null) {
            attacker = target;
        }

        ModDamageSpec spec = ModDamageSpec.lunar(ElementalReactionType.LUNAR_CHARGED);
        spec.setLunarContributors(contributors);
        ModDamageSource source = ModDamageSource.from(spec, attacker);
        target.hurtServer(level, source, 0f);

        LOGGER.info("[雷暴云攻击] target={} | contributors={}",
                target.getName().getString(), contributors.size());
    }

    private LivingEntity resolveAttacker(ServerLevel level) {
        if (contributorExpireTicks.isEmpty()) return null;
        String firstKey = contributorExpireTicks.keySet().iterator().next();
        String[] parts = firstKey.split("::", 2);
        if (parts.length != 2) return null;
        return level.getPlayerByUUID(UUID.fromString(parts[0]));
    }

    @Override
    protected void clientTick() {
        if (!particlesCalculated) {
            calculateParticlePositions();
            particlesCalculated = true;
        }
        spawnCachedParticles();
    }

    private void calculateParticlePositions() {
        particlePositions.clear();
        double cx = this.getX();
        double cy = this.getY();
        double cz = this.getZ();
        double r = horizontalRadius;
        double h = verticalRadius;

        int rings = 3;
        int perRing = 12;
        for (int ring = 0; ring < rings; ring++) {
            double ringR = r * (ring + 1) / rings;
            for (int i = 0; i < perRing; i++) {
                double angle = 2 * Math.PI * i / perRing;
                double x = cx + ringR * Math.cos(angle);
                double z = cz + ringR * Math.sin(angle);
                particlePositions.add(new Vec3(x, cy, z));
            }
        }

        int lineSteps = 8;
        for (int yStep = 0; yStep < lineSteps; yStep++) {
            double dy = -h + 2 * h * yStep / (lineSteps - 1);
            for (int i = 0; i < 4; i++) {
                double angle = 2 * Math.PI * i / 4;
                double x = cx + r * Math.cos(angle);
                double z = cz + r * Math.sin(angle);
                particlePositions.add(new Vec3(x, cy + dy, z));
            }
        }

        int innerCount = 20;
        for (int i = 0; i < innerCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = random.nextDouble() * r;
            double x = cx + dist * Math.cos(angle);
            double z = cz + dist * Math.sin(angle);
            double dy = -h + random.nextDouble() * h;
            particlePositions.add(new Vec3(x, cy + dy, z));
        }
    }

    private void spawnCachedParticles() {
        for (Vec3 pos : particlePositions) {
            this.level().addParticle(ParticleTypes.CLOUD,
                    pos.x + (random.nextFloat() - 0.5f) * 0.3f,
                    pos.y + (random.nextFloat() - 0.5f) * 0.3f,
                    pos.z + (random.nextFloat() - 0.5f) * 0.3f,
                    0, 0, 0);
        }
    }

    public AABB getAreaOfInfluence() {
        double hw = horizontalRadius;
        double hh = verticalRadius;
        return new AABB(
                position().x - hw, position().y - hh, position().z - hw,
                position().x + hw, position().y, position().z + hw);
    }
}