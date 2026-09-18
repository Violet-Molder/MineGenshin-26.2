package com.linweiyun.genshin.core.character.sword.vesna;

import com.linweiyun.genshin.core.character.attachment.CharacterAttachment;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

public class VesnaEnergy extends CharacterAttachment {

    public static final String TYPE_ID = Vesna.VESNA_ENERGY;

    @Persisted(key = "energy")
    private float energy;

    @Persisted(key = "max_energy")
    private float maxEnergy;
    public VesnaEnergy() {
        super(TYPE_ID);
        this.energy = 0;
        this.maxEnergy = 16;
    }
    public float getEnergy() { return energy; }
    public void addEnergy(float v) { energy = Math.min(energy + v, maxEnergy); }
    public void consumeEnergy(float v) { energy = Math.max(energy - v, 0); }

}