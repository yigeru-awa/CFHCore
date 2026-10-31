package com.onehumanawa.cfhcore.content.gui.menu;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType;
import com.onehumanawa.cfhcore.content.gui.core.module.CFHGuiModule;
import com.onehumanawa.cfhcore.content.gui.menu.client.MenuActions;
import com.onehumanawa.cfhcore.content.gui.menu.client.MenuClient;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CFHGuiMenu implements CFHGuiModule {
   public static final String MOD_ID = "cfhgui";
   public static final Logger LOGGER = LoggerFactory.getLogger("CFHGui Menu");
   public static final CFHGuiMenu MODULE = new CFHGuiMenu();
   private static JsonConfig<MenuConfig> config;

   private CFHGuiMenu() {
   }

   public static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath("cfhgui", path);
   }

   public static synchronized JsonConfig<MenuConfig> configFile() {
      if (config == null) {
         config = JsonConfig.of("menu", MenuConfig.class, MenuConfig::new);
         if (!((MenuConfig)config.get()).friendsPanelOptIn || !config.fileHas("friendsPanelOptIn")) {
            config.update((c) -> {
               c.showFriendsPanel = false;
               c.friendsPanelOptIn = true;
            });
         }
      }

      return config;
   }

   public static MenuConfig config() {
      return (MenuConfig)configFile().get();
   }

   public String id() {
      return "cfhgui";
   }

   public Component displayName() {
      return Component.translatable("cfhgui.name");
   }

   public Icon icon() {
      return Icon.HOME;
   }

   public void init() {
      CFHGui.init();
      configFile();
      LOGGER.info("[CFHGui Menu] init");
   }

   public void initClient() {
      MenuClient.init();
      LOGGER.info("[CFHGui Menu] client init");
   }

   public List<CFHGuiModule.HubEntry> hubEntries() {
      return MenuClient.hubEntries();
   }

   public List<ActionType> actions() {
      return MenuActions.all();
   }
}
