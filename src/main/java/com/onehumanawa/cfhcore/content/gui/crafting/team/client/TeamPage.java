package com.onehumanawa.cfhcore.content.gui.crafting.team.client;

import com.mojang.blaze3d.platform.InputConstants.Type;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStackInteraction;
import com.onehumanawa.cfhcore.content.gui.emi.bom.BoM;
import com.onehumanawa.cfhcore.content.gui.emi.config.EmiConfig;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarPages;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarSide;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarType;
import com.onehumanawa.cfhcore.content.gui.emi.input.EmiBind;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiFavorite;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.crafting.team.Pin;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class TeamPage {
	private static final int CELL = 18;
	private static final int HEADER = 18;
	@Nullable
	private static SidebarPages.SidebarPage injected;
	private static boolean refused;
	private static List<TeamPinStack> stacks = List.of();
	private static int builtAt = -1;
	private static int builtGeneration = -1;
	private static final EmiBind MENU = new EmiBind("key.cfhgui_crafting.team.menu", new EmiBind.ModifiedKey(Type.MOUSE.getOrCreate(1), 0));
	private static final EmiBind MENU_SHIFT = new EmiBind("key.cfhgui_crafting.team.menu", new EmiBind.ModifiedKey(Type.MOUSE.getOrCreate(1), 4));
	private static final Map<EmiScreenManager.SidebarPanel, Anim> PAGE_MARK = new IdentityHashMap<>();

	public static List<? extends EmiIngredient> stacks() {
		if (!TeamPinsClient.active()) {
			return List.of();
		} else {
			if (builtAt != TeamPinsClient.version() || builtGeneration != BoM.generation) {
				builtAt = TeamPinsClient.version();
				builtGeneration = BoM.generation;
				List<TeamPinStack> out = new ArrayList<>();

				for (Pin pin : TeamPinsClient.pins()) {
					try {
						TeamPinStack stack = TeamPinStack.of(pin);
						if (stack != null) {
							out.add(stack);
						}
					} catch (RuntimeException var4) {
					}
				}

				stacks = List.copyOf(out);
			}

			return stacks;
		}
	}

	static void changed() {
		try {
			EmiScreenManager.repopulatePanels(SidebarType.TEAM);
		} catch (RuntimeException var1) {
		}
	}

	public static EmiIngredient plain(EmiIngredient stack) {
		return (EmiIngredient)(stack instanceof TeamPinStack pin ? pin.plain() : stack);
	}

	static void teamChanged(boolean other) {
		if (other) {
			refused = false;
		}

		tick();
	}

	static void tick() {
		List<SidebarPages.SidebarPage> pages = EmiConfig.leftSidebarPages.pages;
		if (injected != null && !pages.contains(injected)) {
			injected = null;
			if (TeamPinsClient.active()) {
				refused = true;
			}
		}

		boolean want = TeamPinsClient.active() && !refused;
		if (want && injected == null && !has(pages)) {
			injected = new SidebarPages.SidebarPage(SidebarType.TEAM);
			pages.add(injected);
			relayout();
		} else if (!want && injected != null) {
			int at = pages.indexOf(injected);
			pages.remove(injected);
			injected = null;
			EmiScreenManager.SidebarPanel panel = EmiScreenManager.getPanelFor(SidebarSide.LEFT);
			if (panel != null && panel.sidebarPage >= at && panel.sidebarPage > 0) {
				panel.sidebarPage = 0;
			}

			relayout();
		}
	}

	private static boolean has(List<SidebarPages.SidebarPage> pages) {
		for (SidebarPages.SidebarPage page : pages) {
			if (page.type == SidebarType.TEAM) {
				return true;
			}
		}

		return false;
	}

	private static void relayout() {
		try {
			EmiScreenManager.dockChanged();
		} catch (RuntimeException var1) {
		}
	}

	public static List<SidebarPages.SidebarPage> written(List<SidebarPages.SidebarPage> pages) {
		if (injected != null && pages.contains(injected)) {
			List<SidebarPages.SidebarPage> out = new ArrayList<>(pages);
			out.remove(injected);
			return out;
		} else {
			return pages;
		}
	}

	public static boolean header(EmiScreenManager.SidebarPanel panel, EmiDrawContext context, int page, int totalPages) {
		EmiScreenManager.ScreenSpace space = panel.space;
		if (space != null && TeamPinsClient.active()) {
			GuiGraphics g = context.raw();
			Theme t = Theme.current();
			boolean van = t.isVanilla();
			TeamsView.Look look = TeamPinsClient.look();
			int x = space.tx;
			int w = space.tw * 18;
			int top = space.ty - 18;
			int from = x + 36 + 2;
			int until = x + w - 18 - 2;
			int textY = CFHGuiDraw.textY(top, 16);
			int text = van ? -1 : t.palette().text();
			int muted = van ? -6250336 : t.palette().textMuted();
			if (totalPages > 1 && until > from) {
				Component full = Component.translatable("cfhgui_crafting.look.page", new Object[]{page + 1, totalPages});
				Component brief = Component.literal(page + 1 + "/" + totalPages);
				Component count = CFHGuiDraw.width(full) <= until - from ? full : (CFHGuiDraw.width(brief) <= until - from ? brief : null);
				if (count != null) {
					g.drawString(CFHGuiDraw.font(), count, until - CFHGuiDraw.width(count), textY, muted, van);
					until -= CFHGuiDraw.width(count) + 6;
				}
			}

			int nameX = from;
			if (until - from >= 8) {
				if (!flag(g, from, top + 2)) {
					CFHGuiDraw.rect(g, from, top + 4, 5, 9, look.color());
				}

				nameX = from + 10;
			}

			Component name = (Component)(look.name().isEmpty() ? SidebarType.TEAM.getText() : Component.literal(look.name()));
			if (CFHGuiDraw.width(name) > until - nameX) {
				name = SidebarType.TEAM.getText();
			}

			if (CFHGuiDraw.width(name) <= until - nameX) {
				g.drawString(CFHGuiDraw.font(), name, nameX, textY, text, van);
			}

			int lineY = space.ty - 2;
			g.fill(x, lineY, x + w, lineY + 1, Colors.withAlpha(look.color(), van ? 255 : 208));
			if (totalPages > 1) {
				Anim mark = PAGE_MARK.computeIfAbsent(panel, k -> new Anim((float)page, 160, Ease.OUT_CUBIC));
				if (Math.abs(mark.target() - (float)page) > 1.0F) {
					mark.snap((float)page);
				} else {
					mark.set((float)page);
				}

				int markW = Math.max(6, Math.round((float)w / (float)totalPages));
				int markX = x + Math.round((float)(w - markW) * (mark.get() / (float)Math.max(1, totalPages - 1)));
				g.fill(markX, lineY - 1, markX + markW, lineY + 1, van ? -1 : t.palette().accent());
			}

			return true;
		} else {
			return space == null;
		}
	}

	static boolean flag(GuiGraphics g, int x, int y) {
		try {
			return TeamsView.flag(g, TeamPinsClient.team(), (float)x, (float)y, 6.0F);
		} catch (LinkageError var4) {
			return false;
		}
	}

	@Nullable
	public static Component hint() {
		return TeamPinsClient.active() ? Component.translatable("cfhgui_crafting.team.empty") : Component.translatable("cfhgui_crafting.team.empty.no_team");
	}

	public static boolean interact(EmiStackInteraction stack, Function<EmiBind, Boolean> function) {
		if (TeamPinsClient.active() && stack instanceof EmiScreenManager.SidebarEmiStackInteraction sidebar) {
			SidebarType type = sidebar.getType();
			Minecraft mc = Minecraft.getInstance();
			double mx = mc.mouseHandler.xpos() * (double)mc.getWindow().getGuiScaledWidth() / (double)Math.max(1, mc.getWindow().getScreenWidth());
			double my = mc.mouseHandler.ypos() * (double)mc.getWindow().getGuiScaledHeight() / (double)Math.max(1, mc.getWindow().getScreenHeight());
			if (type == SidebarType.TEAM && stack.getStack() instanceof TeamPinStack pin) {
				if (function.apply(MENU) || function.apply(MENU_SHIFT)) {
					TeamMenus.pin(pin, mx, my);
					return true;
				} else if (pin.pin.isTree() && function.apply(EmiConfig.viewRecipes)) {
					CFHGuiSounds.click();
					TeamTrees.open(pin.pin, false);
					return true;
				} else {
					return false;
				}
			} else if (type == SidebarType.FAVORITES
				&& function.apply(MENU_SHIFT)
				&& stack.getStack() instanceof EmiFavorite fav
				&& !(fav instanceof EmiFavorite.Synthetic)) {
				TeamMenus.favorite(fav, mx, my);
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	private TeamPage() {
	}
}
