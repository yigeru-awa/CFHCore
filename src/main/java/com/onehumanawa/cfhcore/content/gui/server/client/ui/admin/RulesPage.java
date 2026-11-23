package com.onehumanawa.cfhcore.content.gui.server.client.ui.admin;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.core.perm.Perms.Node;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSearchField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.server.Nodes;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerActions;
import com.onehumanawa.cfhcore.content.gui.server.client.ServerState;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.ServerPage;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog.AdminForms;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.dialog.Forms;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.CommitField;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Txt;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.kit.Ui;
import com.onehumanawa.cfhcore.content.gui.server.model.Hello;
import com.onehumanawa.cfhcore.content.gui.server.model.ServerRules;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class RulesPage extends ServerPage {
	private static final Map<String, String> PARTS = Map.of(
		"homes", "travel", "afk", "players", "stats", "statistics", "operatorsHaveEverything", "roles", "staffChat", "moderation"
	);
	@Nullable
	private ServerRules rules;
	private CFHGuiScrollPanel panel;
	private String query = "";
	private final Map<Field, CommitField> typed = new HashMap<>();

	RulesPage(AdminScreen screen) {
		super(screen, "rules", Txt.adminPage("rules"), Icon.SLIDERS);
	}

	@Override
	protected Node node() {
		return Nodes.ADMIN_RULES;
	}

	@Override
	public void stateChanged() {
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		this.panel = null;
		ServerPage.Bar bar = new ServerPage.Bar(s, area);
		CFHGuiButton reload = new CFHGuiButton(
				0,
				0,
				110,
				Txt.t("dash.reload"),
				() -> Forms.confirm(
						Txt.t("rules.reload.title"), Txt.t("rules.reload.body"), Txt.t("dash.reload"), () -> ServerActions.rulesReload(AdminScreen.after(this::ask))
					)
			)
			.icon(Icon.REFRESH);
		reload.setWidth(reload.preferredWidth() + 12);
		reload.tip(Txt.t("dash.reload.tip"));
		bar.right(reload);
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
		search.placeholder(Txt.t("rules.search"));
		Rect r = under(area);
		this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(r.x(), r.y(), r.w(), r.h()));
		this.fill();
		if (this.rules == null) {
			this.ask();
		}
	}

	private void ask() {
		ServerActions.adminRules(answer -> {
			this.rules = answer.map(ServerRules::sanitize).orElse(null);
			if (this.panel != null) {
				refill(this.panel, this::fill);
			}
		});
	}

	private static Component label(String field) {
		String key = "cfhgui_server.ui.rule." + field;
		if (I18n.exists(key)) {
			return Component.translatable(key);
		} else {
			StringBuilder sb = new StringBuilder();

			for (char c : field.toCharArray()) {
				if (Character.isUpperCase(c)) {
					sb.append(' ').append(Character.toLowerCase(c));
				} else {
					sb.append(sb.isEmpty() ? Character.toUpperCase(c) : c);
				}
			}

			return Component.literal(sb.toString());
		}
	}

	private void fill() {
		ServerRules r = this.rules;
		int w = this.panel.innerWidth();
		this.typed.clear();
		if (r == null) {
			int h = Math.max(96, this.panel.getHeight() - 4);
			this.panel.add(new PaintedCard(w, h, (g, f) -> CFHGuiSpinner.draw(g, f.x() + f.w() / 2 - 6, f.y() + f.h() / 2 - 6, 12, Ui.soft())).bare(), 0, 0);
			this.panel.setContentHeight(h);
		} else {
			int y = 0;
			int shown = 0;
			String part = null;
			boolean headed = true;

			for (Field field : ServerRules.class.getFields()) {
				if (!Modifier.isStatic(field.getModifiers())) {
					if (PARTS.containsKey(field.getName())) {
						part = PARTS.get(field.getName());
						headed = false;
					}

					Component label = label(field.getName());
					if (this.query.isEmpty()
						|| label.getString().toLowerCase(Locale.ROOT).contains(this.query)
						|| field.getName().toLowerCase(Locale.ROOT).contains(this.query)) {
						if (!headed && part != null) {
							y = section(this.panel, y + (shown == 0 ? 0 : 4), w, Txt.t("rules.part." + part));
							headed = true;
						}

						this.panel.add(this.row(r, field, label, w), 0, y);
						y += 26;
						shown++;
					}
				}
			}

			if (shown == 0) {
				this.panel
					.add(
						new PaintedCard(
								w,
								100,
								(g, f) -> Ui.empty(g, new Rect(f.x(), f.y(), f.w(), f.h()), Icon.SEARCH, Txt.t("rules.no_match.title"), Txt.t("rules.no_match", this.query), f.alpha())
							)
							.bare(),
						0,
						0
					);
				y = 100;
			}

			this.panel.setContentHeight(y + 2);
		}
	}

	private PaintedCard row(ServerRules r, Field field, Component label, int w) {
		String name = field.getName();
		String tipKey = "cfhgui_server.ui.rule." + name + ".tip";
		boolean locked = name.startsWith("maintenance") && !ServerState.may(Nodes.ADMIN_MAINTENANCE);
		AbstractWidget control = this.control(r, field, label, w);
		control.active = control.active && !locked;
		int room = w - control.getWidth() - 22;
		PaintedCard card = new PaintedCard(w, 24, (g, f) -> {
			Ui.textFit(g, label, f.x() + 8, f.y() + 8, room, locked ? Ui.caption() : Ui.strong(), f.alpha());
			List<Component> tip = new ArrayList<>();
			tip.add(label);
			if (I18n.exists(tipKey)) {
				tip.add(Component.translatable(tipKey).withStyle(ChatFormatting.GRAY));
			}

			tip.add(Component.literal(name).withStyle(ChatFormatting.DARK_GRAY));
			if (locked) {
				tip.add(Txt.t("denied").withStyle(ChatFormatting.RED));
			}

			f.tip(0, 0, room + 8, f.h(), tip);
		}).flat().narrate(label);
		card.add(control, w - 8 - control.getWidth(), (24 - control.getHeight()) / 2);
		return card;
	}

	private AbstractWidget control(ServerRules r, Field field, Component label, int w) {
		String name = field.getName();

		try {
			Object value = field.get(r);
			Class<?> type = field.getType();
			if (type == boolean.class) {
				CFHGuiToggle toggle = new CFHGuiToggle(0, 0, 26, Component.empty(), (Boolean)value, v -> this.set(field, v));
				if (name.equals("maintenance")) {
					toggle.onChange(v -> this.maintenance(field, toggle, v));
				}

				return toggle;
			} else if (type == int.class || type == long.class) {
				CommitField number = CommitField.number(64, label, ((Number)value).longValue(), 0L, 100000000L, v -> this.set(field, type == int.class ? v.intValue() : v));
				this.typed.put(field, number);
				return number;
			} else if (type == String.class) {
				CommitField text = new CommitField(Math.min(220, w / 2), label, (String)value, v -> v.length() <= 256, v -> this.set(field, v));
				this.typed.put(field, text);
				return text;
			} else if (List.class.isAssignableFrom(type) && field.getGenericType() instanceof ParameterizedType pt) {
				boolean links = pt.getActualTypeArguments()[0] == ServerRules.Link.class;
				List<?> list = value == null ? List.of() : (List)value;
				CFHGuiButton edit = new CFHGuiButton(
						0,
						0,
						80,
						18,
						Txt.t(list.isEmpty() ? "rules.list.empty" : "rules.list", list.size()),
						() -> {
							List<String> lines = new ArrayList<>();
							if (links) {
								for (Object ox : list) {
									ServerRules.Link l = (ServerRules.Link)ox;
									lines.add(l.name + " | " + l.url);
								}
							} else {
								for (Object o : list) {
									lines.add(String.valueOf(o));
								}
							}

							new AdminForms.LinesForm(
									label,
									Txt.t(links ? "rules.links.hint" : (I18n.exists("cfhgui_server.ui.rule." + name + ".tip") ? "rule." + name + ".tip" : "rules.lines.hint")),
									lines,
									200,
									links ? RulesPage::linkLine : v -> true,
									(out, then) -> {
										Object sent = links ? out.stream().map(RulesPage::link).toList() : out;
										ServerActions.ruleSet(name, sent, res -> {
											then.accept(res);
											if (res.ok()) {
												this.ask();
											}
										});
									}
								)
								.show();
						}
					)
					.icon(Icon.EDIT)
					.iconSize(8);
				edit.setWidth(edit.preferredWidth() + 12);
				return edit;
			} else {
				CFHGuiButton shown = new CFHGuiButton(0, 0, 80, 18, Component.literal(JsonWire.GSON.toJson(value)), () -> {
				});
				shown.active = false;
				return shown;
			}
		} catch (IllegalAccessException var12) {
			CFHGuiButton none = new CFHGuiButton(0, 0, 40, 18, Component.literal("?"), () -> {
			});
			none.active = false;
			return none;
		}
	}

	private static boolean linkLine(String line) {
		int bar = line.indexOf(124);
		return bar <= 0 ? false : !line.substring(0, bar).isBlank() && Hello.web(line.substring(bar + 1));
	}

	private static ServerRules.Link link(String line) {
		ServerRules.Link l = new ServerRules.Link();
		int bar = line.indexOf(124);
		l.name = line.substring(0, bar).trim();
		l.url = line.substring(bar + 1).trim();
		return l;
	}

	private void maintenance(Field field, CFHGuiToggle toggle, boolean on) {
		if (!on) {
			this.set(field, false);
		} else {
			toggle.setValue(false);
			Forms.confirm(Txt.t("dash.maintenance.title"), Txt.t("dash.maintenance.body"), Txt.t("dash.maintenance.on"), () -> {
				toggle.setValue(true);
				this.set(field, true);
			});
		}
	}

	private void set(Field field, Object value) {
		ServerActions.ruleSet(field.getName(), value, r -> {
			if (r.ok()) {
				try {
					if (this.rules != null) {
						field.set(this.rules, value);
						this.settle();
					}
				} catch (IllegalArgumentException | IllegalAccessException var5) {
					this.ask();
				}
			} else {
				CFHGuiToasts.show(Component.translatable("cfhgui_server.result.failed"), ServerActions.describe(r), Icon.WARNING);
				this.ask();
			}
		});
	}

	private void settle() {
		ServerRules r = this.rules;
		if (r != null) {
			r.sanitize();

			for (Entry<Field, CommitField> row : this.typed.entrySet()) {
				try {
					row.getValue().sync(String.valueOf(row.getKey().get(r)));
				} catch (IllegalAccessException var5) {
				}
			}
		}
	}
}
