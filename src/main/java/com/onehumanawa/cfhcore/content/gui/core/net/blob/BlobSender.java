package com.onehumanawa.cfhcore.content.gui.core.net.blob;

import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class BlobSender {
   private static final Link SERVER_LINK = new Link() {
      public boolean ready() {
         return Minecraft.getInstance().getConnection() != null && CFHGuiNetwork.get().serverHasChannel(BlobPayloads.Start.TYPE);
      }

      public void send(CustomPacketPayload payload) {
         CFHGuiNetwork.get().sendToServer(payload);
      }
   };
   private static final int CHUNKS_PER_TICK = 3;
   private static final SecureRandom RANDOM = new SecureRandom();
   private static final Deque<Pending> QUEUE = new ArrayDeque();
   private static volatile Link link;
   private static Consumer<String> onComplete;
   private static final Map<String, Consumer<String>> COMPLETIONS;

   public static void setLink(Link l) {
      link = l == null ? SERVER_LINK : l;
   }

   public static Link serverLink() {
      return SERVER_LINK;
   }

   public static boolean ready() {
      return link.ready();
   }

   public static String newId() {
      return "%08x".formatted(RANDOM.nextInt());
   }

   public static String send(String kind, byte[] bytes, int durationMs, String target, String meta, Consumer<String> whenSent) {
      String id = newId();
      QUEUE.add(new Pending(new BlobPayloads.Start(id, kind, bytes.length, durationMs, "", target == null ? "" : target, meta == null ? "" : meta), (String)null));
      int at = 0;

      for(int index = 0; at < bytes.length; ++index) {
         int n = Math.min(24000, bytes.length - at);
         byte[] slice = new byte[n];
         System.arraycopy(bytes, at, slice, 0, n);
         QUEUE.add(new Pending(new BlobPayloads.Chunk(id, index, slice), (String)null));
         at += n;
      }

      QUEUE.add(new Pending(new BlobPayloads.End(id), id));
      if (whenSent != null) {
         COMPLETIONS.put(id, whenSent);
      }

      return id;
   }

   public static int queued() {
      return QUEUE.size();
   }

   public static void tick() {
      if (!QUEUE.isEmpty()) {
         if (!link.ready()) {
            QUEUE.clear();
            COMPLETIONS.clear();
         } else {
            for(int i = 0; i < 3 && !QUEUE.isEmpty(); ++i) {
               Pending p = (Pending)QUEUE.poll();
               link.send(p.payload());
               if (p.completeId() != null) {
                  Consumer<String> c = (Consumer)COMPLETIONS.remove(p.completeId());
                  if (c != null) {
                     c.accept(p.completeId());
                  }

                  onComplete.accept(p.completeId());
               }
            }

         }
      }
   }

   private BlobSender() {
   }

   static {
      link = SERVER_LINK;
      onComplete = (id) -> {
      };
      COMPLETIONS = new HashMap();
   }

   private static record Pending(CustomPacketPayload payload, String completeId) {
   }

   public interface Link {
      boolean ready();

      void send(CustomPacketPayload var1);
   }
}
