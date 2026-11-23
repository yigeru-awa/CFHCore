package com.onehumanawa.cfhcore.content.gui.veinminer.client;

import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfigApi;
import com.onehumanawa.cfhcore.content.gui.config.api.SettingsTabs;
import com.onehumanawa.cfhcore.content.gui.config.api.SettingsTabs.Tab;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.Choice;
import com.onehumanawa.cfhcore.content.gui.config.option.NumberRange;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionValues;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.veinminer.VeinShape;
import com.onehumanawa.cfhcore.content.gui.veinminer.config.VeinClientConfig;
import com.onehumanawa.cfhcore.content.gui.veinminer.config.VeinServerConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class VeinSettingsTab {
	public static final String TAB_ID = "veinminer";
	private static final VeinClientConfig CLIENT_DEFAULTS = new VeinClientConfig();
	private static final VeinServerConfig SERVER_DEFAULTS = new VeinServerConfig();

	public static void register() {
		SettingsTabs.register("gameplay", new Tab("veinminer", text("tab"), Icon.BOLT, VeinSettingsTab::sections, 120));
		CFHGuiConfigApi.registerReloadHook("veinminer", () -> {
			VeinClientConfig.reload();
			if (editable()) {
				VeinServerConfig.reload();
			}
		});
		CoreActions.SCREEN_FACTORIES.put("cfhgui_config:hub/gameplay/veinminer", p -> CFHGuiConfigApi.hub(p, "gameplay/veinminer"));
	}

	private static List<Section> sections() {
		List<Section> out = new ArrayList<>();
		Section mode = new Section("mode", text("section.mode"), text("section.mode.about"));
		mode.add(
			Binding.of("cfhgui_veinminer:key", OptionType.INFO, text("key"))
				.tooltip(text("key.tip"))
				.getter(() -> VeinKeys.ACTIVATE.getTranslatedKeyMessage())
				.format(v -> (Component)(v instanceof Component c ? c : Component.literal(String.valueOf(v))))
				.searchWords("veinminer key bind grave tilde ultimine")
		);
		mode.add(choice("activation", VeinClientConfig.Activation.values(), c -> c.activation().name(), (c, v) -> c.activation = v, CLIENT_DEFAULTS.activation));
		mode.add(bool("scrollCycles", c -> c.scrollCycles, (c, v) -> c.scrollCycles = v, CLIENT_DEFAULTS.scrollCycles));
		List<Choice> shapes = new ArrayList<>();

		for (VeinShape s : VeinShape.all()) {
			shapes.add(new Choice(s.id(), s.label()));
		}

		mode.add(
			Binding.of("cfhgui_veinminer:shape", OptionType.CHOICE, text("shape"))
				.tooltip(text("shape.tip"))
				.choices(shapes)
				.getter(() -> VeinClientConfig.get().shape().id())
				.setter(v -> {
					VeinClient.setShape(VeinShape.parse(OptionValues.asString(v), VeinShape.SHAPELESS));
					VeinClientConfig.edit(c -> {
					});
				})
				.def(CLIENT_DEFAULTS.shape)
				.searchWords("veinminer shape tunnel staircase square column shapeless")
		);
		mode.add(bool("diagonals", c -> c.diagonals, (c, v) -> c.diagonals = v, CLIENT_DEFAULTS.diagonals));
		mode.add(bool("treeDiagonals", c -> c.treeDiagonals, (c, v) -> c.treeDiagonals = v, CLIENT_DEFAULTS.treeDiagonals));
		out.add(mode);
		Section preview = new Section("preview", text("section.preview"), text("section.preview.about"));
		preview.add(bool("preview", c -> c.preview, (c, v) -> c.preview = v, CLIENT_DEFAULTS.preview));
		preview.add(
			choice("previewStyle", VeinClientConfig.Style.values(), c -> c.style().name(), (c, v) -> c.previewStyle = v, CLIENT_DEFAULTS.previewStyle)
				.enabledIf(() -> VeinClientConfig.get().preview)
		);
		preview.add(
			bool("throughWalls", c -> c.throughWalls, (c, v) -> c.throughWalls = v, CLIENT_DEFAULTS.throughWalls).enabledIf(() -> VeinClientConfig.get().preview)
		);
		preview.add(
			choice("color", VeinClientConfig.ColorMode.values(), c -> c.colorMode().name(), (c, v) -> c.color = v, CLIENT_DEFAULTS.color)
				.enabledIf(() -> VeinClientConfig.get().preview)
		);
		preview.add(
			Binding.of("cfhgui_veinminer:customColor", OptionType.COLOR, text("customColor"))
				.tooltip(text("customColor.tip"))
				.getter(() -> VeinClientConfig.get().customArgb())
				.setter(v -> VeinClientConfig.edit(c -> c.customColor = Colors.toHex(OptionValues.asColor(v, -2055080))))
				.def(CLIENT_DEFAULTS.customArgb())
				.enabledIf(() -> VeinClientConfig.get().preview && VeinClientConfig.get().colorMode() == VeinClientConfig.ColorMode.CUSTOM)
				.searchWords("veinminer preview colour color custom")
		);
		preview.add(
			Binding.of("cfhgui_veinminer:maxPreview", OptionType.INT, text("maxPreview"))
				.tooltip(text("maxPreview.tip"))
				.range(new NumberRange(16.0, 1024.0, 16.0))
				.getter(() -> VeinClientConfig.get().maxPreview)
				.setter(v -> VeinClientConfig.edit(c -> c.maxPreview = OptionValues.asInt(v, CLIENT_DEFAULTS.maxPreview)))
				.def(CLIENT_DEFAULTS.maxPreview)
				.enabledIf(() -> VeinClientConfig.get().preview)
				.searchWords("veinminer preview most blocks outline")
		);
		out.add(preview);
		Section hud = new Section("hud", text("section.hud"), text("section.hud.about"));
		hud.add(bool("hud", c -> c.hud, (c, v) -> c.hud = v, CLIENT_DEFAULTS.hud));
		hud.add(
			choice("hudPosition", VeinClientConfig.HudPosition.values(), c -> c.hudPosition().name(), (c, v) -> c.hudPosition = v, CLIENT_DEFAULTS.hudPosition)
				.enabledIf(() -> VeinClientConfig.get().hud)
		);
		out.add(hud);
		out.add(serverSection());
		return out;
	}

	private static Section serverSection() {
		Section s = new Section("server", text("section.server"), text("section.server.about"));
		s.add(
			Binding.of("cfhgui_veinminer:server_source", OptionType.INFO, text("server.source"))
				.getter(() -> text(editable() ? "server.source.local" : (ClientRules.config() != null ? "server.source.remote" : "server.source.none")))
				.format(v -> (Component)(v instanceof Component c ? c : Component.literal(String.valueOf(v))))
				.searchWords("veinminer server rules")
		);
		s.add(sbool("enabled", c -> c.enabled, (c, v) -> c.enabled = v, SERVER_DEFAULTS.enabled));
		s.add(schoice("permission", VeinServerConfig.Permission.values(), c -> c.permission().name(), (c, v) -> c.permission = v, SERVER_DEFAULTS.permission));
		s.add(sint("maxBlocks", c -> c.maxBlocks, (c, v) -> c.maxBlocks = v, SERVER_DEFAULTS.maxBlocks, new NumberRange(1.0, 1024.0, 1.0)));
		s.add(sint("blocksPerTick", c -> c.blocksPerTick, (c, v) -> c.blocksPerTick = v, SERVER_DEFAULTS.blocksPerTick, new NumberRange(1.0, 256.0, 1.0)));
		s.add(sbool("requireHarvestTool", c -> c.requireHarvestTool, (c, v) -> c.requireHarvestTool = v, SERVER_DEFAULTS.requireHarvestTool));
		s.add(
			Binding.of("cfhgui_veinminer:server.exhaustionPerBlock", OptionType.DOUBLE, text("server.exhaustionPerBlock"))
				.tooltip(text("server.exhaustionPerBlock.tip"))
				.range(new NumberRange(0.0, 2.0, 0.005))
				.getter(() -> view().exhaustionPerBlock)
				.setter(v -> editServer(c -> c.exhaustionPerBlock = OptionValues.asDouble(v, SERVER_DEFAULTS.exhaustionPerBlock)))
				.def(SERVER_DEFAULTS.exhaustionPerBlock)
				.enabledIf(VeinSettingsTab::editable)
				.searchWords("veinminer server hunger exhaustion food")
		);
		s.add(sbool("stopBeforeToolBreaks", c -> c.stopBeforeToolBreaks, (c, v) -> c.stopBeforeToolBreaks = v, SERVER_DEFAULTS.stopBeforeToolBreaks));
		s.add(schoice("drops", VeinServerConfig.Drops.values(), c -> c.drops().name(), (c, v) -> c.drops = v, SERVER_DEFAULTS.drops));
		s.add(sbool("harvestCrops", c -> c.harvestCrops, (c, v) -> c.harvestCrops = v, SERVER_DEFAULTS.harvestCrops));
		s.add(sbool("toolActions", c -> c.toolActions, (c, v) -> c.toolActions = v, SERVER_DEFAULTS.toolActions));
		s.add(sbool("allowDiagonals", c -> c.allowDiagonals, (c, v) -> c.allowDiagonals = v, SERVER_DEFAULTS.allowDiagonals));
		s.add(sbool("allowBlockEntities", c -> c.allowBlockEntities, (c, v) -> c.allowBlockEntities = v, SERVER_DEFAULTS.allowBlockEntities));

		for (VeinShape shape : VeinShape.all()) {
			s.add(
				Binding.of(
						"cfhgui_veinminer:server.shape." + shape.id(),
						OptionType.BOOLEAN,
						Component.translatable("cfhgui_veinminer.settings.server.shape", new Object[]{shape.label()})
					)
					.tooltip(shape.description())
					.getter(() -> view().shapes.enabled(shape))
					.setter(v -> editServer(c -> c.shapes.set(shape, OptionValues.asBoolean(v, true))))
					.def(true)
					.enabledIf(VeinSettingsTab::editable)
					.searchWords("veinminer server shape " + shape.id().replace('_', ' '))
			);
		}

		s.add(slist("allow", c -> c.allow, (c, v) -> c.allow = v, SERVER_DEFAULTS.allow));
		s.add(slist("deny", c -> c.deny, (c, v) -> c.deny = v, SERVER_DEFAULTS.deny));
		s.add(slist("oreTags", c -> c.oreTags, (c, v) -> c.oreTags = v, SERVER_DEFAULTS.oreTags));
		s.add(sbool("deepslateOres", c -> c.deepslateOres, (c, v) -> c.deepslateOres = v, SERVER_DEFAULTS.deepslateOres));
		s.add(sbool("logsByWood", c -> c.logsByWood, (c, v) -> c.logsByWood = v, SERVER_DEFAULTS.logsByWood));
		s.add(sbool("plantsAsOne", c -> c.plantsAsOne, (c, v) -> c.plantsAsOne = v, SERVER_DEFAULTS.plantsAsOne));
		s.add(slist("groups", c -> c.groups, (c, v) -> c.groups = v, SERVER_DEFAULTS.groups));
		return s;
	}

	private static boolean editable() {
		Minecraft mc = Minecraft.getInstance();
		return mc.hasSingleplayerServer() || mc.getConnection() == null;
	}

	private static VeinServerConfig view() {
		if (editable()) {
			return VeinServerConfig.get();
		} else {
			VeinServerConfig remote = ClientRules.config();
			return remote != null ? remote : SERVER_DEFAULTS;
		}
	}

	private static void editServer(Consumer<VeinServerConfig> change) {
		if (editable()) {
			VeinServerConfig.edit(change);
		}
	}

	private static Binding bool(String field, Function<VeinClientConfig, Boolean> get, BiConsumer<VeinClientConfig, Boolean> put, boolean def) {
		return Binding.of("cfhgui_veinminer:" + field, OptionType.BOOLEAN, text(field))
			.tooltip(text(field + ".tip"))
			.getter(() -> get.apply(VeinClientConfig.get()))
			.setter(v -> VeinClientConfig.edit(c -> put.accept(c, OptionValues.asBoolean(v, def))))
			.def(def)
			.searchWords("veinminer " + words(field));
	}

	private static Binding choice(String field, Enum<?>[] values, Function<VeinClientConfig, String> get, BiConsumer<VeinClientConfig, String> put, String def) {
		return Binding.of("cfhgui_veinminer:" + field, OptionType.CHOICE, text(field))
			.tooltip(text(field + ".tip"))
			.choices(choices(field, values))
			.getter(() -> get.apply(VeinClientConfig.get()))
			.setter(v -> VeinClientConfig.edit(c -> put.accept(c, OptionValues.asString(v))))
			.def(def.toUpperCase(Locale.ROOT))
			.searchWords("veinminer " + words(field));
	}

	private static Binding sbool(String field, Function<VeinServerConfig, Boolean> get, BiConsumer<VeinServerConfig, Boolean> put, boolean def) {
		return Binding.of("cfhgui_veinminer:server." + field, OptionType.BOOLEAN, text("server." + field))
			.tooltip(text("server." + field + ".tip"))
			.getter(() -> get.apply(view()))
			.setter(v -> editServer(c -> put.accept(c, OptionValues.asBoolean(v, def))))
			.def(def)
			.enabledIf(VeinSettingsTab::editable)
			.searchWords("veinminer server " + words(field));
	}

	private static Binding sint(String field, Function<VeinServerConfig, Integer> get, BiConsumer<VeinServerConfig, Integer> put, int def, NumberRange range) {
		return Binding.of("cfhgui_veinminer:server." + field, OptionType.INT, text("server." + field))
			.tooltip(text("server." + field + ".tip"))
			.range(range)
			.getter(() -> get.apply(view()))
			.setter(v -> editServer(c -> put.accept(c, OptionValues.asInt(v, def))))
			.def(def)
			.enabledIf(VeinSettingsTab::editable)
			.searchWords("veinminer server " + words(field));
	}

	private static Binding schoice(String field, Enum<?>[] values, Function<VeinServerConfig, String> get, BiConsumer<VeinServerConfig, String> put, String def) {
		return Binding.of("cfhgui_veinminer:server." + field, OptionType.CHOICE, text("server." + field))
			.tooltip(text("server." + field + ".tip"))
			.choices(choices("server." + field, values))
			.getter(() -> get.apply(view()))
			.setter(v -> editServer(c -> put.accept(c, OptionValues.asString(v))))
			.def(def)
			.enabledIf(VeinSettingsTab::editable)
			.searchWords("veinminer server " + words(field));
	}

	private static Binding slist(String field, Function<VeinServerConfig, List<String>> get, BiConsumer<VeinServerConfig, List<String>> put, List<String> def) {
		return Binding.of("cfhgui_veinminer:server." + field, OptionType.LIST, text("server." + field))
			.tooltip(text("server." + field + ".tip"))
			.getter(() -> new ArrayList<>(get.apply(view())))
			.setter(v -> editServer(c -> put.accept(c, new ArrayList<>(OptionValues.asList(v)))))
			.def(List.copyOf(def))
			.enabledIf(VeinSettingsTab::editable)
			.searchWords("veinminer server " + words(field));
	}

	private static List<Choice> choices(String field, Enum<?>[] values) {
		List<Choice> out = new ArrayList<>();

		for (Enum<?> e : values) {
			out.add(new Choice(e.name(), text(field + "." + e.name().toLowerCase(Locale.ROOT))));
		}

		return out;
	}

	private static String words(String field) {
		return field.replaceAll("([a-z])([A-Z0-9])", "$1 $2").toLowerCase(Locale.ROOT);
	}

	private static Component text(String key) {
		return Component.translatable("cfhgui_veinminer.settings." + key);
	}

	private VeinSettingsTab() {
	}
}
