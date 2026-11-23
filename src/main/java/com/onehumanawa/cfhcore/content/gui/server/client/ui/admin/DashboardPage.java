package com.onehumanawa.cfhcore.content.gui.server.client.ui.admin;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerPage;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog.Forms;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.server.model.AdminData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

final class DashboardPage extends ServerPage {
	private final AdminScreen admin;
	private CFHGuiScrollPanel panel;
	private int builtDimensions = -1;
	private boolean builtMaintenance;

	DashboardPage(AdminScreen screen) {
		super(screen, "dashboard", Txt.adminPage("dashboard"), Icon.SIGNAL);
		this.admin = screen;
	}

	@Override
	protected Node node() {
		return Nodes.ADMIN_DASHBOARD;
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		this.admin.watch();
		this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(area.x(), area.y(), area.w(), area.h()));
		this.fill();
	}

	@Override
	public void stateChanged() {
		this.dashboardChanged();
	}

	@Override
	public void dashboardChanged() {
		AdminData.Dashboard d = ServerState.dashboard();
		int dims = d == null ? -1 : d.dimensions.size();
		if (this.panel == null || dims != this.builtDimensions || ServerState.hello().maintenance != this.builtMaintenance) {
			this.screen.refreshPage();
		}
	}

	private void fill() {
		AdminData.Dashboard first = ServerState.dashboard();
		int w = this.panel.innerWidth();
		this.builtDimensions = first == null ? -1 : first.dimensions.size();
		this.builtMaintenance = ServerState.hello().maintenance;
		if (first == null) {
			int h = Math.max(96, this.panel.getHeight() - 4);
			this.panel.add(new PaintedCard(w, h, (g, f) -> {
				CFHGuiSpinner.draw(g, f.x() + f.w() / 2 - 6, f.y() + f.h() / 2 - 16, 12, Colors.scaleAlpha(Ui.soft(), f.alpha()));
				Ui.textCentered(g, Txt.t("dash.waiting"), f.x() + f.w() / 2, f.y() + f.h() / 2 + 4, Ui.soft(), f.alpha());
			}).bare(), 0, 0);
			this.panel.setContentHeight(h);
		} else {
			int y = 0;
			List<PaintedCard> tiles = new ArrayList<>();
			tiles.add(
				tile(
					Txt.t("dash.tps"),
					d -> String.format(Locale.ROOT, "%.1f", d.tps),
					d -> Txt.t("dash.tps.minute", String.format(Locale.ROOT, "%.1f", d.tpsMinute)),
					d -> (float)(d.tps / 20.0),
					d -> AdminScreen.tpsColor(d.tps, Theme.current().palette())
				)
			);
			tiles.add(
				tile(
					Txt.t("dash.mspt"),
					d -> String.format(Locale.ROOT, "%.1f ms", d.mspt),
					d -> Txt.t("dash.mspt.worst", String.format(Locale.ROOT, "%.1f", d.msptWorst)),
					d -> (float)(d.mspt / 50.0),
					d -> AdminScreen.tickColor(d.mspt, Theme.current().palette())
				)
			);
			tiles.add(
				tile(
					Txt.t("dash.players"),
					d -> d.players + " / " + d.maxPlayers,
					d -> Txt.t("dash.players.sub", Math.max(0, d.maxPlayers - d.players)),
					d -> d.maxPlayers <= 0 ? 0.0F : (float)d.players / (float)d.maxPlayers,
					d -> Ui.strong()
				)
			);
			tiles.add(
				tile(
					Txt.t("dash.memory"),
					d -> Txt.number(d.memoryUsed) + " MB",
					d -> Txt.t("dash.memory.sub", Txt.number(d.memoryMax)),
					d -> d.memoryMax <= 0L ? 0.0F : (float)d.memoryUsed / (float)d.memoryMax,
					d -> d.memoryMax > 0L && (double)d.memoryUsed > (double)d.memoryMax * 0.9
							? Theme.current().palette().danger()
							: (d.memoryMax > 0L && (double)d.memoryUsed > (double)d.memoryMax * 0.75 ? Theme.current().palette().warning() : Theme.current().palette().success())
				)
			);
			tiles.add(
				tile(
					Txt.t("dash.uptime"),
					d -> Txt.span(d.uptimeMs / 1000L),
					d -> d.externalPermissions.isEmpty() ? Txt.t("dash.uptime.sub") : Txt.t("dash.external", d.externalPermissions),
					d -> -1.0F,
					d -> Ui.strong()
				)
			);
			y = grid(this.panel, y, w, tiles.size(), 86, 48, (i, cw) -> {
				tiles.get(i).setWidth(cw);
				return (AbstractWidget)tiles.get(i);
			});
			this.panel.add(graph(w), 0, y);
			y += 122;
			this.panel.add(dimensions(w, first.dimensions.size()), 0, y);
			y += 26 + first.dimensions.size() * 18 + 12;
			List<CFHGuiButton> actions = this.actions();
			if (!actions.isEmpty()) {
				y = section(this.panel, y, w, Txt.t("dash.actions"));
				y = grid(this.panel, y, w, actions.size(), 130, 20, (i, cw) -> {
					actions.get(i).setWidth(cw);
					return (AbstractWidget)actions.get(i);
				});
			}

			this.panel.setContentHeight(y + 2);
		}
	}

	private static PaintedCard tile(
		Component caption,
		Function<AdminData.Dashboard, String> value,
		Function<AdminData.Dashboard, Component> sub,
		Function<AdminData.Dashboard, Float> share,
		Function<AdminData.Dashboard, Integer> color
	) {
		return new PaintedCard(104, 48, (g, f) -> {
			AdminData.Dashboard d = ServerState.dashboard();
			if (d != null) {
				float a = f.alpha();
				Ui.textFit(g, caption, f.x() + 8, f.y() + 7, f.w() - 16, Ui.caption(), a);
				Ui.textFit(g, Fonts.heading(value.apply(d)), f.x() + 8, f.y() + 18, f.w() - 16, color.apply(d), a);
				Ui.textFit(g, sub.apply(d), f.x() + 8, f.y() + 30, f.w() - 16, Ui.soft(), a);
				float s = share.apply(d);
				if (s >= 0.0F) {
					Ui.bar(g, f.x() + 3, f.y() + f.h() - 5, f.w() - 6, 2, s * f.grow(), color.apply(d), a);
				}
			}
		}).flat().narrate(caption);
	}

	private static PaintedCard graph(int w) {
		return new PaintedCard(
				w,
				116,
				(g, f) -> {
					AdminData.Dashboard d = ServerState.dashboard();
					if (d != null) {
						Theme t = Theme.current();
						Palette p = t.palette();
						float a = f.alpha();
						Ui.textFit(g, Fonts.heading(Txt.t("dash.graph")), f.x() + 8, f.y() + 7, f.w() / 2, Ui.strong(), a);
						Ui.textRight(g, Txt.t("dash.graph.sub", String.format(Locale.ROOT, "%.1f", d.mspt)), f.x() + f.w() - 8, f.y() + 7, Ui.caption(), a);
						int gx = f.x() + 8;
						int gy = f.y() + 22;
						int gw = f.w() - 16;
						int gh = f.h() - 30;
						Ui.well(g, gx, gy, gw, gh, a);
						float[] h = d.msptHistory;
						float top = 60.0F;

						for (float v : h) {
							top = Math.max(top, v * 1.1F);
						}

						int ix = gx + 2;
						int iw = gw - 4;
						int ib = gy + gh - 2;
						int ih = gh - 4;
						int budgetY = ib - Math.round(50.0F / top * (float)ih);
						int halfY = ib - Math.round(25.0F / top * (float)ih);
						CFHGuiDraw.hline(g, ix, halfY, iw, Colors.scaleAlpha(Colors.withAlpha(p.textDim(), 48), a));

						for (int x = ix; x < ix + iw; x += 6) {
							CFHGuiDraw.hline(g, x, budgetY, 3, Colors.scaleAlpha(Colors.withAlpha(p.warning(), 176), a));
						}

						int n = Math.max(1, h.length);
						int hot = -1;
						if (f.mouseX() >= ix && f.mouseX() < ix + iw && f.mouseY() >= gy && f.mouseY() < gy + gh) {
							hot = Math.min(n - 1, (f.mouseX() - ix) * n / iw);
						}

						for (int i = 0; i < h.length; i++) {
							int x0 = ix + i * iw / n;
							int x1 = ix + (i + 1) * iw / n;
							int bh = Math.max(1, Math.round(Math.min(top, h[i]) / top * (float)ih * f.grow()));
							int c = AdminScreen.tickColor((double)h[i], p);
							if (i == hot) {
								c = Colors.brighten(c, 0.35F);
							}

							CFHGuiDraw.rect(g, x0, ib - bh, Math.max(1, x1 - x0 - 1), bh, Colors.scaleAlpha(Colors.withAlpha(c, i == hot ? 255 : 208), a));
						}

						Component budget = Txt.t("dash.graph.budget");
						int bw = CFHGuiDraw.width(budget);
						CFHGuiDraw.rect(
							g,
							ix + iw - bw - 5,
							budgetY - 11,
							bw + 5,
							10,
							Colors.scaleAlpha(t.isVanilla() ? -1073741824 : Colors.withAlpha(Colors.brighten(p.bg(), -0.25F), 208), a)
						);
						Ui.text(g, budget, ix + iw - bw - 2, budgetY - 10, Colors.withAlpha(p.warning(), 224), a);
						if (hot >= 0 && hot < h.length) {
							f.tip(
								0,
								0,
								f.w(),
								f.h(),
								List.of(Component.literal(String.format(Locale.ROOT, "%.1f ms", h[hot])), Txt.t("dash.graph.ago", h.length - 1 - hot).withStyle(ChatFormatting.GRAY))
							);
						}
					}
				}
			)
			.flat()
			.narrate(Txt.t("dash.graph"));
	}

	private static PaintedCard dimensions(int w, int rows) {
		return new PaintedCard(w, 26 + rows * 18 + 6, (g, f) -> {
			AdminData.Dashboard d = ServerState.dashboard();
			if (d != null) {
				Palette p = Theme.current().palette();
				float a = f.alpha();
				int nameW = Math.max(70, f.w() * 30 / 100);
				int colW = (f.w() - nameW - 16) / 4;
				Component[] heads = new Component[]{Txt.t("dash.dim.players"), Txt.t("dash.dim.entities"), Txt.t("dash.dim.chunks"), Txt.t("dash.dim.block_entities")};
				Ui.textFit(g, Fonts.heading(Txt.t("dash.dim")), f.x() + 8, f.y() + 8, nameW - 8, Ui.strong(), a);

				for (int c = 0; c < 4; c++) {
					Ui.textFit(g, heads[c], f.x() + 8 + nameW + c * colW, f.y() + 8, colW - 4, Ui.caption(), a);
				}

				Ui.rule(g, f.x() + 6, f.y() + 21, f.w() - 12, a);
				int most = 1;

				for (AdminData.Dimension dim : d.dimensions) {
					most = Math.max(most, Math.max(dim.entities, dim.chunks));
				}

				for (int i = 0; i < d.dimensions.size(); i++) {
					AdminData.Dimension dim = d.dimensions.get(i);
					int ry = f.y() + 26 + i * 18;
					Ui.textFit(g, Txt.dimension(dim.id), f.x() + 8, ry + 4, nameW - 8, dim.players > 0 ? Ui.strong() : Ui.soft(), a);
					int[] values = new int[]{dim.players, dim.entities, dim.chunks, dim.blockEntities};

					for (int c = 0; c < 4; c++) {
						int cx = f.x() + 8 + nameW + c * colW;
						Ui.text(g, Txt.number((long)values[c]), cx, ry + 4, values[c] == 0 ? Ui.caption() : p.textMuted(), a);
						if (c == 1 || c == 2) {
							Ui.bar(g, cx, ry + 14, Math.max(8, colW - 12), 2, (float)values[c] / (float)most * f.grow(), c == 1 ? p.accent() : p.success(), a * 0.9F);
						}
					}
				}
			}
		}).flat().narrate(Txt.t("dash.dim"));
	}

	private List<CFHGuiButton> actions() {
		List<CFHGuiButton> out = new ArrayList<>();
		if (ServerState.may(Nodes.MOD_BROADCAST)) {
			out.add(
				new CFHGuiButton(
						0,
						0,
						100,
						Txt.t("dash.broadcast"),
						() -> new Forms.Prompt(
									Txt.t("dash.broadcast.title"), Icon.SEND, Txt.t("dash.broadcast.caption"), "", 200, Txt.t("dash.broadcast.ok"), v -> true, ServerActions::broadcast
								)
								.show()
					)
					.icon(Icon.SEND)
			);
		}

		if (ServerState.feature("staff_chat") && ServerState.may(Nodes.MOD_STAFF_CHAT)) {
			out.add(
				new CFHGuiButton(
						0,
						0,
						100,
						Txt.t("dash.staff"),
						() -> new Forms.Prompt(
									Txt.t("dash.staff.title"),
									Icon.CHAT,
									Txt.t("dash.staff.caption"),
									"",
									200,
									Txt.t("mail.send"),
									v -> true,
									(v, then) -> ServerActions.send("staff_chat", text("message", v), then)
								)
								.show()
					)
					.icon(Icon.CHAT)
			);
		}

		if (ServerState.may(Nodes.ADMIN_MAINTENANCE)) {
			boolean on = ServerState.hello().maintenance;
			CFHGuiButton b = new CFHGuiButton(
					0,
					0,
					100,
					Txt.t(on ? "dash.maintenance.off" : "dash.maintenance.on"),
					() -> {
						if (on) {
							ServerActions.ruleSet("maintenance", false, AdminScreen.after(() -> {
							}));
						} else {
							Forms.confirm(
								Txt.t("dash.maintenance.title"),
								Txt.t("dash.maintenance.body"),
								Txt.t("dash.maintenance.on"),
								() -> ServerActions.ruleSet("maintenance", true, AdminScreen.after(() -> {
									}))
							);
						}
					}
				)
				.icon(Icon.WRENCH);
			if (on) {
				b.variant(Variant.PRIMARY);
			}

			out.add(b);
		}

		if (ServerState.may(Nodes.ADMIN_RULES)) {
			out.add(new CFHGuiButton(0, 0, 100, Txt.t("dash.reload"), () -> ServerActions.rulesReload(AdminScreen.after(() -> {
				}))).icon(Icon.REFRESH).tip(Txt.t("dash.reload.tip")));
		}

		if (ServerState.may(Nodes.ADMIN_WORLD)) {
			out.add(new CFHGuiButton(0, 0, 100, Txt.t("dash.day"), () -> ServerActions.worldSet("time", null, "day", null, AdminScreen.after(() -> {
				}))).icon(Icon.SUN));
			out.add(new CFHGuiButton(0, 0, 100, Txt.t("dash.clear"), () -> ServerActions.worldSet("weather", null, "clear", null, AdminScreen.after(() -> {
				}))).icon(Icon.CLOUD));
		}

		return out;
	}

	private static CompoundTag text(String key, String value) {
		CompoundTag tag = new CompoundTag();
		tag.putString(key, value);
		return tag;
	}

	public void render(SidebarScreen s, GuiGraphics g, Rect area, int mouseX, int mouseY, float partialTick) {
	}
}
