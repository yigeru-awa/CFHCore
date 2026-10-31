package com.onehumanawa.cfhcore.content.gui.config.api;

import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;

public final class SettingsTabs {
   public static final String GAMEPLAY = "gameplay";
   public static final String INTERFACE = "interface";
   public static final String MULTIPLAYER = "multiplayer";
   public static final String CUSTOMIZATION = "customization";
   private static final Map<String, Map<String, Tab>> TABS = new LinkedHashMap();

   public static synchronized void register(String categoryId, Tab tab) {
      ((Map)TABS.computeIfAbsent(categoryId, (k) -> new LinkedHashMap())).put(tab.id(), tab);
   }

   public static synchronized List<Tab> tabs(String categoryId) {
      Map<String, Tab> m = (Map)TABS.get(categoryId);
      if (m == null) {
         return List.of();
      } else {
         List<Tab> out = new ArrayList(m.values());
         out.sort(Comparator.comparingInt(Tab::order));
         return out;
      }
   }

   private SettingsTabs() {
   }

   public static record Tab(String id, Component title, Icon icon, Supplier<List<Section>> sections, int order) {
   }
}
