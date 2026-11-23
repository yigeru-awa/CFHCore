package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import com.onehumanawa.cfhcore.content.gui.crafting.team.client.TeamTrees;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class QuickAccess {
	static final int HEIGHT = 27;
	static final int PITCH = 26;
	static final int RADIUS = 11;
	private int x;
	private int y;
	private int w;
	private final List<QuickAccess.Chip> chips = new ArrayList<>();
	private final Map<Object, QuickAccess.Chip> kept = new HashMap<>();
	private static final Object ALL = new Object();
	@Nullable
	private QuickAccess.Chip hovered;
	@Nullable
	private QuickAccess.Chip pressed;
	private double pressX;
	private boolean dragging;
	private int dragX;

	static boolean any() {
		if (!TeamTrees.quick().isEmpty()) {
			return true;
		} else {
			TreeEntry shown = Trees.shown();

			for (TreeEntry e : Trees.store().all()) {
				if (e != shown || e.saved) {
					return true;
				}
			}

			return false;
		}
	}

	void setBounds(int x, int y, int w) {
		this.x = x;
		this.y = y;
		this.w = w;
	}

	private void collect() {
		int room = Math.max(1, (this.w + 3) / 26);
		List<TreeEntry> pinned = Trees.quick();
		TreeEntry recent = Trees.recent();
		int forPinned = Math.max(0, room - 1 - (recent != null ? 1 : 0));
		this.chips.clear();
		int slot = 0;

		for (int i = 0; i < pinned.size() && i < forPinned; i++) {
			this.chips.add(this.chip(pinned.get(i), false, slot++));
		}

		for (TreeEntry team : TeamTrees.quick()) {
			if (slot < forPinned) {
				this.chips.add(this.chip(team, false, slot++));
			}
		}

		if (recent != null && slot < room - 1) {
			this.chips.add(this.chip(recent, true, slot++));
		}

		if (slot < room) {
			this.chips.add(this.chip(null, false, slot));
		}

		this.kept.keySet().removeIf(key -> this.chips.stream().noneMatch(c -> (c.entry == null ? ALL : c.entry) == key));
	}

	private QuickAccess.Chip chip(@Nullable TreeEntry entry, boolean recent, int slot) {
		Object key = entry == null ? ALL : entry;
		QuickAccess.Chip chip = this.kept.get(key);
		if (chip == null || chip.recent != recent) {
			chip = new QuickAccess.Chip(entry, recent);
			this.kept.put(key, chip);
		}

		chip.slot = slot;
		return chip;
	}

	private int centerX(int slot) {
		return this.x + 11 + 1 + slot * 26;
	}

	private int centerY() {
		return this.y + 13;
	}

	void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		this.collect();
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		TreeEntry shown = Trees.shown();
		int cy = this.centerY();
		this.hovered = null;
		int dropSlot = this.dragging && this.pressed != null ? this.dropSlot() : -1;

		for (QuickAccess.Chip chip : this.chips) {
			int slot = chip.slot;
			if (dropSlot >= 0 && chip != this.pressed && chip.entry != null && !chip.recent) {
				int from = this.pressed.slot;
				if (from < dropSlot && slot > from && slot <= dropSlot) {
					slot--;
				} else if (from > dropSlot && slot >= dropSlot && slot < from) {
					slot++;
				}
			}

			if (!chip.placed) {
				chip.at.snap((float)this.centerX(slot));
				chip.placed = true;
			} else {
				chip.at.set((float)this.centerX(slot));
			}
		}

		for (QuickAccess.Chip chip : this.chips) {
			boolean held = this.dragging && chip == this.pressed;
			int cx = held ? Mth.clamp(this.dragX, this.x + 11, this.x + this.w - 11) : Math.round(chip.at.get());
			boolean over = !Popups.any() && !this.dragging && (mouseX - cx) * (mouseX - cx) + (mouseY - cy) * (mouseY - cy) <= 144;
			if (over) {
				this.hovered = chip;
			}

			chip.hover.set(over || held);
			float hov = chip.hover.get();
			boolean current = chip.entry != null && chip.entry == shown;
			if (!held) {
				this.draw(g, chip, cx, cy, hov, current, delta, van, p);
			}
		}

		if (this.dragging && this.pressed != null) {
			this.draw(g, this.pressed, Mth.clamp(this.dragX, this.x + 11, this.x + this.w - 11), cy - 1, 1.0F, this.pressed.entry == shown, delta, van, p);
		}

		if (this.hovered != null) {
			this.tip(this.hovered);
		}
	}

	private void draw(GuiGraphics g, QuickAccess.Chip chip, int cx, int cy, float hov, boolean current, float delta, boolean van, Palette p) {
		int plate = van ? Colors.lerp(-11908534, -9803158, hov) : Colors.lerp(current ? p.surfaceActive() : p.surface(), p.surfaceHover(), hov);
		CFHGuiDraw.pixelCircle(g, cx, cy, 10, plate);
		int track = van ? -16777216 : Colors.lerp(p.border(), p.borderStrong(), Math.max(hov, current ? 1.0F : 0.0F));
		if (chip.entry == null) {
			TreePaint.ring(g, cx, cy, 11, 1.0F, track);
			Icons.draw(g, Icon.LIST, cx - 4, cy - 4, 8, Colors.lerp(TreePaint.muted(), TreePaint.text(), hov));
		} else {
			float fraction = Trees.fraction(chip.entry);
			boolean unreadable = fraction < 0.0F;
			if (chip.recent) {
				TreePaint.ringDashes(g, cx, cy, 11, 2, 2, 0.0F, track);
			} else {
				TreePaint.ring(g, cx, cy, 11, 1.0F, track);
			}

			if (fraction > 0.0F) {
				TreePaint.ring(g, cx, cy, 11, fraction, fraction >= 1.0F ? TreePaint.success() : TreePaint.accent());
			}

			EmiStack icon = chip.entry.icon();
			if (icon != null) {
				icon.render(g, cx - 8, cy - 8, delta, 1);
			}

			g.pose().pushPose();
			g.pose().translate(0.0F, 0.0F, 260.0F);
			if (unreadable) {
				CFHGuiDraw.pixelCircle(g, cx, cy, 10, Colors.withAlpha(TreePaint.ground(), 168));
			}

			if (chip.recent) {
				TreePaint.badge(g, cx + 11 - 3, cy + 11 - 3, TreePaint.muted(), TreePaint.CLOCK_SMALL, 1.0F);
			} else if (fraction >= 1.0F) {
				TreePaint.badge(g, cx + 11 - 3, cy + 11 - 3, TreePaint.success(), TreePaint.TICK_SMALL, 1.0F);
			}

			g.pose().popPose();
			if (TeamTrees.isTeam(chip.entry)) {
				TeamTrees.mark(g, cx, cy, 11);
			}

			if (current) {
				g.fill(cx - 3, cy + 11 + 2, cx + 4, cy + 11 + 3, TreePaint.accent());
			}
		}
	}

	private void tip(QuickAccess.Chip chip) {
		List<Component> lines = new ArrayList<>();
		if (chip.entry == null) {
			lines.add(Component.translatable("cfhgui_crafting.tree.quick.all"));
		} else {
			lines.add(chip.entry.name());
			float fraction = Trees.fraction(chip.entry);
			TreeSession s = chip.entry.session();
			if (!(fraction < 0.0F) && s != null) {
				lines.add(
					(s.complete()
							? Component.translatable("cfhgui_crafting.tree.quick.done")
							: TreeTips.counted("cfhgui_crafting.tree.quick.progress", (long)s.steps(), Math.round(fraction * 100.0F), s.stepsDone(), s.steps()))
						.withStyle(st -> st.withColor((s.complete() ? TreePaint.success() : TreePaint.muted()) & 16777215))
				);
			} else {
				lines.add(Component.translatable("cfhgui_crafting.tree.quick.unknown").withStyle(st -> st.withColor(TreePaint.warning() & 16777215)));
			}

			lines.add(
				Component.translatable(chip.recent ? "cfhgui_crafting.tree.quick.recent" : "cfhgui_crafting.tree.quick.keys")
					.withStyle(st -> st.withColor(TreePaint.dim() & 16777215))
			);
			TeamTrees.tip(chip.entry, lines);
		}

		CFHGuiTooltips.request(lines, null);
	}

	private int dropSlot() {
		int pinned = 0;

		for (QuickAccess.Chip chip : this.chips) {
			if (chip.entry != null && !chip.recent && !TeamTrees.isTeam(chip.entry)) {
				pinned++;
			}
		}

		return Mth.clamp(Math.round((float)(this.dragX - this.x - 11 - 1) / 26.0F), 0, Math.max(0, pinned - 1));
	}

	boolean contains(double mx, double my) {
		return mx >= (double)this.x && mx < (double)(this.x + this.w) && my >= (double)this.y && my < (double)(this.y + 27);
	}

	boolean mouseClicked(double mx, double my, int button) {
		if (!this.contains(mx, my)) {
			return false;
		} else {
			QuickAccess.Chip chip = this.hovered;
			if (chip == null) {
				return true;
			} else if (button == 1) {
				if (TeamTrees.isTeam(chip.entry)) {
					TeamTrees.menu(chip.entry, mx, my);
				} else if (chip.entry != null) {
					TreeMenus.entry(chip.entry, mx, my);
				}

				return true;
			} else if (button != 0) {
				return false;
			} else {
				this.pressed = chip;
				this.pressX = mx;
				this.dragging = false;
				return true;
			}
		}
	}

	boolean mouseDragged(double mx, double my) {
		if (this.pressed == null) {
			return false;
		} else {
			if (!this.dragging && this.pressed.entry != null && !this.pressed.recent && !TeamTrees.isTeam(this.pressed.entry) && Math.abs(mx - this.pressX) > 4.0) {
				this.dragging = true;
			}

			this.dragX = (int)mx;
			return this.dragging;
		}
	}

	boolean mouseReleased(double mx, double my, int button) {
		QuickAccess.Chip chip = this.pressed;
		if (chip != null && button == 0) {
			this.pressed = null;
			if (this.dragging) {
				this.dragging = false;
				if (chip.entry != null) {
					this.dragX = (int)mx;
					Trees.store().move(chip.entry, this.dropSlot());
					Trees.store().save();
					chip.at.snap((float)Mth.clamp(this.dragX, this.x + 11, this.x + this.w - 11));
					CFHGuiSounds.tick();
				}

				return true;
			} else {
				if (chip.entry == null) {
					CFHGuiSounds.click();
					SavedTreesPopup.open(this.centerX(chip.slot) - 11, this.y + 27);
				} else if (TeamTrees.isTeam(chip.entry)) {
					if (TeamTrees.open(chip.entry)) {
						CFHGuiSounds.click();
					}
				} else if (chip.entry != Trees.shown() && Trees.show(chip.entry)) {
					CFHGuiSounds.click();
				}

				return true;
			}
		} else {
			return false;
		}
	}

	int[] placeOf(int slot) {
		return new int[]{this.centerX(slot), this.centerY()};
	}

	void hidden() {
		this.hovered = null;
		this.pressed = null;
		this.dragging = false;
	}

	private static final class Chip {
		@Nullable
		final TreeEntry entry;
		final boolean recent;
		int slot;
		final Anim at = new Anim(0.0F, 160, Ease.OUT_CUBIC);
		final Anim hover = new Anim(0.0F, 120, Ease.OUT_CUBIC);
		boolean placed;

		Chip(@Nullable TreeEntry entry, boolean recent) {
			this.entry = entry;
			this.recent = recent;
		}
	}
}
