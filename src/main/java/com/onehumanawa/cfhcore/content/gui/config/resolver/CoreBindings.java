package com.onehumanawa.cfhcore.content.gui.config.resolver;

import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.Choice;
import com.onehumanawa.cfhcore.content.gui.config.option.NumberRange;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionValues;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.DevModeButton;
import com.onehumanawa.cfhcore.content.gui.core.client.CFHGuiCursor;
import com.onehumanawa.cfhcore.content.gui.core.client.DevModeButton.Place;
import com.onehumanawa.cfhcore.content.gui.core.client.CFHGuiCursor.Style;
import com.onehumanawa.cfhcore.content.gui.core.config.CoreConfig;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop.Kind;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.EarlyWindowProps;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.PixelFont;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;

public final class CoreBindings {
   private static final Map<String, Supplier<Binding>> DEFS = new LinkedHashMap();
   private static final Map<String, OptionBinding> CACHE = new LinkedHashMap();
   private static final CoreConfig DEFAULTS = new CoreConfig();

   private static void save(Consumer<CoreConfig> edit) {
      CFHGui.configFile().update(edit);
      Theme.reload();
      Reskin.invalidate();
   }

   private static Binding base(String key, OptionType type) {
      return Binding.of("cfhgui:core:" + key, type, (Component)Component.translatable("cfhgui_config.core." + key)).tooltip((Component)Component.translatable("cfhgui_config.core." + key + ".tip")).searchWords("cfhgui core theme");
   }

   private static void bool(String key, Function<CoreConfig, Boolean> get, BiConsumer<CoreConfig, Boolean> set) {
      DEFS.put(key, () -> base(key, OptionType.BOOLEAN).getter(() -> get.apply(CFHGui.config())).setter((v) -> save((c) -> set.accept(c, OptionValues.asBoolean(v, false)))).def(get.apply(DEFAULTS)));
   }

   private static List<Choice> pixelFonts() {
      List<Choice> out = new ArrayList();

      for(PixelFont f : PixelFont.values()) {
         out.add(new Choice(f.key(), f.label()));
      }

      return out;
   }

   private static List<Choice> layouts() {
      List<Choice> out = new ArrayList();

      for(Layout l : Layout.values()) {
         out.add(new Choice(l.name(), Component.translatable("cfhgui.layout." + l.key())));
      }

      return out;
   }

   private static List<Choice> scopes() {
      List<Choice> out = new ArrayList();

      for(String s : List.of("VANILLA_AND_SLATE", "ALLOWLIST", "ALL_NON_CONTAINER", "NONE")) {
         out.add(new Choice(s, Component.translatable("cfhgui.reskin." + s.toLowerCase(Locale.ROOT))));
      }

      return out;
   }

   public static Optional<OptionBinding> get(String key) {
      OptionBinding c = (OptionBinding)CACHE.get(key);
      if (c != null) {
         return Optional.of(c);
      } else {
         Supplier<Binding> s = (Supplier)DEFS.get(key);
         if (s == null) {
            return Optional.empty();
         } else {
            Binding b = (Binding)s.get();
            CACHE.put(key, b);
            return Optional.of(b);
         }
      }
   }

   public static List<OptionBinding> all(String... keys) {
      List<OptionBinding> out = new ArrayList();

      for(String k : keys) {
         Optional<OptionBinding> var10000 = get(k);
         Objects.requireNonNull(out);
         var10000.ifPresent(out::add);
      }

      return out;
   }

   private CoreBindings() {
   }

   static {
      DEFS.put("accent", () -> base("accent", OptionType.COLOR).getter(() -> Colors.fromHex(CFHGui.config().accent, -7726052)).setter((v) -> save((c) -> c.accent = Colors.toHex(OptionValues.asColor(v, -7726052)))).def(-7726052));
      DEFS.put("radius", () -> base("radius", OptionType.INT).range(NumberRange.ints(0, 4)).getter(() -> CFHGui.config().radius).setter((v) -> save((c) -> c.radius = Math.max(0, Math.min(4, OptionValues.asInt(v, 3))))).def(DEFAULTS.radius));
      DEFS.put("smoothCorners", () -> base("smoothCorners", OptionType.BOOLEAN).getter(() -> CFHGui.config().smoothCorners).setter((v) -> save((c) -> c.smoothCorners = OptionValues.asBoolean(v, false))).def(DEFAULTS.smoothCorners).searchWords("cfhgui core theme corners rounded round smooth hd pixel pixelated radius anti-aliased"));
      DEFS.put("cursor", () -> base("cursor", OptionType.CHOICE).choices(Arrays.stream(Style.values()).map((c) -> new Choice(c.name(), c.title())).toList()).getter(() -> CFHGuiCursor.style().name()).setter((v) -> save((c) -> c.cursor = Style.parse(OptionValues.asString(v)).name())).def(DEFAULTS.cursor).searchWords("cfhgui core theme cursor pointer mouse arrow"));
      bool("headingFont", (c) -> c.headingFont, (c, v) -> c.headingFont = v);
      DEFS.put("pixelFont", () -> base("pixelFont", OptionType.CHOICE).choices(pixelFonts()).getter(() -> PixelFont.parse(CFHGui.config().pixelFont).key()).setter((v) -> save((c) -> c.pixelFont = PixelFont.parse(OptionValues.asString(v)).key())).def(PixelFont.DEFAULT.key()).enabledIf(() -> CFHGui.config().headingFont));
      bool("blurInGame", (c) -> c.blurInGame, (c, v) -> c.blurInGame = v);
      DEFS.put("translucentPanels", () -> base("translucentPanels", OptionType.BOOLEAN).getter(() -> CFHGui.config().translucentPanels).setter((v) -> save((c) -> c.translucentPanels = OptionValues.asBoolean(v, false))).def(DEFAULTS.translucentPanels).enabledIf(() -> CFHGui.config().customStyle()).searchWords("cfhgui core theme translucent transparent transparency glass opaque panels popups see-through"));
      DEFS.put("menuBackground", () -> base("menuBackground", OptionType.CHOICE).choices(Arrays.stream(Kind.values()).map((k) -> new Choice(k.id(), k.title())).toList()).getter(() -> MenuBackdrop.kind().id()).setter((v) -> save((c) -> c.menuBackground = Kind.of(OptionValues.asString(v)).id())).def(DEFAULTS.menuBackground).enabledIf(MenuSlots::uiModuleLoaded).searchWords("cfhgui core background backdrop scene sky title main menu menus loading"));
      DEFS.put("startupScene", () -> base("startupScene", OptionType.CHOICE).choices(List.of(new Choice("factory", Component.translatable("cfhgui.startup_scene.factory")), new Choice("island", Component.translatable("cfhgui.startup_scene.island")))).getter(() -> "island".equalsIgnoreCase(CFHGui.config().startupScene) ? "island" : "factory").setter((v) -> save((c) -> c.startupScene = "island".equalsIgnoreCase(OptionValues.asString(v)) ? "island" : "factory")).def(DEFAULTS.startupScene).enabledIf(MenuSlots::uiModuleLoaded).searchWords("cfhgui core loading start startup window vanilla island factory blocks"));
      DEFS.put("startupTimer", () -> base("startupTimer", OptionType.BOOLEAN).getter(() -> CFHGui.config().startupTimer).setter((v) -> {
            save((c) -> c.startupTimer = OptionValues.asBoolean(v, true));
            EarlyWindowProps.write();
         }).def(DEFAULTS.startupTimer).searchWords("cfhgui core loading start startup timer time clock how long fast slow seconds"));
      DEFS.put("motion", () -> base("motion", OptionType.DOUBLE).range(NumberRange.of((double)0.0F, (double)2.0F, (double)0.25F)).format((v) -> {
            double d = OptionValues.asDouble(v, (double)1.0F);
            return d <= (double)0.0F ? Component.translatable("options.off") : Component.literal(String.format(Locale.ROOT, "%.2fx", d));
         }).getter(() -> CFHGui.config().motion).setter((v) -> save((c) -> c.motion = OptionValues.asDouble(v, (double)1.0F))).def(DEFAULTS.motion));
      bool("transitions", (c) -> c.transitions, (c, v) -> c.transitions = v);
      DEFS.put("containerOpen", () -> base("containerOpen", OptionType.CHOICE).choices(Arrays.stream(com.onehumanawa.cfhcore.content.gui.core.screen.ContainerMotion.Style.values()).map((s) -> new Choice(s.name(), Component.translatable("cfhgui.container_open." + s.key()))).toList()).getter(() -> com.onehumanawa.cfhcore.content.gui.core.screen.ContainerMotion.Style.parse(CFHGui.config().containerOpen).name()).setter((v) -> save((c) -> c.containerOpen = com.onehumanawa.cfhcore.content.gui.core.screen.ContainerMotion.Style.parse(OptionValues.asString(v)).name())).def(DEFAULTS.containerOpen).searchWords("cfhgui core inventory container chest backpack open opening animation pop slide zoom motion"));
      DEFS.put("containerOpenMs", () -> base("containerOpenMs", OptionType.INT).range(NumberRange.ints(60, 300)).format((v) -> Component.literal(OptionValues.asInt(v, 110) + " ms")).getter(() -> CFHGui.config().containerOpenMs).setter((v) -> save((c) -> c.containerOpenMs = Math.max(60, Math.min(300, OptionValues.asInt(v, 110))))).def(DEFAULTS.containerOpenMs).enabledIf(() -> com.onehumanawa.cfhcore.content.gui.core.screen.ContainerMotion.Style.parse(CFHGui.config().containerOpen) != com.onehumanawa.cfhcore.content.gui.core.screen.ContainerMotion.Style.OFF).searchWords("cfhgui core inventory container open opening animation speed duration"));
      bool("uiSounds", (c) -> c.uiSounds, (c, v) -> c.uiSounds = v);
      bool("toasts", (c) -> c.toasts, (c, v) -> c.toasts = v);
      bool("quickActions", (c) -> c.quickActions, (c, v) -> c.quickActions = v);
      DEFS.put("reskinScope", () -> base("reskinScope", OptionType.CHOICE).choices(scopes()).getter(() -> CFHGui.config().reskinScope).setter((v) -> save((c) -> c.reskinScope = OptionValues.asString(v))).def(DEFAULTS.reskinScope));
      DEFS.put("reskinAllowlist", () -> base("reskinAllowlist", OptionType.LIST).getter(() -> new ArrayList(CFHGui.config().reskinAllowlist)).setter((v) -> save((c) -> c.reskinAllowlist = new ArrayList(OptionValues.asList(v)))).def(new ArrayList(DEFAULTS.reskinAllowlist)));
      bool("reskinContainers", (c) -> c.reskinContainers, (c, v) -> c.reskinContainers = v);
      DEFS.put("reskinDenylist", () -> base("reskinDenylist", OptionType.LIST).getter(() -> new ArrayList(CFHGui.config().reskinDenylist)).setter((v) -> save((c) -> c.reskinDenylist = new ArrayList(OptionValues.asList(v)))).def(new ArrayList(DEFAULTS.reskinDenylist)));
      bool("devMode", (c) -> c.devMode, (c, v) -> c.devMode = v);
      bool("devGrid", (c) -> c.devGrid, (c, v) -> c.devGrid = v);
      DEFS.put("devSnap", () -> base("devSnap", OptionType.INT).range(NumberRange.ints(1, 16)).format((v) -> Component.literal(OptionValues.asInt(v, 4) + " px")).getter(() -> CFHGui.config().devSnap).setter((v) -> save((c) -> c.devSnap = Math.max(1, OptionValues.asInt(v, 4)))).def(DEFAULTS.devSnap));
      DEFS.put("devPencil", () -> base("devPencil", OptionType.CHOICE).choices(Arrays.stream(Place.values()).map((p) -> new Choice(p.name(), p.title())).toList()).getter(() -> DevModeButton.place().name()).setter((v) -> save((c) -> c.devPencil = Place.parse(OptionValues.asString(v)).name())).def(DEFAULTS.devPencil).searchWords("cfhgui core dev mode layout editor pencil button corner hide hidden f7 key"));
   }
}
