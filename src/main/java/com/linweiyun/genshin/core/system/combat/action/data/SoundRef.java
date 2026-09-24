package com.linweiyun.genshin.core.system.combat.action.data;



    public final class SoundRef {
        public final int delay;
        public final String name;
        public final float volume;
        public final float pitch;
        /** 随机权重；默认 1。想要「七成 A、三成 B」就配 7 和 3，不必凑成 100。 */
        public final int weight;

        public SoundRef(int delay, String name, float volume, float pitch) {
            this(delay, name, volume, pitch, 1);
        }

        public SoundRef(int delay, String name, float volume, float pitch, int weight) {
            this.delay = delay;
            this.name = name;
            this.volume = volume;
            this.pitch = pitch;
            this.weight = Math.max(1, weight);
        }

        /** 静音条目：抽中它这一格就不出声。 */
        public static SoundRef silent() {
            return new SoundRef(0, null, 1.0f, 1.0f, 1);
        }

        /** 设置权重。 */
        public SoundRef weighted(int value) {
            return new SoundRef(delay, name, volume, pitch, value);
        }

        public boolean isSilent() {
            return name == null || name.isEmpty();
        }
    }
