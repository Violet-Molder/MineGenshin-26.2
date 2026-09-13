package com.linweiyun.genshin.core.monster;

import com.linweiyun.genshin.content.entities.teyvat.TeyvatLiving;
import com.linweiyun.genshin.core.world.TeyvatWorldInvasion;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import org.slf4j.Logger;

@EventBusSubscriber
public class MonsterLevelSpawnHandler {
    public static final Logger LOGGER = LogUtils.getLogger();
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (!TeyvatWorldInvasion.get(serverLevel).isInvaded()) return;
        if (!(event.getEntity() instanceof Monster monster)) return;
        if (!(monster instanceof TeyvatLiving monsterLevel)) return;
        if (monsterLevel.getMonsterLevel() != 0) return;

        try {
            Vec3 pos = monster.position();
            long seed = monster.getUUID().getMostSignificantBits() ^ monster.getRandom().nextLong();
            int lv = MonsterLevelCalculator.getMonsterLevelNatural(serverLevel, pos, seed);
            monsterLevel.setMonsterLevel(lv);
        } catch (Exception e) {

        }
    }
}