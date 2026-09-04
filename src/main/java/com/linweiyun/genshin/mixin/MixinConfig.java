package com.linweiyun.genshin.mixin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinConfig implements IMixinConfigPlugin {

    private static final String TEYVAT_INTERFACE = "com/linweiyun/genshin/content/entities/teyvat/TeyvatLivingEntity";

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        boolean skipTeyvat = mixinClassName.endsWith("MonsterLevelMixin")
                || mixinClassName.endsWith("LivingEntityHurtMixin");
        if (!skipTeyvat) return true;
        return !implementsTeyvat(targetClassName.replace('.', '/'));
    }

    private static boolean implementsTeyvat(String internalName) {
        String current = internalName;
        while (current != null && !current.equals("java/lang/Object")) {
            try {
                ClassReader reader = new ClassReader(current);
                ClassNode node = new ClassNode();
                reader.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                if (node.interfaces != null) {
                    for (String iface : node.interfaces) {
                        if (TEYVAT_INTERFACE.equals(iface)) return true;
                    }
                }
                current = node.superName;
            } catch (Exception e) {
                break;
            }
        }
        return false;
    }
}