package com.onehumanawa.cfhcore.content.gui.teams.client.ui.settings;

import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfigApi;
import com.onehumanawa.cfhcore.content.gui.config.api.SettingsTabs;
import com.onehumanawa.cfhcore.content.gui.config.api.SettingsTabs.Tab;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.Choice;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.teams.TeamsConfig;
import com.onehumanawa.cfhcore.content.gui.teams.client.ui.kit.TeamsText;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class TeamsSettingsTab {
	public static final String TAB_ID = "teams";
	public static final String TAB_PATH = "gameplay/teams";

	public static void register() {
		SettingsTabs.register("gameplay", new Tab("teams", TeamsText.t("settings.tab"), Icon.TEAM, TeamsSettingsTab::sections, 110));
		CFHGuiConfigApi.registerReloadHook("teams", () -> TeamsConfig.file().load());
	}

	public static void open(@Nullable Screen parent) {
		CFHGuiConfigApi.openHub(parent, "gameplay/teams");
	}

	private static List<Section> sections() {
		List<Section> out = new ArrayList<>();

		for (TeamsSettings.Group g : TeamsSettings.groups()) {
			Section s = new Section(g.id(), g.title(), null);

			for (TeamsSettings.Setting setting : g.settings()) {
				s.add(binding(setting));
			}

			out.add(s);
		}

		return out;
	}

	private static Binding binding(TeamsSettings.Setting s) {
		OptionType type = switch (s.type()) {
			case BOOL -> OptionType.BOOLEAN;
			case INT -> OptionType.INT;
			case DOUBLE -> OptionType.DOUBLE;
			case CHOICE -> OptionType.CHOICE;
		};
		Binding b = Binding.of("cfhgui_teams:" + s.id(), type, s.label())
			.tooltip(s.tooltip())
			.getter(s.get())
			.setter(s.set())
			.def(s.def())
			.searchWords("teams cfhgui_teams team party claims waypoints " + s.id());
		if (s.type() == TeamsSettings.Type.INT || s.type() == TeamsSettings.Type.DOUBLE) {
			b.range(s.min(), s.max(), s.step());
		}

		if (s.type() == TeamsSettings.Type.CHOICE) {
			List<Choice> choices = new ArrayList<>();

			for (String id : s.choices()) {
				choices.add(new Choice(id, s.choiceLabel(id)));
			}

			b.choices(choices);
		}

		if (s.format() != null) {
			b.format(s.format());
		}

		return b;
	}

	private TeamsSettingsTab() {
	}
}
