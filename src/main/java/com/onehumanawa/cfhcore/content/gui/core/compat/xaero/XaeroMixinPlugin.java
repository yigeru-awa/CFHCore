package com.onehumanawa.cfhcore.content.gui.core.compat.xaero;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class XaeroMixinPlugin implements IMixinConfigPlugin {
   public static boolean hasClass(String className) {
      ClassLoader var10000 = XaeroMixinPlugin.class.getClassLoader();
      String var10001 = className.replace('.', '/');
      return var10000.getResource(var10001 + ".class") != null;
   }

   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      return hasClass(targetClassName);
   }

   public void onLoad(String mixinPackage) {
   }

   public String getRefMapperConfig() {
      return null;
   }

   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   public List<String> getMixins() {
      return null;
   }

   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }
}
