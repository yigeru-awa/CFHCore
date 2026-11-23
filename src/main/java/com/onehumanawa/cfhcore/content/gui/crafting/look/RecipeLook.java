package com.onehumanawa.cfhcore.content.gui.crafting.look;

import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.Bounds;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.SlotWidget;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarSide;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarType;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiFavorite;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiFavorites;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiPersistentData;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.emi.screen.RecipeScreen;
import com.onehumanawa.cfhcore.content.gui.emi.screen.RecipeTab;
import com.onehumanawa.cfhcore.content.gui.emi.widget.RecipeButtonWidget;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.crafting.tree.TreeDockSeam;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public final class RecipeLook {
	private static final int TAB_ROW = 25;
	private static final int TAB_W = 24;
	private static final int CLOSE = 14;
	private static final int CLOSE_ROOM = 16;
	private static final int TITLE_ICON = 12;
	private static final int RISE = 6;
	private static final int ENTER_MS = 190;
	private static final int SWAP_MS = 110;
	private static final Map<RecipeScreen, RecipeLook.State> STATES = new WeakHashMap<>();
	private static long lastFrameMs;
	private static long resolutionSince;

	private static RecipeLook.State state(RecipeScreen screen) {
		return STATES.computeIfAbsent(screen, s -> {
			RecipeLook.State st = new RecipeLook.State();
			st.swap = Clock.nowMs() - lastFrameMs < 400L;
			return st;
		});
	}

	public static int closeRoom() {
		return Look.cfh() ? 16 : 0;
	}

	public static boolean leftTabs() {
		return Look.cfh();
	}

	public static int windowX(Screen screen, int windowWidth) {
		int reserve = Math.min(TreeDockSeam.reserve(screen), Math.max(0, screen.width - windowWidth));
		return reserve + (screen.width - reserve - windowWidth) / 2;
	}

	private static float entrance(RecipeLook.State st) {
		int ms = Theme.current().ms(st.swap ? 110 : 190);
		return ms <= 0 ? 1.0F : Ease.OUT_CUBIC.apply(Math.min(1.0F, (float)(Clock.nowMs() - st.enteredAt) / (float)ms));
	}

	public static void begin(RecipeScreen screen, EmiDrawContext context) {
		if (Look.cfh()) {
			RecipeLook.State st = state(screen);
			context.push();
			st.pushed++;
			if (!st.swap) {
				context.matrices().translate(0.0F, (float)Math.round((1.0F - entrance(st)) * 6.0F), 0.0F);
			}
		}
	}

	public static void end(RecipeScreen screen, EmiDrawContext context, boolean veil) {
		RecipeLook.State st = STATES.get(screen);
		if (st != null && st.pushed > 0) {
			float in = entrance(st);
			if (veil && in < 1.0F) {
				RecipeLook.Frame f = screen.cfhFrame();
				GuiGraphics g = context.raw();
				g.pose().pushPose();
				g.pose().translate(0.0F, 0.0F, 350.0F);
				CFHGuiDraw.pixelRound(
					g, f.x(), f.y() - 25, f.width(), f.height() + 25, Colors.withAlpha(Look.palette().bg(), Math.round(232.0F * (1.0F - in))), Theme.current().radius()
				);
				g.pose().popPose();
			}

			context.pop();
			st.pushed--;
		}
	}

	public static boolean window(RecipeScreen screen, EmiDrawContext context, int mouseX, int mouseY, float delta) {
		if (!Look.cfh()) {
			return false;
		} else {
			lastFrameMs = Clock.nowMs();
			RecipeLook.Frame f = screen.cfhFrame();
			if (f.tabs().isEmpty()) {
				return false;
			} else {
				RecipeLook.State st = state(screen);
				GuiGraphics g = context.raw();
				Theme t = Theme.current();
				Palette p = t.palette();
				int x = f.x();
				int y = f.y();
				int w = f.width();
				CFHGuiDraw.floatingPanel(g, x, y - 25, w, f.height() + 25, 1.0F);
				Theme.enterPanel();

				try {
					tabRow(g, f, st, p, t, mouseX, mouseY, delta);
					closeButton(g, f, st, mouseX, mouseY);
					titles(g, f, st, p, mouseX, mouseY, delta);
				} finally {
					Theme.exitPanel();
				}

				return true;
			}
		}
	}

	private static int tabX(RecipeLook.Frame f, int index) {
		return f.x() + f.tabOff() + 16 + (index - f.tabPage() * f.tabPageSize()) * 24;
	}

	private static void tabRow(GuiGraphics g, RecipeLook.Frame f, RecipeLook.State st, Palette p, Theme t, int mouseX, int mouseY, float delta) {
		int y = f.y();
		CFHGuiDraw.hline(g, f.x() + 1, y - 1, f.width() - 2, p.border());
		int first = f.tabPage() * f.tabPageSize();
		int last = Math.min(f.tabs().size(), first + f.tabPageSize());
		boolean inRow = mouseY >= y - 24 && mouseY < y;

		for (int i = first; i < last; i++) {
			int tx = tabX(f, i);
			boolean hovered = inRow && mouseX >= tx && mouseX < tx + 24;
			Anim hover = st.tabHover.computeIfAbsent(i, k -> new Anim(0.0F, 140, Ease.OUT_CUBIC));
			hover.set(hovered && i != f.tab());
			float hv = hover.get();
			if (i == f.tab()) {
				CFHGuiDraw.pixelRound(g, tx + 1, y - 23, 22, 20, p.surfaceActive(), t.radius());
			} else if (hv > 0.01F) {
				CFHGuiDraw.pixelRound(g, tx + 1, y - 23, 22, 20, Colors.scaleAlpha(p.surfaceHover(), hv), t.radius());
			}

			f.tabs().get(i).category.render(g, tx + 4, y - 21, delta);
		}

		if (st.underlinePage != f.tabPage()) {
			st.underline.snap((float)f.tab());
			st.underlinePage = f.tabPage();
		} else {
			st.underline.set((float)f.tab());
		}

		float at = st.underline.get();
		if (at >= (float)first - 0.5F && at <= (float)last - 0.5F) {
			int ux = f.x() + f.tabOff() + 16 + Math.round((at - (float)first) * 24.0F);
			CFHGuiDraw.rect(g, ux + 4, y - 2, 16, 2, p.accent());
		}
	}

	private static int closeX(RecipeLook.Frame f) {
		return f.x() + f.width() - 14 - 3;
	}

	private static int closeY(RecipeLook.Frame f) {
		return f.y() - 25 + 5;
	}

	private static boolean overClose(RecipeLook.Frame f, double mouseX, double mouseY) {
		int cx = closeX(f);
		int cy = closeY(f);
		return mouseX >= (double)cx && mouseX < (double)(cx + 14) && mouseY >= (double)cy && mouseY < (double)(cy + 14);
	}

	private static void closeButton(GuiGraphics g, RecipeLook.Frame f, RecipeLook.State st, int mouseX, int mouseY) {
		boolean over = overClose(f, (double)mouseX, (double)mouseY);
		st.closeHover.set(over);
		st.closePress.set(over && Minecraft.getInstance().mouseHandler.isLeftPressed());
		int cx = closeX(f);
		int cy = closeY(f);
		float press = st.closePress.get();
		int fg = LookDraw.face(g, cx, cy, 14, 14, LookDraw.Tone.GHOST, st.closeHover.get(), press, true, 1.0F);
		LookDraw.glyph(g, Icon.CLOSE, cx, cy + LookDraw.nudge(press), 14, 14, 8, fg);
		if (!over) {
			st.closeSince = 0L;
		} else {
			if (st.closeSince == 0L) {
				st.closeSince = Clock.nowMs();
			}

			if (Clock.nowMs() - st.closeSince > 350L) {
				CFHGuiTooltips.request(Component.translatable("cfhgui_crafting.look.close"), (AbstractWidget)null);
			}
		}
	}

	private static void titles(GuiGraphics g, RecipeLook.Frame f, RecipeLook.State st, Palette p, int mouseX, int mouseY, float delta) {
		int x = f.x();
		int y = f.y();
		int cx = x + f.width() / 2;
		RecipeTab tab = f.tabs().get(f.tab());
		boolean hovered = mouseX >= x + 19 + f.buttonOff() && mouseY >= y + 5 && mouseX < x + f.minimumWidth() + f.buttonOff() - 19 && mouseY < y + 5 + 12;
		st.title.set(hovered);
		FormattedCharSequence name = CFHGuiDraw.truncate(Fonts.heading(tab.category.getName()), f.minimumWidth() - 40 - 12 - 4);
		int nameW = CFHGuiDraw.width(name);
		int ix = cx - (16 + nameW) / 2;
		g.pose().pushPose();
		g.pose().translate((float)ix, (float)(y + 5), 0.0F);
		g.pose().scale(0.75F, 0.75F, 1.0F);
		tab.category.render(g, 0, 0, delta);
		g.pose().popPose();
		g.drawString(CFHGuiDraw.font(), name, ix + 12 + 4, y + 7, Colors.lerp(p.text(), p.accentInk(), st.title.get()), false);
		int pages = tab.getPageCount();
		Component count;
		if (pages > 1) {
			count = Component.translatable("cfhgui_crafting.look.page", new Object[]{f.page() + 1, pages});
		} else {
			int recipes = tab.getPage(0).size();
			count = Component.translatable(recipes == 1 ? "cfhgui_crafting.look.recipes.one" : "cfhgui_crafting.look.recipes", new Object[]{recipes});
		}

		CFHGuiDraw.textCentered(g, count, cx, y + 21, p.textMuted());
	}

	public static boolean clicked(RecipeScreen screen, double mouseX, double mouseY, int button) {
		if (Look.cfh() && button == 0 && overClose(screen.cfhFrame(), mouseX, mouseY)) {
			CFHGuiSounds.click();
			screen.onClose();
			return true;
		} else {
			return false;
		}
	}

	public static int buttonGap() {
		return Look.cfh() ? 3 : 0;
	}

	public static int buttonColumnBottom(int height, int count, int upstream) {
		return !Look.cfh() ? upstream : (height - (count * 14 - 2)) / 2 + (count - 1) * 14;
	}

	public static boolean card(GuiGraphics g, int x, int y, int w, int h) {
		if (!Look.cfh()) {
			return false;
		} else {
			LookDraw.card(g, x, y + 1, w, h - 2);
			return true;
		}
	}

	public static void workstations(EmiDrawContext context, RecipeScreen screen, Bounds first, int extent, SidebarSide side) {
		if (side == SidebarSide.LEFT || side == SidebarSide.RIGHT) {
			RecipeLook.Frame f = screen.cfhFrame();
			GuiGraphics g = context.raw();
			int px = side == SidebarSide.LEFT ? first.x() - 5 : f.x() + f.width() - 1;
			CFHGuiDraw.floatingPanel(g, px, first.y() - 5, first.x() + 18 + 5 - px - (side == SidebarSide.LEFT ? 4 : 0), extent, 1.0F);
		}
	}

	public static SlotWidget workstation(EmiIngredient stack, int x, int y) {
		return new RecipeLook.Chip(stack, x, y);
	}

	public static boolean resolution(AbstractWidget w, GuiGraphics g, int state) {
		if (!Look.cfh()) {
			return false;
		} else {
			Palette p = Look.palette();
			int x = w.getX();
			int y = w.getY();
			int width = w.getWidth();
			int height = w.getHeight();
			LookDraw.chip(g, x, y, width, height, w.isHovered() ? 1.0F : 0.0F);
			int ring = state == 18 ? p.danger() : (state == 36 ? p.success() : p.accent());

			for (int i = 2; i < width - 2; i += 4) {
				CFHGuiDraw.rect(g, x + i, y, 2, 1, ring);
				CFHGuiDraw.rect(g, x + i, y + height - 1, 2, 1, ring);
			}

			for (int i = 2; i < height - 2; i += 4) {
				CFHGuiDraw.rect(g, x, y + i, 1, 2, ring);
				CFHGuiDraw.rect(g, x + width - 1, y + i, 1, 2, ring);
			}

			if (!w.isHovered()) {
				resolutionSince = 0L;
			} else {
				if (resolutionSince == 0L) {
					resolutionSince = Clock.nowMs();
				}

				if (Clock.nowMs() - resolutionSince > 350L) {
					CFHGuiTooltips.request(
						List.of(
							Component.translatable("tooltip.emi.resolution"),
							Component.translatable("tooltip.emi.select_resolution"),
							Component.translatable("tooltip.emi.default_resolution"),
							Component.translatable("tooltip.emi.clear_resolution")
						),
						w
					);
				}
			}

			return true;
		}
	}

	public static boolean hasFavoriteButton(EmiRecipe recipe) {
		try {
			return recipe != null && recipe.getId() != null && !recipe.getOutputs().isEmpty() && EmiFavorites.canFavorite(recipe.getOutputs().get(0), recipe);
		} catch (RuntimeException var2) {
			return false;
		}
	}

	public static RecipeButtonWidget favoriteButton(int x, int y, EmiRecipe recipe) {
		return new RecipeLook.FavoriteButton(x, y, recipe);
	}

	private RecipeLook() {
	}

	private static final class Chip extends SlotWidget {
		private final Anim hover = new Anim(0.0F, 140, Ease.OUT_CUBIC);

		Chip(EmiIngredient stack, int x, int y) {
			super(stack, x, y);
		}

		@Override
		public void drawBackground(GuiGraphics draw, int mouseX, int mouseY, float delta) {
			if (!Look.cfh()) {
				super.drawBackground(draw, mouseX, mouseY, delta);
			} else {
				Bounds b = this.getBounds();
				this.hover.set(b.contains(mouseX, mouseY));
				LookDraw.chip(draw, b.x(), b.y(), b.width(), b.height(), this.hover.get());
			}
		}

		@Override
		public void drawSlotHighlight(GuiGraphics draw, Bounds bounds) {
			if (!Look.cfh()) {
				super.drawSlotHighlight(draw, bounds);
			}
		}
	}

	private static final class FavoriteButton extends RecipeButtonWidget {
		FavoriteButton(int x, int y, EmiRecipe recipe) {
			super(x, y, 0, 0, recipe);
		}

		@Nullable
		private EmiFavorite kept() {
			for (EmiFavorite fav : EmiFavorites.favorites) {
				if (fav.getRecipe() == this.recipe) {
					return fav;
				}
			}

			return null;
		}

		@Override
		public void render(GuiGraphics raw, int mouseX, int mouseY, float delta) {
			LookButtons.small(
				this,
				raw,
				this.x,
				this.y,
				this.kept() != null ? Icon.STAR_FILLED : Icon.STAR,
				this.kept() != null ? LookDraw.Tone.ON : LookDraw.Tone.SOLID,
				true,
				this.getBounds().contains(mouseX, mouseY)
			);
		}

		@Override
		public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
			return List.of(
				ClientTooltipComponent.create(
					Component.translatable(this.kept() != null ? "cfhgui_crafting.look.favorite.remove" : "cfhgui_crafting.look.favorite.add").getVisualOrderText()
				)
			);
		}

		@Override
		public boolean mouseClicked(int mouseX, int mouseY, int button) {
			EmiFavorite kept = this.kept();
			if (kept != null) {
				EmiFavorites.favorites.remove(kept);
				EmiPersistentData.save();
			} else {
				List<EmiStack> outputs = this.recipe.getOutputs();
				if (outputs.isEmpty()) {
					return false;
				}

				EmiFavorites.addFavorite(outputs.get(0), this.recipe);
			}

			EmiScreenManager.repopulatePanels(SidebarType.FAVORITES);
			this.playButtonSound();
			return true;
		}
	}

	public static record Frame(
		int x, int y, int width, int height, int minimumWidth, int buttonOff, int tabOff, List<RecipeTab> tabs, int tabPage, int tabPageSize, int tab, int page
	) {
	}

	private static final class State {
		final Anim underline = new Anim(0.0F, 220, Ease.OUT_CUBIC);
		int underlinePage = -1;
		final Anim title = new Anim(0.0F, 140, Ease.OUT_CUBIC);
		final Anim closeHover = new Anim(0.0F, 140, Ease.OUT_CUBIC);
		final Anim closePress = new Anim(0.0F, 90, Ease.OUT_CUBIC);
		final Map<Integer, Anim> tabHover = new HashMap<>();
		long closeSince;
		long enteredAt = Clock.nowMs();
		boolean swap;
		int pushed;
	}
}
