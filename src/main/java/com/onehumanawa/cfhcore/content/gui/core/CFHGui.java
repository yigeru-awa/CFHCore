package com.onehumanawa.cfhcore.content.gui.core;

import com.onehumanawa.cfhcore.content.gui.core.config.CoreConfig;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.net.blob.BlobChannel;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CFHGui {
   public static final String MOD_ID = "cfhgui";
   public static final Logger LOGGER = LoggerFactory.getLogger("CFHGui");
   private static volatile boolean initialised;
   private static JsonConfig<CoreConfig> config;

   public static synchronized void init() {
      if (!initialised) {
         initialised = true;
         config = JsonConfig.<CoreConfig>of("core", CoreConfig.class, CoreConfig::new);
         boolean unmatched = !config.fileHas("betterInventoryStyle");
         if (unmatched) {
            (config.get()).betterInventoryStyle = "";
         }

         boolean settled = CoreConfig.settle(config);
         if (settled || unmatched) {
            config.save();
         }

         BlobChannel.register();
         LOGGER.info("[CFHGui] core {} on {}", version(), CFHGuiPlatform.get().loader());
      }
   }

   public static CoreConfig config() {
      init();
      return config.get();
   }

   public static JsonConfig<CoreConfig> configFile() {
      init();
      return config;
   }

   public static String version() {
      return (String)CFHGuiPlatform.get().modInfo("cfhcore").map((i) -> i.version()).orElse("dev");
   }

   public static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
   }

   private CFHGui() {
   }
}
