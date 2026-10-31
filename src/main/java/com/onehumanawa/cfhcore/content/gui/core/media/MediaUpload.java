package com.onehumanawa.cfhcore.content.gui.core.media;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.net.blob.BlobSender;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public final class MediaUpload {
   public static final int MAX_BYTES = 2800000;

   public static boolean ready() {
      return BlobSender.ready();
   }

   public static String sendImage(byte[] bytes, String target, @Nullable Consumer<String> onSent) {
      String id = BlobSender.send("image", bytes, 0, target, "", onSent);
      MediaCache.putLocal(id, bytes, "image", 0);
      return id;
   }

   public static String sendVoice(byte[] bytes, int durationMs, String target, @Nullable Consumer<String> onSent) {
      String id = BlobSender.send("voice", bytes, durationMs, target, "", onSent);
      MediaCache.putLocal(id, bytes, "voice", durationMs);
      return id;
   }

   public static String sendFile(byte[] bytes, String name, String target, @Nullable Consumer<String> onSent) {
      String safe = Attachment.safeName(name);
      String id = BlobSender.send("file", bytes, 0, target, safe, onSent);
      MediaCache.putLocal(id, bytes, "file", 0, safe);
      return id;
   }

   public static void sendImageFile(Path path, String target, @Nullable Consumer<String> onSent, @Nullable Consumer<String> onError) {
      Thread worker = new Thread(() -> {
         try {
            byte[] raw = Files.readAllBytes(path);
            byte[] send = ClipboardImages.shrinkToFit(raw, 2800000);
            if (send == null) {
               fail(onError, "too large (" + raw.length / 1024 + " KB, limit 2734 KB)");
               return;
            }

            Minecraft.getInstance().execute(() -> sendImage(send, target, onSent));
         } catch (Throwable e) {
            CFHGui.LOGGER.warn("[CFHGui] send image file failed: {}", e.toString());
            fail(onError, e.getMessage() == null ? e.toString() : e.getMessage());
         }

      }, "cfhgui-sendfile");
      worker.setDaemon(true);
      worker.start();
   }

   public static void sendAnyFile(Path path, String target, @Nullable Consumer<String> onSent, @Nullable Consumer<String> onError) {
      Thread worker = new Thread(() -> {
         try {
            long size = Files.size(path);
            if (size > 2800000L) {
               fail(onError, "too large (" + size / 1024L + " KB, limit 2734 KB)");
               return;
            }

            byte[] raw = Files.readAllBytes(path);
            String name = path.getFileName().toString();
            Minecraft.getInstance().execute(() -> {
               if (!ClipboardImages.isImage(raw) && !ClipboardImages.isGif(raw)) {
                  sendFile(raw, name, target, onSent);
               } else {
                  sendImage(raw, target, onSent);
               }

            });
         } catch (Throwable e) {
            CFHGui.LOGGER.warn("[CFHGui] send file failed: {}", e.toString());
            fail(onError, e.getMessage() == null ? e.toString() : e.getMessage());
         }

      }, "cfhgui-sendfile");
      worker.setDaemon(true);
      worker.start();
   }

   private static void fail(@Nullable Consumer<String> onError, String why) {
      if (onError != null) {
         Minecraft.getInstance().execute(() -> onError.accept(why));
      }

   }

   private MediaUpload() {
   }
}
