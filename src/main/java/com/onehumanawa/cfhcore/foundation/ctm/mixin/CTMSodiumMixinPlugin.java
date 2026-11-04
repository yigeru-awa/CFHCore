package com.onehumanawa.cfhcore.foundation.ctm.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Gates the sodium-only mixin config: every mixin in it targets a sodium class, so a
 * mixin whose target class isn't on the classpath (sodium missing or a different
 * version) is skipped instead of killing the launch.
 * <p>
 * The probe must NOT load the target class: mixin configs are prepared before they are
 * registered with the transformer, so triggering a class load here would define the
 * class un-transformed and the mixin would never apply to it. Checking for the class
 * file as a resource side-steps that entirely.
 * <p>
 * Several classloaders are probed because the plugin itself is loaded by mixin's
 * bootstrap loader, which doesn't necessarily see mod classes.
 */
public class CTMSodiumMixinPlugin implements IMixinConfigPlugin {

	@Override
	public void onLoad(String mixinPackage) {}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		String resource = targetClassName.replace('.', '/') + ".class";
		ClassLoader[] loaders = { Thread.currentThread().getContextClassLoader(),
				CTMSodiumMixinPlugin.class.getClassLoader(), ClassLoader.getSystemClassLoader(),
				modlauncherLoader() };
		for (ClassLoader loader : loaders) {
			if (loader != null && loader.getResource(resource) != null)
				return true;
		}
		return false;
	}

	/** Modlauncher's transforming classloader, which actually carries the mod classes. */
	private static ClassLoader modlauncherLoader() {
		try {
			Class<?> launch = Class.forName("cpw.mods.modlauncher.Launch");
			Object instance = launch.getField("instance").get(null);
			if (instance != null) {
				try {
					return (ClassLoader) launch.getMethod("getClassLoader").invoke(instance);
				} catch (NoSuchMethodException ignored) {
					return (ClassLoader) launch.getField("classLoader").get(instance);
				}
			}
		} catch (Throwable ignored) {}
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
