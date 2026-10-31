package com.onehumanawa.cfhcore.content.gui.config;

import com.onehumanawa.cfhcore.content.gui.config.hub.ConfigHubScreen;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class CFHGuiConfigApi {
   private static final Map<String, Runnable> RELOAD_HOOKS = new ConcurrentHashMap();

   public static void openHub(@Nullable Screen parent, @Nullable String page) {
      Minecraft.getInstance().setScreen(ConfigHubScreen.forLayout(parent, page));
   }

   public static Screen hub(@Nullable Screen parent, @Nullable String page) {
      return ConfigHubScreen.forLayout(parent, page);
   }

   public static void registerReloadHook(String module, Runnable reload) {
      RELOAD_HOOKS.put(module, reload);
   }

   public static boolean hasReloadHook(String module) {
      return RELOAD_HOOKS.containsKey(module);
   }

   public static void runReloadHook(String module) {
      Runnable r = (Runnable)RELOAD_HOOKS.get(module);
      if (r != null) {
         try {
            r.run();
         } catch (Exception e) {
            CFHGuiConfig.LOGGER.error("[CFHGui Config] reload hook of {} failed", module, e);
         }

      }
   }

   private CFHGuiConfigApi() {
   }
}
