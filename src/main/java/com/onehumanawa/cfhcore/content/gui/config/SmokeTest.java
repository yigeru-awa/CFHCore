package com.onehumanawa.cfhcore.content.gui.config;

import com.onehumanawa.cfhcore.content.gui.config.api.SettingsTabs;
import com.onehumanawa.cfhcore.content.gui.config.doc.Documents;
import com.onehumanawa.cfhcore.content.gui.config.doc.FileDocument;
import com.onehumanawa.cfhcore.content.gui.config.editor.FileEditorScreen;
import com.onehumanawa.cfhcore.content.gui.config.hub.ConfigHubScreen;
import com.onehumanawa.cfhcore.content.gui.config.mods.ModConfigTargets;
import com.onehumanawa.cfhcore.content.gui.config.mods.ModLibraries;
import com.onehumanawa.cfhcore.content.gui.config.option.KeySearch;
import com.onehumanawa.cfhcore.content.gui.config.page.ModsPage;
import com.onehumanawa.cfhcore.content.gui.config.preset.Presets;
import com.onehumanawa.cfhcore.content.gui.config.resolver.VanillaOptions;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.config.ui.TabHost;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.platform.ModInfo;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.core.screen.SidebarPage;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.popup.Popups;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

final class SmokeTest {
   private static int ticks;
   private static int step;
   private static int pageIdx;
   private static int tabIdx;
   private static int pass;
   private static int worldTicks;
   private static ConfigHubScreen hub;
   private static List<String> pageIds = List.of();
   private static boolean done;
   private static String animShot;
   private static int animWait;

   static void install() {
      CFHGuiConfig.LOGGER.warn("[CFHGui Config] SMOKE TEST enabled: driving the settings screens, then exiting");
      CoreActions.SCREEN_FACTORIES.putIfAbsent("cfhgui_config:smoke", (p) -> new ConfigHubScreen(p, (String)null));
      Supplier<List<Section>> sample = () -> List.of(Section.of("first", Component.literal("First"), VanillaOptions.all("fov", "bobView")), Section.of("second", Component.literal("Second"), VanillaOptions.all("autoJump")));
      SettingsTabs.register("gameplay", new SettingsTabs.Tab("sample", Component.literal("Sample module"), Icon.BLOCK, sample, 100));
      CFHGuiEvents.CLIENT_TICK_END.register(SmokeTest::tick);
   }

   private static void shot(String name) {
      Minecraft mc = Minecraft.getInstance();
      Screenshot.grab(mc.gameDirectory, "cfhgui-config-" + name + ".png", mc.getMainRenderTarget(), (c) -> CFHGuiConfig.LOGGER.info("[smoke] {}", c.getString()));
   }

   private static void tick() {
      if (!done) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.getOverlay() == null) {
            if (animShot != null && ++animWait >= 2) {
               shot(animShot);
               animShot = null;
            }

            if (step >= 20 || mc.screen != null) {
               ++ticks;
               if (ticks >= 60 && ticks % 20 == 0) {
                  try {
                     advance(mc);
                  } catch (Exception e) {
                     CFHGuiConfig.LOGGER.error("[smoke] step {} failed", step, e);
                     done = true;
                     mc.stop();
                  }

               }
            }
         }
      }
   }

   private static void skin(boolean vanilla) {
      CFHGui.configFile().update((c) -> c.skin = vanilla ? "VANILLA" : "DARK");
      Theme.reload();
   }

   private static void advance(Minecraft mc) {
      String skin = pass == 0 ? "dark" : "vanilla";
      switch (step) {
         case 0:
            skin(pass == 1);
            if (pass == 0 && !ConfigSettings.isFavourite("optionsTxt:renderDistance")) {
               ConfigSettings.toggleFavourite("optionsTxt:renderDistance");
            }

            hub = new ConfigHubScreen(mc.screen, "video");
            mc.setScreen(hub);
            pageIds = hub.pages().stream().map(SidebarPage::id).toList();
            CFHGuiConfig.LOGGER.info("[smoke] {} pass: pages {}", skin, pageIds);
            pageIdx = 0;
            tabIdx = 0;
            step = 1;
            break;
         case 1:
            if (mc.screen != hub) {
               mc.setScreen(hub);
               return;
            }

            String page = ((String)pageIds.get(pageIdx)).replace(':', '_');
            shot(skin + "-" + String.format("%02d", pageIdx) + "-" + page + "-" + tabIdx);
            SidebarPage var5 = hub.currentPage();
            int var10000;
            if (var5 instanceof TabHost th) {
               var10000 = th.tabCount();
            } else {
               var10000 = 1;
            }

            int n = var10000;
            if (tabIdx + 1 < n) {
               ++tabIdx;
               ((TabHost)hub.currentPage()).cycleTab(1, false);
               return;
            }

            if (n > 1) {
               ((TabHost)hub.currentPage()).cycleTab(1, false);
            }

            tabIdx = 0;
            ++pageIdx;
            if (pageIdx < pageIds.size()) {
               hub.showPage((String)pageIds.get(pageIdx));
            } else {
               step = 2;
            }
            break;
         case 2:
            hub.open("multiplayer/chat");
            SidebarPage var12 = hub.leaf();
            if (var12 instanceof TabHost th) {
               th.cycleTab(1, false);
            }

            step = 3;
            break;
         case 3:
            shot(skin + "-secondary-tab");
            hub.open("video");
            hub.debugSearch("chat");
            step = 4;
            break;
         case 4:
            shot(skin + "-search");
            Popups.closeAll();
            hub.debugSearch("");
            if (pass == 0) {
               searchChecks(mc);
               modListChecks();
            }

            hub.open("interface");
            SidebarPage r = hub.currentPage();
            if (r instanceof TabHost th) {
               th.cycleTab(1, false);
            }

            animShot = skin + "-transition";
            animWait = 0;
            step = 5;
            break;
         case 5:
            FileDocument doc = (FileDocument)ConfigPlatform.get().nativeConfig("neoforge-client.toml").orElseGet(() -> Documents.json(JsonConfig.dir().resolve("core.json"), "cfhgui"));
            CFHGuiConfig.LOGGER.info("[smoke] editor: {} ({} sections)", doc.path(), doc.sections().size());
            mc.setScreen(new FileEditorScreen(hub, doc));
            step = 6;
            break;
         case 6:
            shot(skin + "-editor");
            step = 7;
            break;
         case 7:
            if (pass == 0) {
               int before = (Integer)mc.options.renderDistance().get();
               Presets.ApplyResult res = Presets.apply((ConfigSettings.Preset)Presets.builtins().get(0));
               CFHGuiConfig.LOGGER.info("[smoke] preset '{}': applied {} skipped {}; renderDistance {} -> {}", new Object[]{((ConfigSettings.Preset)Presets.builtins().get(0)).name, res.applied(), res.skipped(), before, mc.options.renderDistance().get()});
               pass = 1;
               step = 0;
               mc.setScreen(new TitleScreen());
            } else {
               skin(false);
               step = 20;
               worldTicks = 0;
               LevelSettings settings = new LevelSettings("cfhgui-config-smoke", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
               mc.createWorldOpenFlows().createFreshLevel("cfhgui-config-smoke-" + System.currentTimeMillis() % 100000L, settings, new WorldOptions(20260924L, false, false), WorldPresets::createNormalWorldDimensions, new TitleScreen());
            }
         case 8:
         case 9:
         case 10:
         case 11:
         case 12:
         case 13:
         case 14:
         case 15:
         case 16:
         case 17:
         case 18:
         case 19:
         default:
            break;
         case 20:
            if (mc.level == null || mc.player == null || mc.screen != null) {
               worldTicks = 0;
               return;
            }

            if (++worldTicks < 4) {
               return;
            }

            hub = new ConfigHubScreen((Screen)null, "gameplay");
            mc.setScreen(hub);
            step = 21;
            break;
         case 21:
            shot("world-dark-gameplay");
            skin(true);
            hub = new ConfigHubScreen((Screen)null, "gameplay");
            mc.setScreen(hub);
            step = 22;
            break;
         case 22:
            shot("world-vanilla-gameplay");
            skin(false);
            done = true;
            CFHGuiConfig.LOGGER.warn("[smoke] DONE - stopping client");
            mc.stop();
      }

   }

   private static void searchChecks(Minecraft mc) {
      hub.open("customization/mods");
      SidebarPage var2 = hub.leaf();
      if (var2 instanceof ModsPage m) {
         m.filter("cfhgui");
         check("the mods tab keeps its own search through the rebuild", "cfhgui".equals(m.search()), m.search());
         m.filter("");
      } else {
         check("customization/mods opens the mods tab", false, hub.leaf());
      }

      KeyMapping[] all = mc.options.keyMappings;
      int tried = 0;

      for(KeyMapping k : all) {
         String key = k.getTranslatedKeyMessage().getString().toLowerCase(Locale.ROOT);
         if (key.codePointCount(0, key.length()) == 1 && tried++ < 6) {
            int hits = 0;
            int onKey = 0;

            for(KeyMapping o : all) {
               if (KeySearch.matches(o, key, "")) {
                  ++hits;
               }

               if (o.getTranslatedKeyMessage().getString().equalsIgnoreCase(key)) {
                  ++onKey;
               }
            }

            check("'" + key + "' finds only the binds on that key", KeySearch.matches(k, key, "") && hits == onKey, hits + " found, " + onKey + " on the key");
         }
      }

      check("'jump' finds the jump bind by name", KeySearch.matches(mc.options.keyJump, "jump", ""), null);
   }

   private static void modListChecks() {
      Map<String, ModLibraries.Reason> libraries = ModLibraries.all();
      CFHGuiConfig.LOGGER.info("[smoke] {} of {} mods count as libraries: {}", new Object[]{libraries.size(), CFHGuiPlatform.get().allMods().size(), new TreeMap(libraries)});
      hub.open("customization/mods");
      SidebarPage var2 = hub.leaf();
      if (!(var2 instanceof ModsPage m)) {
         check("customization/mods opens the mods tab", false, hub.leaf());
      } else {
         ArrayList var16 = new ArrayList(ConfigSettings.pinnedMods());
         String sortBefore = ConfigSettings.get().modsSort;
         boolean hideBefore = ConfigSettings.get().modsHideLibraries;
         ConfigSettings.file().update((c) -> {
            c.pinnedMods = new ArrayList();
            c.modsSort = "NAME";
            c.modsHideLibraries = false;
         });
         hub.refreshPage();
         List<String> byName = m.shown();
         if (byName.size() < 3) {
            CFHGuiConfig.LOGGER.info("[smoke] only {} mods: the list checks need three", byName.size());
         } else {
            String last = (String)byName.get(byName.size() - 1);
            String middle = (String)byName.get(byName.size() / 2);
            m.togglePin(last);
            m.togglePin(middle);
            check("pinned mods stand first, in the order they were pinned", m.shown().subList(0, 2).equals(List.of(last, middle)), m.shown().subList(0, 2));
            ConfigSettings.file().load();
            check("the pins are in config.json", ConfigSettings.pinnedMods().equals(List.of(last, middle)), ConfigSettings.pinnedMods());
            ConfigSettings.file().update((c) -> c.pinnedMods.add(0, "cfhgui_config_smoke_not_installed"));
            hub.refreshPage();
            check("a pinned mod that is not installed is passed over", m.shown().size() == byName.size() && ((String)m.shown().get(0)).equals(last), m.shown().size());
            ConfigSettings.file().update((c) -> c.modsSort = "LAST_MODIFIED");
            hub.refreshPage();
            List<String> byChange = m.shown();
            long newer = Long.MAX_VALUE;
            String outOfOrder = null;

            for(String id : byChange.subList(2, byChange.size())) {
               long t = ModConfigTargets.lastModified(ModConfigTargets.find(id).files());
               if (t > newer) {
                  outOfOrder = id;
               }

               newer = t;
            }

            List<String> withoutLibraries;
            List<String> hidden;
            boolean var20;
            label36: {
               check("by last change, the mod whose config was written last comes first (after the pinned)", outOfOrder == null, outOfOrder);
               CFHGuiConfig.LOGGER.info("[smoke] by last change: {}", byChange.subList(0, Math.min(8, byChange.size())));
               ConfigSettings.file().update((c) -> {
                  c.modsSort = "NAME";
                  c.modsHideLibraries = true;
               });
               hub.refreshPage();
               withoutLibraries = m.shown();
               hidden = byName.stream().filter((idx) -> libraries.containsKey(idx) && !idx.equals(last) && !idx.equals(middle)).toList();
               if (withoutLibraries.size() == byName.size() - hidden.size()) {
                  Stream var10001 = hidden.stream();
                  Objects.requireNonNull(withoutLibraries);
                  if (var10001.noneMatch(withoutLibraries::contains)) {
                     var20 = true;
                     break label36;
                  }
               }

               var20 = false;
            }

            int var10002 = hidden.size();
            check("hiding libraries leaves out exactly the libraries that are not pinned", var20, var10002 + " hidden, " + withoutLibraries.size() + " shown");
            if (!hidden.isEmpty()) {
               String library = (String)hidden.get(0);
               m.filter((String)CFHGuiPlatform.get().modInfo(library).map(ModInfo::name).orElse(library));
               check("the search still finds a hidden library", m.shown().contains(library), library);
               m.filter("");
               m.togglePin(library);
               check("a pinned library is not hidden", m.shown().contains(library), library);
            }
         }

         ConfigSettings.file().update((c) -> {
            c.pinnedMods = var16;
            c.modsSort = sortBefore;
            c.modsHideLibraries = hideBefore;
         });
         hub.refreshPage();
      }
   }

   private static void check(String name, boolean ok, Object detail) {
      if (ok) {
         CFHGuiConfig.LOGGER.info("[smoke] CHECK ok: {}", name);
      } else {
         CFHGuiConfig.LOGGER.error("[smoke] CHECK FAIL: {} ({})", name, detail);
      }

   }

   private SmokeTest() {
   }
}
