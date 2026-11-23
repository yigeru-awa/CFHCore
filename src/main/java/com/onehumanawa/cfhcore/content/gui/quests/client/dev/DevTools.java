package com.onehumanawa.cfhcore.content.gui.quests.client.dev;

import com.onehumanawa.cfhcore.content.gui.config.api.DeveloperSections;
import com.onehumanawa.cfhcore.content.gui.config.api.DeveloperSections.Panel;
import com.onehumanawa.cfhcore.content.gui.config.api.DeveloperSections.Section;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.quests.CFHGuiQuests;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.BookExtensions;
import com.onehumanawa.cfhcore.content.gui.quests.client.ui.QuestBookScreen;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public final class DevTools {
	private static boolean initialised;

	public static synchronized void init() {
		if (!initialised) {
			initialised = true;
			CFHGuiEvents.CLIENT_LEFT_SERVER.register(AdvClient::left);
			BookExtensions.register(new DevTools.InBook());
			CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:adv", AdvTreesScreen::new);
			CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:adv/game", p -> new AdvConvertScreen(p, List.of("minecraft")));
			CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:adv/all", p -> new AdvConvertScreen(p, List.of("*")));
			String tree = System.getProperty("cfh.quests.advTree");
			if (tree != null && !tree.isBlank()) {
				CoreActions.SCREEN_FACTORIES.put("cfhgui_quests:adv/tree", p -> new AdvConvertScreen(p, List.of(tree.trim())));
			}

			if (CFHGuiPlatform.get().isModLoaded("cfhgui_config")) {
				try {
					DevTools.ConfigSide.register();
				} catch (Throwable var2) {
					CFHGuiQuests.LOGGER.warn("[CFHGui Quests] the Developer page of CFHGui Config could not be given its section ({})", var2.toString());
				}
			}
		}
	}

	public static boolean shown() {
		return CFHGui.config().devMode && QuestsState.canEdit();
	}

	private DevTools() {
	}

	private static final class ConfigSide {
		static void register() {
			final AdvTreesPanel panel = new AdvTreesPanel();
			DeveloperSections.register(new Section("advancements", Component.translatable("cfhgui_quests.adv.title"), Icon.TROPHY, 10, new Panel() {
				public void build(SidebarScreen screen, Rect area) {
					panel.build(area, screen, screen::addPageWidget);
				}

				public void render(SidebarScreen screen, GuiGraphics g, Rect area, int mouseX, int mouseY, float partialTick) {
					panel.render(g, mouseX, mouseY);
				}

				public void tick() {
					panel.tick();
				}
			}));
		}
	}

	private static final class InBook implements BookExtensions.Extension {
		@Override
		public void header(QuestBookScreen screen, Consumer<AbstractWidget> add) {
			if (DevTools.shown()) {
				CFHGuiIconButton b = new CFHGuiIconButton(
					0, 0, 20, Icon.TROPHY, Component.translatable("cfhgui_quests.adv.open"), () -> Minecraft.getInstance().setScreen(new AdvTreesScreen(screen))
				);
				b.tip(Component.translatable("cfhgui_quests.adv.open.tip"));
				add.accept(b);
			}
		}

		@Override
		public boolean keyPressed(QuestBookScreen screen, int keyCode, int scanCode, int modifiers) {
			boolean ctrl = (modifiers & (Minecraft.ON_OSX ? 8 : 2)) != 0;
			boolean shift = (modifiers & 1) != 0;
			if (keyCode == 90 && ctrl && !shift && DevTools.shown() && AdvClient.undoes(screen.shown())) {
				int chapters = AdvClient.lastSize();
				AdvClient.undo(
					problem -> {
						if (problem == null) {
							CFHGuiToasts.show(
								Component.translatable("cfhgui_quests.adv.undone"), Component.translatable("cfhgui_quests.adv.undone.n", new Object[]{chapters}), Icon.UNDO
							);
						} else {
							CFHGuiToasts.show(Component.translatable("cfhgui_quests.adv.failed"), problem, Icon.WARNING);
						}
					}
				);
				return true;
			} else {
				return false;
			}
		}
	}
}
