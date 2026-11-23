package com.onehumanawa.cfhcore.content.gui.crafting.look;

import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStackInteraction;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.Bounds;
import com.onehumanawa.cfhcore.content.gui.emi.config.EmiConfig;
import com.onehumanawa.cfhcore.content.gui.emi.config.HelpLevel;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarPages;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarSide;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarType;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiReloadManager;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.crafting.CFHGuiCrafting;
import com.onehumanawa.cfhcore.content.gui.crafting.tree.TreeDockSeam;
import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

final class LookShots {
	private static final String PREFIX = "cfhgui_crafting:look/";
	@Nullable
	private static Integer realStatus;

	static void init() {
		if (System.getProperty("cfh.autoScreens") != null || Boolean.getBoolean("cfh.autoWorld")) {
			Set<String> ids = new LinkedHashSet<>();

			for (String property : new String[]{"cfh.autoScreens", "cfh.autoWorldScreens"}) {
				for (String entry : System.getProperty(property, "").split(",")) {
					String id = entry.trim();
					int hash = id.indexOf(35);
					if (hash >= 0) {
						id = id.substring(0, hash);
					}

					int at = id.lastIndexOf(64);
					if (at > 0) {
						id = id.substring(0, at);
					}

					if (id.startsWith("cfhgui_crafting:look/")) {
						ids.add(id);
					}
				}
			}

			for (String idx : ids) {
				String rest = idx.substring("cfhgui_crafting:look/".length());
				int slash = rest.indexOf(47);
				if (slash >= 0) {
					String verb = rest.substring(0, slash);
					String arg = rest.substring(slash + 1);
					CoreActions.SCREEN_FACTORIES.put(idx, parent -> {
						try {
							run(verb, arg);
						} catch (RuntimeException var5) {
							CFHGuiCrafting.LOGGER.error("[CFHGui Crafting] harness: {} failed", idx, var5);
						}

						return Minecraft.getInstance().screen;
					});
				}
			}
		}
	}

	private static void run(String verb, String arg) {
		switch (verb) {
			case "style":
				CFHGui.config().skin = "vanilla".equalsIgnoreCase(arg) ? "VANILLA" : "DARK";
				Theme.reload();
				Reskin.invalidate();
				break;
			case "panels":
				CFHGui.config().translucentPanels = "glass".equalsIgnoreCase(arg);
				Theme.reload();
				break;
			case "motion":
				CFHGui.config().motion = Double.parseDouble(arg);
				Theme.reload();
				break;
			case "kind":
				EmiScreenManager.focusSidebarType(SidebarType.fromName(arg.toLowerCase(Locale.ROOT)));
				break;
			case "page":
				EmiScreenManager.SidebarPanel panel = EmiScreenManager.getPanelFor(SidebarSide.RIGHT);
				if (panel != null) {
					panel.scroll(Integer.parseInt(arg));
				}
				break;
			case "drag":
				EmiStack stack = stack(arg);
				if (stack != null) {
					EmiScreenManager.pressedStack = stack;
					EmiScreenManager.draggedStack = stack;
				}
				break;
			case "drop":
				EmiScreenManager.pressedStack = EmiStack.EMPTY;
				EmiScreenManager.draggedStack = EmiStack.EMPTY;
				break;
			case "hide":
				HelpLevel was = EmiConfig.helpLevel;
				EmiConfig.helpLevel = HelpLevel.VERBOSE;
				EmiScreenManager.toggleVisibility(true);
				EmiConfig.helpLevel = was;
				EmiConfig.writeConfig();
				break;
			case "settings":
				Minecraft mc = Minecraft.getInstance();
				mc.tell(() -> Look.openSettings(mc.screen, "now".equals(arg) ? null : arg));
				break;
			case "dock":
				LookShots.StandInDock.INSTANCE.open = "open".equalsIgnoreCase(arg);
				TreeDockSeam.install(LookShots.StandInDock.INSTANCE);
				TreeDockSeam.changed();
				break;
			case "status":
				status(arg);
				break;
			case "chess":
				EmiConfig.rightSidebarSize.values.set(0, 8);
				EmiConfig.rightSidebarSize.values.set(1, 8);
				if (!EmiConfig.rightSidebarPages.pages.stream().anyMatch(page -> page.type == SidebarType.CHESS)) {
					EmiConfig.rightSidebarPages.pages.add(new SidebarPages.SidebarPage(SidebarType.CHESS));
				}

				EmiScreenManager.forceRecalculate();
				EmiScreenManager.focusSidebarType(SidebarType.CHESS);
				break;
			default:
				CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: no such look shot: {}/{}", verb, arg);
		}
	}

	private static void status(String arg) {
		try {
			Field field = EmiReloadManager.class.getDeclaredField("status");
			field.setAccessible(true);
			if ("real".equals(arg)) {
				if (realStatus != null) {
					field.setInt(null, realStatus);
				}

				realStatus = null;
				return;
			}

			if (realStatus == null) {
				realStatus = field.getInt(null);
			}

			EmiReloadManager.reloadWorry = "slow".equals(arg) ? 0L : System.currentTimeMillis() + 60000L;

			field.setInt(null, switch (arg) {
				case "error" -> -1;
				case "waiting" -> 0;
				default -> 1;
			});
		} catch (ReflectiveOperationException var4) {
			CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] harness: the viewer's state cannot be put on: {}", var4.toString());
		}
	}

	@Nullable
	private static EmiStack stack(String item) {
		ResourceLocation id = ResourceLocation.tryParse(item.trim());
		Item found = id == null ? null : (Item)BuiltInRegistries.ITEM.getOptional(id).orElse(null);
		return found == null ? null : EmiStack.of(found);
	}

	private LookShots() {
	}

	private static final class StandInDock implements TreeDockSeam.Dock {
		static final LookShots.StandInDock INSTANCE = new LookShots.StandInDock();
		private static final int WIDTH = 160;
		boolean open;
		private Bounds at = Bounds.EMPTY;

		@Override
		public Bounds layout(Screen screen, Bounds room, List<Bounds> exclusion) {
			this.at = this.open ? new Bounds(0, 0, Math.min(160, Math.max(0, room.width() - 4)), screen.height - 24) : Bounds.EMPTY;
			return this.at;
		}

		@Override
		public void render(EmiDrawContext context, int mouseX, int mouseY, float delta) {
			if (!Look.plate(context.raw(), this.at.x(), this.at.y(), this.at.width(), this.at.height())) {
				context.raw().fill(this.at.x(), this.at.y(), this.at.x() + this.at.width(), this.at.y() + this.at.height(), -1072689136);
			}

			Icons.draw(context.raw(), Icon.LAN, this.at.x() + (this.at.width() - 16) / 2, this.at.y() + (this.at.height() - 16) / 2, 16, Look.palette().textDim());
		}

		@Override
		public EmiStackInteraction hovered(int mouseX, int mouseY) {
			return EmiStackInteraction.EMPTY;
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			return false;
		}

		@Override
		public boolean mouseReleased(double mouseX, double mouseY, int button) {
			return false;
		}

		@Override
		public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
			return false;
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
			return false;
		}

		@Override
		public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			return false;
		}

		@Override
		public boolean charTyped(char chr, int modifiers) {
			return false;
		}

		@Override
		public int reserve(Screen screen) {
			return this.open ? 160 : 0;
		}

		@Override
		public void buttonPressed() {
			this.open = !this.open;
			TreeDockSeam.changed();
		}

		@Override
		public boolean isOpen() {
			return this.open;
		}
	}
}
