package com.onehumanawa.cfhcore.content.gui.veinminer.client;

import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.veinminer.CFHGuiVeinminer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import org.jetbrains.annotations.Nullable;

final class IrisCompat {
	@Nullable
	private static MethodHandle instance;
	@Nullable
	private static MethodHandle packInUse;
	@Nullable
	private static MethodHandle shadowPass;
	private static boolean resolved;

	static boolean shaderPackInUse() {
		resolve();
		if (instance != null && packInUse != null) {
			try {
				return (boolean)packInUse.invoke((Object)(Object)instance.invoke());
			} catch (Throwable var1) {
				instance = null;
				CFHGuiVeinminer.LOGGER.warn("[CFHGui Veinminer] Iris API call failed; treating shader packs as off ({})", var1.toString());
				return false;
			}
		} else {
			return false;
		}
	}

	static boolean shadowPass() {
		resolve();
		if (instance != null && shadowPass != null) {
			try {
				return (boolean)shadowPass.invoke((Object)(Object)instance.invoke());
			} catch (Throwable var1) {
				shadowPass = null;
				return false;
			}
		} else {
			return false;
		}
	}

	private static void resolve() {
		if (!resolved) {
			resolved = true;
			if (loaded("iris") || loaded("oculus")) {
				try {
					Lookup lookup = MethodHandles.publicLookup();
					Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
					instance = lookup.findStatic(api, "getInstance", MethodType.methodType(api));
					packInUse = lookup.findVirtual(api, "isShaderPackInUse", MethodType.methodType(boolean.class));
					shadowPass = lookup.findVirtual(api, "isRenderingShadowPass", MethodType.methodType(boolean.class));
				} catch (LinkageError | ReflectiveOperationException var2) {
					instance = null;
					CFHGuiVeinminer.LOGGER.warn("[CFHGui Veinminer] Iris is installed but its API was not found; the preview will not adapt to shader packs");
				}
			}
		}
	}

	private static boolean loaded(String id) {
		try {
			return CFHGuiPlatform.get().isModLoaded(id);
		} catch (RuntimeException var2) {
			return false;
		}
	}

	private IrisCompat() {
	}
}
