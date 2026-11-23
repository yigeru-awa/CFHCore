package com.onehumanawa.cfhcore.content.gui.quests.compat;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class FtbMixins implements IMixinConfigPlugin {
	public void onLoad(String mixinPackage) {
	}

	public String getRefMapperConfig() {
		return null;
	}

	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return present(targetClassName);
	}

	private static boolean present(String className) {
		String resource = className.replace('.', '/') + ".class";

		try {
			ClassLoader own = FtbMixins.class.getClassLoader();
			if (own != null && own.getResource(resource) != null) {
				return true;
			} else {
				ClassLoader context = Thread.currentThread().getContextClassLoader();
				return context != null && context != own && context.getResource(resource) != null ? true : own == null && context == null;
			}
		} catch (Throwable var4) {
			return true;
		}
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
