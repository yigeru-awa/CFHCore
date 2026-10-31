package com.onehumanawa.cfhcore.content.gui.core.net.blob;

import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.net.Flow;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.util.function.Consumer;

public final class BlobChannel {
   private static boolean registered;

   public static synchronized void register() {
      if (!registered) {
         registered = true;
         CFHGuiNetwork net = CFHGuiNetwork.get();
         net.register(BlobPayloads.Start.TYPE, BlobPayloads.Start.CODEC, Flow.BOTH, (p, ctx) -> {
            if (ctx.isClient()) {
               BlobChannel.ClientSide.start(p);
            } else {
               BlobRelay.onStart(p, ctx.sender());
            }

         });
         net.register(BlobPayloads.Chunk.TYPE, BlobPayloads.Chunk.CODEC, Flow.BOTH, (p, ctx) -> {
            if (ctx.isClient()) {
               BlobChannel.ClientSide.chunk(p);
            } else {
               BlobRelay.onChunk(p, ctx.sender());
            }

         });
         net.register(BlobPayloads.End.TYPE, BlobPayloads.End.CODEC, Flow.BOTH, (p, ctx) -> {
            if (ctx.isClient()) {
               BlobChannel.ClientSide.end(p);
            } else {
               BlobRelay.onEnd(p, ctx.sender());
            }

         });
         CFHGuiEvents.PLAYER_LEFT.register((p) -> BlobRelay.onDisconnect(p.getUUID()));
         if (CFHGuiPlatform.get().isClient()) {
            BlobChannel.ClientSide.init();
         }

      }
   }

   private BlobChannel() {
   }

   private static final class ClientSide {
      static void init() {
         CFHGuiEvents.CLIENT_TICK_END.register(() -> {
            BlobSender.tick();
            BlobReceiver.tick();
         });
         CFHGuiEvents.CLIENT_LEFT_SERVER.register(BlobReceiver::clear);
      }

      static void start(BlobPayloads.Start p) {
         BlobReceiver.handleStart(p);
      }

      static void chunk(BlobPayloads.Chunk p) {
         BlobReceiver.handleChunk(p);
      }

      static void end(BlobPayloads.End p) {
         BlobReceiver.handleEnd(p);
      }
   }
}
