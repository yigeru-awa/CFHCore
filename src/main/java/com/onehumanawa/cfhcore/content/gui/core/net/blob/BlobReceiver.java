package com.onehumanawa.cfhcore.content.gui.core.net.blob;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class BlobReceiver {
   private static final Map<String, Pending> PENDING = new HashMap();
   private static final List<Consumer<BlobPayloads.Start>> START_LISTENERS = new ArrayList();
   private static final List<Consumer<Received>> DONE_LISTENERS = new ArrayList();
   private static final List<ProgressListener> PROGRESS_LISTENERS = new ArrayList();
   public static volatile int maxIncomingBytes = 8388608;

   public static void onStart(Consumer<BlobPayloads.Start> l) {
      START_LISTENERS.add(l);
   }

   public static void onComplete(Consumer<Received> l) {
      DONE_LISTENERS.add(l);
   }

   public static void onProgress(ProgressListener l) {
      PROGRESS_LISTENERS.add(l);
   }

   public static boolean isPending(String id) {
      return PENDING.containsKey(id);
   }

   public static void handleStart(BlobPayloads.Start start) {
      if (BlobPayloads.validId(start.id()) && start.totalBytes() > 0 && start.totalBytes() <= maxIncomingBytes) {
         PENDING.put(start.id(), new Pending(start));

         for(Consumer<BlobPayloads.Start> l : START_LISTENERS) {
            try {
               l.accept(start);
            } catch (Exception e) {
               CFHGui.LOGGER.error("[CFHGui] blob start listener threw", e);
            }
         }

      }
   }

   public static void handleChunk(BlobPayloads.Chunk chunk) {
      Pending p = (Pending)PENDING.get(chunk.id());
      if (p != null) {
         if (chunk.index() == p.nextIndex && p.filled + chunk.data().length <= p.buffer.length) {
            System.arraycopy(chunk.data(), 0, p.buffer, p.filled, chunk.data().length);
            p.filled += chunk.data().length;
            ++p.nextIndex;
            p.lastMs = System.currentTimeMillis();

            for(ProgressListener l : PROGRESS_LISTENERS) {
               l.progress(chunk.id(), p.filled, p.buffer.length);
            }

         } else {
            PENDING.remove(chunk.id());
         }
      }
   }

   public static void handleEnd(BlobPayloads.End end) {
      Pending p = (Pending)PENDING.remove(end.id());
      if (p != null && p.filled == p.buffer.length) {
         Received r = new Received(p.start, p.buffer);

         for(Consumer<Received> l : DONE_LISTENERS) {
            try {
               l.accept(r);
            } catch (Exception e) {
               CFHGui.LOGGER.error("[CFHGui] blob listener threw", e);
            }
         }

      }
   }

   public static void tick() {
      if (!PENDING.isEmpty()) {
         long now = System.currentTimeMillis();
         PENDING.values().removeIf((p) -> now - p.lastMs > 30000L);
      }
   }

   public static void clear() {
      PENDING.clear();
   }

   private BlobReceiver() {
   }

   public static record Received(BlobPayloads.Start start, byte[] bytes) {
      public String id() {
         return this.start.id();
      }

      public String kind() {
         return this.start.kind();
      }
   }

   private static final class Pending {
      final BlobPayloads.Start start;
      final byte[] buffer;
      int filled;
      int nextIndex;
      long lastMs = System.currentTimeMillis();

      Pending(BlobPayloads.Start start) {
         this.start = start;
         this.buffer = new byte[start.totalBytes()];
      }
   }

   @FunctionalInterface
   public interface ProgressListener {
      void progress(String var1, int var2, int var3);
   }
}
