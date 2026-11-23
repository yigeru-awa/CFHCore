package com.onehumanawa.cfhcore.content.gui.teams.client.ui;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icons;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarScreen;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSegmented;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSlider;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSwatches;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiTextField;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamActions;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.PaintedCard;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamFlows;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.settings.TeamsSettings;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.settings.TeamsSettingsTab;
import com.onehumanawa.cfhcore.content.gui.teams.model.Permission;
import com.onehumanawa.cfhcore.content.gui.teams.model.Privacy;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.net.TeamsNet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class SettingsPage extends TeamsPage {
	private static final int ROW_H = 24;
	private static final int LABEL_W = 82;
	private CFHGuiScrollPanel panel;
	private final List<Runnable> syncs = new ArrayList<>();
	@Nullable
	private Rank builtFor;
	private boolean builtWithTeam;
	private boolean builtPersonal;

	SettingsPage(TeamsHubScreen screen) {
		super(screen, "settings", Icon.SETTINGS);
	}

	@Override
	protected boolean needsServer() {
		return false;
	}

	@Override
	protected void buildPage(SidebarScreen s, Rect area) {
		this.panel = (CFHGuiScrollPanel)s.addPageWidget(new CFHGuiScrollPanel(area.x(), area.y(), area.w(), area.h()));
		this.fill();
	}

	@Override
	void stateChanged() {
		if (this.panel != null
			&& TeamsView.hasTeam() == this.builtWithTeam
			&& TeamsView.rank() == this.builtFor
			&& TeamsView.personal() != null == this.builtPersonal) {
			for (Runnable sync : this.syncs) {
				sync.run();
			}
		} else {
			super.stateChanged();
		}
	}

	private void fill() {
		this.syncs.clear();
		TeamData t = TeamsView.team();
		this.builtWithTeam = t != null;
		this.builtPersonal = TeamsView.personal() != null;
		this.builtFor = TeamsView.rank();
		int w = this.panel.innerWidth();
		int y = 0;
		if (t == null && this.builtPersonal) {
			y = section(this.panel, y, w, TeamsText.t("settings.team"));
			CFHGuiLabel note = new CFHGuiLabel(0, 0, w - 4, TeamsText.t("solo.share.desc")).style(Style.CAPTION).wrap(true);
			this.panel.add(note, 2, y);
			y += note.getHeight() + 8;
		}

		if (t != null) {
			boolean may = TeamsView.may(Permission.SETTINGS);
			y = section(this.panel, y, w, TeamsText.t("settings.team"));
			if (!may) {
				CFHGuiLabel note = new CFHGuiLabel(0, 0, w - 4, TeamsText.t("settings.rank", TeamsText.rank(Permission.SETTINGS.min()))).style(Style.CAPTION).wrap(true);
				this.panel.add(note, 2, y);
				y += note.getHeight() + 4;
			}

			this.panel.add(this.textRow(w, "name", () -> team().name, 24, TeamFlows::validName, may, TeamActions::rename), 0, y);
			y += 26;
			this.panel.add(this.textRow(w, "tag", () -> team().tag, 5, TeamFlows::validTag, may, TeamActions::setTag), 0, y);
			y += 26;
			this.panel.add(this.textRow(w, "motto", () -> team().motto, 96, v -> true, may, (v, then) -> {
				CompoundTag a = new CompoundTag();
				a.putString("motto", v);
				TeamActions.send("set_motto", a, then);
			}), 0, y);
			y += 26;
			PaintedCard colour = this.colourRow(w, may);
			this.panel.add(colour, 0, y);
			y += colour.getHeight() + 2;
			this.panel.add(this.bannerRow(w, may), 0, y);
			y += 52;
			PaintedCard privacy = this.privacyRow(w, may);
			this.panel.add(privacy, 0, y);
			y += privacy.getHeight() + 2;
			this.panel.add(this.friendlyFireRow(w, may), 0, y);
			y += 32;
		}

		y = section(this.panel, y, w, TeamsText.t("settings.display"));
		if (CFHGuiPlatform.get().isModLoaded("cfhgui_config")) {
			CFHGuiButton open = new CFHGuiButton(0, 0, 92, TeamsText.t("settings.display.open"), () -> SettingsPage.ConfigSide.open(this.screen)).icon(Icon.SLIDERS);
			open.setWidth(Math.max(84, open.preferredWidth() + 10));
			int openW = open.getWidth();
			PaintedCard link = new PaintedCard(
					w,
					30,
					(g, f) -> {
						Palette p = Theme.current().palette();
						TeamsDraw.textFit(
							g, TeamsText.t("settings.display.config"), f.x() + 6, f.y() + 6, f.w() - openW - 18, Theme.current().isVanilla() ? -1 : p.text(), f.alpha()
						);
						TeamsDraw.textFit(
							g, TeamsText.t("settings.display.config.desc", TeamsSettings.count()), f.x() + 6, f.y() + 16, f.w() - openW - 18, p.textDim(), f.alpha()
						);
					}
				)
				.flat();
			link.add(open, w - openW - 5, 5);
			this.panel.add(link, 0, y);
			y += 34;
		} else {
			for (TeamsSettings.Group group : TeamsSettings.groups()) {
				CFHGuiLabel caption = new CFHGuiLabel(0, 0, w - 4, group.title()).style(Style.CAPTION);
				this.panel.add(caption, 2, y + 2);
				y += 14;

				for (TeamsSettings.Setting setting : group.settings()) {
					this.panel.add(preference(setting, w), 0, y);
					y += 26;
				}

				y += 4;
			}
		}

		if (t != null) {
			y = section(this.panel, y + 6, w, TeamsText.t("settings.danger"));
			PaintedCard danger = this.dangerRow(t, w);
			this.panel.add(danger, 0, y);
			y += danger.getHeight() + 2;
		}

		this.panel.setContentHeight(y + 2);
	}

	private static TeamData team() {
		TeamData t = TeamsView.team();
		return t == null ? new TeamData() : t;
	}

	private static void label(GuiGraphics g, PaintedCard.Frame f, String key, boolean may) {
		Theme th = Theme.current();
		Palette p = th.palette();
		TeamsDraw.textFit(g, TeamsText.t("settings." + key), f.x() + 6, f.y() + 8, 72, may ? (th.isVanilla() ? -1 : p.text()) : p.textMuted(), f.alpha());
		f.tip(0, 0, 78, 24, TeamsText.t("settings." + key + ".desc"));
	}

	private PaintedCard textRow(
		int w, String key, Supplier<String> current, int maxLength, Predicate<String> valid, boolean may, BiConsumer<String, Consumer<TeamsNet.Result>> send
	) {
		CFHGuiTextField field = new CFHGuiTextField(0, 0, w - 82 - 26, 20, TeamsText.t("settings." + key));
		field.maxLength(maxLength).text(current.get());
		field.active = may;
		CFHGuiIconButton save = new CFHGuiIconButton(0, 0, 18, Icon.CHECK, TeamsText.t("settings.save"), null);
		Runnable commit = () -> {
			String v = field.getValue().trim();
			if (!v.equals(current.get()) && valid.test(v)) {
				send.accept(v, result -> {
					if (!result.ok()) {
						TeamFlows.refused(result);
					}
				});
			}
		};
		field.onEnter(commit);
		save.onPress(commit);
		this.syncs.add(() -> {
			if (!field.isFocused()) {
				field.setValue(current.get());
			}
		});
		PaintedCard card = new PaintedCard(w, 24, (g, f) -> {
			label(g, f, key, may);
			String v = field.getValue().trim();
			boolean dirty = !v.equals(current.get());
			field.setInvalid(dirty && !valid.test(v));
			save.active = may && dirty && valid.test(v);
			save.visible = may;
		}).flat().narrate(TeamsText.t("settings." + key));
		card.add(field, 82, 2);
		card.add(save, w - 22, 3);
		return card;
	}

	private PaintedCard colourRow(int w, boolean may) {
		CFHGuiSwatches swatches = new CFHGuiSwatches(0, 0, w - 82 - 6, Palette.ACCENTS, 0xFF000000 | team().color, c -> {
			CompoundTag a = new CompoundTag();
			a.putInt("color", c & 16777215);
			TeamFlows.set("set_color", a, () -> {
				for (Runnable sync : this.syncs) {
					sync.run();
				}
			});
		});
		swatches.active = may;
		this.syncs.add(() -> swatches.setSelectedColor(0xFF000000 | team().color));
		int h = Math.max(24, swatches.getHeight() + 10);
		PaintedCard card = new PaintedCard(w, h, (g, f) -> label(g, f, "colour", may)).flat().narrate(TeamsText.t("settings.colour"));
		card.add(swatches, 82, (h - swatches.getHeight()) / 2);
		return card;
	}

	private PaintedCard bannerRow(int w, boolean may) {
		PaintedCard card = new PaintedCard(
				w,
				50,
				(g, f) -> {
					label(g, f, "banner", may);
					TeamData t = team();
					TeamsDraw.banner(g, t.banner, f.x() + 82 + 4, f.y() + 4, 20, 1.0F, 1.2F, f.alpha());
					Palette p = Theme.current().palette();
					TeamsDraw.textFit(
						g, TeamsText.t("settings.banner.layers", t.banner.layers.size(), 6), f.x() + 82 + 34, f.y() + 8, f.w() - 82 - 42, p.textMuted(), f.alpha()
					);
				}
			)
			.flat()
			.narrate(TeamsText.t("settings.banner"));
		CFHGuiButton edit = new CFHGuiButton(
				0, 0, 96, 18, TeamsText.t("settings.banner.edit"), () -> this.flows().dialogs().banner(team().banner, TeamActions::setBanner)
			)
			.icon(Icon.BRUSH);
		edit.setWidth(Math.max(84, edit.preferredWidth() + 10));
		edit.active = may;
		card.add(edit, 116, 24);
		return card;
	}

	private PaintedCard privacyRow(int w, boolean may) {
		boolean stacked = w - 82 - 6 < 190;
		int controlW = stacked ? w - 12 : w - 82 - 6;
		CFHGuiSegmented<Privacy> choice = new CFHGuiSegmented<>(0, 0, controlW, List.of(Privacy.values()), team().privacy, TeamsText::privacy, v -> {
			CompoundTag a = new CompoundTag();
			a.putString("privacy", v.name());
			TeamFlows.set("set_privacy", a, () -> {
				for (Runnable sync : this.syncs) {
					sync.run();
				}
			});
		});
		choice.optionTip(TeamsText::privacyHint);
		choice.active = may;
		this.syncs.add(() -> choice.setValue(team().privacy));
		int h = stacked ? 46 : 24;
		PaintedCard card = new PaintedCard(w, h, (g, f) -> {
			label(g, f, "privacy", may);
			if (stacked) {
				Palette p = Theme.current().palette();
				TeamsDraw.textFit(g, TeamsText.privacyHint((Privacy)choice.value()), f.x() + 82 + 4, f.y() + 8, f.w() - 82 - 10, p.textDim(), f.alpha());
			}
		}).flat().narrate(TeamsText.t("settings.privacy"));
		card.add(choice, stacked ? 6 : 82, stacked ? 23 : 2);
		return card;
	}

	private PaintedCard friendlyFireRow(int w, boolean may) {
		CFHGuiToggle toggle = new CFHGuiToggle(0, 0, 22, Component.empty(), team().friendlyFire, v -> {
			CompoundTag a = new CompoundTag();
			a.putBoolean("on", v);
			TeamFlows.set("set_friendly_fire", a, () -> {
				for (Runnable sync : this.syncs) {
					sync.run();
				}
			});
		});
		toggle.active = may;
		this.syncs.add(() -> toggle.setValue(team().friendlyFire));
		PaintedCard card = new PaintedCard(
				w,
				24,
				(g, f) -> {
					Theme th = Theme.current();
					Palette p = th.palette();
					TeamsDraw.textFit(
						g, TeamsText.t("settings.friendly_fire"), f.x() + 6, f.y() + 8, f.w() - 40, may ? (th.isVanilla() ? -1 : p.text()) : p.textMuted(), f.alpha()
					);
					f.tip(0, 0, f.w() - 34, 24, TeamsText.t("settings.friendly_fire.desc"));
				}
			)
			.flat()
			.narrate(TeamsText.t("settings.friendly_fire"));
		card.add(toggle, w - 22 - 6, 2);
		return card;
	}

	private static PaintedCard preference(TeamsSettings.Setting s, int w) {
		AbstractWidget control = (AbstractWidget)(switch (s.type()) {
			case BOOL -> new CFHGuiToggle(0, 0, 22, Component.empty(), Boolean.TRUE.equals(s.get().get()), v -> s.set().accept(v));
			case CHOICE -> new CFHGuiDropdown<>(0, 0, Math.min(112, w / 2), s.choices(), String.valueOf(s.get().get()), s::choiceLabel, v -> s.set().accept(v));
			default -> new CFHGuiSlider(
				0,
				0,
				Math.min(112, w / 2 - 8),
				Component.empty(),
				s.min(),
				s.max(),
				s.step(),
				((Number)s.get().get()).doubleValue(),
				v -> s.format() != null
						? s.format().apply(v).getString()
						: (s.type() == TeamsSettings.Type.INT ? Integer.toString((int)Math.round(v)) : String.format(Locale.ROOT, "%.2f", v)),
				v -> {
				}
			)
			.compact(true)
			.onCommit(v -> s.set().accept(v));
		});
		int controlW = control.getWidth();
		PaintedCard card = new PaintedCard(w, 24, (g, f) -> {
			Theme th = Theme.current();
			TeamsDraw.textFit(g, s.label(), f.x() + 6, f.y() + 8, f.w() - controlW - 18, th.isVanilla() ? -1 : th.palette().text(), f.alpha());
			f.tip(0, 0, f.w() - controlW - 10, 24, s.tooltip());
		}).flat().narrate(s.label());
		card.add(control, w - controlW - 6, (24 - control.getHeight()) / 2);
		return card;
	}

	private PaintedCard dangerRow(TeamData t, int w) {
		boolean owner = TeamsView.rank() == Rank.OWNER;
		List<CFHGuiButton> buttons = new ArrayList<>();
		if (owner) {
			buttons.add(
				new CFHGuiButton(0, 0, 80, 18, TeamsText.t("action.transfer"), () -> this.flows().transferPick())
					.icon(Icon.CROWN)
					.tip(TeamsText.t("settings.transfer.desc"))
			);
			buttons.add(
				new CFHGuiButton(0, 0, 80, 18, TeamsText.t("action.disband"), () -> this.flows().disband())
					.variant(Variant.DANGER)
					.icon(Icon.TRASH)
					.tip(TeamsText.t("settings.disband.desc"))
			);
		} else {
			buttons.add(
				new CFHGuiButton(0, 0, 80, 18, TeamsText.t("action.leave"), () -> this.flows().leave())
					.variant(Variant.DANGER)
					.icon(Icon.EXIT)
					.tip(TeamsText.t("settings.leave.desc"))
			);
		}

		int total = 0;

		for (CFHGuiButton b : buttons) {
			b.setWidth(Math.max(72, b.preferredWidth() + 10));
			total += b.getWidth() + 4;
		}

		boolean stacked = total + 100 > w;
		int h = stacked ? 46 : 30;
		PaintedCard card = new PaintedCard(w, h, (g, f) -> {
			Palette p = Theme.current().palette();
			CFHGuiDraw.rect(g, f.x(), f.y() + 3, 2, f.h() - 6, Colors.scaleAlpha(p.danger(), f.alpha()));
			Icons.draw(g, Icon.WARNING, f.x() + 8, f.y() + 7, 8, Colors.scaleAlpha(p.danger(), f.alpha()));
			Component words = TeamsText.t(owner ? "settings.danger.owner" : "settings.danger.member", t.name);
			TeamsDraw.textFit(g, words, f.x() + 20, f.y() + 7, stacked ? f.w() - 28 : f.w() - 28 - buttonsWidth(buttons), p.textMuted(), f.alpha());
			if (!stacked) {
				TeamsDraw.textFit(g, TeamsText.t("settings.danger.asked"), f.x() + 20, f.y() + 17, f.w() - 28 - buttonsWidth(buttons), p.textDim(), f.alpha());
			}
		}).flat();
		int x = w - 6;

		for (int i = buttons.size() - 1; i >= 0; i--) {
			CFHGuiButton b = buttons.get(i);
			x -= b.getWidth();
			card.add(b, x, stacked ? 22 : 6);
			x -= 4;
		}

		return card;
	}

	private static int buttonsWidth(List<CFHGuiButton> buttons) {
		int total = 4;

		for (CFHGuiButton b : buttons) {
			total += b.getWidth() + 4;
		}

		return total;
	}

	private static final class ConfigSide {
		static void open(TeamsHubScreen from) {
			TeamsSettingsTab.open(from);
		}
	}
}
