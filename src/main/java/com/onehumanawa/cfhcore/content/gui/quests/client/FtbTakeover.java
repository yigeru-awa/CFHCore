package com.onehumanawa.cfhcore.content.gui.quests.client;

import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import net.minecraft.client.Minecraft;

public final class FtbTakeover {
	private static boolean complained;

	public static boolean active() {
		try {
			return QuestsState.replacesFtb();
		} catch (Throwable var1) {
			return false;
		}
	}

	public static boolean open(Object file) {
		if (!active()) {
			return false;
		} else {
			try {
				if (Boolean.TRUE.equals(file.getClass().getMethod("canEdit").invoke(file))) {
					return false;
				}
			} catch (Throwable var2) {
			}

			try {
				Minecraft mc = Minecraft.getInstance();
				mc.execute(() -> QuestsClient.open(null, null));
				return true;
			} catch (Throwable var3) {
				if (!complained) {
					complained = true;
					CFHGuiQuests.LOGGER.warn("[CFHGui Quests] CFHGui's book could not be opened in the place of FTB Quests' ({}); FTB Quests' own opens", var3.toString());
				}

				return false;
			}
		}
	}

	private FtbTakeover() {
	}
}
