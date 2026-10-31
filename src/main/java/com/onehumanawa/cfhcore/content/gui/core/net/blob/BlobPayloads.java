package com.onehumanawa.cfhcore.content.gui.core.net.blob;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class BlobPayloads {
   public static final int CHUNK_BYTES = 24000;

   public static boolean validId(String id) {
      if (id != null && id.length() == 8) {
         for(int i = 0; i < 8; ++i) {
            char c = id.charAt(i);
            if ((c < '0' || c > '9') && (c < 'a' || c > 'f')) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private BlobPayloads() {
   }

   public static record Start(String id, String kind, int totalBytes, int durationMs, String sender, String target, String meta) implements CustomPacketPayload {
      public static final CustomPacketPayload.Type<Start> TYPE = new CustomPacketPayload.Type(CFHGui.id("blob_start"));
      public static final StreamCodec<ByteBuf, Start> CODEC = StreamCodec.of((buf, s) -> {
         FriendlyByteBuf f = new FriendlyByteBuf(buf);
         f.writeUtf(s.id, 16);
         f.writeUtf(s.kind, 32);
         f.writeVarInt(s.totalBytes);
         f.writeVarInt(s.durationMs);
         f.writeUtf(s.sender, 64);
         f.writeUtf(s.target, 96);
         f.writeUtf(s.meta, 512);
      }, (buf) -> {
         FriendlyByteBuf f = new FriendlyByteBuf(buf);
         return new Start(f.readUtf(16), f.readUtf(32), f.readVarInt(), f.readVarInt(), f.readUtf(64), f.readUtf(96), f.readUtf(512));
      });

      public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }

      public Start withSender(String name) {
         return new Start(this.id, this.kind, this.totalBytes, this.durationMs, name, this.target, this.meta);
      }
   }

   public static record Chunk(String id, int index, byte[] data) implements CustomPacketPayload {
      public static final CustomPacketPayload.Type<Chunk> TYPE = new CustomPacketPayload.Type(CFHGui.id("blob_chunk"));
      public static final StreamCodec<ByteBuf, Chunk> CODEC;

      public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }

      static {
         CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(16), Chunk::id, ByteBufCodecs.VAR_INT, Chunk::index, ByteBufCodecs.byteArray(24000), Chunk::data, Chunk::new);
      }
   }

   public static record End(String id) implements CustomPacketPayload {
      public static final CustomPacketPayload.Type<End> TYPE = new CustomPacketPayload.Type(CFHGui.id("blob_end"));
      public static final StreamCodec<ByteBuf, End> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(16), End::id, End::new);

      public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }
}
