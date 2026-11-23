package com.onehumanawa.cfhcore.content.gui.quests.client.ui;

import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfigApi;
import com.onehumanawa.cfhcore.content.gui.config.api.SettingsTabs;
import com.onehumanawa.cfhcore.content.gui.config.api.SettingsTabs.Tab;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.Choice;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.quests.QuestsConfig;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.chat.Component;

final class QuestsSettingsTab {
	static final String TAB_ID = "quests";
	static final String PATH = "gameplay/quests";
	private static final QuestsConfig DEFAULTS = new QuestsConfig();

	static void register() {
		SettingsTabs.register("gameplay", new Tab("quests", Component.translatable("cfhgui_quests.settings.tab"), Icon.QUEST, QuestsSettingsTab::sections, 120));
		CFHGuiConfigApi.registerReloadHook("quests", () -> QuestsConfig.file().load());
	}

	private static List<Section> sections() {
		List<Section> out = new ArrayList<>();
		Section tracker = new Section("tracker", text("section.tracker"), text("section.tracker.about"));
		tracker.add(bool("tracker", c -> c.tracker, (c, v) -> c.tracker = v));
		List<Choice> corners = new ArrayList<>();

		for (String corner : new String[]{"TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"}) {
			corners.add(new Choice(corner, text("corner." + corner.toLowerCase(Locale.ROOT))));
		}

		tracker.add(
			row("trackerAnchor", OptionType.CHOICE)
				.choices(corners)
				.getter(() -> QuestsConfig.get().trackerAnchor)
				.setter(v -> set(c -> c.trackerAnchor = String.valueOf(v)))
				.def(DEFAULTS.trackerAnchor)
				.enabledIf(() -> QuestsConfig.get().tracker)
		);
		tracker.add(number("trackerX", 0, 400, 1, c -> c.trackerX, (c, v) -> c.trackerX = v.intValue()).enabledIf(() -> QuestsConfig.get().tracker));
		tracker.add(number("trackerY", 0, 400, 1, c -> c.trackerY, (c, v) -> c.trackerY = v.intValue()).enabledIf(() -> QuestsConfig.get().tracker));
		tracker.add(
			row("trackerScale", OptionType.DOUBLE)
				.range(0.5, 2.0, 0.05)
				.getter(() -> QuestsConfig.get().trackerScale)
				.setter(v -> set(c -> c.trackerScale = ((Number)v).doubleValue()))
				.def(DEFAULTS.trackerScale)
				.format(v -> Component.literal(Math.round(((Number)v).doubleValue() * 100.0) + "%"))
				.enabledIf(() -> QuestsConfig.get().tracker)
		);
		tracker.add(number("trackerMax", 1, 10, 1, c -> c.trackerMax, (c, v) -> c.trackerMax = v.intValue()).enabledIf(() -> QuestsConfig.get().tracker));
		tracker.add(bool("trackerAuto", c -> c.trackerAuto, (c, v) -> c.trackerAuto = v).enabledIf(() -> QuestsConfig.get().tracker));
		out.add(tracker);
		Section notices = new Section("notices", text("section.notices"), text("section.notices.about"));
		notices.add(bool("toasts", c -> c.toasts, (c, v) -> c.toasts = v));
		notices.add(bool("chapterBanner", c -> c.chapterBanner, (c, v) -> c.chapterBanner = v));
		notices.add(bool("celebrations", c -> c.celebrations, (c, v) -> c.celebrations = v));
		notices.add(bool("sounds", c -> c.sounds, (c, v) -> c.sounds = v));
		out.add(notices);
		Section book = new Section("book", text("section.book"), text("section.book.about"));
		book.add(bool("flowingLines", c -> c.flowingLines, (c, v) -> c.flowingLines = v));
		book.add(bool("subtreePreview", c -> c.subtreePreview, (c, v) -> c.subtreePreview = v));
		book.add(
			number("subtreePreviewDelayMs", 0, 1500, 20, c -> c.subtreePreviewDelayMs, (c, v) -> c.subtreePreviewDelayMs = v.intValue())
				.format(v -> Component.translatable("cfhgui_quests.settings.ms", new Object[]{((Number)v).intValue()}))
				.enabledIf(() -> QuestsConfig.get().subtreePreview)
		);
		book.add(bool("zoomToPointer", c -> c.zoomToPointer, (c, v) -> c.zoomToPointer = v));
		book.add(bool("overview", c -> c.overview, (c, v) -> c.overview = v));
		book.add(bool("turnTallTrees", c -> c.turnTallTrees, (c, v) -> c.turnTallTrees = v));
		book.add(bool("dimCompleted", c -> c.dimCompleted, (c, v) -> c.dimCompleted = v));
		book.add(bool("inventoryButton", c -> c.inventoryButton, (c, v) -> c.inventoryButton = v));
		book.add(bool("advancements", c -> c.advancements, (c, v) -> c.advancements = v));
		book.add(bool("ftbTakeover", c -> c.ftbTakeover, (c, v) -> c.ftbTakeover = v));
		book.add(
			Binding.of("cfhgui_quests:open", OptionType.ACTION, text("open"))
				.tooltip(text("open.tip"))
				.action(Component.translatable("cfhgui_quests.settings.open.button"), () -> QuestsClient.open(null, null))
				.actionIcon(Icon.QUEST)
				.searchWords("quests cfhgui_quests book")
		);
		out.add(book);
		return out;
	}

	private static Component text(String key) {
		return Component.translatable("cfhgui_quests.settings." + key);
	}

	private static void set(Consumer<QuestsConfig> change) {
		change.accept(QuestsConfig.get());
		QuestsConfig.save();
	}

	private static Binding row(String field, OptionType type) {
		return Binding.of("cfhgui_quests:" + field, type, text(field)).tooltip(text(field + ".tip")).searchWords("quests cfhgui_quests " + field);
	}

	private static Binding bool(String field, Function<QuestsConfig, Boolean> get, BiConsumer<QuestsConfig, Boolean> put) {
		return row(field, OptionType.BOOLEAN)
			.getter(() -> get.apply(QuestsConfig.get()))
			.setter(v -> set(c -> put.accept(c, Boolean.TRUE.equals(v))))
			.def(get.apply(DEFAULTS));
	}

	private static Binding number(String field, int min, int max, int step, Function<QuestsConfig, Number> get, BiConsumer<QuestsConfig, Number> put) {
		return row(field, OptionType.INT).range((double)min, (double)max, (double)step).getter(() -> get.apply(QuestsConfig.get())).setter(v -> {
			if (v instanceof Number n) {
				set(c -> put.accept(c, n));
			}
		}).def(get.apply(DEFAULTS));
	}

	private QuestsSettingsTab() {
	}
}
