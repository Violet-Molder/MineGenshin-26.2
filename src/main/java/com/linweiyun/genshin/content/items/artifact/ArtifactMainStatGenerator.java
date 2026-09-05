package com.linweiyun.genshin.content.items.artifact;

import com.linweiyun.genshin.content.stat.TeyvatItemStat;
import com.linweiyun.genshin.core.system.registry.register.ModAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

public class ArtifactMainStatGenerator {
    public static final Logger LOGGER = LoggerFactory.getLogger("Minegenshin/ArtifactMainStatGenerator");

    // SANDS 可选池：HP% / ATK% / DEF% / 元素精通 / 充能效率
    private static final List<TeyvatItemStat> SANDS_POOL = List.of(
            new TeyvatItemStat(ModAttributes.MAX_HP.get(), 0, TeyvatItemStat.StatKind.PERCENT),
            new TeyvatItemStat(ModAttributes.ATK.get(), 0, TeyvatItemStat.StatKind.PERCENT),
            new TeyvatItemStat(ModAttributes.DEF.get(), 0, TeyvatItemStat.StatKind.PERCENT),
            new TeyvatItemStat(ModAttributes.ELEMENTAL_MASTERY.get(), 0, TeyvatItemStat.StatKind.FLAT),
            new TeyvatItemStat(ModAttributes.ER.get(), 0, TeyvatItemStat.StatKind.PERCENT)
    );

    // CIRCLET 可选池：暴击率 / 暴击伤害 / 治疗加成 / HP% / ATK% / DEF% / 元素精通
    private static final List<TeyvatItemStat> CIRCLET_POOL = List.of(
            new TeyvatItemStat(ModAttributes.CR.get(), 0, TeyvatItemStat.StatKind.PERCENT),
            new TeyvatItemStat(ModAttributes.CDG.get(), 0, TeyvatItemStat.StatKind.PERCENT),
            new TeyvatItemStat(ModAttributes.HB.get(), 0, TeyvatItemStat.StatKind.PERCENT),
            new TeyvatItemStat(ModAttributes.MAX_HP.get(), 0, TeyvatItemStat.StatKind.PERCENT),
            new TeyvatItemStat(ModAttributes.ATK.get(), 0, TeyvatItemStat.StatKind.PERCENT),
            new TeyvatItemStat(ModAttributes.DEF.get(), 0, TeyvatItemStat.StatKind.PERCENT),
            new TeyvatItemStat(ModAttributes.ELEMENTAL_MASTERY.get(), 0, TeyvatItemStat.StatKind.FLAT)
    );
    static List<TeyvatItemStat> GOBLET_POOL = List.of(
            new TeyvatItemStat(ModAttributes.PYRO_BONUS.get(), 0, TeyvatItemStat.StatKind.PERCENT),
            new TeyvatItemStat(ModAttributes.HYDRO_BONUS.get(), 0, TeyvatItemStat.StatKind.PERCENT),
            new TeyvatItemStat(ModAttributes.CYRO_BONUS.get(), 0, TeyvatItemStat.StatKind.PERCENT),
            new TeyvatItemStat(ModAttributes.ELECTRO_BONUS.get(), 0, TeyvatItemStat.StatKind.PERCENT),
            new TeyvatItemStat(ModAttributes.ANEMO_BONUS.get(), 0, TeyvatItemStat.StatKind.PERCENT),
            new TeyvatItemStat(ModAttributes.GEO_BONUS.get(), 0, TeyvatItemStat.StatKind.PERCENT),
            new TeyvatItemStat(ModAttributes.DENDRO_BONUS.get(), 0, TeyvatItemStat.StatKind.PERCENT),
            new TeyvatItemStat(ModAttributes.PHYSICAL_BONUS.get(), 0, TeyvatItemStat.StatKind.PERCENT),
            new TeyvatItemStat(ModAttributes.ATK.get(), 0, TeyvatItemStat.StatKind.PERCENT),
            new TeyvatItemStat(ModAttributes.MAX_HP.get(), 0, TeyvatItemStat.StatKind.PERCENT),
            new TeyvatItemStat(ModAttributes.DEF.get(), 0, TeyvatItemStat.StatKind.PERCENT)
    );

    public static TeyvatItemStat generate(ArtifactType type, int star, Random random) {
        LOGGER.info("[ArtifactMainStatGenerator] generate called | type={} | star={}", type, star);
        TeyvatItemStat result = switch (type) {
            case FLOWER -> {
                double base = ArtifactStatData.getMainStatBase(ModAttributes.MAX_HP.get(), TeyvatItemStat.StatKind.FLAT, star);
                yield new TeyvatItemStat(ModAttributes.MAX_HP.get(), base, TeyvatItemStat.StatKind.FLAT);
            }
            case PLUME -> {
                double base = ArtifactStatData.getMainStatBase(ModAttributes.ATK.get(), TeyvatItemStat.StatKind.FLAT, star);
                yield new TeyvatItemStat(ModAttributes.ATK.get(), base, TeyvatItemStat.StatKind.FLAT);
            }
            case SANDS -> generateSands(star, random);
            case GOBLET -> generateGoblet(star, random);
            case CIRCLET -> generateCirclet(star, random);
        };
        LOGGER.info("[ArtifactMainStatGenerator] generated | attr={} | value={} | kind={}",
                result.getAttribute(),
                result.getValue(),
                result.getKind());
        return result;
    }

    private static TeyvatItemStat generateSands(int star, Random random) {
        TeyvatItemStat template = SANDS_POOL.get(random.nextInt(SANDS_POOL.size()));
        LOGGER.info("[ArtifactMainStatGenerator] generateSands picked | attr={}", template.getAttribute());
        return buildFromTemplate(template, star);
    }

    private static TeyvatItemStat generateGoblet(int star, Random random) {
        // 元素伤害加成池：PYRO/HYDRO/ELECTRO/ANEMO/GEO/DENDRO/CRYOBONUS + 物理 + ATK%/HP%/DEF%
        // 简化：先从已注册的元素伤害加成属性里随机，后续补全

        TeyvatItemStat template = GOBLET_POOL.get(random.nextInt(GOBLET_POOL.size()));
        LOGGER.info("[ArtifactMainStatGenerator] generateGoblet picked | attr={}", template.getAttribute());
        return buildFromTemplate(template, star);
    }

    private static TeyvatItemStat generateCirclet(int star, Random random) {
        TeyvatItemStat template = CIRCLET_POOL.get(random.nextInt(CIRCLET_POOL.size()));
        LOGGER.info("[ArtifactMainStatGenerator] generateCirclet picked | attr={}", template.getAttribute());
        return buildFromTemplate(template, star);
    }

    private static TeyvatItemStat buildFromTemplate(TeyvatItemStat template, int star) {
        double base = ArtifactStatData.getMainStatBase(template.getAttribute(), template.getKind(), star);
        LOGGER.info("[ArtifactMainStatGenerator] buildFromTemplate | attr={} | kind={} | star={} | base={}",
                template.getAttribute(), template.getKind(), star, base);
        return new TeyvatItemStat(template.getAttribute(), base, template.getKind());
    }
}