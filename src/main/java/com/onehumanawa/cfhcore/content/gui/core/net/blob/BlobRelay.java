package com.onehumanawa.cfhcore.content.gui.core.net.blob;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class BlobRelay {
   public static volatile int maxBlobBytes = 3145728;
   public static volatile int maxChunksPerSecond = 80;
   public static volatile int maxConcurrentTransfers = 3;
   private static final Map<UUID, Map<String, Transfer>> TRANSFERS = new HashMap();
   private static final Map<UUID, long[]> RATE = new HashMap();
   private static final Map<String, BlobRouter> ROUTERS = new HashMap();
   private static final List<BlobTap> TAPS = new CopyOnWriteArrayList();

   public static void registerRouter(String prefix, BlobRouter router) {
      ROUTERS.put(prefix, router);
   }

   public static void addTap(BlobTap tap) {
      if (!TAPS.contains(tap)) {
         TAPS.add(tap);
      }

   }

   public static void removeTap(BlobTap tap) {
      TAPS.remove(tap);
   }

   private static void tap(Consumer<BlobTap> call) {
      for(BlobTap t : TAPS) {
         try {
            call.accept(t);
         } catch (Exception e) {
            CFHGui.LOGGER.error("[CFHGui] a blob tap threw", e);
         }
      }

   }

   private static List<ServerPlayer> route(ServerPlayer sender, BlobPayloads.Start start) {
      String t = start.target() == null ? "" : start.target();
      if (t.isEmpty()) {
         List<ServerPlayer> all = new ArrayList();

         for(ServerPlayer p : sender.server.getPlayerList().getPlayers()) {
            if (!p.getUUID().equals(sender.getUUID())) {
               all.add(p);
            }
         }

         return all;
      } else {
         int colon = t.indexOf(58);
         BlobRouter r = (BlobRouter)ROUTERS.get(colon < 0 ? t : t.substring(0, colon + 1));
         return r == null ? List.of() : r.recipients(sender, start);
      }
   }

   public static void onStart(BlobPayloads.Start start, ServerPlayer sender) {
      if (BlobPayloads.validId(start.id()) && start.totalBytes() > 0 && start.totalBytes() <= maxBlobBytes) {
         Map<String, Transfer> mine = (Map)TRANSFERS.computeIfAbsent(sender.getUUID(), (k) -> new HashMap());
         if (mine.size() >= maxConcurrentTransfers) {
            CFHGui.LOGGER.warn("[CFHGui] {} exceeded concurrent blob cap", sender.getGameProfile().getName());
         } else {
            List<ServerPlayer> recipients = route(sender, start);
            Transfer t = new Transfer(start, recipients);
            mine.put(start.id(), t);
            BlobPayloads.Start stamped = start.withSender(sender.getGameProfile().getName());
            if (!TAPS.isEmpty()) {
               tap((tap) -> tap.started(sender, stamped));
            }

            forward(t, stamped);
         }
      } else {
         CFHGui.LOGGER.warn("[CFHGui] rejected blob {} from {}: {} bytes (cap {})", new Object[]{start.id(), sender.getGameProfile().getName(), start.totalBytes(), maxBlobBytes});
      }
   }

   public static void onChunk(BlobPayloads.Chunk chunk, ServerPlayer sender) {
      Map<String, Transfer> mine = (Map)TRANSFERS.get(sender.getUUID());
      Transfer t = mine == null ? null : (Transfer)mine.get(chunk.id());
      if (t != null) {
         long now = System.currentTimeMillis();
         long[] window = (long[])RATE.computeIfAbsent(sender.getUUID(), (k) -> new long[]{now, 0L});
         if (now - window[0] > 1000L) {
            window[0] = now;
            window[1] = 0L;
         }

         if (++window[1] > (long)maxChunksPerSecond) {
            drop(sender, chunk.id(), "chunk rate");
         } else if (chunk.index() == t.nextIndex && chunk.data().length != 0 && t.bytesSeen + chunk.data().length <= t.start.totalBytes()) {
            ++t.nextIndex;
            t.bytesSeen += chunk.data().length;
            if (!TAPS.isEmpty()) {
               tap((tap) -> tap.chunk(sender, chunk));
            }

            forward(t, chunk);
         } else {
            drop(sender, chunk.id(), "sequence/size violation");
         }
      }
   }

   public static void onEnd(BlobPayloads.End end, ServerPlayer sender) {
      Map<String, Transfer> mine = (Map)TRANSFERS.get(sender.getUUID());
      Transfer t = mine == null ? null : (Transfer)mine.remove(end.id());
      if (t != null) {
         boolean complete = t.bytesSeen == t.start.totalBytes();
         if (!TAPS.isEmpty()) {
            tap((tap) -> tap.ended(sender, end.id(), complete));
         }

         if (!complete) {
            CFHGui.LOGGER.warn("[CFHGui] blob {} from {} ended at {}/{} bytes - not forwarded", new Object[]{end.id(), sender.getGameProfile().getName(), t.bytesSeen, t.start.totalBytes()});
         } else {
            forward(t, end);
         }
      }
   }

   public static void onDisconnect(UUID player) {
      TRANSFERS.remove(player);
      RATE.remove(player);
   }

   private static void drop(ServerPlayer sender, String id, String why) {
      Map<String, Transfer> mine = (Map)TRANSFERS.get(sender.getUUID());
      if (mine != null) {
         mine.remove(id);
      }

      if (!TAPS.isEmpty()) {
         tap((tap) -> tap.ended(sender, id, false));
      }

      CFHGui.LOGGER.warn("[CFHGui] dropped blob {} from {}: {}", new Object[]{id, sender.getGameProfile().getName(), why});
   }

   private static void forward(Transfer t, CustomPacketPayload payload) {
      CFHGuiNetwork net = CFHGuiNetwork.get();

      for(ServerPlayer p : t.recipients) {
         if (!p.hasDisconnected()) {
            net.sendToPlayer(p, payload);
         }
      }

   }

   private BlobRelay() {
   }

   private static final class Transfer {
      final BlobPayloads.Start start;
      final List<ServerPlayer> recipients;
      int bytesSeen;
      int nextIndex;

      Transfer(BlobPayloads.Start start, List<ServerPlayer> recipients) {
         this.start = start;
         this.recipients = recipients;
      }
   }
}
