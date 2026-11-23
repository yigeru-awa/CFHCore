package com.onehumanawa.cfhcore.content.gui.veinminer.client;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.veinminer.VeinShape;
import com.onehumanawa.cfhcore.content.gui.veinminer.config.VeinClientConfig;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinFinder;
import com.onehumanawa.cfhcore.content.gui.veinminer.vein.VeinRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class VeinHud {
	private static final int PAD = 4;
	private static final int ICON = 12;
	private static final int ROW_ICON = 12;
	private static final int ROW_GAP = 5;
	private static final long ROW_MS = 1600L;
	private static final Anim FADE = new Anim(0.0F, 140, Ease.OUT_CUBIC);
	private static final Anim ROW = new Anim(0.0F, 160, Ease.OUT_CUBIC);
	private static boolean built;
	private static int keyFlags;
	@Nullable
	private static VeinShape keyShape;
	private static int keyCount;
	@Nullable
	private static VeinFinder.Reason keyReason;
	private static Component name = Component.empty();
	@Nullable
	private static Component detail;
	private static boolean detailWarn;

	static void render(GuiGraphics g, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		VeinClientConfig cfg = VeinClientConfig.get();
		if (mc.player != null && !mc.options.hideGui && cfg.hud) {
			boolean recent = Clock.nowMs() - VeinClient.shapeChangedMs() < 1600L && VeinClient.shapeChangedMs() > 0L;
			FADE.set(!VeinClient.wanted() && !recent ? 0.0F : 1.0F);
			ROW.set(recent ? 1.0F : 0.0F);
			float a = FADE.get();
			if (!(a <= 0.01F)) {
				words();
				Theme t = Theme.current();
				Palette p = t.palette();
				boolean vanilla = t.isVanilla();
				VeinShape shape = VeinClient.shape();
				boolean offline = !VeinClient.active() && VeinClient.wanted();
				int nameW = CFHGuiDraw.width(name);
				int detailW = detail == null ? 0 : CFHGuiDraw.width(detail);
				int dotW = detail == null ? 0 : 9;
				int w = 4 + (offline ? 0 : 16) + nameW + dotW + detailW + 4 + 1;
				int h = 16;
				int sw = g.guiWidth();
				int sh = g.guiHeight();
				int x;
				int y;
				switch (cfg.hudPosition()) {
					case BELOW_CROSSHAIR:
						x = sw / 2 - w / 2;
						y = sh / 2 + 12;
						break;
					case ABOVE_HOTBAR:
						x = sw / 2 - w / 2;
						y = sh - 82;
						break;
					case TOP_LEFT:
						x = 6;
						y = 6;
						break;
					default:
						x = sw / 2 + 10;
						y = sh / 2 + 6;
				}

				int slide = Math.round((1.0F - a) * 4.0F);
				int py = y + slide;
				pill(g, x, py, w, 16, a, vanilla, p);
				int cx = x + 4;
				if (!offline) {
					ShapeGlyphs.draw(g, shape, cx, py + 2, Colors.scaleAlpha(vanilla ? -1 : p.accentInk(), a));
					cx += 16;
				}

				int ty = py + 4;
				CFHGuiDraw.text(g, name, cx, ty, Colors.scaleAlpha(offline ? p.textMuted() : p.text(), a), vanilla);
				cx += nameW;
				if (detail != null) {
					CFHGuiDraw.rect(g, cx + 3, ty + 3, 2, 2, Colors.scaleAlpha(vanilla ? -8355712 : p.textDim(), a));
					cx += dotW;
					CFHGuiDraw.text(g, detail, cx, ty, Colors.scaleAlpha(detailWarn ? p.warning() : p.textMuted(), a), vanilla);
				}

				float r = ROW.get() * a;
				if (r > 0.01F && !offline) {
					shapeRow(g, x, py + 16 + 3, shape, r, vanilla, p);
				}
			}
		} else {
			FADE.snap(0.0F);
		}
	}

	private static void shapeRow(GuiGraphics g, int x, int y, VeinShape current, float a, boolean vanilla, Palette p) {
		VeinRules rules = ClientRules.get();
		int n = 0;

		for (VeinShape s : VeinShape.all()) {
			if (rules == null || rules.shapeEnabled(s)) {
				n++;
			}
		}

		if (n > 1) {
			int w = 8 + n * 12 + (n - 1) * 5;
			int h = 20;
			pill(g, x, y, w, 20, a, vanilla, p);
			int cx = x + 4;

			for (VeinShape sx : VeinShape.all()) {
				if (rules == null || rules.shapeEnabled(sx)) {
					boolean on = sx == current;
					ShapeGlyphs.draw(g, sx, cx, y + 4 - 1, Colors.scaleAlpha(on ? (vanilla ? -1 : p.accentInk()) : (vanilla ? -7697782 : p.textDim()), a));
					if (on) {
						CFHGuiDraw.rect(g, cx, y + 20 - 2, 12, 1, Colors.scaleAlpha(vanilla ? -1 : p.accent(), a));
					}

					cx += 17;
				}
			}
		}
	}

	private static void pill(GuiGraphics g, int x, int y, int w, int h, float a, boolean vanilla, Palette p) {
		if (vanilla) {
			CFHGuiDraw.rect(g, x + 1, y, w - 2, h, Colors.scaleAlpha(-535822320, a));
			CFHGuiDraw.rect(g, x, y + 1, w, h - 2, Colors.scaleAlpha(-535822320, a));
			CFHGuiDraw.outline(g, x + 1, y + 1, w - 2, h - 2, Colors.scaleAlpha(1627389951, a), 0);
		} else {
			CFHGuiDraw.popupPlate(g, x, y, w, h, p.surface(), p.border(), 0.45F, a);
		}
	}

	private static void words() {
		boolean active = VeinClient.active();
		boolean serverHas = ClientRules.serverHasMod();
		VeinShape shape = VeinClient.shape();
		boolean target = VeinPreview.hasTarget();
		int count = VeinPreview.count();
		boolean cut = VeinPreview.truncated();
		VeinFinder.Reason reason = VeinPreview.reason();
		boolean tool = VeinPreview.toolLimited();
		boolean hungry = VeinPreview.hungry();
		int flags = (active ? 1 : 0)
			| (serverHas ? 2 : 0)
			| (ClientRules.heard() ? 4 : 0)
			| (target ? 8 : 0)
			| (cut ? 16 : 0)
			| (tool ? 32 : 0)
			| (hungry ? 64 : 0)
			| (VeinClient.wanted() ? 128 : 0);
		if (!built || flags != keyFlags || shape != keyShape || count != keyCount || reason != keyReason) {
			built = true;
			keyFlags = flags;
			keyShape = shape;
			keyCount = count;
			keyReason = reason;
			detail = null;
			detailWarn = false;
			if (!active && VeinClient.wanted()) {
				name = Component.translatable(
					!serverHas ? "cfhgui_veinminer.hud.no_server" : (!ClientRules.heard() ? "cfhgui_veinminer.hud.waiting" : "cfhgui_veinminer.hud.not_allowed")
				);
			} else {
				name = shape.label();
				if (active && target) {
					if (count > 0) {
						if (hungry) {
							detail = Component.translatable("cfhgui_veinminer.hud.hungry");
							detailWarn = true;
						} else if (cut) {
							detail = Component.translatable(tool ? "cfhgui_veinminer.hud.count_tool" : "cfhgui_veinminer.hud.count_max", new Object[]{count});
							detailWarn = tool;
						} else {
							detail = Component.literal(Integer.toString(count));
						}
					} else {
						String why = switch (reason) {
							case NEEDS_TOOL -> "needs_tool";
							case DENIED, BLOCK_ENTITY -> "denied";
							case UNBREAKABLE -> "unbreakable";
							case SHAPE_OFF -> "shape_off";
							default -> null;
						};
						if (why != null) {
							detail = Component.translatable("cfhgui_veinminer.hud." + why);
						}
					}
				}
			}
		}
	}

	private VeinHud() {
	}
}
