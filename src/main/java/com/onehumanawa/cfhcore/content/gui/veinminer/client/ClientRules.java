package com.onehumanawa.cfhcore.content.gui.veinminer.client;

import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.veinminer.config.VeinServerConfig;
import com.onehumanawa.cfhcore.content.gui.veinminer.net.VeinNet;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinRules;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public final class ClientRules {
	@Nullable
	private static VeinServerConfig synced;
	@Nullable
	private static VeinRules compiled;
	private static boolean allowed;
	private static boolean heard;
	private static int version;

	static void accept(VeinNet.Rules payload) {
		VeinServerConfig c = (VeinServerConfig)JsonWire.decode(payload.data(), VeinServerConfig.class);
		synced = c == null ? null : c.sanitize();
		compiled = null;
		allowed = payload.allowed();
		heard = true;
		version++;
	}

	static void clear() {
		synced = null;
		compiled = null;
		allowed = false;
		heard = false;
		version++;
	}

	@Nullable
	public static VeinRules get() {
		if (!heard) {
			return null;
		} else if (local()) {
			return VeinRules.local();
		} else {
			VeinServerConfig c = synced;
			if (c == null) {
				return null;
			} else {
				if (compiled == null) {
					compiled = VeinRules.of(c);
				}

				return compiled;
			}
		}
	}

	@Nullable
	public static VeinServerConfig config() {
		return local() ? VeinServerConfig.get() : synced;
	}

	public static boolean local() {
		return Minecraft.getInstance().hasSingleplayerServer();
	}

	public static boolean serverHasMod() {
		return Minecraft.getInstance().getConnection() != null && CFHGuiNetwork.get().serverHasChannel(VeinNet.State.TYPE);
	}

	public static boolean allowed() {
		VeinRules r = get();
		return r != null && allowed && r.enabled;
	}

	public static boolean heard() {
		return heard;
	}

	public static int version() {
		return local() ? version * 31 + VeinServerConfig.version() : version;
	}

	private ClientRules() {
	}
}
