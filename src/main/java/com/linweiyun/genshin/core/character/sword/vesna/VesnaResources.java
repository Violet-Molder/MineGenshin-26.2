package com.linweiyun.genshin.core.character.sword.vesna;

import com.linweiyun.genshin.core.system.combat.action.data.CharacterActionData;
import com.linweiyun.genshin.core.system.combat.action.data.CharacterRenderData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VesnaResources {

    public static final String ID = Vesna.ID;

    public static final CharacterRenderData RENDER_DATA = new CharacterRenderData(
            ID,
            "default",
            "default_texture.png",
            "vesna",
            CharacterRenderData.defaultAnimMapping(),
            1.0f
    );

    public static final CharacterActionData ACTION_DATA = buildActionData();

    private static CharacterActionData buildActionData() {
        Map<Integer, CharacterActionData.ActionStep> comboSteps = new LinkedHashMap<>();
        comboSteps.put(1, new CharacterActionData.ActionStep(
                "attack_1", 40, 0, 2,
                List.of(new CharacterActionData.Move(0, 1.2)),
                List.of(new CharacterActionData.Hit(3, 2.0, 1.5, 1.0, 0.0, 6.0, false)),
                List.of(new CharacterActionData.SoundRef(3, "vesna_attack_1", 1.0f, 1.0f)),
                0, 2, 0, 8
        ));
        comboSteps.put(2, new CharacterActionData.ActionStep(
                "attack_2", 48, 0, 2,
                List.of(new CharacterActionData.Move(0, 1.2)),
                List.of(new CharacterActionData.Hit(3, 2.0, 1.5, 2.0, 0.0, 6.0, false)),
                List.of(new CharacterActionData.SoundRef(3, "vesna_attack_2", 1.0f, 1.0f)),
                0, 2, 0, 8
        ));
        comboSteps.put(3, new CharacterActionData.ActionStep(
                "attack_3", 25, 0, 2,
                List.of(new CharacterActionData.Move(0, 1.3)),
                List.of(
                        new CharacterActionData.Hit(3, 3.0, 1.5, 2.0, 0.0, 8.0, false),
                        new CharacterActionData.Hit(6, 0.0, 1.5, 2.0, 0.0, 10.0, false)
                ),
                List.of(new CharacterActionData.SoundRef(3, "vesna_attack_3", 1.0f, 1.0f)),
                0, 3, 0, 8
        ));
        comboSteps.put(4, new CharacterActionData.ActionStep(
                "attack_4", 30, 0, 2,
                List.of(new CharacterActionData.Move(2, 1.5)),
                List.of(
                        new CharacterActionData.Hit(10, 4.0, 1.5, 1.0, 0.0, 8.0, true),
                        new CharacterActionData.Hit(12, 4.0, 1.5, 1.0, 0.0, 8.0, true),
                        new CharacterActionData.Hit(14, 4.0, 1.5, 1.0, 0.0, 8.0, true),
                        new CharacterActionData.Hit(16, 4.0, 1.5, 1.0, 0.0, 8.0, true)
                ),
                List.of(new CharacterActionData.SoundRef(10, "vesna_attack_4", 1.0f, 1.0f)),
                0, 4, 0, 8
        ));
        comboSteps.put(5, new CharacterActionData.ActionStep(
                "attack_5", 50, 0, 2,
                List.of(new CharacterActionData.Move(4, 1.8)),
                List.of(
                        new CharacterActionData.Hit(2, 0.0, 1.5, 1.0, 0.0, 10.0, false),
                        new CharacterActionData.Hit(4, 0.0, 1.5, 1.0, 0.0, 10.0, false),
                        new CharacterActionData.Hit(6, 0.0, 1.5, 1.0, 0.0, 10.0, false),
                        new CharacterActionData.Hit(8, 0.0, 1.5, 1.0, 0.0, 10.0, false)
                ),
                List.of(new CharacterActionData.SoundRef(2, "vesna_attack_5", 1.0f, 1.0f)),
                0, 5, 0, 10
        ));
        CharacterActionData.ActionStep attack6Step = new CharacterActionData.ActionStep(
                "air_attack_long", 80, 0, 2,
                List.of(new CharacterActionData.Move(6, 2.0)),
                List.of(
                        new CharacterActionData.Hit(5, 0.0, 1.5, 2.0, 0.0, 12.0, false),
                        new CharacterActionData.Hit(10, 0.0, 1.5, 2.0, 0.0, 12.0, false),
                        new CharacterActionData.Hit(15, 0.0, 1.5, 2.0, 0.0, 12.0, false),
                        new CharacterActionData.Hit(20, 0.0, 1.5, 2.5, 0.0, 14.0, true)
                ),
                List.of(new CharacterActionData.SoundRef(5, "vesna_attack_6", 1.0f, 1.0f)),
                0, 6, 0, 12
        );
        attack6Step.comboEndAnim = "air_attack_end";
        comboSteps.put(6, attack6Step);

        CharacterActionData.ComboData combo = new CharacterActionData.ComboData(6, comboSteps);

        CharacterActionData.ActionStep skillStep = new CharacterActionData.ActionStep(
                "skill_no_energy", 20, 0, 3,
                List.of(new CharacterActionData.Move(0, 0.8)),
                List.of(new CharacterActionData.Hit(6, 2.0, 1.0, 1.2, 0.0, 2.5, false)),
                List.of(new CharacterActionData.SoundRef(6, "vesna_skill", 1.0f, 1.0f)),
                0, 0, 360, 0
        );

        CharacterActionData.ActionStep burstStep = new CharacterActionData.ActionStep(
                "burst", 40, 40, 4,
                List.of(),
                List.of(new CharacterActionData.Hit(10, 3.0, 1.0, 3.0, 0.0, 4.0, false)),
                List.of(new CharacterActionData.SoundRef(10, "vesna_burst", 1.0f, 1.0f)),
                0, 0, 400, 0
        );

        CharacterActionData.ActionStep dodgeStep = new CharacterActionData.ActionStep(
                // 基准名；实际播放时按输入方向换成 dodge_front / dodge_back / dodge_left / dodge_right
                "dodge_front", 12, 0, 3,
                List.of(new CharacterActionData.Move(0, 2.0)),
                List.of(),
                List.of(new CharacterActionData.SoundRef(0, "vesna_dodge", 1.0f, 1.0f)),
                0, 0, 0, 0
        );

        return new CharacterActionData(
                combo,
                new CharacterActionData.SkillData(skillStep, null),
                new CharacterActionData.BurstData(burstStep, 80f),
                new CharacterActionData.DodgeData(dodgeStep)
        );
    }
}