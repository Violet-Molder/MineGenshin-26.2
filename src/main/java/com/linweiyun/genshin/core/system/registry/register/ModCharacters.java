package com.linweiyun.genshin.core.system.registry.register;

import com.linweiyun.genshin.core.character.PGCharacter;
import com.linweiyun.genshin.core.character.catalyst.columbina.Columbina;
import com.linweiyun.genshin.core.character.polearm.arlecchino.Arlecchino;
import com.linweiyun.genshin.core.character.polearm.raiden_shogun.RaidenShogun;
import com.linweiyun.genshin.core.character.polearm.shenhe.Shenhe;
import com.linweiyun.genshin.core.character.polearm.test.TestCharacter;
import com.linweiyun.genshin.core.system.registry.ModRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ModCharacters {

    public static final DeferredRegister<PGCharacter> CHARACTERS = ModRegistries.CHARACTERS;

    public static final DeferredHolder<PGCharacter, Shenhe> SHENHE = CHARACTERS.register("shenhe", Shenhe::new);

    public static final DeferredHolder<PGCharacter, Arlecchino> ARLECCHINO = CHARACTERS.register("arlecchino", Arlecchino::new);

    public static final DeferredHolder<PGCharacter, Columbina> COLUMBINA = CHARACTERS.register("columbina", Columbina::new);
    public static final DeferredHolder<PGCharacter, RaidenShogun> RAIDEN_SHOGUN = CHARACTERS.register("raiden_shogun", RaidenShogun::new);

    public static final DeferredHolder<PGCharacter, TestCharacter> TEST = CHARACTERS.register("test", TestCharacter::new);

    private static final Map<Integer, Supplier<PGCharacter>> FACTORIES = new LinkedHashMap<>();

    private static final Map<Identifier, Supplier<PGCharacter>> FACTORIES_BY_ID = new LinkedHashMap<>();

    static {
        FACTORIES.put(135001, Shenhe::new);
        FACTORIES.put(135002, Arlecchino::new);
        FACTORIES.put(145001, Columbina::new);
        FACTORIES.put(135003, RaidenShogun::new);
        FACTORIES.put(135005, TestCharacter::new);

        FACTORIES_BY_ID.put(Identifier.fromNamespaceAndPath("minegenshin", "shenhe"), Shenhe::new);
        FACTORIES_BY_ID.put(Identifier.fromNamespaceAndPath("minegenshin", "arlecchino"), Arlecchino::new);
        FACTORIES_BY_ID.put(Identifier.fromNamespaceAndPath("minegenshin", "columbina"), Columbina::new);
        FACTORIES_BY_ID.put(Identifier.fromNamespaceAndPath("minegenshin", "raiden_shogun"), RaidenShogun::new);
        FACTORIES_BY_ID.put(Identifier.fromNamespaceAndPath("minegenshin", "test"), TestCharacter::new);
    }

    public static PGCharacter getByUUID(int uuid) {
        Supplier<PGCharacter> factory = FACTORIES.get(uuid);
        if (factory == null) return null;
        PGCharacter instance = factory.get();
        Double baseHP = null, baseATK = null, baseDEF = null;
        for (Map.Entry<Identifier, Supplier<List<? extends Integer>>> entry : instance.getStatGrowthMap().entrySet()) {
            List<? extends Integer> list = entry.getValue().get();
            if (list != null && !list.isEmpty()) {
                double first = list.get(0);
                if (entry.getKey().equals(ModAttributes.MAX_HP.getId())) baseHP = first;
                else if (entry.getKey().equals(ModAttributes.ATK.getId())) baseATK = first;
                else if (entry.getKey().equals(ModAttributes.DEF.getId())) baseDEF = first;
            }
        }
        if (baseHP != null && baseATK != null && baseDEF != null) {
            instance.getData().initBaseStats(baseHP, baseATK, baseDEF);
        }
        return instance;
    }

    public static PGCharacter getById(Identifier id) {
        Supplier<PGCharacter> factory = FACTORIES_BY_ID.get(id);
        if (factory == null) return null;
        PGCharacter instance = factory.get();
        Double baseHP = null, baseATK = null, baseDEF = null;
        for (Map.Entry<Identifier, Supplier<List<? extends Integer>>> entry : instance.getStatGrowthMap().entrySet()) {
            List<? extends Integer> list = entry.getValue().get();
            if (list != null && !list.isEmpty()) {
                double first = list.get(0);
                if (entry.getKey().equals(ModAttributes.MAX_HP.getId())) baseHP = first;
                else if (entry.getKey().equals(ModAttributes.ATK.getId())) baseATK = first;
                else if (entry.getKey().equals(ModAttributes.DEF.getId())) baseDEF = first;
            }
        }
        if (baseHP != null && baseATK != null && baseDEF != null) {
            instance.getData().initBaseStats(baseHP, baseATK, baseDEF);
        }
        return instance;
    }

    public static Collection<PGCharacter> getAllCharacters() {
        return CHARACTERS.getRegistry().get().stream().toList();
    }

    public static void register(IEventBus bus) {
        CHARACTERS.register(bus);
    }
}