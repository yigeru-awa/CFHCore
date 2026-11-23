package com.onehumanawa.cfhcore.content.gui.shops.client.ui.kit;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiAvatar;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiAvatar.Status;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public final class Ui {
	private static final Map<String, ItemStack> STACKS = new HashMap<>();

	public static void text(GuiGraphics g, Component text, int x, int y, int color, float alpha) {
		if (!(alpha <= 0.004F)) {
			g.drawString(CFHGuiDraw.font(), text, x, y, Colors.scaleAlpha(color, alpha), Theme.current().isVanilla());
		}
	}

	public static void text(GuiGraphics g, FormattedCharSequence text, int x, int y, int color, float alpha) {
		if (!(alpha <= 0.004F)) {
			g.drawString(CFHGuiDraw.font(), text, x, y, Colors.scaleAlpha(color, alpha), Theme.current().isVanilla());
		}
	}

	public static void text(GuiGraphics g, String text, int x, int y, int color, float alpha) {
		text(g, Component.literal(text), x, y, color, alpha);
	}

	public static int textFit(GuiGraphics g, Component text, int x, int y, int width, int color, float alpha) {
		if (width <= 4) {
			return 0;
		} else {
			FormattedCharSequence seq = CFHGuiDraw.truncate(text, width);
			text(g, seq, x, y, color, alpha);
			return CFHGuiDraw.width(seq);
		}
	}

	public static int textFit(GuiGraphics g, String text, int x, int y, int width, int color, float alpha) {
		return textFit(g, Component.literal(text), x, y, width, color, alpha);
	}

	public static void textRight(GuiGraphics g, Component text, int right, int y, int color, float alpha) {
		text(g, text, right - CFHGuiDraw.width(text), y, color, alpha);
	}

	public static void textCentered(GuiGraphics g, Component text, int cx, int y, int color, float alpha) {
		text(g, text, cx - CFHGuiDraw.width(text) / 2, y, color, alpha);
	}

	public static int wrapped(GuiGraphics g, Component text, int x, int y, int width, int maxLines, int color, float alpha) {
		List<FormattedCharSequence> lines = CFHGuiDraw.font().split(text, Math.max(20, width));
		int n = Math.min(lines.size(), maxLines);

		for (int i = 0; i < n; i++) {
			text(g, lines.get(i), x, y + i * 10, color, alpha);
		}

		return n * 10;
	}

	public static int wrappedHeight(Component text, int width, int maxLines) {
		return Math.min(CFHGuiDraw.font().split(text, Math.max(20, width)).size(), maxLines) * 10;
	}

	public static int strong() {
		Theme t = Theme.current();
		return t.isVanilla() ? -1 : t.palette().text();
	}

	public static int soft() {
		return Theme.current().palette().textMuted();
	}

	public static int caption() {
		Theme t = Theme.current();
		return t.isVanilla() ? t.palette().textMuted() : t.palette().textDim();
	}

	public static void card(GuiGraphics g, int x, int y, int w, int h, float lift, float alpha) {
		if (!(alpha <= 0.004F) && w > 0 && h > 0) {
			Theme t = Theme.current();
			Palette p = t.palette();
			if (t.isVanilla()) {
				CFHGuiDraw.rect(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(1610612736, Integer.MIN_VALUE, lift), alpha));
				CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(-12566464, -6250336, lift), alpha), 0);
			} else {
				CFHGuiDraw.plate(g, x, y, w, h, Colors.lerp(p.surface(), p.surfaceHover(), lift), Colors.lerp(p.border(), p.borderStrong(), lift), 0.3F + 0.3F * lift, alpha);
			}
		}
	}

	public static void well(GuiGraphics g, int x, int y, int w, int h, float alpha) {
		if (!(alpha <= 0.004F) && w > 0 && h > 0) {
			Theme t = Theme.current();
			Palette p = t.palette();
			if (t.isVanilla()) {
				CFHGuiDraw.rect(g, x, y, w, h, Colors.scaleAlpha(-1073741824, alpha));
				CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(-16777216, alpha), 0);
				CFHGuiDraw.outline(g, x - 1, y - 1, w + 2, h + 2, Colors.scaleAlpha(-7631989, alpha), 0);
			} else {
				CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(Colors.brighten(p.bg(), -0.25F), alpha), t.radius());
				CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(p.border(), alpha), t.radius());
			}
		}
	}

	public static void wash(GuiGraphics g, int x, int y, int w, int h, int rgb, float alpha) {
		if (!(alpha <= 0.004F)) {
			int strength = Theme.current().isVanilla() ? 64 : 48;
			CFHGuiDraw.hgradient(g, x, y, w, h, Colors.scaleAlpha(Colors.withAlpha(rgb, strength), alpha), Colors.withAlpha(rgb, 0));
		}
	}

	public static void rule(GuiGraphics g, int x, int y, int w, float alpha) {
		Theme t = Theme.current();
		CFHGuiDraw.hline(g, x, y, w, Colors.scaleAlpha(t.isVanilla() ? 1090519039 : t.palette().border(), alpha));
	}

	public static int opaque(int rgb) {
		return 0xFF000000 | rgb;
	}

	public static int ink(int rgb) {
		int c = opaque(rgb);
		float floor = Theme.current().isVanilla() ? 0.6F : 0.42F;
		float l = Colors.luminance(c);
		if (l < floor) {
			c = Colors.brighten(c, Math.min(0.75F, (floor - l) * 1.6F));
		}

		return c;
	}

	public static int chip(GuiGraphics g, Component label, int x, int y, int color, float alpha) {
		return CFHGuiDraw.chip(g, label, x, y, color, alpha);
	}

	public static int chipWidth(Component label) {
		return CFHGuiDraw.width(label) + 8;
	}

	public static int chipRight(GuiGraphics g, Component label, int right, int y, int color, float alpha) {
		int w = chipWidth(label);
		CFHGuiDraw.chip(g, label, right - w, y, color, alpha);
		return right - w;
	}

	public static void bar(GuiGraphics g, int x, int y, int w, int h, float fraction, int color, float alpha) {
		if (!(alpha <= 0.004F) && w > 0) {
			Theme t = Theme.current();
			int track = t.isVanilla() ? -16777216 : t.palette().surfaceActive();
			CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(track, alpha), 1);
			int fw = Math.round((float)w * Math.max(0.0F, Math.min(1.0F, fraction)));
			if (fw > 0) {
				CFHGuiDraw.pixelRound(g, x, y, fw, h, Colors.scaleAlpha(color, alpha), 1);
				if (h >= 3 && fw > 2) {
					CFHGuiDraw.hline(g, x + 1, y, fw - 2, Colors.scaleAlpha(Colors.withAlpha(16777215, 56), alpha));
				}
			}
		}
	}

	public static void head(GuiGraphics g, UUID id, String name, int x, int y, int size, @Nullable Status status, float alpha) {
		if (!(alpha <= 0.004F)) {
			CFHGuiAvatar.draw(g, CFHGuiAvatar.skinFor(id, name), x, y, size, status == null ? Status.NONE : status, alpha);
		}
	}

	@Nullable
	public static Icon glyph(@Nullable String mark) {
		if (mark != null && !mark.isEmpty() && mark.indexOf(58) < 0) {
			try {
				return Icon.valueOf(mark.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException var2) {
				return null;
			}
		} else {
			return null;
		}
	}

	public static ItemStack stack(@Nullable String mark) {
		return mark != null && mark.indexOf(58) >= 0 ? STACKS.computeIfAbsent(mark, id -> {
			ResourceLocation rl = ResourceLocation.tryParse(id);
			if (rl == null) {
				return ItemStack.EMPTY;
			} else {
				Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(Items.AIR);
				return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
			}
		}) : ItemStack.EMPTY;
	}

	public static void item(GuiGraphics g, ItemStack stack, int x, int y, int size, float alpha) {
		if (!stack.isEmpty() && !(alpha <= 0.3F)) {
			if (size == 16) {
				g.renderItem(stack, x, y);
			} else {
				float s = (float)size / 16.0F;
				g.pose().pushPose();
				g.pose().translate((float)x, (float)y, 0.0F);
				g.pose().scale(s, s, 1.0F);
				g.renderItem(stack, 0, 0);
				g.pose().popPose();
			}
		}
	}

	public static void markTile(GuiGraphics g, @Nullable String mark, Icon fallback, int x, int y, int size, int rgb, float alpha) {
		if (!(alpha <= 0.004F)) {
			Theme t = Theme.current();
			int c = opaque(rgb);
			if (t.isVanilla()) {
				CFHGuiDraw.rect(g, x, y, size, size, Colors.scaleAlpha(-1610612736, alpha));
				CFHGuiDraw.outline(g, x, y, size, size, Colors.scaleAlpha(Colors.withAlpha(c, 192), alpha), 0);
			} else {
				CFHGuiDraw.pixelRound(g, x, y, size, size, Colors.scaleAlpha(Colors.withAlpha(c, 48), alpha), 2);
				CFHGuiDraw.outline(g, x, y, size, size, Colors.scaleAlpha(Colors.withAlpha(c, 144), alpha), 2);
			}

			ItemStack stack = stack(mark);
			if (!stack.isEmpty()) {
				int is = size >= 22 ? 16 : Math.max(8, size - 6);
				item(g, stack, x + (size - is) / 2, y + (size - is) / 2, is, alpha);
			} else {
				Icon icon = glyph(mark);
				int gs = size >= 20 ? 12 : 8;
				Icons.draw(g, icon == null ? fallback : icon, x + (size - gs) / 2, y + (size - gs) / 2, gs, Colors.scaleAlpha(ink(rgb & 16777215), alpha));
			}
		}
	}

	public static void glyphTile(GuiGraphics g, Icon icon, int x, int y, int size, int color, float alpha) {
		markTile(g, icon.name(), icon, x, y, size, color & 16777215, alpha);
	}

	public static void figure(GuiGraphics g, Component caption, Component value, int x, int y, int w, int valueColor, float alpha) {
		textFit(g, caption, x, y, w, caption(), alpha);
		textFit(g, Fonts.heading(value), x, y + 11, w, valueColor, alpha);
	}

	public static int empty(GuiGraphics g, Rect area, Icon icon, Component title, @Nullable Component body, float alpha) {
		Theme t = Theme.current();
		Palette p = t.palette();
		int bodyW = Math.min(area.w() - 16, 260);
		List<FormattedCharSequence> lines = body == null ? List.of() : CFHGuiDraw.font().split(body, Math.max(40, bodyW));
		int h = 42 + lines.size() * 10;
		int y = area.y() + Math.max(0, (area.h() - h) / 2 - 8);
		int cx = area.centerX();
		if (alpha > 0.004F) {
			int plate = t.isVanilla() ? 1342177280 : Colors.withAlpha(p.surface(), 255);
			CFHGuiDraw.pixelCircle(g, cx, y + 12, 12, Colors.scaleAlpha(plate, alpha));
			if (!t.isVanilla()) {
				CFHGuiDraw.pixelRing(g, cx, y + 12, 12, Colors.scaleAlpha(p.border(), alpha));
			}

			Icons.draw(g, icon, cx - 6, y + 6, 12, Colors.scaleAlpha(t.isVanilla() ? -4144960 : p.textMuted(), alpha));
		}

		y += 30;
		Component head = Fonts.heading(title);
		text(g, CFHGuiDraw.truncate(head, area.w() - 8), cx - Math.min(area.w() - 8, CFHGuiDraw.width(head)) / 2, y, strong(), alpha);
		y += 14;

		for (FormattedCharSequence line : lines) {
			text(g, line, cx - CFHGuiDraw.width(line) / 2, y, p.textMuted(), alpha);
			y += 10;
		}

		return y + 6;
	}

	public static int emptyButtonsY(int areaH, @Nullable Component body, int width) {
		int bodyW = Math.min(width - 16, 260);
		int lines = body == null ? 0 : CFHGuiDraw.font().split(body, Math.max(40, bodyW)).size();
		int h = 42 + lines * 10;
		return Math.max(0, (areaH - h) / 2 - 8) + 30 + 14 + lines * 10 + 6;
	}

	public static int mouseX() {
		Minecraft mc = Minecraft.getInstance();
		return (int)(mc.mouseHandler.xpos() * (double)mc.getWindow().getGuiScaledWidth() / (double)Math.max(1, mc.getWindow().getScreenWidth()));
	}

	public static int mouseY() {
		Minecraft mc = Minecraft.getInstance();
		return (int)(mc.mouseHandler.ypos() * (double)mc.getWindow().getGuiScaledHeight() / (double)Math.max(1, mc.getWindow().getScreenHeight()));
	}

	private Ui() {
	}
}
