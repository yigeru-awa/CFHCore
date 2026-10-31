package com.onehumanawa.cfhcore.content.gui.core.config;

import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Style;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public final class CoreConfig {
   public String layout = "OVERHAUL";
   public Map<String, ScreenOverride> screens = defaultScreens();
   public boolean customLayout = true;
   public boolean layoutMigrated = true;
   public boolean setupDone = false;
   public boolean yieldToMenuMods = true;
   public String skin = "DARK";
   public String accent = "#8A1C1C";
   public boolean accentMigrated = false;
   public double motion = (double)1.0F;
   public boolean headingFont = true;
   public String pixelFont = "pixeloid";
   public boolean transitions = true;
   public String containerOpen = "POP";
   public int containerOpenMs = 110;
   public boolean quickActions = true;
   public boolean uiSounds = true;
   public boolean toasts = true;
   public int radius = 3;
   public boolean smoothCorners = false;
   public String cursor = "SYSTEM";
   public boolean blurInGame = true;
   public boolean translucentPanels = true;
   public boolean compactAutoScale = true;
   public String menuBackground = "title";
   public String startupScene = "factory";
   public boolean startupTimer = true;
   public boolean devMode = false;
   public boolean devGrid = true;
   public int devSnap = 4;
   public String devPencil = "BOTTOM_RIGHT";
   public String reskinScope = "ALLOWLIST";
   public List<String> reskinAllowlist = new ArrayList(List.of("me.shedaniel.clothconfig2", "dev.isxander.yacl3", "com.mrcrayfish.configured", "net.neoforged.neoforge.client.gui", "com.terraformersmc.modmenu", "com.blamejared.controlling", "com.mojang.realmsclient", "net.caffeinemc.mods.sodium.client.gui", "net.irisshaders.iris.gui"));
   public List<String> reskinDenylist = new ArrayList(List.of("de.keksuccino.fancymenu", "mezz.jei", "com.simibubi.create"));
   public boolean xaeroMigrated = false;
   public boolean reskinContainers = true;
   public String betterInventoryStyle = "CFHGUI";

   private static Map<String, ScreenOverride> defaultScreens() {
      Map<String, ScreenOverride> screens = new LinkedHashMap();

      for(String slot : new String[]{"minecraft:disconnected", "cfhgui_quests:book"}) {
         ScreenOverride custom = new ScreenOverride();
         custom.layout = "CUSTOM";
         screens.put(slot, custom);
      }

      return screens;
   }

   // the overhaul layout is the only supported layout; the field is kept for config compatibility
   public Layout layout() {
      return Layout.OVERHAUL;
   }

   public void setLayout(Layout l) {
      this.layout = Layout.OVERHAUL.name();
      this.customLayout = true;
   }

   public boolean hasCustomLayout() {
      return true;
   }

   public void setCustomLayout(boolean on) {
      this.setLayout(Layout.OVERHAUL);
   }

   public @Nullable ScreenOverride override(String slotId) {
      return this.screens == null ? null : (ScreenOverride)this.screens.get(slotId);
   }

   public ScreenOverride overrideOrCreate(String slotId) {
      if (this.screens == null) {
         this.screens = new LinkedHashMap();
      }

      return (ScreenOverride)this.screens.computeIfAbsent(slotId, (k) -> new ScreenOverride());
   }

   public void clearEmptyOverrides() {
      if (this.screens != null) {
         Iterator<Map.Entry<String, ScreenOverride>> it = this.screens.entrySet().iterator();

         while(it.hasNext()) {
            ScreenOverride o = (ScreenOverride)((Map.Entry)it.next()).getValue();
            if (o == null || o.isEmpty()) {
               it.remove();
            }
         }

      }
   }

   public static boolean settle(JsonConfig<CoreConfig> file) {
      CoreConfig c = file.get();
      c.layoutMigrated = !file.fileHas("customLayout") || file.fileHas("layout");
      return c.migrate();
   }

   public boolean migrate() {
      boolean changed = false;
      if (!this.accentMigrated) {
         if ("#D9805E".equalsIgnoreCase(this.accent)) {
            this.accent = (new CoreConfig()).accent;
         }

         this.accentMigrated = true;
         changed = true;
      }

      if (!this.xaeroMigrated) {
         if (this.reskinDenylist != null) {
            this.reskinDenylist.remove("xaero");
         }

         this.xaeroMigrated = true;
         changed = true;
      }

      if (this.layoutMigrated) {
         return changed;
      } else {
         this.setLayout(this.customLayout ? Layout.CUSTOM : Layout.VANILLA);
         this.layoutMigrated = true;
         return true;
      }
   }

   // the CFHGui skin is always on; the field is kept for config compatibility
   public boolean isVanillaSkin() {
      return false;
   }

   public Style style() {
      return Style.CFHGUI;
   }

   public void setStyle(Style s) {
      this.skin = "DARK";
   }

   public boolean customStyle() {
      return true;
   }

   public void setCustomStyle(boolean on) {
      this.skin = "DARK";
   }

   public static final class ScreenOverride {
      public @Nullable String layout;
      public @Nullable String style;

      public boolean isEmpty() {
         return this.layout == null && this.style == null;
      }
   }
}
