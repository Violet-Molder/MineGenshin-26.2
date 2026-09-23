package com.linweiyun.genshin.core.system.about.block;

import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.PlayerCharactersAttachment;
import com.linweiyun.genshin.core.attachment.StatusContainer;
import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.element.GenshinElement;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.status.StatusInstance;
import com.linweiyun.genshin.core.system.about.AttachmentProfile;
import com.linweiyun.genshin.core.system.about.AttachmentSource;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.genshin.core.system.reaction.ElementalReactionManager;
import com.linweiyun.genshin.core.system.reaction.ReactionContext;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;

/**
 * 方块元素操作 —— 元素附着是唯一入口，反应由反应系统统一触发。
 *
 * 水体富含水元素(HYDRO)，冰富含冰元素(CYRO)。
 *
 * 规则：
 *   水 + 冰元素(CYRO)     → 附着 → 冻结反应 → 浮冰 + 携带冻元素(FROZEN)
 *   浮冰冻元素衰减到 0     → 水
 *   浮冰 + 火元素(PYRO)   → 附着 → 融化反应 → 水
 *   LivingEntity 在水中/雨中 → 周期性挂弱水元素
 *   大型冰史莱姆踏水       → 给水附着冰 → 冻结 → 浮冰+冻元素
 *   大型冰史莱姆踏冰       → 给冰附着冻 → 刷新冻元素量
 */
public final class BlockElementHelper {

    public static final Logger LOGGER = LogUtils.getLogger();

    private static final int WATER_CHECK_INTERVAL = 20;
    private static int waterEntityCheckCounter = 0;

    private BlockElementHelper() {}

    // ==================== 方块可否接受元素 ====================

    public static boolean canBlockAcceptElement(BlockState state, GenshinElement element) {
        if (state.is(Blocks.WATER)) {
            return element == ModElements.CYRO.get();
        }
        if (state.is(Blocks.FROSTED_ICE) || state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)) {
            return element == ModElements.PYRO.get()
                    || element == ModElements.CYRO.get()
                    || element == ModElements.FROZEN.get();
        }
        return false;
    }

    // ==================== 元素附着入口（唯一） ====================

    /**
     * 对方块附着元素。这是唯一入口 —— 不直接调用反应，不直接改方块状态。
     *
     * 流程：构建方块环境容器（隐含元素 + 已有元素）→ 挂攻击元素 → 反应系统处理 → 反应结果驱动方块状态变化。
     */
    public static void applyElement(ServerLevel level, BlockPos pos,
                                    GenshinElement element,
                                    float gauge, float decayPerSec) {
        BlockState state = level.getBlockState(pos);

        if (!canBlockAcceptElement(state, element)) {
            return;
        }

        // 构建方块容器：隐含环境元素 + Chunk 里已有的元素实例
        StatusContainer container = buildBlockContainer(level, pos, state);

        // 攻击者附着
        ElementalAttachmentInstance attacker = new ElementalAttachmentInstance(
                element,
                AttachmentSource.ENVIRONMENTAL,
                AttachmentProfile.WEAK,
                gauge);
        container.add(attacker);

        // 走反应系统
        ReactionContext ctx = new ReactionContext(
                element, gauge,
                AttachmentSource.ENVIRONMENTAL,
                AttachmentProfile.WEAK,
                null, null,
                container, null);

        ElementalReactionManager.tryReactForBlock(ctx, level, pos);

        // 反应后：根据容器里还剩什么来决定方块状态
        boolean waterToIce = state.is(Blocks.WATER)
                && containerHasElement(container, ModElements.FROZEN.get());
        boolean iceToWater = isIceBlock(state)
                && !containerHasAnyCyroOrFrozen(container);

        if (waterToIce) {
            level.setBlock(pos, Blocks.FROSTED_ICE.defaultBlockState(), 3);
            level.scheduleTick(pos, Blocks.FROSTED_ICE, 1);
            float frozenQty = sumElementQuantity(container, ModElements.FROZEN.get());
            storeFrozenToChunk(level, pos, frozenQty, decayPerSec);
            LOGGER.info("[BlockElement] Water frozen at {}: FROZEN qty={}, decay={}",
                    pos, frozenQty, decayPerSec);
        } else if (iceToWater) {
            clearFrozenFromChunk(level, pos);
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            LOGGER.info("[BlockElement] Ice melted at {}", pos);
        } else if (isIceBlock(state)) {
            // 没有发生导致状态变化的反应 → 以攻击元素量为准刷新冻元素量
            if (gauge > 0f) {
                storeOrRefreshFrozen(level, pos, gauge, decayPerSec);
            }
        }
    }

    // ==================== 水环境给实体挂水 ====================

    public static void checkAndApplyWaterToEntity(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (entity.isSpectator()) return;

        boolean inWater = entity.isInWater();
        boolean exposedToRain = level.isRainingAt(entity.blockPosition())
                && level.canSeeSky(entity.blockPosition());

        if (!inWater && !exposedToRain) return;

        MobCategory cat = entity.getType().getCategory();
        if (cat == MobCategory.WATER_CREATURE || cat == MobCategory.WATER_AMBIENT) return;

        StatusContainer container;
        if (entity instanceof Player player) {
            PlayerCharactersAttachment chars = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
            if (chars == null) return;
            PGCharacter character = chars.getCurrentCharacter();
            if (character == null) return;
            container = character.getData().getStatusContainer();
        } else {
            container = entity.getData(AttachmentRegistration.CONTAINER);
            if (container == null) return;
        }

        ElementalAttachmentHelper.attach(entity, container,
                ModElements.HYDRO.get(),
                AttachmentSource.ENVIRONMENTAL,
                AttachmentProfile.WEAK);
    }

    public static void onServerTick(ServerLevel level) {
        if (++waterEntityCheckCounter < WATER_CHECK_INTERVAL) return;
        waterEntityCheckCounter = 0;

        for (Entity e : level.getEntities().getAll()) {
            if (e instanceof LivingEntity living) {
                checkAndApplyWaterToEntity(living);
            }
        }
    }

    // ==================== 方块元素衰减（由 FrostedIceMixin.tick 调用） ====================

    public static void tickBlockElementDecay(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (!chunk.hasData(AttachmentRegistration.CHUNK_ELEMENTS)) return;

        ChunkBlockElements data = chunk.getData(AttachmentRegistration.CHUNK_ELEMENTS);
        ElementalAttachmentInstance inst = data.get(pos);
        if (inst == null) return;

        inst.tick();


        if (inst.isFinished()) {
            data.remove(pos);
            chunk.setData(AttachmentRegistration.CHUNK_ELEMENTS, data);
            if (level.getBlockState(pos).is(Blocks.FROSTED_ICE)) {
                level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            }
        }
    }

    // ==================== 内部：容器构建 ====================

    /**
     * 根据方块状态构建 StatusContainer：
     * - 水 → 隐含 HYDRO
     * - 冰 → 隐含 CYRO + Chunk 里已有的 FROZEN
     */
    private static StatusContainer buildBlockContainer(ServerLevel level, BlockPos pos,
                                                       BlockState state) {
        StatusContainer container = new StatusContainer();

        if (state.is(Blocks.WATER)) {
            ElementalAttachmentInstance hydro = new ElementalAttachmentInstance(
                    ModElements.HYDRO.get(),
                    AttachmentSource.ENVIRONMENTAL,
                    AttachmentProfile.WEAK,
                    1.0f);
            container.add(hydro);
        }

        if (isIceBlock(state)) {
            ElementalAttachmentInstance cyro = new ElementalAttachmentInstance(
                    ModElements.CYRO.get(),
                    AttachmentSource.ENVIRONMENTAL,
                    AttachmentProfile.WEAK,
                    1.0f);
            container.add(cyro);

            LevelChunk chunk = level.getChunkAt(pos);
            if (chunk.hasData(AttachmentRegistration.CHUNK_ELEMENTS)) {
                ChunkBlockElements data = chunk.getData(AttachmentRegistration.CHUNK_ELEMENTS);
                ElementalAttachmentInstance existing = data.get(pos);
                if (existing != null && !existing.isFinished()) {
                    container.add(existing);
                }
            }
        }

        return container;
    }

    // ==================== 内部：容器查询 ====================

    private static boolean containerHasElement(StatusContainer container, GenshinElement target) {
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (inst instanceof ElementalAttachmentInstance ea
                    && ea.getElement() == target) {
                return true;
            }
        }
        return false;
    }

    private static boolean containerHasAnyCyroOrFrozen(StatusContainer container) {
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (inst instanceof ElementalAttachmentInstance ea
                    && (ea.getElement() == ModElements.CYRO.get()
                    || ea.getElement() == ModElements.FROZEN.get())) {
                return true;
            }
        }
        return false;
    }

    private static float sumElementQuantity(StatusContainer container, GenshinElement target) {
        float sum = 0f;
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (inst instanceof ElementalAttachmentInstance ea
                    && ea.getElement() == target) {
                sum += ea.getUnit();
            }
        }
        return sum;
    }

    // ==================== 内部：Chunk 元素存储 ====================

    private static void storeFrozenToChunk(ServerLevel level, BlockPos pos,
                                           float frozenQty, float decayPerSec) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkBlockElements data = chunk.hasData(AttachmentRegistration.CHUNK_ELEMENTS)
                ? chunk.getData(AttachmentRegistration.CHUNK_ELEMENTS)
                : new ChunkBlockElements();

        ElementalAttachmentInstance inst = new ElementalAttachmentInstance(
                ModElements.FROZEN.get(),
                AttachmentSource.ENVIRONMENTAL,
                AttachmentProfile.WEAK,
                frozenQty);
        inst.overrideDecayRate(decayPerSec);
        data.put(pos, inst);
        chunk.setData(AttachmentRegistration.CHUNK_ELEMENTS, data);
    }

    private static void clearFrozenFromChunk(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (!chunk.hasData(AttachmentRegistration.CHUNK_ELEMENTS)) return;
        ChunkBlockElements data = chunk.getData(AttachmentRegistration.CHUNK_ELEMENTS);
        data.remove(pos);
        chunk.setData(AttachmentRegistration.CHUNK_ELEMENTS, data);
    }

    /**
     * 刷新已有冻元素量（不变衰减率），无则新建。
     */
    private static void storeOrRefreshFrozen(ServerLevel level, BlockPos pos,
                                             float gauge, float decayPerSec) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkBlockElements data = chunk.hasData(AttachmentRegistration.CHUNK_ELEMENTS)
                ? chunk.getData(AttachmentRegistration.CHUNK_ELEMENTS)
                : new ChunkBlockElements();

        ElementalAttachmentInstance existing = data.get(pos);
        if (existing != null && !existing.isFinished()) {
            existing.refreshQuantity(gauge);
        } else {
            ElementalAttachmentInstance inst = new ElementalAttachmentInstance(
                    ModElements.FROZEN.get(),
                    AttachmentSource.ENVIRONMENTAL,
                    AttachmentProfile.WEAK,
                    gauge);
            inst.overrideDecayRate(decayPerSec);
            data.put(pos, inst);
        }

        chunk.setData(AttachmentRegistration.CHUNK_ELEMENTS, data);
    }

    // ==================== 工具 ====================

    private static boolean isIceBlock(BlockState state) {
        return state.is(Blocks.FROSTED_ICE) || state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE);
    }
}