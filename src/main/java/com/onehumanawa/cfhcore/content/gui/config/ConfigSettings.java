package com.onehumanawa.cfhcore.content.gui.config;

import com.google.gson.JsonObject;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public final class ConfigSettings {
   public List<String> favourites = new ArrayList();
   public List<Preset> presets = new ArrayList();
   public double musicVolumeBeforeMute = (double)1.0F;
   public List<String> collapsedSections = new ArrayList();
   public boolean swapVanillaScreens = true;
   public Map<String, String> lastTabs = new LinkedHashMap();
   public boolean dfPageRetired = false;
   public boolean liveViewMinimised = false;
   public List<String> pinnedMods = new ArrayList();
   public String modsSort = "NAME";
   public boolean modsHideLibraries = false;
   private static JsonConfig<ConfigSettings> file;

   public static synchronized JsonConfig<ConfigSettings> file() {
      if (file == null) {
         file = JsonConfig.of("config", ConfigSettings.class, ConfigSettings::new);
      }

      return file;
   }

   public static ConfigSettings get() {
      return (ConfigSettings)file().get();
   }

   public static boolean isFavourite(String id) {
      return get().favourites.contains(id);
   }

   public static void toggleFavourite(String id) {
      file().update((c) -> {
         if (!c.favourites.remove(id)) {
            c.favourites.add(id);
         }

      });
   }

   public static List<String> pinnedMods() {
      List<String> pinned = get().pinnedMods;
      return pinned == null ? List.of() : pinned;
   }

   public static boolean isPinnedMod(String modId) {
      return pinnedMods().contains(modId);
   }

   public static void togglePinnedMod(String modId) {
      file().update((c) -> {
         if (c.pinnedMods == null) {
            c.pinnedMods = new ArrayList();
         }

         if (!c.pinnedMods.remove(modId)) {
            c.pinnedMods.add(modId);
         }

      });
   }

   public static @Nullable String lastTab(String key) {
      Map<String, String> m = get().lastTabs;
      return m == null ? null : (String)m.get(key);
   }

   public static void setLastTab(String key, String tab) {
      if (!tab.equals(lastTab(key))) {
         file().update((c) -> {
            if (c.lastTabs == null) {
               c.lastTabs = new LinkedHashMap();
            }

            c.lastTabs.put(key, tab);
         });
      }
   }

   public static boolean isCollapsed(String key) {
      return get().collapsedSections.contains(key);
   }

   public static void setCollapsed(String key, boolean collapsed) {
      file().update((c) -> {
         if (collapsed) {
            if (!c.collapsedSections.contains(key)) {
               c.collapsedSections.add(key);
            }
         } else {
            c.collapsedSections.remove(key);
         }

      });
   }

   public static final class Preset {
      public String name = "";
      public String description = "";
      public JsonObject values = new JsonObject();
      public transient boolean builtin;

      public Preset() {
      }

      public Preset(String name, JsonObject values, boolean builtin) {
         this.name = name;
         this.values = values;
         this.builtin = builtin;
      }
   }
}
