package com.onehumanawa.cfhcore.content.gui.profile;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule.HubEntry;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.profile.client.ProfileClient;
import com.onehumanawa.cfhcore.content.gui.profile.net.ProfileNet;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CFHGuiProfile implements CFHGuiModule {
	public static final String MOD_ID = "cfhgui_profile";
	public static final Logger LOGGER = LoggerFactory.getLogger("CFHGui Profile");
	public static final CFHGuiProfile MODULE = new CFHGuiProfile();

	private CFHGuiProfile() {
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath("cfhgui_profile", path);
	}

	public String id() {
		return "cfhgui_profile";
	}

	public Component displayName() {
		return Component.translatable("cfhgui_profile.name");
	}

	public Icon icon() {
		return Icon.USER;
	}

	public void init() {
		CFHGui.init();
		ProfileNet.register();
		LOGGER.info("[CFHGui Profile] init");
	}

	public void initClient() {
		CFHGuiProfile.ClientSide.init();
	}

	public List<HubEntry> hubEntries() {
		return CFHGuiPlatform.get().isClient() ? CFHGuiProfile.ClientSide.hubEntries() : List.of();
	}

	private static final class ClientSide {
		static void init() {
			ProfileClient.init();
		}

		static List<HubEntry> hubEntries() {
			return ProfileClient.hubEntries();
		}
	}
}
