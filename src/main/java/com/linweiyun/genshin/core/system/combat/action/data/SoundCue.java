package com.linweiyun.genshin.core.system.combat.action.data;


import java.util.List;

    public final class SoundCue {

        /** 同一格里怎么播。 */
        public enum PickMode {
            /** 全部播（叠在一起，一般只放一条）。 */
            PLAY_ALL,
            /** 按权重抽一条；抽到静音条目就什么都不播。 */
            PICK_ONE
        }

        public final int delay;
        public final PickMode mode;
        public final List<SoundRef> variants;

        public SoundCue(int delay, PickMode mode, List<SoundRef> variants) {
            this.delay = delay;
            this.mode = mode == null ? PickMode.PLAY_ALL : mode;
            this.variants = variants == null ? List.of() : List.copyOf(variants);
        }

        /** 这一格固定播这些（可多条叠播）。 */
        public static SoundCue play(int delay, String... soundNames) {
            List<SoundRef> refs = new java.util.ArrayList<>(soundNames.length);
            for (String name : soundNames) {
                refs.add(new SoundRef(delay, name, 1.0f, 1.0f));
            }
            return new SoundCue(delay, PickMode.PLAY_ALL, refs);
        }

        /** 这一格按权重从候选里抽一条；候选里可以放 {@link SoundRef#silent()} 表示「不出声」。 */
        public static SoundCue pickOne(int delay, SoundRef... candidates) {
            return new SoundCue(delay, PickMode.PICK_ONE, List.of(candidates));
        }

        /** 这一格不播任何东西（占位用，方便对齐时序）。 */
        public static SoundCue silent(int delay) {
            return new SoundCue(delay, PickMode.PLAY_ALL, List.of());
        }

        public boolean isEmpty() {
            return variants.isEmpty();
        }
    }
