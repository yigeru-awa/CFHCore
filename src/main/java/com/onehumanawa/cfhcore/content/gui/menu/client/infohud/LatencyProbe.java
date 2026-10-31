package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import java.util.Arrays;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.util.debugchart.LocalSampleLogger;

public final class LatencyProbe {
   private static final int EVERY_TICKS = 20;
   private static final int SLOW_EVERY_TICKS = 300;
   private static final int TRIES = 5;
   private static final long FRESH_MS = 5000L;
   private static final long STALL_MS = 2500L;
   private static final long[] recent = new long[5];
   private static final long[] sorted = new long[5];
   private static int count;
   private static int at;
   private static long lastPongMs;
   private static boolean hooked;
   private static int ticks;
   private static int unanswered;
   private static long waitingSinceMs;
   private static boolean wanted;
   private static int logSize;
   private static long logLast;
   private static boolean logSeen;

   private LatencyProbe() {
   }

   static void want(boolean on) {
      wanted = on;
   }

   static void tick() {
      Minecraft mc = Minecraft.getInstance();
      ClientPacketListener connection = mc.getConnection();
      if (wanted && connection != null && mc.player != null && mc.getSingleplayerServer() == null) {
         readLog(mc);
         if (!mc.getDebugOverlay().showNetworkCharts()) {
            boolean silent;
            synchronized(LatencyProbe.class) {
               silent = count == 0 && unanswered >= 5;
            }

            if (++ticks >= (silent ? 300 : 20)) {
               ticks = 0;
               long now = Util.getMillis();
               synchronized(LatencyProbe.class) {
                  if (unanswered == 0) {
                     waitingSinceMs = now;
                  }

                  if (unanswered < Integer.MAX_VALUE) {
                     ++unanswered;
                  }
               }

               connection.send(new ServerboundPingRequestPacket(now));
            }
         }
      } else {
         ticks = 0;
      }
   }

   public static void pong(long sentMs) {
      long now = Util.getMillis();
      boolean first;
      synchronized(LatencyProbe.class) {
         first = !hooked;
         hooked = true;
      }

      if (first && InfoHudHarness.PROFILE) {
         CFHGuiMenu.LOGGER.info("[CFHGui Menu] info overlay: ping answers are read where the game's network chart reads them");
      }

      record(now - sentMs, now);
   }

   private static void record(long trip, long now) {
      if (trip >= 0L && trip <= 60000L) {
         synchronized(LatencyProbe.class) {
            recent[at] = trip;
            at = (at + 1) % recent.length;
            if (count < recent.length) {
               ++count;
            }

            lastPongMs = now;
            unanswered = 0;
         }
      }
   }

   private static void readLog(Minecraft mc) {
      synchronized(LatencyProbe.class) {
         if (hooked) {
            return;
         }
      }

      try {
         LocalSampleLogger log = mc.getDebugOverlay().getPingLogger();
         int size = log.size();
         long latest = size > 0 ? log.get(size - 1) : 0L;
         boolean changed = logSeen && size > 0 && (size != logSize || latest != logLast);
         logSeen = true;
         logSize = size;
         logLast = latest;
         if (changed) {
            record(latest, Util.getMillis());
         }
      } catch (RuntimeException var6) {
      }

   }

   static void reset() {
      synchronized(LatencyProbe.class) {
         count = 0;
         at = 0;
         lastPongMs = 0L;
         unanswered = 0;
         waitingSinceMs = 0L;
      }

      ticks = 0;
      logSeen = false;
   }

   static int latencyMs() {
      Minecraft mc = Minecraft.getInstance();
      long now = Util.getMillis();
      synchronized(LatencyProbe.class) {
         if (count > 0) {
            if (unanswered > 0 && now - waitingSinceMs > 2500L) {
               return (int)Math.min(99999L, now - waitingSinceMs);
            }

            if (now - lastPongMs < 5000L) {
               System.arraycopy(recent, 0, sorted, 0, count);
               Arrays.sort(sorted, 0, count);
               return (int)sorted[count / 2];
            }
         }
      }

      if (mc.player != null && mc.getConnection() != null) {
         PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
         if (info != null && info.getLatency() > 0) {
            return info.getLatency();
         }
      }

      return -1;
   }

   static float tickMs() {
      IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
      return server == null ? -1.0F : server.getCurrentSmoothedTickTime();
   }

   static float tickBudgetMs() {
      IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
      return server == null ? 50.0F : server.tickRateManager().millisecondsPerTick();
   }
}
