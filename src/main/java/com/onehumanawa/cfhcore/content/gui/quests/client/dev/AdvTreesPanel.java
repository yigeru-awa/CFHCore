package com.onehumanawa.cfhcore.content.gui.quests.client.dev;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiList;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiList.RowRenderer;
import com.onehumanawa.cfhcore.content.gui.quests.client.view.QuestIcons;
import com.onehumanawa.cfhcore.content.gui.quests.server.adv.AdvService;
import com.onehumanawa.cfhcore.content.gui.quests.server.adv.AdvTrees;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

final class AdvTreesPanel {
	private static final int ROW = 18;
	private static final int LINE = 20;
	private static final int GAP = 4;
	private static final int BOX = 10;
	private static String search = "";
	@Nullable
	private CFHGuiList<AdvTreesPanel.Row> list;
	@Nullable
	private CFHGuiSearchField field;
	@Nullable
	private CFHGuiButton convert;
	@Nullable
	private CFHGuiIconButton undo;
	@Nullable
	private CFHGuiIconButton all;
	@Nullable
	private CFHGuiIconButton none;
	@Nullable
	private CFHGuiIconButton again;
	private Rect area = new Rect(0, 0, 0, 0);
	private Rect footer = new Rect(0, 0, 0, 0);
	@Nullable
	private Screen host;
	private int seen = -1;
	private String seenSearch = "";
	private final Map<String, Component> titles = new HashMap<>();
	@Nullable
	private Object titlesOf;
	private final Map<String, List<AdvTrees.Tree>> byMod = new LinkedHashMap<>();
	private final Anim appear = new Anim(0.0F, 220, Ease.OUT_CUBIC);
	private long hoverSinceMs;
	@Nullable
	private String hoverRoot;
	private int countColumn;

	void build(Rect area, Screen host, Consumer<AbstractWidget> add) {
		this.area = area;
		this.host = host;
		int x = area.x();
		int y = area.y();
		int w = area.w();
		int buttons = 66;
		CFHGuiSearchField f = new CFHGuiSearchField(x, y, Math.max(60, w - 66 - 4), s -> search = s);
		f.placeholder(Component.translatable("cfhgui_quests.adv.search"));
		f.setValue(search);
		add.accept(f);
		this.field = f;
		int bx = area.right() - 66 + 2;
		this.all = new CFHGuiIconButton(bx, y, 20, Icon.CHECK, Component.translatable("cfhgui_quests.adv.all"), () -> this.tickShown(true));
		this.all.tip(Component.translatable("cfhgui_quests.adv.all"));
		add.accept(this.all);
		bx += 22;
		this.none = new CFHGuiIconButton(bx, y, 20, Icon.CLOSE, Component.translatable("cfhgui_quests.adv.none"), () -> this.tickShown(false));
		this.none.tip(Component.translatable("cfhgui_quests.adv.none"));
		add.accept(this.none);
		bx += 22;
		this.again = new CFHGuiIconButton(bx, y, 20, Icon.REFRESH, Component.translatable("cfhgui_quests.adv.refresh"), AdvClient::refresh);
		this.again.tip(Component.translatable("cfhgui_quests.adv.refresh.tip"));
		add.accept(this.again);
		int listTop = y + 20 + 4;
		int listBottom = area.bottom() - 20 - 4;
		CFHGuiList<AdvTreesPanel.Row> l = new CFHGuiList(x, listTop, w, Math.max(18, listBottom - listTop), 18, new AdvTreesPanel.Rows());
		l.gap(1);
		l.plainRows();
		l.onActivate(this::toggle);
		add.accept(l);
		this.list = l;
		this.footer = new Rect(x, area.bottom() - 20, clearRight(host, area.right(), area.bottom()) - x, 20);
		boolean wide = this.footer.w() >= 300;
		Component label = Component.translatable(wide ? "cfhgui_quests.adv.next" : "cfhgui_quests.adv.next.short");
		CFHGuiButton c = new CFHGuiButton(0, this.footer.y(), 20, 20, label, this::next).variant(Variant.PRIMARY).icon(Icon.ARROW_RIGHT);
		c.setWidth(c.preferredWidth() + 6);
		c.setX(this.footer.right() - c.getWidth());
		add.accept(c);
		this.convert = c;
		this.undo = new CFHGuiIconButton(c.getX() - 20 - 4, this.footer.y(), 20, Icon.UNDO, Component.translatable("cfhgui_quests.adv.undo"), this::undoLast);
		this.undo.variant(Variant.GHOST);
		add.accept(this.undo);
		this.seen = -1;
		this.appear.snap(Theme.current().motion() > 0.0F ? 0.0F : 1.0F);
		AdvClient.want();
		this.sync();
	}

	static int clearRight(Screen screen, int right, int bottom) {
		if (!CFHGui.config().devMode) {
			return right;
		} else {
			int pencilLeft = screen.width - 24;
			int pencilTop = screen.height - 24;
			return right > pencilLeft - 2 && bottom > pencilTop ? pencilLeft - 4 : right;
		}
	}

	boolean typing() {
		return this.field != null && this.field.isFocused();
	}

	void focusSearch(Screen screen) {
		if (this.field != null && this.field.visible) {
			screen.setFocused(this.field);
			this.field.setFocused(true);
		}
	}

	void tick() {
		AdvClient.want();
		this.sync();
	}

	private void sync() {
		AdvClient.State state = AdvClient.state();
		boolean ready = state == AdvClient.State.READY;
		if (this.list != null) {
			this.list.visible = ready;
		}

		if (this.field != null) {
			this.field.visible = ready;
		}

		if (this.all != null) {
			this.all.visible = ready;
		}

		if (this.none != null) {
			this.none.visible = ready;
		}

		if (this.again != null) {
			this.again.visible = ready || state == AdvClient.State.FAILED;
		}

		if (this.convert != null) {
			this.convert.visible = ready;
		}

		if (this.undo != null) {
			this.undo.visible = ready;
		}

		if (!ready) {
			this.seen = -1;
		} else {
			this.appear.set(1.0F);
			int chosen = this.chosenCount();
			if (this.convert != null) {
				this.convert.active = chosen > 0;
				this.convert.tip(Component.translatable(chosen > 0 ? "cfhgui_quests.adv.next.tip" : "cfhgui_quests.adv.next.none"));
			}

			if (this.undo != null) {
				int n = AdvClient.undoable().size();
				this.undo.active = n > 0;
				this.undo.tip(n > 0 ? Component.translatable("cfhgui_quests.adv.undo.tip", new Object[]{n}) : Component.translatable("cfhgui_quests.adv.undo.none"));
			}

			if (this.seen != AdvClient.version() || !this.seenSearch.equals(search)) {
				this.seen = AdvClient.version();
				this.seenSearch = search;
				this.rows();
			}
		}
	}

	private Component title(AdvTrees.Tree tree) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (this.titlesOf != connection) {
			this.titles.clear();
			this.titlesOf = connection;
		}

		return this.titles.computeIfAbsent(tree.root, r -> AdvText.component(tree.title, tree.root));
	}

	private static Component count(AdvTrees.Tree tree) {
		return Component.translatable(tree.count == 1 ? "cfhgui_quests.adv.count.one" : "cfhgui_quests.adv.count", new Object[]{tree.count});
	}

	private void rows() {
		AdvService.Trees trees = AdvClient.trees();
		List<AdvTreesPanel.Row> rows = new ArrayList<>();
		this.byMod.clear();
		this.countColumn = 0;
		if (trees != null) {
			for (AdvTrees.Tree t : trees.trees) {
				this.byMod.computeIfAbsent(t.namespace, k -> new ArrayList<>()).add(t);
				this.countColumn = Math.max(this.countColumn, CFHGuiDraw.width(count(t)));
			}

			String q = search.trim().toLowerCase(Locale.ROOT);

			for (Entry<String, List<AdvTrees.Tree>> mod : this.byMod.entrySet()) {
				List<AdvTrees.Tree> shown = new ArrayList<>();

				for (AdvTrees.Tree t : mod.getValue()) {
					if (q.isEmpty() || this.title(t).getString().toLowerCase(Locale.ROOT).contains(q) || t.mod.toLowerCase(Locale.ROOT).contains(q) || t.root.contains(q)) {
						shown.add(t);
					}
				}

				if (!shown.isEmpty()) {
					rows.add(new AdvTreesPanel.Row(mod.getKey(), null));

					for (AdvTrees.Tree tx : shown) {
						rows.add(new AdvTreesPanel.Row(mod.getKey(), tx));
					}
				}
			}
		}

		if (this.list != null) {
			this.list.items(rows);
			this.list.emptyText(Component.translatable(trees != null && trees.trees.isEmpty() ? "cfhgui_quests.adv.empty" : "cfhgui_quests.adv.no_match"));
		}
	}

	private int chosenCount() {
		AdvService.Trees trees = AdvClient.trees();
		int n = 0;
		if (trees != null) {
			for (AdvTrees.Tree t : trees.trees) {
				if (AdvClient.chosen().contains(t.root)) {
					n++;
				}
			}
		}

		return n;
	}

	private int ticked(String namespace) {
		List<AdvTrees.Tree> trees = this.byMod.get(namespace);
		if (trees != null && !trees.isEmpty()) {
			int n = 0;

			for (AdvTrees.Tree t : trees) {
				if (AdvClient.chosen().contains(t.root)) {
					n++;
				}
			}

			return n == 0 ? 0 : (n == trees.size() ? 1 : 2);
		} else {
			return 0;
		}
	}

	private void toggle(AdvTreesPanel.Row row) {
		if (row.header()) {
			boolean on = this.ticked(row.namespace()) != 1;
			List<AdvTrees.Tree> trees = this.byMod.get(row.namespace());
			if (trees != null) {
				for (AdvTrees.Tree t : trees) {
					AdvClient.choose(t.root, on);
				}
			}
		} else {
			AdvClient.choose(row.tree().root, !AdvClient.chosen().contains(row.tree().root));
		}

		CFHGuiSounds.tick();
	}

	private void tickShown(boolean on) {
		if (this.list != null) {
			if (on) {
				for (AdvTreesPanel.Row row : this.list.items()) {
					if (!row.header()) {
						AdvClient.choose(row.tree().root, true);
					}
				}
			} else {
				for (String root : new ArrayList<>(AdvClient.chosen())) {
					AdvClient.choose(root, false);
				}
			}
		}
	}

	private void next() {
		AdvService.Trees trees = AdvClient.trees();
		if (trees != null) {
			List<String> roots = new ArrayList<>();

			for (AdvTrees.Tree t : trees.trees) {
				if (AdvClient.chosen().contains(t.root)) {
					roots.add(t.root);
				}
			}

			if (!roots.isEmpty()) {
				Minecraft.getInstance().setScreen(new AdvConvertScreen(this.host, roots));
			}
		}
	}

	private void undoLast() {
		AdvClient.undo(problem -> {
			if (problem == null) {
				CFHGuiToasts.show(Component.translatable("cfhgui_quests.adv.undone"), Component.translatable("cfhgui_quests.adv.undone.body"), Icon.UNDO);
			} else {
				CFHGuiToasts.show(Component.translatable("cfhgui_quests.adv.failed"), problem, Icon.WARNING);
			}
		});
	}

	void render(GuiGraphics g, int mouseX, int mouseY) {
		Theme t = Theme.current();
		Palette p = t.palette();
		boolean van = t.isVanilla();
		AdvClient.State state = AdvClient.state();
		if (state != AdvClient.State.READY) {
			this.said(g, state, van, p);
		} else {
			if (this.list == null || !this.list.contains((double)mouseX, (double)mouseY)) {
				this.hoverRoot = null;
			}

			if (this.list != null) {
				int lx = this.list.getX() - 1;
				int ly = this.list.getY() - 1;
				int lw = this.list.getWidth() + 2;
				int lh = this.list.getHeight() + 2;
				if (van) {
					g.fill(lx, ly, lx + lw, ly + lh, 1879048192);
					CFHGuiDraw.outline(g, lx, ly, lw, lh, 1090519039, 0);
				} else {
					CFHGuiDraw.pixelRound(g, lx, ly, lw, lh, t.panel(Colors.mix(p.bg(), -16777216, 0.18F)), t.radius());
					CFHGuiDraw.outline(g, lx, ly, lw, lh, p.border(), t.radius());
				}
			}

			AdvService.Trees trees = AdvClient.trees();
			int chosen = 0;
			int advancements = 0;
			if (trees != null) {
				for (AdvTrees.Tree tree : trees.trees) {
					if (AdvClient.chosen().contains(tree.root)) {
						chosen++;
						advancements += tree.count;
					}
				}
			}

			Component line = chosen == 0
				? Component.translatable("cfhgui_quests.adv.chosen.none", new Object[]{trees == null ? 0 : trees.trees.size()})
				: Component.translatable(chosen == 1 ? "cfhgui_quests.adv.chosen.one" : "cfhgui_quests.adv.chosen", new Object[]{chosen, advancements});
			int room = (this.undo != null ? this.undo.getX() : this.footer.right()) - this.footer.x() - 6;
			g.drawString(
				CFHGuiDraw.font(),
				CFHGuiDraw.truncate(line, room),
				this.footer.x() + 1,
				CFHGuiDraw.textY(this.footer.y(), this.footer.h()),
				van ? (chosen > 0 ? -1 : -6250336) : (chosen > 0 ? p.text() : p.textMuted()),
				van
			);
		}
	}

	private void said(GuiGraphics g, AdvClient.State state, boolean van, Palette p) {
		int cx = this.area.centerX();
		int cy = this.area.y() + Math.max(24, this.area.h() / 2 - 22);
		if (state == AdvClient.State.LOADING) {
			CFHGuiSpinner.draw(g, cx - 6, cy - 6, 12, van ? -1 : p.accentInk());
			CFHGuiDraw.textCentered(g, Component.translatable("cfhgui_quests.adv.state.loading"), cx, cy + 14, van ? -6250336 : p.textMuted(), van);
		} else {
			String key = switch (state) {
				case NO_WORLD -> "no_world";
				case NO_SERVER -> "no_server";
				case NO_PERMISSION -> "no_permission";
				default -> "failed";
			};
			Icon icon = state == AdvClient.State.NO_PERMISSION ? Icon.LOCK : (state == AdvClient.State.FAILED ? Icon.WARNING : Icon.TROPHY);
			Icons.draw(g, icon, cx - 8, cy - 8, 16, van ? -1 : p.textDim());
			CFHGuiDraw.textCentered(g, Fonts.heading(Component.translatable("cfhgui_quests.adv.state." + key)), cx, cy + 14, van ? -1 : p.text(), van);
			int y = cy + 28;

			for (FormattedCharSequence line : CFHGuiDraw.font()
				.split(Component.translatable("cfhgui_quests.adv.state." + key + ".body"), Math.max(60, this.area.w() - 24))) {
				g.drawString(CFHGuiDraw.font(), line, cx - CFHGuiDraw.width(line) / 2, y, van ? -6250336 : p.textMuted(), van);
				y += 10;
			}
		}
	}

	static void box(GuiGraphics g, int x, int y, int state, float hover, float alpha) {
		Theme t = Theme.current();
		Palette p = t.palette();
		if (t.isVanilla()) {
			CFHGuiDraw.vanillaCheckbox(g, x, y, 10, state == 1, hover, alpha);
			if (state == 2) {
				g.fill(x + 3, y + 4, x + 10 - 3, y + 6, Colors.scaleAlpha(-1, alpha));
			}
		} else {
			float k = state == 0 ? 0.0F : 1.0F;
			int fill = Colors.lerp(Colors.lerp(p.surface(), p.surfaceHover(), hover), p.accent(), k);
			int border = Colors.lerp(Colors.lerp(p.borderStrong(), p.textDim(), hover), p.accentHover(), k);
			CFHGuiDraw.pixelRound(g, x, y, 10, 10, Colors.scaleAlpha(fill, alpha), 2);
			CFHGuiDraw.outline(g, x, y, 10, 10, Colors.scaleAlpha(border, alpha), 2);
			if (state == 1) {
				Icons.draw(g, Icon.CHECK, x + 1, y + 1, 8, Colors.scaleAlpha(p.accentText(), alpha));
			} else if (state == 2) {
				g.fill(x + 3, y + 4, x + 10 - 3, y + 6, Colors.scaleAlpha(p.accentText(), alpha));
			}
		}
	}

	private static record Row(@Nullable String namespace, @Nullable AdvTrees.Tree tree) {
		boolean header() {
			return this.tree == null;
		}
	}

	private final class Rows implements RowRenderer<AdvTreesPanel.Row> {
		public void render(GuiGraphics g, AdvTreesPanel.Row row, int index, int x, int y, int w, int h, boolean hovered, boolean selected, int mouseX, int mouseY) {
			Theme t = Theme.current();
			Palette p = t.palette();
			boolean van = t.isVanilla();
			float in = Math.max(0.0F, Math.min(1.0F, AdvTreesPanel.this.appear.get() * 1.6F - (float)Math.min(index, 12) * 0.05F));
			if (!(in <= 0.01F)) {
				int ty = CFHGuiDraw.textY(y, h);
				if (hovered || selected) {
					if (van) {
						g.fill(x, y, x + w, y + h, selected ? 1073741824 : 553648127);
					} else {
						CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(t.panel(selected ? p.surfaceActive() : p.surfaceHover()), in), t.radius());
					}

					if (selected && AdvTreesPanel.this.list != null && AdvTreesPanel.this.list.isFocused() && CFHGuiDraw.focusShown()) {
						CFHGuiDraw.outline(g, x, y, w, h, van ? -1 : Colors.withAlpha(p.accent(), 160), van ? 0 : t.radius());
					}
				}

				if (row.header()) {
					List<AdvTrees.Tree> trees = AdvTreesPanel.this.byMod.get(row.namespace());
					int n = trees == null ? 0 : trees.size();
					AdvTreesPanel.box(g, x + 4, y + (h - 10) / 2, AdvTreesPanel.this.ticked(row.namespace()), hovered ? 1.0F : 0.0F, in);
					Component count = Component.translatable(n == 1 ? "cfhgui_quests.adv.trees.one" : "cfhgui_quests.adv.trees", new Object[]{n});
					int cw = CFHGuiDraw.width(count);
					Component name = Fonts.heading(Component.literal(n == 0 ? row.namespace() : trees.get(0).mod));
					g.drawString(CFHGuiDraw.font(), CFHGuiDraw.truncate(name, w - cw - 34), x + 20, ty, Colors.scaleAlpha(van ? -1 : p.text(), in), van);
					g.drawString(CFHGuiDraw.font(), count, x + w - cw - 6, ty, Colors.scaleAlpha(van ? -6250336 : p.textDim(), in), van);
				} else {
					AdvTrees.Tree tree = row.tree();
					boolean on = AdvClient.chosen().contains(tree.root);
					AdvTreesPanel.box(g, x + 14, y + (h - 10) / 2, on ? 1 : 0, hovered ? 1.0F : 0.0F, in);
					QuestIcons.of(tree.icon.isEmpty() ? "icon:trophy" : tree.icon)
						.draw(g, (float)x + 37.0F, (float)y + (float)h / 2.0F, 14.0F, Colors.scaleAlpha(van ? -1 : p.textMuted(), in));
					Component count = AdvTreesPanel.count(tree);
					int right = x + w - 6;
					int cw = CFHGuiDraw.width(count);
					g.drawString(CFHGuiDraw.font(), count, right - cw, ty, Colors.scaleAlpha(van ? -6250336 : p.textMuted(), in), van);
					right -= Math.max(cw, AdvTreesPanel.this.countColumn) + 8;
					if (!tree.converted.isEmpty() && w >= 240) {
						Component there = Component.translatable("cfhgui_quests.adv.in_book");
						int bw = CFHGuiDraw.width(there) + 8;
						CFHGuiDraw.chip(g, there, right - bw, y + (h - 12) / 2, van ? -8323200 : p.success(), in);
						right -= bw + 6;
					}

					int ink = van ? (on ? -1 : -2039584) : (on ? p.text() : Colors.lerp(p.textMuted(), p.text(), hovered ? 1.0F : 0.45F));
					g.drawString(
						CFHGuiDraw.font(), CFHGuiDraw.truncate(AdvTreesPanel.this.title(tree), Math.max(20, right - (x + 48))), x + 48, ty, Colors.scaleAlpha(ink, in), van
					);
					if (hovered) {
						if (!tree.root.equals(AdvTreesPanel.this.hoverRoot)) {
							AdvTreesPanel.this.hoverRoot = tree.root;
							AdvTreesPanel.this.hoverSinceMs = Clock.nowMs();
						} else if (Clock.nowMs() - AdvTreesPanel.this.hoverSinceMs > 350L) {
							List<Component> lines = new ArrayList<>();
							lines.add(AdvTreesPanel.this.title(tree));
							Component about = AdvText.component(tree.description, "");
							if (!about.getString().isBlank()) {
								lines.add(about.copy().withStyle(ChatFormatting.GRAY));
							}

							lines.add(Component.literal(tree.root).withStyle(ChatFormatting.DARK_GRAY));
							if (tree.hidden > 0) {
								lines.add(Component.translatable("cfhgui_quests.adv.hidden", new Object[]{tree.hidden}).withStyle(ChatFormatting.DARK_GRAY));
							}

							if (!tree.converted.isEmpty()) {
								lines.add(Component.translatable("cfhgui_quests.adv.in_book.tip", new Object[]{tree.converted}).withStyle(ChatFormatting.DARK_GRAY));
							}

							CFHGuiTooltips.requestAt(lines, mouseX, mouseY);
						}
					}
				}
			}
		}

		public boolean click(AdvTreesPanel.Row row, int index, int x, int y, int w, int h, double mouseX, double mouseY, int button) {
			if (button != 0) {
				return false;
			} else {
				AdvTreesPanel.this.toggle(row);
				return true;
			}
		}
	}
}
