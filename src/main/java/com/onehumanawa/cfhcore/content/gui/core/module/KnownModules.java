package com.onehumanawa.cfhcore.content.gui.core.module;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;

public final class KnownModules {
   public static final String CORE = "cfhgui";
   public static final String UI = "cfhgui";
   public static final String CONFIG = "cfhgui_config";
   public static final String MULTIPLAYER = "cfhgui_multiplayer";
   public static final String CHAT = "cfhgui_chat";
   public static final String PROFILE = "cfhgui_profile";
   public static final String TEAMS = "cfhgui_teams";
   public static final String QUESTS = "cfhgui_quests";
   public static final String CRAFTING = "cfhgui_crafting";
   public static final String SERVER = "cfhgui_server";
   public static final String BRIDGE = "cfhgui_bridge";
   public static final String VEINMINER = "cfhgui_veinminer";
   private static final String URL = "https://github.com/falling-colud/CFHGui";
   private static final List<KnownModule> ALL;

   private static KnownModule entry(String id, Icon icon) {
      return new KnownModule(id, Component.translatable("cfhgui.module." + id), icon, Component.translatable("cfhgui.module." + id + ".pitch"), "https://github.com/falling-colud/CFHGui");
   }

   public static List<KnownModule> all() {
      return ALL;
   }

   public static List<KnownModule> features() {
      return ALL.subList(1, ALL.size());
   }

   public static Optional<KnownModule> get(String id) {
      for(KnownModule m : ALL) {
         if (m.id().equals(id)) {
            return Optional.of(m);
         }
      }

      return Optional.empty();
   }

   public static Component name(String id) {
      return (Component)get(id).map(KnownModule::name).orElseGet(() -> (Component)Modules.get(id).map(CFHGuiModule::displayName).orElse(Component.literal(id)));
   }

   public static boolean installed(String id) {
      return Modules.isLoaded(id);
   }

   private KnownModules() {
   }

   static {
      ALL = List.of(entry("cfhgui", Icon.CFHGUI), entry("cfhgui", Icon.HOME), entry("cfhgui_config", Icon.SLIDERS), entry("cfhgui_multiplayer", Icon.FRIENDS), entry("cfhgui_chat", Icon.CHAT), entry("cfhgui_profile", Icon.USER), entry("cfhgui_teams", Icon.TEAM), entry("cfhgui_quests", Icon.QUEST), entry("cfhgui_crafting", Icon.GRID), entry("cfhgui_server", Icon.SHIELD), entry("cfhgui_bridge", Icon.LINK), entry("cfhgui_veinminer", Icon.BOLT));
   }
}
