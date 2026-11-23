package com.onehumanawa.cfhcore.content.gui.teams.client.vanilla;

import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette.AccentPreset;
import com.onehumanawa.cfhcore.content.gui.teams.client.TeamActions;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamFlows;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsDraw;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsView;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.settings.TeamsSettingsTab;
import com.onehumanawa.cfhcore.content.gui.teams.model.Permission;
import com.onehumanawa.cfhcore.content.gui.teams.model.Privacy;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class SettingsTab extends TeamsTab {
	private static final int COLUMN = 150;
	private static final int GAP = 8;
	@Nullable
	private EditBox name;
	@Nullable
	private EditBox tag;
	@Nullable
	private EditBox motto;
	@Nullable
	private Button save;
	@Nullable
	private CycleButton<AccentPreset> colour;
	private final List<AbstractWidget> grid = new ArrayList<>();
	private int left;
	private int top;

	SettingsTab(VanillaTeamsScreen screen) {
		super(screen, "settings");
	}

	@Nullable
	@Override
	protected Component message() {
		return null;
	}

	@Override
	protected void build() {
		this.grid.clear();
		this.name = this.tag = this.motto = null;
		this.save = null;
		this.colour = null;
		TeamData t = TeamsView.team();
		if (t != null) {
			boolean may = TeamsView.may(Permission.SETTINGS);
			Component rank = may ? null : TeamsText.t("settings.rank", TeamsText.rank(Permission.SETTINGS.min()));
			this.name = this.box(t.name, 24, "settings.name", may);
			this.tag = this.box(t.tag, 5, "settings.tag", may);
			this.motto = this.box(t.motto, 96, "settings.motto", may);
			AccentPreset current = (AccentPreset)Palette.ACCENTS.get(0);

			for (AccentPreset p : Palette.ACCENTS) {
				if ((p.color() & 16777215) == (t.color & 16777215)) {
					current = p;
				}
			}

			this.colour = CycleButton.<AccentPreset>builder(px -> Component.literal(px.name()))
				.withValues(Palette.ACCENTS)
				.withInitialValue(current)
				.create(0, 0, 150, 20, TeamsText.t("settings.colour"), (button, value) -> {
					CompoundTag a = new CompoundTag();
					a.putInt("color", value.color() & 16777215);
					TeamFlows.set("set_color", a, this.screen::remake);
				});
			this.cell(this.colour, may, rank, "settings.colour.desc");
			this.cell(
				Button.builder(TeamsText.t("vanilla.banner"), b -> this.flows().dialogs().banner(t.banner, TeamActions::setBanner)).size(150, 20).build(),
				may,
				rank,
				"settings.banner.desc"
			);
			this.cell(
				CycleButton.<Privacy>builder(TeamsText::privacy)
					.withValues(List.of(Privacy.values()))
					.withInitialValue(t.privacy)
					.create(0, 0, 150, 20, TeamsText.t("settings.privacy"), (button, value) -> {
						CompoundTag a = new CompoundTag();
						a.putString("privacy", value.name());
						TeamFlows.set("set_privacy", a, this.screen::remake);
					}),
				may,
				rank,
				"settings.privacy.desc"
			);
			this.cell(CycleButton.onOffBuilder(t.friendlyFire).create(0, 0, 150, 20, TeamsText.t("settings.friendly_fire"), (button, value) -> {
				CompoundTag a = new CompoundTag();
				a.putBoolean("on", value);
				TeamFlows.set("set_friendly_fire", a, this.screen::remake);
			}), may, rank, "settings.friendly_fire.desc");
		}

		this.cell(Button.builder(TeamsText.t("vanilla.display"), b -> this.openDisplay()).size(150, 20).build(), true, null, "settings.display.config");
		if (t != null) {
			if (TeamsView.rank() == Rank.OWNER) {
				this.cell(Button.builder(TeamsText.t("vanilla.transfer"), b -> this.flows().transferPick()).size(150, 20).build(), true, null, "settings.transfer.desc");
				this.cell(Button.builder(TeamsText.t("vanilla.disband"), b -> this.flows().disband()).size(150, 20).build(), true, null, "settings.disband.desc");
			} else {
				this.cell(Button.builder(TeamsText.t("action.leave"), b -> this.flows().leave()).size(150, 20).build(), true, null, "settings.leave.desc");
			}

			this.save = this.footButton(TeamsText.t("vanilla.save"), this::save);
			this.update();
		}
	}

	private EditBox box(String value, int maxLength, String key, boolean may) {
		EditBox b = this.add(new EditBox(this.mc.font, 0, 0, 100, 20, TeamsText.t(key)));
		b.setMaxLength(maxLength);
		put(b, value);
		b.setEditable(may);
		b.setTooltip(Tooltip.create(TeamsText.t(key + ".desc")));
		b.setResponder(s -> this.update());
		return b;
	}

	private static void put(EditBox box, String value) {
		if (!box.getValue().equals(value)) {
			box.setValue(value);
		}

		box.moveCursorToStart(false);
	}

	private void cell(AbstractWidget widget, boolean may, @Nullable Component whyNot, String tipKey) {
		widget.active = may;
		widget.setTooltip(Tooltip.create((Component)(!may && whyNot != null ? whyNot : TeamsText.t(tipKey))));
		this.grid.add(this.add(widget));
	}

	private void openDisplay() {
		if (CFHGuiPlatform.get().isModLoaded("cfhgui_config")) {
			SettingsTab.ConfigSide.open(this.screen);
		} else {
			this.mc.setScreen(new VanillaPrefsScreen(this.screen));
		}
	}

	private boolean dirty(@Nullable EditBox box, String current) {
		return box != null && !box.getValue().trim().equals(current);
	}

	private void update() {
		TeamData t = TeamsView.team();
		if (t != null && this.save != null && this.name != null && this.tag != null) {
			boolean valid = TeamFlows.validName(this.name.getValue()) && TeamFlows.validTag(this.tag.getValue());
			this.save.active = TeamsView.may(Permission.SETTINGS)
				&& valid
				&& (this.dirty(this.name, t.name) || this.dirty(this.tag, t.tag) || this.dirty(this.motto, t.motto));
			this.name.setTextColor(TeamFlows.validName(this.name.getValue()) ? -2039584 : -43691);
			this.tag.setTextColor(TeamFlows.validTag(this.tag.getValue()) ? -2039584 : -43691);
		}
	}

	private void save() {
		TeamData t = TeamsView.team();
		if (t != null && this.save != null && this.save.active && this.name != null && this.tag != null && this.motto != null) {
			if (this.dirty(this.name, t.name)) {
				TeamActions.rename(this.name.getValue().trim(), result -> {
					if (!result.ok()) {
						TeamFlows.refused(result);
					}
				});
			}

			if (this.dirty(this.tag, t.tag)) {
				TeamActions.setTag(this.tag.getValue().trim(), result -> {
					if (!result.ok()) {
						TeamFlows.refused(result);
					}
				});
			}

			if (this.dirty(this.motto, t.motto)) {
				TeamActions.setMotto(this.motto.getValue().trim());
			}
		}
	}

	boolean enter() {
		if (this.save != null && this.save.active) {
			this.save();
			return true;
		} else {
			return false;
		}
	}

	@Override
	void refresh() {
		TeamData t = TeamsView.team();
		if (t != null && this.name != null && this.tag != null && this.motto != null) {
			if (!this.name.isFocused()) {
				put(this.name, t.name);
			}

			if (!this.tag.isFocused()) {
				put(this.tag, t.tag);
			}

			if (!this.motto.isFocused()) {
				put(this.motto, t.motto);
			}

			if (this.colour != null) {
				for (AccentPreset p : Palette.ACCENTS) {
					if ((p.color() & 16777215) == (t.color & 16777215)) {
						this.colour.setValue(p);
					}
				}
			}

			this.update();
		} else {
			super.refresh();
		}
	}

	@Override
	protected void layout(ScreenRectangle area) {
		int width = Math.min(308, area.width() - 20);
		this.left = area.left() + (area.width() - width) / 2;
		this.top = area.top() + 8;
		int y = this.top;
		if (this.name != null && this.tag != null && this.motto != null) {
			int label = 44;
			place(this.name, this.left + 44, y, width - 44);
			y += 24;
			place(this.tag, this.left + 44, y, 54);
			place(this.motto, this.left + 44 + 54 + 44, y, width - 88 - 54);
			y += 28;
		}

		int column = (width - 8) / 2;

		for (int i = 0; i < this.grid.size(); i++) {
			AbstractWidget w = this.grid.get(i);
			place(w, this.left + i % 2 * (column + 8), y + i / 2 * 24, column);
		}
	}

	private static void place(AbstractWidget w, int x, int y, int width) {
		w.setX(x);
		w.setY(y);
		w.setWidth(width);
	}

	@Override
	void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		Palette p = Theme.current().palette();
		TeamData t = TeamsView.team();
		if (t == null) {
			Component none = TeamsView.personal() != null ? TeamsText.t("solo.share") : TeamsText.shared("state.no_team");
			if (TeamsView.available()) {
				TeamsDraw.textCentered(g, none, this.area.left() + this.area.width() / 2, this.area.bottom() - 20, p.textMuted(), 1.0F);
			}
		} else if (this.name != null && this.tag != null && this.motto != null) {
			TeamsDraw.text(g, TeamsText.t("settings.name"), this.left, this.name.getY() + 6, p.text(), 1.0F);
			TeamsDraw.text(g, TeamsText.t("settings.tag"), this.left, this.tag.getY() + 6, p.text(), 1.0F);
			TeamsDraw.text(g, TeamsText.t("settings.motto"), this.motto.getX() - 40, this.motto.getY() + 6, p.text(), 1.0F);
			if (this.colour != null) {
				int sx = this.colour.getX() + this.colour.getWidth() - 14;
				int sy = this.colour.getY() + 6;
				g.fill(sx - 1, sy - 1, sx + 9, sy + 9, -16777216);
				g.fill(sx, sy, sx + 8, sy + 8, TeamsDraw.opaque(((AccentPreset)this.colour.getValue()).color() & 16777215));
			}
		}
	}

	private static final class ConfigSide {
		static void open(VanillaTeamsScreen from) {
			TeamsSettingsTab.open(from);
		}
	}
}
