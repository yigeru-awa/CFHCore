package com.onehumanawa.cfhcore.content.gui.server.client.ui.admin;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Fonts;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiBadge;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSegmented;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerPage;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.CommitField;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.server.model.AdminData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class WorldPage extends ServerPage {
	private static final List<String> DIFFICULTIES = List.of("peaceful", "easy", "normal", "hard");
	private static final List<String> WEATHER = List.of("clear", "rain", "thunder");
	private static final String[] TIMES = new String[]{"day", "noon", "night", "midnight"};
	@Nullable
	private AdminData.WorldInfo info;
	private CFHGuiScrollPanel panel;
	private String query = "";

	WorldPage(AdminScreen screen) {
		super(screen, "world", Txt.adminPage("world"), Icon.WORLD);
	}

	@Override
	protected Node node() {
		return Nodes.ADMIN_WORLD;
	}

	@Override
	public void stateChanged() {
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		this.panel = null;
		ServerPage.Bar bar = new ServerPage.Bar(s, area);
		bar.right(new CFHGuiIconButton(0, 0, 18, Icon.REFRESH, Txt.t("refresh"), this::ask));
		CFHGuiSearchField search = bar.fill(new CFHGuiSearchField(0, 0, 160, q -> {
			String typed = q.trim().toLowerCase(Locale.ROOT);
			if (!typed.equals(this.query)) {
				this.query = typed;
				if (this.panel != null) {
					refill(this.panel, this::fill);
					this.panel.snapScroll(0.0);
				}
			}
		}), 220);
		search.setValue(this.query);
		search.placeholder(Txt.t("world.search"));
		Rect r = under(area);
		this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(r.x(), r.y(), r.w(), r.h()));
		this.fill();
		if (this.info == null) {
			this.ask();
		}
	}

	private void ask() {
		ServerActions.adminWorld(answer -> {
			this.info = answer.map(AdminData.WorldInfo::sanitize).orElse(null);
			if (this.info != null) {
				for (AdminData.GameRule rule : this.info.gameRules) {
					if (rule.nameKey == null) {
						rule.nameKey = "";
					}

					if (rule.categoryKey == null) {
						rule.categoryKey = "";
					}
				}
			}

			if (this.panel != null) {
				refill(this.panel, this::fill);
			}
		});
	}

	private void set(String what, @Nullable String key, String value, @Nullable String dimension, boolean again) {
		ServerActions.worldSet(what, key, value, dimension, r -> {
			if (!r.ok()) {
				CFHGuiToasts.show(Component.translatable("cfhgui_server.result.failed"), ServerActions.describe(r), Icon.WARNING);
			}

			if (again || !r.ok()) {
				this.ask();
			}
		});
	}

	private void fill() {
		AdminData.WorldInfo w = this.info;
		int width = this.panel.innerWidth();
		if (w == null) {
			int h = Math.max(96, this.panel.getHeight() - 4);
			this.panel.add(new PaintedCard(width, h, (g, f) -> CFHGuiSpinner.draw(g, f.x() + f.w() / 2 - 6, f.y() + f.h() / 2 - 6, 12, Ui.soft())).bare(), 0, 0);
			this.panel.setContentHeight(h);
		} else {
			int y = 0;
			if (this.query.isEmpty()) {
				y = section(this.panel, y, width, Txt.t("world.difficulty"));
				CFHGuiSegmented<String> difficulty = new CFHGuiSegmented<>(
					0,
					0,
					Math.min(width, 320),
					DIFFICULTIES,
					DIFFICULTIES.contains(w.difficulty) ? w.difficulty : "normal",
					d -> Component.translatable("options.difficulty." + d),
					d -> {
						w.difficulty = d;
						this.set("difficulty", null, d, null, false);
					}
				);
				difficulty.active = !w.difficultyLocked;
				if (w.difficultyLocked) {
					difficulty.tip(Txt.t("world.difficulty.locked"));
				}

				this.panel.add(difficulty, 0, y);
				if (w.hardcore && width > 420) {
					this.panel.add(new CFHGuiBadge(0, 0, Txt.t("world.hardcore"), Theme.current().palette().danger()), 328, y + 5);
				}

				y += 28;
				y = section(this.panel, y, width, Txt.t("world.time"));
				this.panel.add(this.everywhere(w, width), 0, y);
				y += 34;

				for (AdminData.DimensionTime d : w.dimensions) {
					this.panel.add(dimension(d, width), 0, y);
					y += 26;
				}

				y += 2;
			}

			List<String> categories = new ArrayList<>();

			for (AdminData.GameRule rule : w.gameRules) {
				if (!categories.contains(rule.categoryKey)) {
					categories.add(rule.categoryKey);
				}
			}

			int shown = 0;

			for (String category : categories) {
				List<AdminData.GameRule> members = new ArrayList<>();

				for (AdminData.GameRule rulex : w.gameRules) {
					if (rulex.categoryKey.equals(category)
						&& (
							this.query.isEmpty() || name(rulex).getString().toLowerCase(Locale.ROOT).contains(this.query) || rulex.id.toLowerCase(Locale.ROOT).contains(this.query)
						)) {
						members.add(rulex);
					}
				}

				if (!members.isEmpty()) {
					y = section(this.panel, y + 2, width, Txt.t("world.rules", I18n.exists(category) ? Component.translatable(category) : Component.literal(category)));

					for (AdminData.GameRule rulexx : members) {
						this.panel.add(this.rule(rulexx, width), 0, y);
						y += 26;
						shown++;
					}
				}
			}

			if (shown == 0 && !this.query.isEmpty()) {
				this.panel
					.add(
						new PaintedCard(
								width,
								100,
								(g, f) -> Ui.empty(g, new Rect(f.x(), f.y(), f.w(), f.h()), Icon.SEARCH, Txt.t("world.no_match.title"), Txt.t("rules.no_match", this.query), f.alpha())
							)
							.bare(),
						0,
						y
					);
				y += 100;
			}

			this.panel.setContentHeight(y + 2);
		}
	}

	private static Component name(AdminData.GameRule rule) {
		return !rule.nameKey.isEmpty() && I18n.exists(rule.nameKey) ? Component.translatable(rule.nameKey) : Component.literal(rule.id);
	}

	private PaintedCard everywhere(AdminData.WorldInfo info, int w) {
		AdminData.DimensionTime sky = info.dimensions.isEmpty() ? null : info.dimensions.get(0);

		for (AdminData.DimensionTime d : info.dimensions) {
			if ("minecraft:overworld".equals(d.id)) {
				sky = d;
			}
		}

		String now = sky == null ? "clear" : (sky.thundering ? "thunder" : (sky.raining ? "rain" : "clear"));
		int room = Math.max(40, w - 30 - 326);
		PaintedCard card = new PaintedCard(w, 30, (g, f) -> {
			float a = f.alpha();
			Icons.draw(g, Icon.CLOCK, f.x() + 8, f.y() + 9, 12, Colors.scaleAlpha(Theme.current().palette().accentInk(), a));
			Ui.textFit(g, Fonts.heading(Txt.t("world.everywhere")), f.x() + 26, f.y() + 5, room, Ui.strong(), a);
			Ui.textFit(g, Txt.t("world.everywhere.sub"), f.x() + 26, f.y() + 16, room, Ui.soft(), a);
			f.tip(0, 0, room + 26, f.h(), List.of(Txt.t("world.everywhere"), Txt.t("world.everywhere.tip").withStyle(ChatFormatting.GRAY)));
		}).flat().narrate(Txt.t("world.everywhere"));
		CFHGuiSegmented<String> weather = new CFHGuiSegmented<>(0, 0, 150, WEATHER, now, v -> Txt.t("world.weather." + v), v -> this.set("weather", null, v, null, true));
		int x = w - 6 - 150;
		card.add(weather, x, 5);

		for (int i = TIMES.length - 1; i >= 0; i--) {
			String time = TIMES[i];
			CFHGuiButton b = new CFHGuiButton(0, 0, 40, 20, Txt.t("world.time." + time), () -> this.set("time", null, time, null, true));
			b.setWidth(Math.max(36, b.preferredWidth() + 8));
			x -= b.getWidth() + 3;
			if (x < 120) {
				break;
			}

			card.add(b, x, 5);
		}

		return card;
	}

	private static PaintedCard dimension(AdminData.DimensionTime d, int w) {
		long tod = Math.floorMod(d.dayTime, 24000L);
		int hour = (int)((tod / 1000L + 6L) % 24L);
		int minute = (int)(tod % 1000L * 60L / 1000L);
		boolean night = tod >= 13000L && tod < 23000L;
		Component clock = Txt.t("world.clock", d.dayTime / 24000L + 1L, String.format(Locale.ROOT, "%02d:%02d", hour, minute));
		return new PaintedCard(
				w,
				24,
				(g, f) -> {
					Palette p = Theme.current().palette();
					float a = f.alpha();
					Icons.draw(
						g,
						d.thundering ? Icon.BOLT : (d.raining ? Icon.CLOUD : (night ? Icon.MOON : Icon.SUN)),
						f.x() + 8,
						f.y() + 6,
						12,
						Colors.scaleAlpha(night ? -6584608 : p.warning(), a)
					);
					Ui.textRight(g, clock, f.x() + f.w() - 8, f.y() + 8, Ui.soft(), a);
					Ui.textFit(g, Txt.dimension(d.id), f.x() + 26, f.y() + 8, f.w() - 42 - CFHGuiDraw.width(clock), Ui.strong(), a);
				}
			)
			.flat()
			.narrate(Txt.dimension(d.id));
	}

	private PaintedCard rule(AdminData.GameRule rule, int w) {
		Component name = name(rule);
		String descriptionKey = rule.nameKey + ".description";
		AbstractWidget control;
		if (rule.bool) {
			control = new CFHGuiToggle(0, 0, 26, Component.empty(), Boolean.parseBoolean(rule.value), v -> {
				rule.value = Boolean.toString(v);
				this.set("gamerule", rule.id, rule.value, null, false);
			});
		} else {
			long value = 0L;

			try {
				value = Long.parseLong(rule.value.trim());
			} catch (NumberFormatException var9) {
			}

			control = CommitField.number(64, name, value, -2147483648L, 2147483647L, v -> {
				rule.value = Long.toString(v);
				this.set("gamerule", rule.id, rule.value, null, false);
			});
		}

		int room = w - control.getWidth() - 22;
		PaintedCard card = new PaintedCard(w, 24, (g, f) -> {
			Ui.textFit(g, name, f.x() + 8, f.y() + 8, room, Ui.strong(), f.alpha());
			List<Component> tip = new ArrayList<>();
			tip.add(name);
			if (I18n.exists(descriptionKey)) {
				tip.add(Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY));
			}

			tip.add(Component.literal(rule.id).withStyle(ChatFormatting.DARK_GRAY));
			f.tip(0, 0, room + 8, f.h(), tip);
		}).flat().narrate(name);
		card.add(control, w - 8 - control.getWidth(), (24 - control.getHeight()) / 2);
		return card;
	}
}
