package com.onehumanawa.cfhcore.content.gui.crafting.look;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;

public final class SearchLook {
	private static final int LEFT = 16;
	private static final int RIGHT = 14;
	private static final int GAME_LEFT = 4;
	private static final int DOCK_SIDE = 10;

	public static boolean render(EditBox w, GuiGraphics g, int mouseX, int mouseY, Runnable text) {
		if (!Look.cfh()) {
			if (w.getValue().isEmpty()) {
				w.setSuggestion(I18n.get("emi.search", new Object[0]));
			}

			return false;
		} else {
			Theme t = Theme.current();
			Palette p = t.palette();
			int x = w.getX();
			int y = w.getY();
			int width = w.getWidth();
			int h = w.getHeight();
			boolean focused = w.isFocused();
			float hover = ReskinDraw.hover(w);
			boolean empty = w.getValue().isEmpty();
			Minecraft mc = Minecraft.getInstance();
			int screenW = mc.getWindow().getGuiScaledWidth();
			int screenH = mc.getWindow().getGuiScaledHeight();
			boolean docked = y + h >= screenH - 4 && Math.abs(x + width / 2 - screenW / 2) <= 1;
			if (docked) {
				LookDraw.panelPlate(g, x - 10, y, width + 20, h, 3, screenW, screenH, 1.0F);
			} else {
				CFHGuiDraw.shadow(g, x, y, width, h, 0.45F);
			}

			int fill = focused ? p.surface() : Colors.lerp(p.bg2(), p.surface(), hover);
			int border = focused ? p.accent() : Colors.lerp(p.borderStrong(), p.textDim(), hover);
			CFHGuiDraw.pixelRound(g, x, y, width, h, fill, t.radius());
			CFHGuiDraw.outline(g, x, y, width, h, border, t.radius());
			Icons.draw(g, Icon.SEARCH, x + 4, y + (h - 8) / 2, 8, focused ? p.accentInk() : Colors.lerp(p.textDim(), p.textMuted(), hover));
			if (!empty) {
				boolean over = overClear(w, (double)mouseX, (double)mouseY);
				Icons.draw(g, Icon.CLOSE, x + width - 12, y + (h - 8) / 2, 8, over ? p.text() : p.textDim());
			}

			w.setSuggestion("");
			int dy = (h - 8) / 2;
			g.pose().pushPose();
			g.pose().translate(16.0F, (float)dy, 0.0F);
			if (empty) {
				int room = width - 16 - 6;
				g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(placeholder(room, p), room), x, y, focused ? Colors.withAlpha(p.textDim(), 176) : p.textDim(), false);
			}

			w.setBordered(false);

			try {
				text.run();
			} finally {
				w.setBordered(true);
			}

			g.pose().popPose();
			return true;
		}
	}

	private static Component placeholder(int room, Palette p) {
		Component full = Component.translatable(
			"cfhgui_crafting.look.search.placeholder",
			new Object[]{prefix("@", "mod", p.accent(), p), prefix("#", "tag", p.success(), p), prefix("$", "tooltip", p.warning(), p)}
		);
		return (Component)(CFHGuiDraw.width(full) <= room ? full : Component.translatable("cfhgui_crafting.look.search.placeholder.short"));
	}

	private static Component prefix(String sign, String word, int color, Palette p) {
		int quiet = Colors.mix(color, p.bg2(), 0.45F) & 16777215;
		return Component.literal(sign).append(Component.translatable("cfhgui_crafting.look.search." + word)).withStyle(s -> s.withColor(quiet));
	}

	public static int innerWidth(EditBox w, int game) {
		return Look.cfh() ? Math.max(1, w.getWidth() - 16 - 14) : game;
	}

	public static int textShift(EditBox w) {
		return Look.cfh() ? 12 : 0;
	}

	public static boolean overClear(EditBox w, double mouseX, double mouseY) {
		return Look.cfh()
			&& w.isVisible()
			&& !w.getValue().isEmpty()
			&& mouseX >= (double)(w.getX() + w.getWidth() - 14)
			&& mouseX < (double)(w.getX() + w.getWidth())
			&& mouseY >= (double)w.getY()
			&& mouseY < (double)(w.getY() + w.getHeight());
	}

	public static boolean highlight(EditBox w, GuiGraphics g) {
		if (!Look.cfh()) {
			return false;
		} else {
			Theme t = Theme.current();
			CFHGuiDraw.outline(g, w.getX() - 1, w.getY() - 1, w.getWidth() + 2, w.getHeight() + 2, t.palette().warning(), t.radius() + 1);
			return true;
		}
	}

	public static FormattedCharSequence colors(FormattedCharSequence text) {
		return text != null && Look.cfh() ? sink -> text.accept((index, style, codePoint) -> sink.accept(index, recolor(style), codePoint)) : text;
	}

	private static Style recolor(Style style) {
		TextColor color = style.getColor();
		if (color == null) {
			return style;
		} else {
			Palette p = Look.palette();

			int to = switch (color.getValue()) {
				case 39146 -> Colors.mix(p.success(), p.text(), 0.55F);
				case 4319950 -> Colors.brighten(p.success(), 0.3F);
				case 5592575 -> p.accent();
				case 5635925 -> p.success();
				case 10181851 -> Colors.brighten(p.accent(), 0.3F);
				case 12448902 -> Colors.brighten(p.warning(), 0.3F);
				case 13383479, 16733525 -> p.danger();
				case 15870962 -> Colors.mix(p.accent(), p.text(), 0.55F);
				case 16039302 -> Colors.mix(p.warning(), p.text(), 0.55F);
				case 16558421 -> Colors.mix(p.danger(), p.text(), 0.5F);
				case 16777045 -> p.warning();
				case 16777215 -> p.text();
				default -> 0;
			};
			return to == 0 ? style : style.withColor(to & 16777215);
		}
	}

	private SearchLook() {
	}
}
