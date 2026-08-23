package com.linweiyun.genshin.core.combat.damage;

import com.linweiyun.genshin.registry.DamageTypeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

/**
 * 自定义伤害源 —— 将 DamageSpec 融入 MC 的 DamageSource 体系
 *
 * MC伤害管线中，所有伤害信息通过 DamageSource 传递。
 * DamageSpec 是纯数据类，无法直接在管线中传递。
 * ModDamageSource 作为桥梁，将规格嵌入伤害源，使其随MC管线流动。
 *
 * 与旧项目 ElementalDamageSourceGIM 的区别：
 * - 旧版只携带一个 ElementalsGIM 元素字段
 * - 新版内嵌完整的 DamageSpec（攻击类型+元素+倍率+元素量+衰减组别）
 * - 任何地方拿到 DamageSource → instanceof ModDamageSource → getSpec() → 获取全部数据
 */
public class ModDamageSource extends DamageSource {

    // 内嵌的伤害规格 —— 携带本次攻击的所有数据
    private ModDamageSpec spec;

    /**
     * 构造函数 —— 基础版（攻击者即直接实体）
     * @param damageType MC的DamageType Holder（控制死亡消息等）
     * @param causer 造成伤害的实体
     * @param spec 伤害规格
     */
    public ModDamageSource(Holder<DamageType> damageType, @Nullable Entity causer, ModDamageSpec spec) {
        super(damageType, causer);
        this.spec = spec;
    }

    /**
     * 构造函数 —— 完整版（区分直接实体和源头实体，用于抛射物等场景）
     * @param damageType MC的DamageType Holder
     * @param directEntity 直接造成伤害的实体（如抛射物）
     * @param causer 造成伤害的源头实体（如发射抛射物的玩家）
     * @param spec 伤害规格
     */
    public ModDamageSource(Holder<DamageType> damageType, @Nullable Entity directEntity,
                           @Nullable Entity causer, ModDamageSpec spec) {
        super(damageType, directEntity, causer);
        this.spec = spec;
    }

    /**
     * 获取内嵌的伤害规格
     * 战斗系统通过此方法从 DamageSource 中提取完整的攻击数据
     * @return 伤害规格
     */
    public ModDamageSpec getSpec() {
        return this.spec;
    }
    public void setSpec(ModDamageSpec newSpec) {
        this.spec = newSpec;
    }


    /**
     * 从规格创建伤害源（自动解析 DamageType）
     * 最常用的创建方式：传入规格和攻击者，自动通过 ModDamageTypes 解析对应的 MC DamageType
     * @param spec 伤害规格
     * @param causer 造成伤害的实体
     * @return 新的 ModDamageSource
     */
    public static ModDamageSource from(ModDamageSpec spec, Entity causer) {
        return new ModDamageSource(
                DamageTypeRegistry.resolveHolder(spec.getAttackType(), causer.level().registryAccess()),
                causer,
                spec
        );
    }
}