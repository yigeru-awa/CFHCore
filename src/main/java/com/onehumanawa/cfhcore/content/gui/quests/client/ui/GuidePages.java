package com.onehumanawa.cfhcore.content.gui.quests.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class GuidePages {
	public static boolean available() {
		try {
			return CFHGuiPlatform.get().isModLoaded("patchouli");
		} catch (RuntimeException var1) {
			return false;
		}
	}

	public static String words(String guide) {
		String[] parts = guide.trim().split("\\s+");
		String entry = parts.length > 1 ? parts[1] : parts[0];
		if (entry.indexOf(58) >= 0) {
			entry = entry.substring(entry.indexOf(58) + 1);
		}

		if (entry.indexOf(47) >= 0) {
			entry = entry.substring(entry.lastIndexOf(47) + 1);
		}

		StringBuilder sb = new StringBuilder();

		for (String word : entry.split("[_.]")) {
			if (!word.isEmpty()) {
				if (sb.length() > 0) {
					sb.append(' ');
				}

				sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
			}
		}

		return sb.toString();
	}

	public static void open(String guide) {
		String[] parts = guide.trim().split("\\s+");
		ResourceLocation book = parts.length > 0 ? ResourceLocation.tryParse(parts[0]) : null;
		ResourceLocation entry = parts.length > 1 ? ResourceLocation.tryParse(parts[1]) : null;
		int page = 0;

		try {
			if (parts.length > 2) {
				page = Math.max(0, Integer.parseInt(parts[2]));
			}
		} catch (NumberFormatException var7) {
			page = 0;
		}

		try {
			if (book == null || !available()) {
				throw new IllegalStateException("no such book");
			}

			Object api = Class.forName("vazkii.patchouli.api.PatchouliAPI").getMethod("get").invoke(null);
			Class<?> type = Class.forName("vazkii.patchouli.api.PatchouliAPI$IPatchouliAPI");
			if (entry == null) {
				type.getMethod("openBookGUI", ResourceLocation.class).invoke(api, book);
			} else {
				type.getMethod("openBookEntry", ResourceLocation.class, ResourceLocation.class, int.class).invoke(api, book, entry, page);
			}
		} catch (Throwable var8) {
			CFHGuiQuests.LOGGER.warn("[CFHGui Quests] the guide page \"{}\" could not be opened: {}", guide, var8.toString());
			CFHGuiToasts.show(Component.translatable("cfhgui_quests.sheet.guide.failed"), Component.literal(guide), Icon.WARNING);
		}
	}

	private GuidePages() {
	}
}
