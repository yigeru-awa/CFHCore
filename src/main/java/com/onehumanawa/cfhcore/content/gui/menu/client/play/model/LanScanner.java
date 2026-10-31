package com.onehumanawa.cfhcore.content.gui.menu.client.play.model;

import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import java.util.List;
import net.minecraft.client.server.LanServer;
import net.minecraft.client.server.LanServerDetection;
import org.jetbrains.annotations.Nullable;

public final class LanScanner {
   private final LanServerDetection.LanServerList list = new LanServerDetection.LanServerList();
   private LanServerDetection.@Nullable LanServerDetector detector;

   public LanScanner() {
      try {
         this.detector = new LanServerDetection.LanServerDetector(this.list);
         this.detector.start();
      } catch (Exception e) {
         CFHGuiMenu.LOGGER.warn("[CFHGui Menu] LAN detection unavailable: {}", e.toString());
         this.detector = null;
      }

   }

   public @Nullable List<LanServer> poll() {
      return this.list.takeDirtyServers();
   }

   public boolean isRunning() {
      return this.detector != null && this.detector.isAlive();
   }

   public void close() {
      if (this.detector != null) {
         this.detector.interrupt();
      }

      this.detector = null;
   }
}
