package com.linweiyun.genshin.core.character.polearm.shenhe;

import com.linweiyun.genshin.config.character.ShenheAttributeConfig;
import com.linweiyun.genshin.core.character.IStellarStateHolder;
import com.linweiyun.genshin.core.element.ModElements;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import com.linweiyun.genshin.core.character.polearm.PolearmCharacter;
import com.linweiyun.genshin.enums.CharacterAscendAttribute;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Shenhe extends PolearmCharacter implements IStellarStateHolder {
    private static final Logger LOGGER = LogUtils.getLogger();

    public Shenhe() {
        // ⚠️ 这里是「三冷却」重载（11 个参数）：skillShort=200 / skillLong=300 /
        //    burst=200 / 最大能量=80。别数错成两冷却重载 —— short ≠ long 是
        //    「E 长按有效果」的前提（ActionStateMachine.hasSkillHoldVariant 就是比这两个）。
        super(135001, 5, Component.translatable("character.name.shenhe"),
                ModElements.CYRO.getId().toString(), CharacterAscendAttribute.ATK,
                10 * 20, 15 * 20, 10 * 20, 80f, "shenhe",
                Map.of(
                        ModAttributes.MAX_HP.getId(), ShenheAttributeConfig::getAllHp,
                        ModAttributes.ATK.getId(), ShenheAttributeConfig::getAllAtk,
                        ModAttributes.DEF.getId(), ShenheAttributeConfig::getAllDef
                ));
        // 三个协作者都在无参构造器里建：客户端反序列化走
        // clazz.getDeclaredConstructor().newInstance()（会跑到这里），双端都拿得到实例。
        this.skill = new ShenheSkill();
        this.talent = new ShenheTalent();
        this.constellation = new ShenheConstellation();
    }

    @Override
    public com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData getActionData() {
        return ShenheResources.ACTION_DATA;
    }

    /**
     * 她的 E / 重击是「技能自己推位移」（{@code DashSystem}：客户端按格走、服务端扫伤害），
     * 所以客户端也要本地跑一次天赋钩子 —— 否则那段突刺在客户端永远不会被调用。
     */
    @Override
    public boolean runsTalentOnClient() {
        return true;
    }

    @Override
    public Map<Identifier, Supplier<List<? extends Integer>>> getStatGrowthMap() {
        return Map.of(
                ModAttributes.MAX_HP.getId(), ShenheAttributeConfig::getAllHp,
                ModAttributes.ATK.getId(), ShenheAttributeConfig::getAllAtk,
                ModAttributes.DEF.getId(), ShenheAttributeConfig::getAllDef
        );
    }
}