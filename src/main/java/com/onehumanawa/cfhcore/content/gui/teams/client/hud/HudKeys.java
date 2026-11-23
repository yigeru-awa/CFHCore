package com.onehumanawa.cfhcore.content.gui.teams.client.hud;

import com.mojang.blaze3d.platform.InputConstants.Type;
import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsClient;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamsState;
import java.lang.reflect.Method;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class HudKeys {
	public static final KeyMapping BORDERS_KEY = new KeyMapping("key.cfhgui_teams.borders", Type.KEYSYM, 79, "key.categories.cfh");
	private static int lastKey = -1;
	private static int lastAction = 0;
	@Nullable
	private static Method selectTeam;
	private static boolean selectTeamLooked;

	static void tick() {
		while (TeamsClient.CHAT_KEY.consumeClick()) {
			openTeamChat();
		}
	}

	static void openTeamChat() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen == null && mc.player != null && TeamsState.available()) {
			if (!TeamsState.hasTeam()) {
				mc.gui.setOverlayMessage(Component.translatable("cfhgui_teams.hud.chat.no_team"), false);
			} else {
				mc.setScreen(new ChatScreen(selectTeamTab() ? "" : "/tc "));
			}
		}
	}

	private static boolean selectTeamTab() {
		if (!Modules.isLoaded("cfhgui_chat")) {
			return false;
		} else {
			try {
				if (!selectTeamLooked) {
					selectTeamLooked = true;
					selectTeam = Class.forName("com.onehumanawa.cfhcore.content.gui.chat.client.ChatChannels").getMethod("selectTeam");
				}

				return selectTeam != null && Boolean.TRUE.equals(selectTeam.invoke(null));
			} catch (LinkageError | RuntimeException | ReflectiveOperationException var1) {
				selectTeam = null;
				return false;
			}
		}
	}

	public static void keyEvent(int key, int action) {
		lastKey = key;
		lastAction = action;
	}

	public static boolean closesTeamsMenu(Screen screen, int key, int scancode) {
		if (TeamsClient.HUB_KEY.isUnbound() || !TeamsClient.HUB_KEY.matches(key, scancode)) {
			return false;
		} else if (lastKey == key && lastAction == 2) {
			return false;
		} else if (teamsScreen(screen) && !typing(screen)) {
			screen.onClose();
			return true;
		} else {
			return false;
		}
	}

	private static boolean teamsScreen(Screen screen) {
		return "cfhgui_teams:hub".equals(MenuSlots.slotOf(screen)) || screen.getClass().getName().startsWith("com.onehumanawa.cfhcore.content.gui.teams.");
	}

	private static boolean typing(Screen screen) {
		GuiEventListener focused = screen.getFocused();

		for (int depth = 0; depth < 8 && focused instanceof ContainerEventHandler; depth++) {
			ContainerEventHandler container = (ContainerEventHandler)focused;
			if (container.getFocused() == null) {
				break;
			}

			focused = container.getFocused();
		}

		if (focused instanceof EditBox box && box.isFocused()) {
			return true;
		}

		return false;
	}

	private HudKeys() {
	}
}
