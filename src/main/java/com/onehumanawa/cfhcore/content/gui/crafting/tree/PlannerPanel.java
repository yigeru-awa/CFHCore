package com.onehumanawa.cfhcore.content.gui.crafting.tree;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.api.EmiApi;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiResolutionRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.handler.EmiCraftContext;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.handler.EmiRecipeHandler;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.handler.StandardRecipeHandler;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiIngredient;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStack;
import com.onehumanawa.cfhcore.content.gui.emi.api.stack.EmiStackInteraction;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.RecipeFillButtonWidget;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.SlotWidget;
import com.onehumanawa.cfhcore.content.gui.emi.api.widget.Widget;
import com.onehumanawa.cfhcore.content.gui.emi.input.EmiBind;
import com.onehumanawa.cfhcore.content.gui.emi.input.EmiInput;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiRecipeFiller;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiDrawContext;
import com.onehumanawa.cfhcore.content.gui.emi.runtime.EmiLog;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.emi.screen.RecipeDisplay;
import com.onehumanawa.cfhcore.content.gui.emi.screen.WidgetGroup;
import com.onehumanawa.cfhcore.content.gui.emi.screen.tooltip.RecipeTooltipComponent;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class PlannerPanel {
	private static final int INSET = 7;
	private static final int PITCH = 21;
	private static final int BUTTON_H = 14;
	private final Font font;
	private Rect rect;
	@Nullable
	private TreeNode node;
	@Nullable
	private String path;
	private int revision;
	private int laidWidth;
	private int laidStatus;
	@Nullable
	private WidgetGroup card;
	private List<EmiRecipe> ways;
	private List<EmiStack> choices;
	private List<EmiIngredient> stations;
	private int headH;
	private int buttonsY;
	private int waysY;
	private int chipsY;
	private int perRow;
	private int chipRows;
	private int waysShown;
	private int stationsY;
	private int stationsChipsX;
	private int stationsChipsY;
	private int content;
	private float scroll;
	private final CFHGuiButton make;
	private final CFHGuiButton recipe;
	private final CFHGuiButton given;
	@Nullable
	private Widget pressedSlot;
	@Nullable
	private EmiStackInteraction hovered;
	@Nullable
	private List<ClientTooltipComponent> tooltip;
	private int lastMouseX;
	private int lastMouseY;

	PlannerPanel(Consumer<AbstractWidget> add) {
		this.font = Minecraft.getInstance().font;
		this.rect = Rect.of(0, 0, 0, 0);
		this.revision = -1;
		this.laidWidth = -1;
		this.laidStatus = -1;
		this.ways = List.of();
		this.choices = List.of();
		this.stations = List.of();
		this.waysY = -1;
		this.stationsY = -1;
		this.make = new CFHGuiButton(0, 0, 60, 16, Component.translatable("cfhgui_crafting.planner.make"), this::make)
			.variant(Variant.PRIMARY)
			.icon(Icon.HAMMER)
			.iconSize(8);
		this.recipe = new CFHGuiButton(0, 0, 60, 16, Component.translatable("cfhgui_crafting.planner.recipes"), () -> {
			if (this.node != null) {
				TreeActions.chooseRecipe(this.node);
			}
		}).icon(Icon.LIST).iconSize(8);
		this.given = new CFHGuiButton(0, 0, 60, 16, Component.empty(), this::toggleGiven).variant(Variant.GHOST).icon(Icon.LOCK).iconSize(8);

		for (CFHGuiButton b : List.of(this.make, this.recipe, this.given)) {
			b.visible = false;
			add.accept(b);
		}
	}

	void setRect(Rect rect) {
		if (rect.w() != this.rect.w() || rect.h() != this.rect.h() || rect.x() != this.rect.x() || rect.y() != this.rect.y()) {
			this.revision = -1;
		}

		this.rect = rect;
	}

	Rect rect() {
		return this.rect;
	}

	boolean contains(double mx, double my) {
		return this.rect.w() > 0 && this.rect.contains(mx, my);
	}

	void hidden() {
		this.make.visible = this.recipe.visible = this.given.visible = false;
		this.node = null;
		this.card = null;
		this.pressedSlot = null;
	}

	@Nullable
	TreeNode node() {
		return this.node;
	}

	@Nullable
	EmiStackInteraction hoveredStack() {
		return this.hovered;
	}

	@Nullable
	List<ClientTooltipComponent> tooltip() {
		return this.tooltip;
	}

	private void make() {
		TreeSession s = Trees.active();
		if (s != null && this.node != null) {
			TreeActions.craft(s, this.node, EmiInput.isShiftDown());
		}
	}

	private void toggleGiven() {
		TreeSession s = Trees.active();
		if (s != null && this.node != null) {
			if (TreeActions.fetched(s, this.node)) {
				TreeActions.makeAgain(s, this.node);
			} else {
				s.give(this.node.path, !this.node.given);
			}

			Trees.synthetic();
		}
	}

	void prepare(TreeSession s) {
		TreeNode chosen = s.outline().at(s.selected);
		if (chosen == null) {
			chosen = s.outline().goal;
		}

		int lines = this.statusLines(chosen, s.figures(chosen.path));
		if (chosen != this.node || !chosen.path.equals(this.path) || this.revision != s.tree.revision || this.laidWidth != this.rect.w() || this.laidStatus != lines) {
			if (!chosen.path.equals(this.path)) {
				this.scroll = 0.0F;
			}

			this.describe(s, chosen, lines);
		}

		this.scroll = Mth.clamp(this.scroll, 0.0F, (float)this.maxScroll());
		this.placeButtons(s);
	}

	private int textWidth() {
		return this.rect.w() - 14 - 24;
	}

	private int statusLines(TreeNode node, TreeFigures f) {
		return Math.max(1, this.twoLines(status(node, f), Math.max(20, this.textWidth())).size());
	}

	private List<FormattedCharSequence> twoLines(Component text, int width) {
		List<FormattedCharSequence> lines = this.font.split(text, width);
		if (lines.size() <= 2) {
			return lines;
		} else {
			StringBuilder first = new StringBuilder();
			lines.get(0).accept((index, style, codePoint) -> {
				first.appendCodePoint(codePoint);
				return true;
			});
			String all = text.getString();
			String rest = all.length() > first.length() ? all.substring(first.length()).trim() : "";
			return List.of(lines.get(0), CFHGuiDraw.truncate(Component.literal(rest).withStyle(text.getStyle()), width));
		}
	}

	private void describe(TreeSession s, TreeNode chosen, int lines) {
		this.node = chosen;
		this.path = chosen.path;
		this.revision = s.tree.revision;
		this.laidWidth = this.rect.w();
		this.laidStatus = lines;
		this.card = null;
		this.ways = List.of();
		this.choices = List.of();
		this.stations = List.of();
		this.pressedSlot = null;
		EmiRecipe used = chosen.recipe();
		EmiIngredient asked = chosen.asked();
		if (chosen.depth > 0 && asked != null && asked.getEmiStacks().size() > 1) {
			this.choices = asked.getEmiStacks();
		}

		EmiIngredient shown = chosen.ingredient();
		if (shown != null && shown.getEmiStacks().size() == 1) {
			EmiStack stack = shown.getEmiStacks().get(0);
			List<EmiRecipe> all = new ArrayList<>();

			for (EmiRecipe r : EmiApi.getRecipeManager().getRecipesByOutput(stack)) {
				if (r.supportsRecipeTree() && r.getOutputs().stream().anyMatch(o -> o.isEqual(stack))) {
					all.add(r);
				}
			}

			if (used != null && all.remove(used)) {
				all.add(0, used);
			}

			this.ways = all;
		}

		if (used != null && chosen.hasRecipe()) {
			this.stations = EmiApi.getRecipeManager().getWorkstations(used.getCategory());
		}

		int inner = this.rect.w() - 14;
		int y = 7;
		this.headH = 10 + lines * 10;
		y += Math.max(18, this.headH) + 4;
		this.buttonsY = y;
		y += 20;
		int chips = this.ways.size() > 1 ? this.ways.size() : this.choices.size();
		this.waysY = -1;
		if (chips > 0) {
			this.waysY = y;
			this.perRow = Math.max(1, (inner + 1) / 21);
			this.chipRows = Math.min(2, (chips + this.perRow - 1) / this.perRow);
			this.chipsY = y + 11;
			y = this.chipsY + this.chipRows * 21 + 4;
		}

		if (used != null && chosen.hasRecipe()) {
			try {
				RecipeDisplay display = new RecipeDisplay(used);
				this.card = display.getWidgets(this.rect.x() + 7, this.rect.y() + y + 5, inner, display.getHeight());
			} catch (Throwable var12) {
				EmiLog.error("Error constructing recipe widgets for the planner", var12);
			}

			if (this.card != null) {
				y += 5 + this.card.getHeight() + 4 + 6;
			}
		} else {
			y += 24;
		}

		this.stationsY = -1;
		if (!this.stations.isEmpty()) {
			this.stationsY = y;
			int captionW = this.font.width(Component.translatable("cfhgui_crafting.planner.stations")) + 6;
			int shown2 = Math.min(this.stations.size(), Math.max(1, (inner + 1) / 21));
			if (captionW + shown2 * 21 <= inner) {
				this.stationsChipsX = this.rect.x() + 7 + captionW;
				this.stationsChipsY = y;
				y += 25;
			} else {
				this.stationsChipsX = this.rect.x() + 7;
				this.stationsChipsY = y + 11;
				y += 36;
			}
		}

		this.content = y + 7 - 6;
	}

	private int maxScroll() {
		return Math.max(0, this.content - this.rect.h());
	}

	private void placeButtons(TreeSession s) {
		this.make.visible = this.recipe.visible = this.given.visible = false;
		TreeNode n = this.node;
		if (n != null) {
			TreeFigures f = s.figures(n.path);
			int y = this.rect.y() + this.buttonsY - Math.round(this.scroll);
			if (y >= this.rect.y() + 2 && y + 14 <= this.rect.bottom() - 2) {
				int x = this.rect.x() + 7;
				int right = this.rect.right() - 7;
				if (n.step() && f.state == TreeFigures.State.READY && f.here) {
					this.make
						.setMessage(
							Component.translatable(
								f.crafts > 1L && EmiInput.isShiftDown() ? "cfhgui_crafting.planner.make_all" : "cfhgui_crafting.planner.make", new Object[]{f.crafts}
							)
						);
					x = place(this.make, x, y, right);
				}

				boolean fetched = TreeActions.fetched(s, n);
				if (n.depth > 0 && (n.hasRecipe() || fetched)) {
					boolean off = n.given || fetched;
					this.given.setMessage(Component.translatable(off ? "cfhgui_crafting.tree.menu.make" : "cfhgui_crafting.tree.menu.given"));
					this.given.icon(off ? Icon.HAMMER : Icon.LOCK);
					x = place(this.given, x, y, right);
				}

				if (!n.hasRecipe() && !fetched && TreeActions.hasRecipes(n)) {
					this.recipe.setMessage(Component.translatable("cfhgui_crafting.planner.recipes"));
					place(this.recipe, x, y, right);
				}
			}
		}
	}

	private static int place(CFHGuiButton b, int x, int y, int right) {
		b.setWidth(b.preferredWidth() + 2);
		if (x + b.getWidth() > right) {
			if (x + 18 > right) {
				return x;
			}

			b.setWidth(18);
		}

		b.setX(x);
		b.setY(y);
		b.visible = true;
		return x + b.getWidth() + 4;
	}

	void render(GuiGraphics g, TreeSession s, int mouseX, int mouseY, float partialTick, boolean free) {
		this.tooltip = null;
		this.hovered = null;
		this.lastMouseX = mouseX;
		this.lastMouseY = mouseY;
		TreeNode n = this.node;
		if (n != null && n.ingredient() != null && this.rect.w() > 0) {
			Theme t = Theme.current();
			Palette p = t.palette();
			boolean van = t.isVanilla();
			TreeFigures f = s.figures(n.path);
			int sc = Math.round(this.scroll);
			boolean inside = free && this.contains((double)mouseX, (double)mouseY);
			int mx = inside ? mouseX : -10000;
			int my = inside ? mouseY + sc : -10000;
			int x = this.rect.x() + 7;
			int right = this.rect.right() - 7;
			int top = this.rect.y();
			g.enableScissor(this.rect.x() + 1, this.rect.y() + 1, this.rect.right() - 1, this.rect.bottom() - 1);
			g.pose().pushPose();
			g.pose().translate(0.0F, (float)(-sc), 0.0F);

			try {
				int hy = top + 7;
				TreePaint.plate(
					g,
					x,
					hy,
					0.0F,
					f.state == TreeFigures.State.DONE,
					f.state == TreeFigures.State.READY ? (van ? -1 : p.accent()) : (n.given ? TreePaint.muted() : 0),
					n.given,
					1.0F
				);
				n.ingredient().render(g, x + 1, hy + 1, partialTick, -3);
				g.drawString(this.font, CFHGuiDraw.truncate(Fonts.heading(TreeActions.name(n.ingredient())), this.textWidth()), x + 24, hy, van ? -1 : p.text(), van);
				List<FormattedCharSequence> status = this.twoLines(status(n, f), Math.max(20, this.textWidth()));

				for (int i = 0; i < status.size(); i++) {
					g.drawString(this.font, status.get(i), x + 24, hy + 10 + i * 10, statusColor(f), van);
				}

				if (mx >= x && mx < x + 18 && my >= hy && my < hy + 18) {
					this.hovered = new EmiStackInteraction(n.ingredient(), n.recipe(), true);
					this.tooltip = new ArrayList<>(n.ingredient().getTooltip());
				}

				int chips = this.ways.size() > 1 ? this.ways.size() : this.choices.size();
				this.waysShown = 0;
				if (this.waysY >= 0 && chips > 0) {
					g.drawString(
						this.font,
						Component.translatable(this.ways.size() > 1 ? "cfhgui_crafting.planner.ways" : "cfhgui_crafting.planner.choices", new Object[]{chips}),
						x,
						top + this.waysY,
						TreePaint.dim(),
						van
					);
					int slots = this.perRow * this.chipRows;
					int fit = chips <= slots ? chips : slots - 1;

					for (int i = 0; i < fit; this.waysShown++) {
						int cx = x + i % this.perRow * 21;
						int cy = top + this.chipsY + i / this.perRow * 21;
						boolean over = mx >= cx && mx < cx + 20 && my >= cy && my < cy + 20;
						boolean current = this.ways.size() > 1
							? this.ways.get(i) == n.recipe()
							: n.tag != null && this.choices.get(i).isEqual(n.ingredient().getEmiStacks().get(0));
						chip(g, cx, cy, over, current);
						if (this.ways.size() > 1) {
							EmiRecipe way = this.ways.get(i);
							way.getCategory().renderSimplified(g, cx + 2, cy + 2, partialTick);
							if (over) {
								List<ClientTooltipComponent> tip = new ArrayList<>();
								tip.add(ClientTooltipComponent.create(way.getCategory().getName().getVisualOrderText()));
								tip.add(new RecipeTooltipComponent(way, true));
								tip.add(
									ClientTooltipComponent.create(
										Component.translatable(current ? "cfhgui_crafting.planner.ways.current" : "cfhgui_crafting.planner.ways.use")
											.withStyle(st -> st.withColor(TreePaint.dim() & 16777215))
											.getVisualOrderText()
									)
								);
								this.tooltip = tip;
							}
						} else {
							EmiStack choice = this.choices.get(i);
							choice.render(g, cx + 2, cy + 2, partialTick, 1);
							if (over) {
								this.tooltip = new ArrayList<>(choice.getTooltip());
								this.hovered = new EmiStackInteraction(choice, null, true);
							}
						}

						i++;
					}

					if (chips > this.waysShown) {
						int cx = x + this.waysShown % this.perRow * 21;
						int cy = top + this.chipsY + this.waysShown / this.perRow * 21;
						boolean over = mx >= cx && mx < cx + 20 && my >= cy && my < cy + 20;
						chip(g, cx, cy, over, false);
						TreePaint.pixelsCentered(g, TreePaint.DOTS, cx + 10, cy + 10, over ? TreePaint.text() : TreePaint.muted());
						if (over) {
							this.tooltip = List.of(
								ClientTooltipComponent.create(Component.translatable("cfhgui_crafting.planner.ways.more", new Object[]{chips - this.waysShown}).getVisualOrderText())
							);
						}
					}
				}

				WidgetGroup group = this.card;
				if (group != null) {
					EmiDrawContext context = EmiDrawContext.wrap(g);
					int gx = mx - group.x();
					int gy = my - group.y();
					context.push();
					context.translate((float)group.x(), (float)group.y());
					EmiPort.applyModelViewMatrix();

					try {
						for (Widget widget : group.widgets) {
							widget.render(g, gx, gy, partialTick);
						}

						for (Widget widget : group.widgets) {
							if (widget instanceof RecipeFillButtonWidget && widget.getBounds().contains(gx, gy)) {
								AbstractContainerScreen hs = EmiApi.getHandledScreen();
								EmiRecipeHandler handler = hs == null ? null : EmiRecipeFiller.getFirstValidHandler(group.recipe, hs);
								if (handler != null) {
									handler.render(group.recipe, new EmiCraftContext(hs, handler.getInventory(hs), EmiCraftContext.Type.FILL_BUTTON), group.widgets, g);
								} else {
									StandardRecipeHandler.renderMissing(group.recipe, Trees.inventory(), group.widgets, g);
								}
								break;
							}
						}
					} catch (Throwable var36) {
						EmiLog.error("Error rendering widget", var36);
						group.error(var36);
					}

					context.pop();
					EmiPort.applyModelViewMatrix();
					if (inside) {
						try {
							for (Widget widgetx : group.widgets) {
								if (widgetx.getBounds().contains(gx, gy)) {
									List<ClientTooltipComponent> tip = widgetx.getTooltip(gx, gy);
									if (!tip.isEmpty()) {
										this.tooltip = tip;
										break;
									}
								}
							}
						} catch (Throwable var35) {
							EmiLog.error("Error rendering widget group", var35);
							group.error(var35);
						}
					}
				} else if (!n.hasRecipe()) {
					int ly = top + (this.waysY >= 0 ? this.chipsY + this.chipRows * 21 + 4 : this.buttonsY + 14 + 6) + 6;
					List<FormattedCharSequence> lines = this.font
						.split(
							Component.translatable(
								TreeActions.fetched(s, n)
									? "cfhgui_crafting.planner.fetched"
									: (TreeActions.hasRecipes(n) ? "cfhgui_crafting.planner.raw.choose" : "cfhgui_crafting.planner.raw")
							),
							right - x
						);

					for (int i = 0; i < lines.size() && i < 2; i++) {
						g.drawString(this.font, lines.get(i), x, ly + i * 10, TreePaint.muted(), van);
					}
				}

				if (this.stationsY >= 0 && n.hasRecipe()) {
					g.drawString(
						this.font,
						Component.translatable("cfhgui_crafting.planner.stations"),
						x,
						top + this.stationsY + (this.stationsChipsY == this.stationsY ? 6 : 0),
						TreePaint.dim(),
						van
					);
					int sx = this.stationsChipsX;
					int sy = top + this.stationsChipsY;

					for (EmiIngredient station : this.stations) {
						if (sx + 20 > right) {
							break;
						}

						boolean over = mx >= sx && mx < sx + 20 && my >= sy && my < sy + 20;
						chip(g, sx, sy, over, false);
						station.render(g, sx + 2, sy + 2, partialTick, 1);
						if (over) {
							this.tooltip = new ArrayList<>(station.getTooltip());
							this.hovered = new EmiStackInteraction(station, null, true);
						}

						sx += 21;
					}
				}
			} finally {
				g.pose().popPose();
				g.disableScissor();
			}

			this.scrollbar(g);
		}
	}

	private void scrollbar(GuiGraphics g) {
		int max = this.maxScroll();
		if (max > 0) {
			Theme t = Theme.current();
			int track = this.rect.h() - 4;
			int barH = Math.max(10, track * this.rect.h() / Math.max(1, this.content));
			int by = this.rect.y() + 2 + Math.round((float)(track - barH) * (this.scroll / (float)max));
			if (t.isVanilla()) {
				g.fill(this.rect.right() - 4, by, this.rect.right() - 2, by + barH, -6250336);
			} else {
				CFHGuiDraw.pixelRound(g, this.rect.right() - 4, by, 2, barH, t.palette().borderStrong(), 1);
			}
		}
	}

	static void chip(GuiGraphics g, int x, int y, boolean over, boolean current) {
		Theme t = Theme.current();
		Palette p = t.palette();
		if (t.isVanilla()) {
			g.fill(x, y, x + 20, y + 20, over ? -9803158 : -11908534);
			CFHGuiDraw.outline(g, x, y, 20, 20, current ? -1 : -16777216, 0);
		} else {
			CFHGuiDraw.pixelRound(g, x, y, 20, 20, over ? p.surfaceHover() : p.surface(), 2);
			CFHGuiDraw.outline(g, x, y, 20, 20, current ? p.accent() : (over ? p.borderStrong() : p.border()), 2);
		}
	}

	static Component status(TreeNode node, TreeFigures f) {
		String have = TreeTips.amount(node.ingredient(), f.have(), f.chance);
		String wanted = TreeTips.amount(node.ingredient(), f.wanted, f.chance);
		String missing = TreeTips.amount(node.ingredient(), f.missing, f.chance);

		return switch (f.state) {
			case DONE -> f.unasked()
			? Component.translatable("cfhgui_crafting.tree.tip.unasked")
			: Component.translatable("cfhgui_crafting.tree.tip.done", new Object[]{wanted});
			case COVERED -> Component.translatable("cfhgui_crafting.tree.tip.covered", new Object[]{wanted});
			case READY -> Component.translatable("cfhgui_crafting.planner.status.ready", new Object[]{have, wanted, f.crafts});
			case WAITING -> Component.translatable("cfhgui_crafting.planner.status.waiting", new Object[]{have, wanted, f.crafts});
			case MISSING -> Component.translatable("cfhgui_crafting.planner.status.missing", new Object[]{missing, have, wanted});
			case GIVEN -> Component.translatable("cfhgui_crafting.planner.status.given", new Object[]{missing});
		};
	}

	static int statusColor(TreeFigures f) {
		return switch (f.state) {
			case DONE -> TreePaint.success();
			default -> TreePaint.muted();
			case READY -> TreePaint.vanilla() ? -171 : TreePaint.accent();
			case MISSING, GIVEN -> TreePaint.warning();
		};
	}

	boolean mouseClicked(double mouseX, double mouseY, int button) {
		this.pressedSlot = null;
		if (!this.contains(mouseX, mouseY)) {
			return false;
		} else {
			TreeSession s = Trees.active();
			TreeNode n = this.node;
			if (s != null && n != null) {
				int mx = (int)mouseX;
				int my = (int)mouseY + Math.round(this.scroll);
				int x = this.rect.x() + 7;
				int top = this.rect.y();
				int chips = this.ways.size() > 1 ? this.ways.size() : this.choices.size();
				if (button == 0 && this.waysY >= 0 && chips > 0 && mx >= x && my >= top + this.chipsY && my < top + this.chipsY + this.chipRows * 21) {
					int col = (mx - x) / 21;
					int row = (my - top - this.chipsY) / 21;
					if ((mx - x) % 21 < 20 && (my - top - this.chipsY) % 21 < 20 && col < this.perRow) {
						int i = row * this.perRow + col;
						if (i < this.waysShown) {
							if (this.ways.size() > 1) {
								s.choose(n, this.ways.get(i));
							} else if (n.asked() != null) {
								s.tree.addResolution(n.asked(), new EmiResolutionRecipe(n.asked(), this.choices.get(i)));
							}

							Trees.synthetic();
							CFHGuiSounds.click();
							return true;
						}

						if (i == this.waysShown && chips > this.waysShown) {
							TreeActions.chooseRecipe(n);
							return true;
						}
					}
				}

				WidgetGroup group = this.card;
				if (group != null) {
					try {
						int ox = mx - group.x();
						int oy = my - group.y();
						boolean over = mx >= group.x() - 4 && mx < group.x() + group.getWidth() + 4 && my >= group.y() - 4 && my < group.y() + group.getHeight() + 4;

						for (Widget widget : group.widgets) {
							if (widget.getBounds().contains(ox, oy)) {
								if (widget instanceof SlotWidget) {
									if (this.pressedSlot == null) {
										this.pressedSlot = widget;
									}
								} else if (widget.mouseClicked(ox, oy, button)) {
									return true;
								}

								over = true;
							}
						}

						if (over && EmiScreenManager.recipeInteraction(group.recipe, bind -> bind.matchesMouse(button))) {
							return true;
						}

						if (this.pressedSlot != null) {
							return true;
						}
					} catch (Throwable var19) {
						EmiLog.error("Error handling widget input", var19);
						group.error(var19);
					}
				}

				return this.hovered != null && EmiScreenManager.stackInteraction(this.hovered, bind -> bind.matchesMouse(button)) ? true : true;
			} else {
				return true;
			}
		}
	}

	boolean mouseReleased(double mouseX, double mouseY, int button) {
		WidgetGroup group = this.card;
		if (this.pressedSlot instanceof SlotWidget slot && group != null && group.widgets.contains(slot)) {
			this.pressedSlot = null;

			try {
				int ox = (int)mouseX - group.x();
				int oy = (int)mouseY + Math.round(this.scroll) - group.y();
				if (slot.getBounds().contains(ox, oy) && slot.mouseClicked(ox, oy, button)) {
					return true;
				}
			} catch (Throwable var10) {
				EmiLog.error("Error handling widget input", var10);
				group.error(var10);
			}
		}

		this.pressedSlot = null;
		return false;
	}

	boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (!this.contains(mouseX, mouseY)) {
			return false;
		} else {
			this.scroll = Mth.clamp(this.scroll - (float)amount * 18.0F, 0.0F, (float)this.maxScroll());
			return true;
		}
	}

	boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!this.contains((double)this.lastMouseX, (double)this.lastMouseY)) {
			return false;
		} else {
			Function<EmiBind, Boolean> bind = b -> b.matchesKey(keyCode, scanCode);
			WidgetGroup group = this.card;
			if (group != null) {
				try {
					int mx = this.lastMouseX - group.x();
					int my = this.lastMouseY + Math.round(this.scroll) - group.y();
					boolean over = mx >= -4 && mx < group.getWidth() + 4 && my >= -4 && my < group.getHeight() + 4;

					for (Widget widget : group.widgets) {
						if (widget.getBounds().contains(mx, my)) {
							if (widget.keyPressed(keyCode, scanCode, modifiers)) {
								return true;
							}

							over = true;
						}
					}

					if (over && EmiScreenManager.recipeInteraction(group.recipe, bind)) {
						return true;
					}
				} catch (Throwable var11) {
					EmiLog.error("Error handling widget input", var11);
					group.error(var11);
				}
			}

			return this.hovered != null && EmiScreenManager.stackInteraction(this.hovered, bind);
		}
	}
}
