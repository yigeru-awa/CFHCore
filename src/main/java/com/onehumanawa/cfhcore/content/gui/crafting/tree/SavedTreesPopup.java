package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popup;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class SavedTreesPopup implements Popup {
	private static final int WIDTH = 196;
	private static final int ROW = 22;
	private static final int PAD = 5;
	private static final int HEAD = 22;
	private static final int CAPTION = 13;
	private static final int MAX_ROWS = 8;
	private int x;
	private int y;
	private int w;
	private int h;
	private final CFHGuiSearchField search;
	private final List<SavedTreesPopup.Line> lines = new ArrayList<>();
	private final Anim open = new Anim(0.0F, 150, Ease.OUT_CUBIC);
	private final Anim scroll = new Anim(0.0F, 140, Ease.OUT_CUBIC);
	private int hovered = -1;
	private boolean overPin;
	private boolean closing;
	private int stored = -1;

	static void open(int atX, int atY) {
		Popups.open(new SavedTreesPopup(atX, atY));
	}

	private SavedTreesPopup(int atX, int atY) {
		Minecraft mc = Minecraft.getInstance();
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		this.w = Math.min(196, sw - 8);
		this.search = new CFHGuiSearchField(0, 0, this.w - 10, text -> {
			this.collect();
			this.scroll.snap(0.0F);
		});
		this.search.setHeight(16);
		this.search.placeholder(Component.translatable("cfhgui_crafting.tree.all.search"));
		this.search.setFocused(true);
		this.collect();
		this.h = this.height();
		this.x = Mth.clamp(atX, 4, Math.max(4, sw - 4 - this.w));
		this.y = atY + this.h > sh - 4 ? Math.max(4, sh - 4 - this.h) : atY;
		this.open.snap(0.0F);
		this.open.set(1.0F);
	}

	private int listHeight() {
		int total = 0;

		for (SavedTreesPopup.Line line : this.lines) {
			total += line.entry == null ? 13 : 22;
		}

		return total;
	}

	private int height() {
		return 27 + Math.max(28, Math.min(this.listHeight(), 176)) + 5;
	}

	private void collect() {
		String typed = this.search.getValue().trim().toLowerCase(Locale.ROOT);
		this.lines.clear();
		List<TreeEntry> saved = new ArrayList<>();
		List<TreeEntry> recent = new ArrayList<>();

		for (TreeEntry e : Trees.store().saved()) {
			if (matches(e, typed)) {
				saved.add(e);
			}
		}

		for (TreeEntry ex : Trees.store().recent()) {
			if (matches(ex, typed)) {
				recent.add(ex);
			}
		}

		if (!saved.isEmpty()) {
			this.lines.add(new SavedTreesPopup.Line(null, Component.translatable("cfhgui_crafting.tree.all.saved")));

			for (TreeEntry exx : saved) {
				this.lines.add(new SavedTreesPopup.Line(exx, null));
			}
		}

		if (!recent.isEmpty()) {
			this.lines.add(new SavedTreesPopup.Line(null, Component.translatable("cfhgui_crafting.tree.all.recent")));

			for (TreeEntry exx : recent) {
				this.lines.add(new SavedTreesPopup.Line(exx, null));
			}
		}

		this.stored = Trees.store().all().size();
	}

	private static boolean matches(TreeEntry e, String typed) {
		return typed.isEmpty() || e.name().getString().toLowerCase(Locale.ROOT).contains(typed);
	}

	private int listTop() {
		return this.y + 22 + 5;
	}

	private int listBottom() {
		return this.y + this.h - 5;
	}

	private int maxScroll() {
		return Math.max(0, this.listHeight() - (this.listBottom() - this.listTop()));
	}

	public boolean contains(double mx, double my) {
		return !this.closing && mx >= (double)this.x && mx < (double)(this.x + this.w) && my >= (double)this.y && my < (double)(this.y + this.h);
	}

	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		float o = Mth.clamp(this.open.get(), 0.0F, 1.0F);
		if (!(o <= 0.01F)) {
			if (this.stored != Trees.store().all().size()) {
				this.collect();
			}

			int dy = Math.round((1.0F - o) * -4.0F);
			g.pose().pushPose();
			g.pose().translate(0.0F, (float)dy, 0.0F);
			if (van) {
				g.fill(this.x, this.y, this.x + this.w, this.y + this.h, Colors.scaleAlpha(-268435456, o));
				CFHGuiDraw.outline(g, this.x, this.y, this.w, this.h, Colors.scaleAlpha(-1, o), 0);
			} else {
				CFHGuiDraw.popupPlate(g, this.x, this.y, this.w, this.h, p.surface(), p.borderStrong(), 0.6F, o);
			}

			Theme.enterPanel();

			try {
				this.search.setX(this.x + 5);
				this.search.setY(this.y + 5 - 1);
				this.search.setAlpha(o);
				this.search.render(g, mouseX, mouseY - dy, partialTick);
				int top = this.listTop();
				int bottom = this.listBottom();
				this.scroll.set(Mth.clamp(this.scroll.target(), 0.0F, (float)this.maxScroll()));
				int scrolled = Math.round(this.scroll.get());
				this.hovered = -1;
				this.overPin = false;
				g.enableScissor(this.x, top + dy, this.x + this.w, bottom + dy);
				if (this.lines.isEmpty()) {
					CFHGuiDraw.textCentered(
						g,
						Component.translatable(this.search.getValue().isBlank() ? "cfhgui_crafting.tree.all.empty" : "cfhgui_crafting.tree.all.none"),
						this.x + this.w / 2,
						top + 8,
						Colors.scaleAlpha(TreePaint.dim(), o),
						van
					);
				}

				TreeEntry shown = Trees.shown();
				int ly = top - scrolled;

				for (int i = 0; i < this.lines.size(); i++) {
					SavedTreesPopup.Line line = this.lines.get(i);
					int lh = line.entry == null ? 13 : 22;
					if (ly + lh > top && ly < bottom) {
						if (line.entry == null) {
							g.drawString(CFHGuiDraw.font(), Fonts.heading(line.caption), this.x + 5 + 1, ly + 3, Colors.scaleAlpha(TreePaint.dim(), o), van);
						} else {
							boolean over = !this.closing
								&& mouseX >= this.x + 2
								&& mouseX < this.x + this.w - 2
								&& mouseY - dy >= Math.max(ly, top)
								&& mouseY - dy < Math.min(ly + lh, bottom);
							if (over) {
								this.hovered = i;
								this.overPin = line.entry.saved && mouseX >= this.x + this.w - 20;
							}

							this.row(g, line.entry, ly, over, line.entry == shown, o, partialTick, van, p);
						}
					}

					ly += lh;
				}

				g.disableScissor();
				if (this.maxScroll() > 0) {
					int view = bottom - top;
					int barH = Math.max(10, view * view / Math.max(1, this.listHeight()));
					int by = top + (int)((float)(view - barH) * (this.scroll.get() / (float)Math.max(1, this.maxScroll())));
					CFHGuiDraw.rect(g, this.x + this.w - 3, by, 2, barH, Colors.scaleAlpha(van ? -8355712 : p.borderStrong(), o));
				}
			} finally {
				Theme.exitPanel();
			}

			g.pose().popPose();
		}
	}

	private void row(GuiGraphics g, TreeEntry entry, int ly, boolean over, boolean shown, float o, float partialTick, boolean van, Palette p) {
		if (over || shown) {
			CFHGuiDraw.pixelRound(
				g, this.x + 2, ly, this.w - 4, 22, Colors.scaleAlpha(van ? (over ? -12566464 : -14013910) : (over ? p.surfaceHover() : p.surfaceActive()), o), van ? 0 : 2
			);
		}

		if (shown) {
			g.fill(this.x + 2, ly + 3, this.x + 4, ly + 22 - 3, Colors.scaleAlpha(TreePaint.accent(), o));
		}

		float fraction = Trees.fraction(entry);
		EmiStack icon = entry.icon();
		if (icon != null && o > 0.6F) {
			icon.render(g, this.x + 5 + 3, ly + 3, partialTick, 1);
		}

		int tx = this.x + 5 + 24;
		int right = this.x + this.w - (entry.saved ? 22 : 6);
		Component state = fraction < 0.0F
			? Component.translatable("cfhgui_crafting.tree.all.unknown")
			: (fraction >= 1.0F ? Component.translatable("cfhgui_crafting.tree.all.done") : Component.literal(Math.round(fraction * 100.0F) + "%"));
		int sw = CFHGuiDraw.width(state);
		g.drawString(
			CFHGuiDraw.font(),
			state,
			right - sw,
			ly + 7,
			Colors.scaleAlpha(fraction < 0.0F ? TreePaint.warning() : (fraction >= 1.0F ? TreePaint.success() : TreePaint.muted()), o),
			van
		);
		g.drawString(
			CFHGuiDraw.font(),
			CFHGuiDraw.truncate(entry.name(), right - sw - 6 - tx),
			tx,
			ly + 7,
			Colors.scaleAlpha(fraction < 0.0F ? TreePaint.dim() : TreePaint.text(), o),
			van
		);
		if (entry.saved) {
			Icons.draw(
				g,
				Icon.PIN,
				this.x + this.w - 16,
				ly + 7,
				8,
				Colors.scaleAlpha(entry.pinned ? TreePaint.accent() : (over && this.overPin ? TreePaint.text() : TreePaint.dim()), o)
			);
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.search.mouseClicked(mouseX, mouseY, button)) {
			return true;
		} else if (this.hovered >= 0 && this.hovered < this.lines.size()) {
			TreeEntry entry = this.lines.get(this.hovered).entry;
			if (entry == null) {
				return true;
			} else if (button == 1) {
				TreeMenus.entry(entry, mouseX, mouseY);
				return true;
			} else if (this.overPin) {
				entry.pinned = !entry.pinned;
				Trees.store().touch();
				Trees.store().save();
				CFHGuiSounds.tick();
				return true;
			} else {
				if (Trees.show(entry)) {
					CFHGuiSounds.click();
					Popups.close(this);
				}

				return true;
			}
		} else {
			return true;
		}
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		this.scroll.set((float)Mth.clamp((double)this.scroll.target() - scrollY * 22.0, 0.0, (double)this.maxScroll()));
		return true;
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256) {
			return false;
		} else if (keyCode != 257 && keyCode != 335) {
			this.search.keyPressed(keyCode, scanCode, modifiers);
			return true;
		} else {
			for (SavedTreesPopup.Line line : this.lines) {
				if (line.entry != null && Trees.show(line.entry)) {
					Popups.close(this);
					break;
				}
			}

			return true;
		}
	}

	public boolean charTyped(char c, int modifiers) {
		this.search.charTyped(c, modifiers);
		return true;
	}

	public boolean beginClose() {
		if (Theme.current().motion() <= 0.0F) {
			return false;
		} else {
			this.closing = true;
			this.open.set(0.0F, 100);
			return true;
		}
	}

	public boolean closeFinished() {
		return this.open.get() <= 0.02F;
	}

	private static record Line(@Nullable TreeEntry entry, @Nullable Component caption) {
	}
}
