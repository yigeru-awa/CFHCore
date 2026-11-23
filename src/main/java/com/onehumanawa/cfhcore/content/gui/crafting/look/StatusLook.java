package com.onehumanawa.cfhcore.content.gui.crafting.look;

import com.onehumanawa.cfhcore.content.gui.emi.config.EmiConfig;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiReloadManager;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public final class StatusLook {
	private static final int CHIP_H = 14;
	private static final Component HIDDEN = Component.translatable("cfhgui_crafting.look.toast.hidden");

	public static boolean reloading(EmiDrawContext context, int x, int screenHeight, int room) {
		if (!Look.cfh()) {
			return false;
		} else {
			int status = EmiReloadManager.getStatus();
			if (status == -1) {
				chip(
					context.raw(),
					x,
					screenHeight,
					room,
					Component.translatable("cfhgui_crafting.look.status.error"),
					Icon.WARNING,
					true,
					Component.translatable("cfhgui_crafting.look.status.error.detail")
				);
			} else if (status == 0) {
				chip(context.raw(), x, screenHeight, room, Component.translatable("cfhgui_crafting.look.status.waiting"), null, false, null);
			} else {
				boolean slow = System.currentTimeMillis() > EmiReloadManager.reloadWorry;
				chip(
					context.raw(),
					x,
					screenHeight,
					room,
					Component.translatable(slow ? "cfhgui_crafting.look.status.slow" : "cfhgui_crafting.look.status.loading"),
					null,
					false,
					EmiReloadManager.reloadStep
				);
			}

			return true;
		}
	}

	public static boolean baking(EmiDrawContext context, int x, int screenHeight, int room) {
		if (!Look.cfh()) {
			return false;
		} else {
			chip(context.raw(), x, screenHeight, room, Component.translatable("cfhgui_crafting.look.status.baking"), null, false, null);
			return true;
		}
	}

	private static void chip(GuiGraphics g, int x, int screenHeight, int room, Component text, @Nullable Icon icon, boolean bad, @Nullable Component detail) {
		Theme t = Theme.current();
		Palette p = t.palette();
		int y = screenHeight - 22 + 3;
		int textRoom = room - x - 4 - 16 - 6;
		int textW = CFHGuiDraw.width(text);
		FormattedCharSequence line = textRoom >= textW ? text.getVisualOrderText() : (textRoom >= 40 ? CFHGuiDraw.truncate(text, textRoom) : null);
		if (line != null) {
			textW = CFHGuiDraw.width(line);
		}

		FormattedCharSequence more = null;
		if (line != null && detail != null && !detail.getString().isEmpty()) {
			int left = textRoom - textW - 8;
			if (CFHGuiDraw.width(detail) <= left) {
				more = detail.getVisualOrderText();
			} else if (!bad && left >= 40) {
				more = CFHGuiDraw.truncate(detail, left);
			}
		}

		int w = 16 + (line == null ? 0 : textW + 6) + (more == null ? 0 : CFHGuiDraw.width(more) + 8);
		int tint = bad ? p.danger() : p.accent();
		CFHGuiDraw.shadow(g, x, y, w, 14, 0.4F);
		CFHGuiDraw.pixelRound(g, x, y, w, 14, p.surface(), 2);
		CFHGuiDraw.outline(g, x, y, w, 14, bad ? Colors.withAlpha(p.danger(), 176) : p.border(), 2);
		if (icon != null) {
			Icons.draw(g, icon, x + 4, y + 3, 8, tint);
		} else {
			CFHGuiSpinner.draw(g, x + 3, y + 2, 10, tint);
		}

		if (line != null) {
			int tx = x + 16;
			g.drawString(CFHGuiDraw.font(), line, tx, CFHGuiDraw.textY(y, 14), bad ? Colors.brighten(p.danger(), 0.25F) : p.text(), false);
			tx += textW + 8;
			if (more != null) {
				g.drawString(CFHGuiDraw.font(), more, tx, CFHGuiDraw.textY(y, 14), p.textDim(), false);
			}
		}
	}

	public static boolean disabledToast() {
		if (!Theme.current().toasts()) {
			return false;
		} else {
			CFHGuiToasts.show(
				HIDDEN, Component.translatable("cfhgui_crafting.look.toast.hidden.key", new Object[]{EmiConfig.toggleVisibility.getBindText()}), Icon.EYE_OFF, null, 8000
			);
			return true;
		}
	}

	public static void viewerBack() {
		CFHGuiToasts.dismiss(HIDDEN);
	}

	private StatusLook() {
	}
}
