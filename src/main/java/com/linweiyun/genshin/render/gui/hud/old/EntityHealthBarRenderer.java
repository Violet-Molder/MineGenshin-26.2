package com.linweiyun.genshin.render.gui.hud.old;

import com.linweiyun.genshin.content.entities.teyvat.boss.ITeyvatBoss;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class EntityHealthBarRenderer {

    /**
     * 判断实体是否应该渲染血条
     * 排除：Player、实现 ITeyvatBoss 接口的实体、已死亡的实体
     */
    public static boolean shouldRenderHealthBar(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (entity instanceof Player) return false;
        if (entity instanceof ITeyvatBoss) return false;
        return living.isAlive();
    }

    /**
     * 获取实体头顶的渲染位置（世界坐标）
     * 在实体 bounding box 上方偏移一定距离
     */
    public static Vec3 getHealthBarPosition(LivingEntity entity, float partialTick) {
        double height = entity.getBbHeight();
        Vec3 pos = entity.getPosition(partialTick);
        return pos.add(0, height + 0.5, 0);
    }
}