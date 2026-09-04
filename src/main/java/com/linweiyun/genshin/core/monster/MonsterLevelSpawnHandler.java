package com.linweiyun.genshin.core.monster;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.mixin.interfaces.IMonsterLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

@EventBusSubscriber
public class MonsterLevelSpawnHandler {

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Monster monster)) return;
        if (!(monster instanceof IMonsterLevel monsterLevel)) return;
        if (monsterLevel.genshin$getMonsterLevel() != 0) return;

        try {
            ServerLevel serverLevel = (ServerLevel) event.getLevel();
            Vec3 pos = monster.position();
            long seed = monster.getUUID().getMostSignificantBits() ^ monster.getRandom().nextLong();
            int lv = MonsterLevelCalculator.getMonsterLevelNatural(serverLevel, pos, seed);
            monsterLevel.genshin$setMonsterLevel(lv);
        } catch (Exception e) {
            Minegenshin.LOGGER.warn("MonsterLevelSpawnHandler failed for {}: {}", monster.getType(), e.getMessage());
        }
    }
}