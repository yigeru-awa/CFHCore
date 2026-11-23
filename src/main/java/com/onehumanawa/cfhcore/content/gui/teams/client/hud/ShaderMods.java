package com.onehumanawa.cfhcore.content.gui.teams.client.hud;

import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import org.jetbrains.annotations.Nullable;

final class ShaderMods {
	@Nullable
	private static MethodHandle instance;
	@Nullable
	private static MethodHandle shadowPass;
	private static boolean resolved;

	static boolean shadowPass() {
		if (!resolved) {
			resolve();
		}

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
		resolved = true;

		try {
			if (!CFHGuiPlatform.get().isModLoaded("iris") && !CFHGuiPlatform.get().isModLoaded("oculus")) {
				return;
			}

			Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			Lookup lookup = MethodHandles.publicLookup();
			instance = lookup.findStatic(api, "getInstance", MethodType.methodType(api));
			shadowPass = lookup.findVirtual(api, "isRenderingShadowPass", MethodType.methodType(boolean.class));
		} catch (LinkageError | RuntimeException | ReflectiveOperationException var2) {
			instance = null;
			shadowPass = null;
		}
	}

	private ShaderMods() {
	}
}
