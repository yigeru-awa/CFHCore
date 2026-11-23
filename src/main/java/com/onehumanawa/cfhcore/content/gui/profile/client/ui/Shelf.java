package com.onehumanawa.cfhcore.content.gui.profile.client.ui;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageFinish;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageSoft;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.StageNode;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.PlayerNode.Part;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSounds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTooltips;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import com.onehumanawa.cfhcore.content.gui.profile.Slot;
import com.onehumanawa.cfhcore.content.gui.profile.client.Cosmetic;
import com.onehumanawa.cfhcore.content.gui.profile.client.render.CosmeticRenderer;
import com.onehumanawa.cfhcore.content.gui.profile.client.render.LookNode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

final class Shelf extends CFHGuiWidget {
	static final int GAP = 4;
	static final int LABEL = 12;
	private static final float TALL = 1.24F;
	private final int box;
	private final Stage stage;
	private final List<Shelf.Entry> entries = new ArrayList<>();
	private final Consumer<Shelf.Entry> onPick;
	private final Consumer<Shelf.Entry> onHover;
	private final Anim scroll = new Anim(0.0F, 200, Ease.OUT_CUBIC);
	@Nullable
	private Shelf.Entry hovered;

	static int boxFor(int screenHeight) {
		return Mth.clamp(screenHeight / 7, 44, 72);
	}

	Shelf(Screen owner, int x, int y, int width, int box, Consumer<Shelf.Entry> onPick, Consumer<Shelf.Entry> onHover) {
		super(x, y, width, box + 12, Component.empty());
		this.box = box;
		this.onPick = onPick;
		this.onHover = onHover;
		Stage s = new Stage().bind(owner);
		this.stage = s;
		s.background(0);
		s.soft(true);
		s.finish(StageFinish.glow());
		s.resolutionScale(Minecraft.getInstance().getWindow().getWidth() <= 2048 ? 2.0F : 1.5F);
		s.focusOutline(false);
		s.lighting().key(1.5F, 3.5F, 6.0F).ambientColor(0.46F, 0.45F, 0.47F).sun(0.3F, 1.0F, 0.7F);
		this.silent();
	}

	Stage stage() {
		return this.stage;
	}

	List<Shelf.Entry> entries() {
		return this.entries;
	}

	void show(List<Shelf.Entry> list) {
		for (Shelf.Entry e : this.entries) {
			if (e.node != null) {
				this.stage.remove(e.node);
			}
		}

		this.entries.clear();
		this.entries.addAll(list);

		for (Shelf.Entry ex : this.entries) {
			if (ex.node != null) {
				this.stage.add(ex.node);
			}
		}

		this.scroll.snap(0.0F);
		this.hovered = null;
	}

	static Shelf.Entry thing(Cosmetic c) {
		Shelf.Entry e = new Shelf.Entry(c.id(), c.name(), c, null);
		e.node = new Shelf.ThingNode(c);
		return e;
	}

	static Shelf.Entry painted(Cosmetic c, boolean slim) {
		Shelf.Entry e = new Shelf.Entry(c.id(), c.name(), c, null);
		LookNode figure = LookNode.sample(c, slim);
		figure.show(parts(c.slot()));
		figure.pickable(false);
		figure.lookAtCursor(false);
		figure.breathe(false);
		figure.hoverFeel(0.0F, 1.0F);
		e.node = figure;
		return e;
	}

	static Shelf.Entry skin(String id, Component label, ResourceLocation texture, boolean slim) {
		Shelf.Entry e = new Shelf.Entry(id, label, null, null);
		LookNode figure = LookNode.ofTexture(texture, slim);
		figure.pickable(false);
		figure.lookAtCursor(false);
		figure.breathe(false);
		figure.hoverFeel(0.0F, 1.0F);
		e.node = figure;
		return e;
	}

	static Shelf.Entry sign(String id, Component label, Icon icon) {
		return new Shelf.Entry(id, label, null, icon);
	}

	private static Set<Part> parts(Slot slot) {
		return switch (slot) {
			case SHIRT -> EnumSet.of(Part.BODY, Part.RIGHT_ARM, Part.LEFT_ARM, Part.HEAD);
			case PANTS -> EnumSet.of(Part.RIGHT_LEG, Part.LEFT_LEG, Part.BODY);
			case RIGHT_ARM -> EnumSet.of(Part.RIGHT_ARM);
			case LEFT_ARM -> EnumSet.of(Part.LEFT_ARM);
			case RIGHT_LEG -> EnumSet.of(Part.RIGHT_LEG);
			case LEFT_LEG -> EnumSet.of(Part.LEFT_LEG);
			case FACE, HAT -> EnumSet.of(Part.HEAD);
			default -> EnumSet.allOf(Part.class);
		};
	}

	private static float[] frame(Shelf.Entry e) {
		if (e.cosmetic == null) {
			return new float[]{0.0F, 0.93F, 1.95F};
		} else {
			return switch (e.cosmetic.slot()) {
				case SHIRT -> new float[]{0.0F, 1.2F, 1.25F};
				case PANTS -> new float[]{0.0F, 0.52F, 1.2F};
				case RIGHT_ARM -> new float[]{-0.36F, 1.12F, 0.95F};
				case LEFT_ARM -> new float[]{0.36F, 1.12F, 0.95F};
				case RIGHT_LEG -> new float[]{-0.12F, 0.38F, 0.95F};
				case LEFT_LEG -> new float[]{0.12F, 0.38F, 0.95F};
				case FACE, HAT -> new float[]{0.0F, 1.62F, 0.72F};
				default -> new float[]{0.0F, 0.93F, 1.95F};
			};
		}
	}

	private int pitch() {
		return this.box + 4;
	}

	private int rowWidth() {
		return Math.max(0, this.entries.size() * this.pitch() - 4);
	}

	private int maxScroll() {
		return Math.max(0, this.rowWidth() - this.getWidth());
	}

	private int boxX(int index) {
		int lead = Math.max(0, (this.getWidth() - this.rowWidth()) / 2);
		return this.getX() + lead + index * this.pitch() - Math.round(this.scroll.get());
	}

	@Nullable
	private Shelf.Entry at(double mx, double my) {
		if (!(my < (double)this.getY())
			&& !(my >= (double)(this.getY() + this.box))
			&& !(mx < (double)this.getX())
			&& !(mx >= (double)(this.getX() + this.getWidth()))) {
			for (int i = 0; i < this.entries.size(); i++) {
				int bx = this.boxX(i);
				if (mx >= (double)bx && mx < (double)(bx + this.box)) {
					return this.entries.get(i);
				}
			}

			return null;
		} else {
			return null;
		}
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.isMouseOver(mouseX, mouseY) && this.maxScroll() > 0) {
			double by = scrollY != 0.0 ? scrollY : -scrollX;
			this.scroll.set(Mth.clamp(this.scroll.target() - (float)by * (float)this.pitch(), 0.0F, (float)this.maxScroll()));
			return true;
		} else {
			return false;
		}
	}

	public void onClick(double mouseX, double mouseY) {
		super.onClick(mouseX, mouseY);
		Shelf.Entry e = this.at(mouseX, mouseY);
		if (e != null) {
			CFHGuiSounds.click();
			this.onPick.accept(e);
		}
	}

	protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY, partialTick, false);
	}

	protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.draw(g, mouseX, mouseY, partialTick, true);
	}

	private void draw(GuiGraphics g, int mouseX, int mouseY, float partialTick, boolean van) {
		Theme t = Theme.current();
		Palette p = t.palette();
		float a = this.effectiveAlpha();
		if (!(a <= 0.004F)) {
			int x = this.getX();
			int y = this.getY() + this.enterOffset();
			int w = this.getWidth();
			Shelf.Entry over = this.isHovered() ? this.at((double)mouseX, (double)mouseY) : null;
			if (over != this.hovered) {
				this.hovered = over;
				this.onHover.accept(over);
			}

			g.enableScissor(x, y - 2, x + w, y + this.box + 14);

			for (int i = 0; i < this.entries.size(); i++) {
				Shelf.Entry e = this.entries.get(i);
				int bx = this.boxX(i);
				if (bx + this.box >= x && bx <= x + w) {
					e.hover.set(e == over);
					float h = e.hover.get();
					int by = y - Math.round(h * 1.5F);
					if (van) {
						g.fill(bx, by, bx + this.box, by + this.box, Colors.scaleAlpha(Colors.lerp(1879048192, -1875890128, h), a));
						CFHGuiDraw.outline(g, bx, by, this.box, this.box, Colors.scaleAlpha(e.chosen ? -1 : Colors.lerp(-16777216, -4144960, h), a), 0);
					} else {
						CFHGuiDraw.pixelRound(g, bx, by, this.box, this.box, Colors.scaleAlpha(t.panel(Colors.lerp(p.surface(), p.surfaceHover(), h)), a), t.radius());
						if (e.chosen) {
							CFHGuiDraw.vgradient(
								g, bx + 1, by + this.box / 2, this.box - 2, this.box / 2 - 1, Colors.withAlpha(p.accent(), 0), Colors.scaleAlpha(Colors.withAlpha(p.accent(), 80), a)
							);
						}

						CFHGuiDraw.outline(g, bx, by, this.box, this.box, Colors.scaleAlpha(e.chosen ? p.accent() : Colors.lerp(p.border(), p.borderStrong(), h), a), t.radius());
					}

					if (e.icon != null) {
						Icons.draw(
							g, e.icon, bx + (this.box - 16) / 2, by + (this.box - 16) / 2, 16, Colors.scaleAlpha(van ? -2039584 : Colors.lerp(p.textMuted(), p.text(), h), a)
						);
					}
				}
			}

			g.disableScissor();
			this.place(w);
			g.enableScissor(x, y - 2, x + w, y + this.box + 2);
			this.stage.alpha(a);
			this.stage.render(g, x, y - 2, w, this.box + 2, -1.0E9, -1.0E9, partialTick);
			g.disableScissor();
			g.enableScissor(x, y, x + w, y + this.box + 14);

			for (int ix = 0; ix < this.entries.size(); ix++) {
				Shelf.Entry e = this.entries.get(ix);
				if (e.chosen || e == over) {
					int bx = this.boxX(ix);
					FormattedCharSequence label = CFHGuiDraw.truncate(e.label, Math.min(this.getWidth(), 150));
					int lw = CFHGuiDraw.font().width(label);
					int lx = Mth.clamp(bx + this.box / 2 - lw / 2, x, Math.max(x, x + w - lw));
					if (e == over || over == null) {
						g.drawString(CFHGuiDraw.font(), label, lx, y + this.box + 3, Colors.scaleAlpha(e.chosen && !van ? p.accentInk() : (van ? -1 : p.text()), a), van);
					}
				}
			}

			g.disableScissor();
			float sc = this.scroll.get();
			if (sc > 1.0F) {
				Icons.draw(g, Icon.CHEVRON_LEFT, x + 1, y + this.box / 2 - 5, 10, Colors.scaleAlpha(van ? -1 : p.accentInk(), a));
			}

			if (sc < (float)this.maxScroll() - 1.0F) {
				Icons.draw(g, Icon.CHEVRON_RIGHT, x + w - 11, y + this.box / 2 - 5, 10, Colors.scaleAlpha(van ? -1 : p.accentInk(), a));
			}

			if (over != null && CFHGuiDraw.font().width(over.label) > Math.min(this.getWidth(), 150)) {
				CFHGuiTooltips.request(over.label, this);
			}
		}
	}

	private void place(int width) {
		float viewTall = 1.24F * (float)(this.box + 2) / (float)this.box;
		float fov = 9.0F;
		float dist = viewTall / (2.0F * (float)Math.tan(Math.toRadians(4.5)));
		this.stage.camera().at(0.0F, 0.0F, dist).lookAt(0.0F, 0.0F, 0.0F).fov(9.0F).clip(0.05F, 60.0F);
		float perPixel = viewTall / (float)(this.box + 2);

		for (int i = 0; i < this.entries.size(); i++) {
			Shelf.Entry e = this.entries.get(i);
			if (e.node != null) {
				int bx = this.boxX(i);
				boolean seen = bx + this.box > this.getX() - this.box && bx < this.getX() + width + this.box;
				e.node.visible(seen);
				if (seen) {
					float h = e.hover.get();
					float cx = ((float)bx + (float)this.box / 2.0F - ((float)this.getX() + (float)width / 2.0F)) * perPixel;
					float cy = (h * 1.5F - 1.0F) * perPixel;
					if (e.node instanceof LookNode figure) {
						float[] f = frame(e);
						float s = (0.96F + 0.06F * h) / f[2];
						figure.scale(s);
						figure.at(cx - f[0] * s, cy - f[1] * s, 0.0F);
						figure.yaw(e.cosmetic == null ? 18.0F : 24.0F + (e.hover.get() > 0.01F ? Mth.sin((float)System.nanoTime() / 1.0E9F * 1.6F) * 22.0F * h : 0.0F));
					} else {
						e.node.scale(0.86F + 0.1F * h);
						e.node.at(cx, cy, 0.0F);
					}
				}
			}
		}
	}

	void close() {
		this.entries.clear();
		this.stage.close();
	}

	static final class Entry {
		final String id;
		final Component label;
		@Nullable
		final Cosmetic cosmetic;
		@Nullable
		final Icon icon;
		@Nullable
		StageNode node;
		boolean chosen;
		final Anim hover = new Anim(0.0F, 140, Ease.OUT_CUBIC);

		Entry(String id, Component label, @Nullable Cosmetic cosmetic, @Nullable Icon icon) {
			this.id = id;
			this.label = label;
			this.cosmetic = cosmetic;
			this.icon = icon;
		}
	}

	private static final class ThingNode extends StageNode {
		private final Cosmetic cosmetic;

		ThingNode(Cosmetic cosmetic) {
			this.cosmetic = cosmetic;
			this.bounds(-0.5F, -0.5F, -0.5F, 0.5F, 0.5F, 0.5F);
			this.pickable(false);
			this.hoverFeel(0.0F, 1.0F);
			this.spin(34.0F);
		}

		protected void draw(StageRenderContext ctx) {
			if (!this.cosmetic.translucent()) {
				this.paint(ctx);
			}
		}

		public boolean hasTranslucentPass() {
			return this.cosmetic.translucent();
		}

		protected void drawTranslucent(StageRenderContext ctx) {
			this.paint(ctx);
		}

		private void paint(StageRenderContext ctx) {
			float age = ctx.timeMs / 50.0F;
			if (ctx.soft) {
				BufferBuilder bb = StageSoft.begin(StageSoft.ENTITY);
				CosmeticRenderer.drawAlone(ctx.pose, this.cosmetic, age, ctx.light, -1, c -> bb);
				StageSoft.end(bb, StageSoft.ENTITY, this.cosmetic.texture(), this.cosmetic.translucent() ? Math.min(0.999F, this.alpha()) : this.alpha(), false);
			} else {
				int color = Math.round(this.alpha() * 255.0F) << 24 | 16777215;
				CosmeticRenderer.drawAlone(ctx.pose, this.cosmetic, age, ctx.light, color, c -> ctx.buffers.getBuffer(RenderType.entityTranslucent(c.texture())));
			}
		}
	}
}
