package com.onehumanawa.cfhcore.content.gui.core.screen.slot;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.config.CoreConfig;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.theme.PixelFont;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class EarlyWindowProps {
   public static final String FILE = "earlywindow.properties";
   private static String last;

   public static synchronized void write() {
      CoreConfig cfg = CFHGui.config();
      Layout layout = MenuSlots.effective("cfhgui:loading");
      Style style = MenuSlots.style("cfhgui:loading");
      String var10000 = layout.name();
      String text = "# Written by CFHGui whenever its settings change; read by the NeoForge start-up window (cfhgui-earlywindow).\n# Do not edit: change the settings in the game instead.\nlayout=" + var10000 + "\nstyle=" + style.name() + "\nloadingScreens=" + (layout != Layout.VANILLA) + "\naccent=" + (cfg.accent == null ? "" : cfg.accent.trim()) + "\nradius=" + cfg.radius + "\nheadingFont=" + cfg.headingFont + "\npixelFont=" + PixelFont.parse(cfg.pixelFont).key() + "\nstartupScene=" + ("island".equalsIgnoreCase(cfg.startupScene == null ? "" : cfg.startupScene.trim()) ? "island" : "factory") + "\nstartupTimer=" + cfg.startupTimer + "\ntitleLayout=" + MenuSlots.effective("minecraft:title").name() + "\n";
      if (!text.equals(last)) {
         Path file = JsonConfig.dir().resolve("earlywindow.properties");

         try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("earlywindow.properties.tmp");
            Files.writeString(tmp, text, StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            last = text;
         } catch (IOException e) {
            CFHGui.LOGGER.warn("[CFHGui] could not write {}: {}", file, e.toString());
         }

      }
   }

   private EarlyWindowProps() {
   }
}
