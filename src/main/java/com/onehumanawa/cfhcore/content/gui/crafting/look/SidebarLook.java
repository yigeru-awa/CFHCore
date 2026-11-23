package com.onehumanawa.cfhcore.content.gui.crafting.look;

import com.mojang.blaze3d.platform.Window;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.Bounds;
import com.onehumanawa.cfhcore.content.gui.emi.config.EmiConfig;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarTheme;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarType;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.crafting.team.client.TeamPage;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class SidebarLook {
	private static final int CELL = 18;
	private static final int HEADER = 18;
	private static final Map<EmiScreenManager.SidebarPanel, Anim> PAGE_MARK = new IdentityHashMap<>();
	private static final int LINE = 10;
	private static final int MIN_HINT_WIDTH = 64;
	private static Object lastSpace;
	private static int lastX = Integer.MIN_VALUE;
	private static int lastY = Integer.MIN_VALUE;
	private static long lastSince;

	public static int[] fit(Bounds bounds, int screenW, int screenH, int xMin, int xMax, int yMax, int hPad, int vPad) {
		if (!Look.cfh()) {
			return new int[]{xMin, xMax, yMax};
		} else {
			Window w = Minecraft.getInstance().getWindow();
			double scale = Math.max(1.0, w.getGuiScale());
			int shownW = Math.min(screenW, (int)Math.floor((double)w.getWidth() / scale));
			int shownH = Math.min(screenH, (int)Math.floor((double)w.getHeight() / scale));
			int side = Math.max(3, hPad);
			int foot = Math.max(3, vPad);
			if (bounds.left() <= 0) {
				xMin = Math.max(xMin, side);
			}

			if (bounds.right() >= screenW) {
				xMax = Math.min(xMax, shownW - side);
			}

			if (bounds.bottom() >= screenH) {
				yMax = Math.min(yMax, shownH - foot);
			}

			return new int[]{xMin, xMax, yMax};
		}
	}

	public static int shownHeight(int screenH) {
		Window w = Minecraft.getInstance().getWindow();
		return Math.min(screenH, (int)Math.floor((double)w.getHeight() / Math.max(1.0, w.getGuiScale())));
	}

	public static boolean background(EmiScreenManager.SidebarPanel panel, EmiDrawContext context, int mouseX, int mouseY, float delta) {
		if (!Look.cfh()) {
			return false;
		} else {
			EmiScreenManager.ScreenSpace main = panel.space;
			if (main == null) {
				return true;
			} else {
				GuiGraphics g = context.raw();
				Minecraft mc = Minecraft.getInstance();
				int screenW = mc.getWindow().getGuiScaledWidth();
				int screenH = mc.getWindow().getGuiScaledHeight();
				Palette p = Look.palette();
				Bounds bounds = panel.getBounds();
				boolean dropping = panel.getType() == SidebarType.FAVORITES && !EmiScreenManager.draggedStack.isEmpty();
				int shown = rowsShown(panel);
				boolean hugs = !dropping && hugs(panel);
				List<FormattedCharSequence> hint = hugs && shown == 0 ? hintLines(panel, main) : List.of();
				int rows = hint.isEmpty() ? Math.max(1, shown) : Math.min(Math.max(1, main.th), (hint.size() * 10 + 6 + 18 - 1) / 18);
				int gridBottom = hugs ? main.ty + rows * 18 : bounds.bottom() - panel.theme.verticalPadding;
				int top = bounds.y();
				int height = gridBottom + panel.theme.verticalPadding - top;
				int pad = Math.max(0, 3 - panel.theme.horizontalPadding);
				LookDraw.panelPlate(g, bounds.x(), top, bounds.width(), height, pad, screenW, screenH, 1.0F);
				if (panel.getType() == SidebarType.CHESS) {
					board(g, main, p, 255);
				} else if (panel.theme == SidebarTheme.MODERN) {
					for (EmiScreenManager.ScreenSpace space : panel.getSpaces()) {
						board(g, space, p, 80);
					}
				}

				for (EmiScreenManager.ScreenSpace space : panel.getSpaces()) {
					if (space != main) {
						CFHGuiDraw.hline(g, space.tx, space.ty - 2, space.tw * 18, p.border());
					}
				}

				if (!hint.isEmpty()) {
					int y = main.ty + (rows * 18 - (hint.size() * 10 - 1)) / 2;

					for (FormattedCharSequence line : hint) {
						g.drawString(CFHGuiDraw.font(), line, main.tx + 4, y, p.textDim(), false);
						y += 10;
					}
				}

				if (dropping) {
					dropZone(g, main, p);
				}

				return true;
			}
		}
	}

	private static boolean hugs(EmiScreenManager.SidebarPanel panel) {
		if (panel.getSpaces().size() == 1 && !panel.isSearch() && !panel.hasMultiplePages()) {
			SidebarType type = panel.getType();
			return type != SidebarType.CHESS && type != SidebarType.INDEX;
		} else {
			return false;
		}
	}

	private static int rowsShown(EmiScreenManager.SidebarPanel panel) {
		EmiScreenManager.ScreenSpace space = panel.space;
		if (space != null && space.pageSize > 0) {
			List<? extends EmiIngredient> stacks = space.getStacks();
			int left = Math.max(0, Math.min(space.pageSize, stacks.size() - space.pageSize * panel.page));
			int rows = 0;

			for (int y = 0; y < space.th && left > 0; y++) {
				int w = space.getWidth(y);
				if (w > 0) {
					rows = y + 1;
				}

				left -= w;
			}

			return rows;
		} else {
			return 0;
		}
	}

	private static void board(GuiGraphics g, EmiScreenManager.ScreenSpace space, Palette p, int alpha) {
		for (int y = 0; y < space.th; y++) {
			for (int x = 0; x < space.tw; x++) {
				int c = (x + y) % 2 == 0 ? p.surfaceActive() : p.bg2();
				CFHGuiDraw.rect(g, space.tx + x * 18, space.ty + y * 18, 18, 18, Colors.withAlpha(c, alpha));
			}
		}
	}

	private static List<FormattedCharSequence> hintLines(EmiScreenManager.SidebarPanel panel, EmiScreenManager.ScreenSpace space) {
		Component hint = (Component)(switch (panel.getType()) {
			case FAVORITES -> EmiConfig.favorite.isBound()
			? Component.translatable("cfhgui_crafting.look.empty.favorites", new Object[]{EmiConfig.favorite.getBindText()})
			: Component.translatable("cfhgui_crafting.look.empty.favorites.unbound");
			case CRAFTABLES -> Component.translatable("cfhgui_crafting.look.empty.craftables");
			case LOOKUP_HISTORY -> Component.translatable("cfhgui_crafting.look.empty.lookup_history");
			case CRAFT_HISTORY -> Component.translatable("cfhgui_crafting.look.empty.craft_history");
			case TEAM -> TeamPage.hint();
			default -> null;
		});
		if (hint == null) {
			return List.of();
		} else {
			int room = space.tw * 18 - 8;
			if (room < 64) {
				return List.of();
			} else {
				int most = Math.max(1, (Math.max(1, space.th) * 18 - 6) / 10);
				List<FormattedCharSequence> lines = CFHGuiDraw.font().split(hint, room);
				if (lines.size() <= most) {
					return lines;
				} else {
					return most == 1 ? List.of(CFHGuiDraw.truncate(hint, room)) : lines.subList(0, most);
				}
			}
		}
	}

	private static void dropZone(GuiGraphics g, EmiScreenManager.ScreenSpace space, Palette p) {
		int x = space.tx;
		int y = space.ty;
		int w = space.tw * 18;
		int h = space.th * 18;
		int c = Colors.withAlpha(p.accent(), 160);

		for (int i = 0; i < w; i += 6) {
			CFHGuiDraw.rect(g, x + i, y, Math.min(3, w - i), 1, c);
			CFHGuiDraw.rect(g, x + i, y + h - 1, Math.min(3, w - i), 1, c);
		}

		for (int i = 0; i < h; i += 6) {
			CFHGuiDraw.rect(g, x, y + i, 1, Math.min(3, h - i), c);
			CFHGuiDraw.rect(g, x + w - 1, y + i, 1, Math.min(3, h - i), c);
		}
	}

	public static boolean header(EmiScreenManager.SidebarPanel panel, EmiDrawContext context, int page, int totalPages) {
		if (panel.getType() == SidebarType.TEAM && TeamPage.header(panel, context, page, totalPages)) {
			return true;
		} else if (!Look.cfh()) {
			return false;
		} else {
			EmiScreenManager.ScreenSpace space = panel.space;
			if (space == null) {
				return true;
			} else {
				GuiGraphics g = context.raw();
				Palette p = Look.palette();
				int x = space.tx;
				int w = space.tw * 18;
				int top = space.ty - 18;
				int from = x + 36 + 2;
				int until = x + w - 18 - 2;
				int textY = CFHGuiDraw.textY(top, 16);
				if (totalPages > 1 && until > from) {
					Component full = Component.translatable("cfhgui_crafting.look.page", new Object[]{page + 1, totalPages});
					Component brief = Component.literal(page + 1 + "/" + totalPages);
					Component count = CFHGuiDraw.width(full) <= until - from ? full : (CFHGuiDraw.width(brief) <= until - from ? brief : null);
					if (count != null) {
						CFHGuiDraw.textRight(g, count, until, textY, p.textMuted());
						until -= CFHGuiDraw.width(count) + 6;
					}
				} else if (searching(panel) && until > from) {
					int found = space.getStacks().size();
					Component count = found == 0
						? Component.translatable("cfhgui_crafting.look.found.none")
						: Component.translatable("cfhgui_crafting.look.found", new Object[]{found});
					if (CFHGuiDraw.width(count) <= until - from) {
						CFHGuiDraw.textRight(g, count, until, textY, found == 0 ? p.textDim() : p.textMuted());
						until -= CFHGuiDraw.width(count) + 6;
					}
				}

				Component name = panel.getType().getText();
				int nameW = CFHGuiDraw.width(name);
				if (nameW <= until - from) {
					CFHGuiDraw.text(g, name, from, textY, p.text());
				}

				int lineY = space.ty - 2;
				CFHGuiDraw.hline(g, x, lineY, w, p.border());
				if (totalPages > 1) {
					Anim mark = PAGE_MARK.computeIfAbsent(panel, k -> new Anim((float)page, 160, Ease.OUT_CUBIC));
					if (Math.abs(mark.target() - (float)page) > 1.0F) {
						mark.snap((float)page);
					} else {
						mark.set((float)page);
					}

					int markW = Math.max(6, Math.round((float)w / (float)totalPages));
					int markX = x + Math.round((float)(w - markW) * (mark.get() / (float)Math.max(1, totalPages - 1)));
					CFHGuiDraw.rect(g, markX, lineY - 1, markW, 2, p.accent());
				}

				return true;
			}
		}
	}

	private static boolean searching(EmiScreenManager.SidebarPanel panel) {
		return panel.isSearch() && panel.getType() != SidebarType.CHESS && !EmiScreenManager.search.getValue().isEmpty();
	}

	public static boolean highlight(EmiScreenManager.ScreenSpace space, EmiDrawContext context, int x, int y) {
		if (!Look.cfh()) {
			return false;
		} else {
			GuiGraphics g = context.raw();
			Palette p = Look.palette();
			long now = Clock.nowMs();
			if (space != lastSpace || x != lastX || y != lastY) {
				lastSpace = space;
				lastX = x;
				lastY = y;
				lastSince = now;
			}

			int fadeMs = Theme.current().ms(90);
			float a = fadeMs <= 0 ? 1.0F : Math.min(1.0F, 0.35F + 0.65F * (float)(now - lastSince) / (float)fadeMs);
			boolean pressed = Minecraft.getInstance().mouseHandler.isLeftPressed();
			int fill = pressed ? Colors.withAlpha(p.accent(), 88) : Colors.withAlpha(16777215, 30);
			CFHGuiDraw.pixelRound(g, x, y, 18, 18, Colors.scaleAlpha(fill, a), 2);
			CFHGuiDraw.outline(g, x, y, 18, 18, Colors.scaleAlpha(Colors.withAlpha(p.accent(), 216), a), 2);
			return true;
		}
	}

	private SidebarLook() {
	}
}
