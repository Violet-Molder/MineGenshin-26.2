package com.linweiyun.genshin.core;

import com.linweiyun.genshin.core.sync.ModSyncAccessors;
import com.lowdragmc.lowdraglib2.plugin.LDLibPlugin;
import com.lowdragmc.lowdraglib2.plugin.ILDLibPlugin;

@LDLibPlugin
public class GenshinLDLibPlugin implements ILDLibPlugin {

    @Override
    public void onLoad() {
        ModSyncAccessors.register();
    }
}