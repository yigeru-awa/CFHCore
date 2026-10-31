package com.onehumanawa.cfhcore.content.gui.core.media;

import com.mojang.blaze3d.platform.NativeImage;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.net.blob.BlobPayloads;
import com.onehumanawa.cfhcore.content.gui.core.net.blob.BlobReceiver;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public final class MediaCache {
   public static final String KIND_IMAGE = "image";
   public static final String KIND_VOICE = "voice";
   public static final String KIND_FILE = "file";
   public static final String KIND_URL = "url";
   public static volatile int maxDownloadBytes = 8388608;
   private static final long BLOB_WAIT_MS = 60000L;
   private static final int MAX_GIF_FRAMES = 96;
   private static final int MAX_IMAGE_DIM = 4096;
   private static final Map<String, Entry> ENTRIES = new ConcurrentHashMap();
   private static final ExecutorService POOL = Executors.newFixedThreadPool(2, (r) -> {
      Thread t = new Thread(r, "cfhgui-media");
      t.setDaemon(true);
      return t;
   });
   private static final Map<String, List<ResourceLocation>> TEXTURES = new LinkedHashMap();
   private static boolean initialised;

   public static synchronized void init() {
      if (!initialised) {
         initialised = true;
         BlobReceiver.onStart(MediaCache::onBlobStart);
         BlobReceiver.onComplete(MediaCache::onBlobComplete);
         POOL.submit(MediaCache::pruneDisk);
      }
   }

   public static Entry get(Attachment att) {
      Entry e = (Entry)ENTRIES.computeIfAbsent(att.id(), (k) -> {
         Entry fresh = new Entry();
         fresh.durationMs = att.durationMs();
         fresh.name = att.name() == null ? "" : att.name();
         switch (att.kind()) {
            case IMAGE_URL:
               fresh.kind = "url";
               fresh.inbound = true;
               POOL.submit(() -> download(att.id(), att.url(), fresh));
               break;
            case VIDEO_URL:
               fresh.kind = "url";
               fresh.status = MediaCache.Status.READY;
               break;
            case VOICE:
               fresh.kind = "voice";
               POOL.submit(() -> loadFromDisk(att.id(), fresh));
               break;
            case FILE_BLOB:
               fresh.kind = "file";
               POOL.submit(() -> loadFromDisk(att.id(), fresh));
               break;
            default:
               fresh.kind = "image";
               POOL.submit(() -> loadFromDisk(att.id(), fresh));
         }

         return fresh;
      });
      e.lastTouched = System.currentTimeMillis();
      if (e.status == MediaCache.Status.LOADING && !e.inbound && System.currentTimeMillis() - e.created > 60000L) {
         e.error = "unavailable";
         e.status = MediaCache.Status.FAILED;
      }

      return e;
   }

   public static @Nullable Entry get(String id) {
      Entry e = (Entry)ENTRIES.get(id);
      if (e != null) {
         e.lastTouched = System.currentTimeMillis();
      }

      return e;
   }

   public static boolean has(String id) {
      return ENTRIES.containsKey(id);
   }

   static void background(Runnable job) {
      POOL.submit(job);
   }

   public static Entry fromUrl(String url) {
      return get(new Attachment(Attachment.Kind.IMAGE_URL, Attachment.idForUrl(url), url, 0));
   }

   public static Entry putLocal(String id, byte[] bytes, String kind, int durationMs) {
      return putLocal(id, bytes, kind, durationMs, "");
   }

   public static Entry putLocal(String id, byte[] bytes, String kind, int durationMs, String name) {
      Entry e = new Entry();
      e.sender = Minecraft.getInstance().getUser().getName();
      e.kind = kind;
      e.durationMs = durationMs;
      e.name = name == null ? "" : name;
      e.inbound = true;
      ENTRIES.put(id, e);
      accept(id, bytes, e);
      evictIfCrowded();
      return e;
   }

   private static void onBlobStart(BlobPayloads.Start start) {
      String kind = start.kind() == null ? "" : start.kind().toLowerCase(Locale.ROOT);
      if ("image".equals(kind) || "voice".equals(kind) || "file".equals(kind)) {
         Entry e = (Entry)ENTRIES.computeIfAbsent(start.id(), (k) -> new Entry());
         e.sender = start.sender() == null ? "" : start.sender();
         e.kind = kind;
         e.durationMs = start.durationMs();
         if ("file".equals(kind)) {
            e.name = Attachment.safeName(start.meta());
         }

         e.inbound = true;
         e.status = MediaCache.Status.LOADING;
         evictIfCrowded();
      }
   }

   private static void onBlobComplete(BlobReceiver.Received r) {
      Entry e = (Entry)ENTRIES.get(r.id());
      if (e != null) {
         if (e.kind.equals("image") || e.kind.equals("voice") || e.kind.equals("file")) {
            accept(r.id(), r.bytes(), e);
         }
      }
   }

   private static void accept(String id, byte[] data, Entry e) {
      e.bytes = data;
      if ("image".equals(e.kind)) {
         POOL.submit(() -> decodeImage(id, data, e, false));
      } else {
         e.status = MediaCache.Status.READY;
      }

      if (MediaConfig.get().diskCache) {
         POOL.submit(() -> writeDisk(id, data, e));
      }

   }

   private static void download(String id, String url, Entry e) {
      try {
         HttpURLConnection conn = (HttpURLConnection)URI.create(url).toURL().openConnection();
         conn.setConnectTimeout(8000);
         conn.setReadTimeout(10000);
         conn.setInstanceFollowRedirects(true);
         conn.setRequestProperty("User-Agent", MediaConfig.get().userAgent);
         int code = conn.getResponseCode();
         String type = String.valueOf(conn.getContentType()).toLowerCase(Locale.ROOT);
         if (code != 200 || !type.startsWith("image/")) {
            throw new IOException("HTTP " + code + " " + type);
         }

         int cap = maxDownloadBytes;
         ByteArrayOutputStream buf = new ByteArrayOutputStream(65536);
         InputStream in = conn.getInputStream();

         try {
            byte[] tmp = new byte[16384];

            int n;
            while((n = in.read(tmp)) > 0) {
               if (buf.size() + n > cap) {
                  throw new IOException("over download cap (" + cap / 1024 + " KB)");
               }

               buf.write(tmp, 0, n);
            }
         } catch (Throwable var12) {
            if (in != null) {
               try {
                  in.close();
               } catch (Throwable var11) {
                  var12.addSuppressed(var11);
               }
            }

            throw var12;
         }

         if (in != null) {
            in.close();
         }

         byte[] data = buf.toByteArray();
         e.bytes = data;
         decodeImage(id, data, e, url.toLowerCase(Locale.ROOT).contains(".gif"));
         if (MediaConfig.get().diskCache) {
            writeDisk(id, data, e);
         }
      } catch (Exception ex) {
         CFHGui.LOGGER.debug("[CFHGui] embed failed for {}: {}", url, ex.toString());
         e.error = ex.getMessage() == null ? ex.toString() : ex.getMessage();
         e.status = MediaCache.Status.FAILED;
      }

   }

   private static void decodeImage(String id, byte[] data, Entry e, boolean gifHint) {
      try {
         boolean gif = gifHint || ClipboardImages.isGif(data);
         List<NativeImage> images = new ArrayList();
         List<Integer> delays = new ArrayList();
         if (gif) {
            try {
               decodeGif(data, images, delays);
            } catch (Exception var8) {
               images.clear();
               delays.clear();
               images.add(decodeStill(data));
               delays.add(0);
            }
         } else {
            images.add(decodeStill(data));
            delays.add(0);
         }

         NativeImage first = (NativeImage)images.get(0);
         if (first.getWidth() > 4096 || first.getHeight() > 4096) {
            images.forEach(NativeImage::close);
            int var10002 = first.getWidth();
            throw new IOException("image too large: " + var10002 + "x" + first.getHeight());
         }

         e.width = first.getWidth();
         e.height = first.getHeight();
         Minecraft.getInstance().execute(() -> {
            List<Frame> frames = new ArrayList(images.size());
            List<ResourceLocation> owned = new ArrayList(images.size());

            for(int i = 0; i < images.size(); ++i) {
               ResourceLocation rl = CFHGui.id("media/" + id + "/" + i);
               DynamicTexture tex = new DynamicTexture((NativeImage)images.get(i));
               tex.setFilter(true, false);
               Minecraft.getInstance().getTextureManager().register(rl, tex);
               frames.add(new Frame(rl, (Integer)delays.get(i)));
               owned.add(rl);
            }

            synchronized(TEXTURES) {
               List<ResourceLocation> old = (List)TEXTURES.put(id, owned);
               if (old != null) {
                  old.forEach((rlx) -> Minecraft.getInstance().getTextureManager().release(rlx));
               }
            }

            e.frames = frames;
            e.status = MediaCache.Status.READY;
         });
      } catch (Exception ex) {
         CFHGui.LOGGER.debug("[CFHGui] decode failed for {}: {}", id, ex.toString());
         e.error = ex.getMessage() == null ? ex.toString() : ex.getMessage();
         e.status = MediaCache.Status.FAILED;
      }

   }

   private static NativeImage decodeStill(byte[] data) throws IOException {
      boolean png = data.length > 3 && (data[0] & 255) == 137 && data[1] == 80 && data[2] == 78 && data[3] == 71;
      if (png) {
         return NativeImage.read(new ByteArrayInputStream(data));
      } else {
         BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
         if (image == null) {
            throw new IOException("not a picture this can read");
         } else {
            return toNative(image);
         }
      }
   }

   private static void decodeGif(byte[] data, List<NativeImage> out, List<Integer> delays) throws IOException {
      Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
      if (!readers.hasNext()) {
         throw new IOException("no GIF reader");
      } else {
         ImageReader reader = (ImageReader)readers.next();

         try {
            ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data));

            try {
               reader.setInput(in, false);
               int count = Math.min(reader.getNumImages(true), 96);
               int screenW = 0;
               int screenH = 0;

               try {
                  IIOMetadata stream = reader.getStreamMetadata();
                  Node root = stream.getAsTree("javax_imageio_gif_stream_1.0");

                  for(Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
                     if (n.getNodeName().equals("LogicalScreenDescriptor")) {
                        NamedNodeMap a = n.getAttributes();
                        screenW = Integer.parseInt(a.getNamedItem("logicalScreenWidth").getNodeValue());
                        screenH = Integer.parseInt(a.getNamedItem("logicalScreenHeight").getNodeValue());
                     }
                  }
               } catch (Exception var27) {
               }

               BufferedImage canvas = null;

               for(int i = 0; i < count; ++i) {
                  BufferedImage frame = reader.read(i);
                  int delay = 100;
                  int x = 0;
                  int y = 0;
                  String disposal = "none";
                  IIOMetadata meta = reader.getImageMetadata(i);
                  Node root = meta.getAsTree("javax_imageio_gif_image_1.0");

                  for(Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
                     if (n.getNodeName().equals("GraphicControlExtension")) {
                        NamedNodeMap a = n.getAttributes();
                        delay = Integer.parseInt(a.getNamedItem("delayTime").getNodeValue()) * 10;
                        disposal = a.getNamedItem("disposalMethod").getNodeValue();
                     } else if (n.getNodeName().equals("ImageDescriptor")) {
                        NamedNodeMap a = n.getAttributes();
                        x = Integer.parseInt(a.getNamedItem("imageLeftPosition").getNodeValue());
                        y = Integer.parseInt(a.getNamedItem("imageTopPosition").getNodeValue());
                     }
                  }

                  if (canvas == null) {
                     int cw = Math.max(screenW, frame.getWidth() + x);
                     int ch = Math.max(screenH, frame.getHeight() + y);
                     canvas = new BufferedImage(cw, ch, 2);
                  }

                  BufferedImage previous = disposal.equals("restoreToPrevious") ? copy(canvas) : null;
                  Graphics2D g = canvas.createGraphics();
                  g.drawImage(frame, x, y, (ImageObserver)null);
                  g.dispose();
                  out.add(toNative(canvas));
                  delays.add(Math.max(delay, 20));
                  if (disposal.equals("restoreToBackgroundColor")) {
                     Graphics2D g2 = canvas.createGraphics();
                     g2.setComposite(AlphaComposite.Clear);
                     g2.fillRect(x, y, frame.getWidth(), frame.getHeight());
                     g2.dispose();
                  } else if (previous != null) {
                     canvas = previous;
                  }
               }
            } catch (Throwable var28) {
               if (in != null) {
                  try {
                     in.close();
                  } catch (Throwable var26) {
                     var28.addSuppressed(var26);
                  }
               }

               throw var28;
            }

            if (in != null) {
               in.close();
            }
         } finally {
            reader.dispose();
         }

         if (out.isEmpty()) {
            throw new IOException("no GIF frames");
         }
      }
   }

   private static BufferedImage copy(BufferedImage src) {
      BufferedImage c = new BufferedImage(src.getWidth(), src.getHeight(), 2);
      Graphics2D g = c.createGraphics();
      g.setComposite(AlphaComposite.Src);
      g.drawImage(src, 0, 0, (ImageObserver)null);
      g.dispose();
      return c;
   }

   private static NativeImage toNative(BufferedImage img) {
      NativeImage ni = new NativeImage(img.getWidth(), img.getHeight(), false);

      for(int y = 0; y < img.getHeight(); ++y) {
         for(int x = 0; x < img.getWidth(); ++x) {
            int argb = img.getRGB(x, y);
            int abgr = argb & -16711936 | (argb & 16711680) >>> 16 | (argb & 255) << 16;
            ni.setPixelRGBA(x, y, abgr);
         }
      }

      return ni;
   }

   public static Frame currentFrame(List<Frame> frames) {
      if (frames.size() == 1) {
         return (Frame)frames.get(0);
      } else {
         int total = 0;

         for(Frame f : frames) {
            total += Math.max(f.delayMs(), 20);
         }

         long at = System.currentTimeMillis() % (long)Math.max(1, total);

         for(Frame f : frames) {
            at -= (long)Math.max(f.delayMs(), 20);
            if (at < 0L) {
               return f;
            }
         }

         return (Frame)frames.get(0);
      }
   }

   public static Path diskDir() {
      return JsonConfig.dir().resolve("cache").resolve("media");
   }

   private static void writeDisk(String id, byte[] data, Entry e) {
      try {
         Path dir = diskDir();
         Files.createDirectories(dir);
         Path tmp = dir.resolve(id + ".tmp");
         Files.write(tmp, data, new OpenOption[0]);
         Files.move(tmp, dir.resolve(id + ".bin"), StandardCopyOption.REPLACE_EXISTING);
         String var10000 = e.kind;
         String meta = var10000 + "\n" + e.durationMs + "\n" + e.sender.replace('\n', ' ') + "\n" + e.name.replace('\n', ' ') + "\n";
         Files.writeString(dir.resolve(id + ".meta"), meta, StandardCharsets.UTF_8);
      } catch (IOException ex) {
         CFHGui.LOGGER.debug("[CFHGui] media disk write failed for {}: {}", id, ex.toString());
      }

   }

   private static void loadFromDisk(String id, Entry e) {
      try {
         Path bin = diskDir().resolve(id + ".bin");
         if (!Files.isRegularFile(bin, new LinkOption[0])) {
            return;
         }

         Path meta = diskDir().resolve(id + ".meta");
         if (Files.isRegularFile(meta, new LinkOption[0])) {
            String[] lines = Files.readString(meta, StandardCharsets.UTF_8).split("\n", -1);
            if (lines.length >= 4) {
               if (e.kind.isEmpty() || e.kind.equals("image")) {
                  e.kind = lines[0].isEmpty() ? e.kind : lines[0];
               }

               try {
                  if (e.durationMs == 0) {
                     e.durationMs = Integer.parseInt(lines[1].trim());
                  }
               } catch (NumberFormatException var6) {
               }

               if (e.sender.isEmpty()) {
                  e.sender = lines[2];
               }

               if (e.name.isEmpty()) {
                  e.name = lines[3];
               }
            }
         }

         if (e.inbound) {
            return;
         }

         byte[] data = Files.readAllBytes(bin);
         e.inbound = true;
         e.bytes = data;
         if (!"image".equals(e.kind) && !"url".equals(e.kind)) {
            e.status = MediaCache.Status.READY;
         } else {
            decodeImage(id, data, e, false);
         }

         Files.setLastModifiedTime(bin, FileTime.fromMillis(System.currentTimeMillis()));
      } catch (Exception ex) {
         CFHGui.LOGGER.debug("[CFHGui] media disk read failed for {}: {}", id, ex.toString());
      }

   }

   private static void pruneDisk() {
      try {
         Path dir = diskDir();
         if (!Files.isDirectory(dir, new LinkOption[0])) {
            return;
         }

         long budget = Math.max(1L, (long)MediaConfig.get().maxDiskCacheMb) * 1024L * 1024L;
         Stream<Path> s = Files.list(dir);

         List<Path> bins;
         try {
            bins = new ArrayList(s.filter((px) -> px.getFileName().toString().endsWith(".bin")).toList());
         } catch (Throwable var9) {
            if (s != null) {
               try {
                  s.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (s != null) {
            s.close();
         }

         long total = 0L;

         for(Path p : bins) {
            total += Files.size(p);
         }

         if (total <= budget) {
            return;
         }

         bins.sort(Comparator.comparingLong((px) -> {
            try {
               return Files.getLastModifiedTime(px).toMillis();
            } catch (IOException var2) {
               return 0L;
            }
         }));

         for(Path p : bins) {
            if (total <= budget) {
               break;
            }

            total -= Files.size(p);
            Files.deleteIfExists(p);
            Files.deleteIfExists(dir.resolve(p.getFileName().toString().replace(".bin", ".meta")));
         }
      } catch (Exception ex) {
         CFHGui.LOGGER.debug("[CFHGui] media cache prune failed: {}", ex.toString());
      }

   }

   public static void clearDisk() {
      POOL.submit(() -> {
         try {
            Path dir = diskDir();
            if (!Files.isDirectory(dir, new LinkOption[0])) {
               return;
            }

            Stream<Path> s = Files.list(dir);

            try {
               for(Path p : s.toList()) {
                  Files.deleteIfExists(p);
               }
            } catch (Throwable var5) {
               if (s != null) {
                  try {
                     s.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (s != null) {
               s.close();
            }
         } catch (Exception ex) {
            CFHGui.LOGGER.warn("[CFHGui] could not clear media cache: {}", ex.toString());
         }

      });
   }

   private static void evictIfCrowded() {
      int max = Math.max(8, MediaConfig.get().maxEntries);
      if (ENTRIES.size() > max) {
         List<Map.Entry<String, Entry>> byAge = new ArrayList(ENTRIES.entrySet());
         byAge.sort((a, b) -> Long.compare(((Entry)a.getValue()).lastTouched, ((Entry)b.getValue()).lastTouched));
         Iterator<Map.Entry<String, Entry>> it = byAge.iterator();

         while(it.hasNext() && ENTRIES.size() > max) {
            String id = (String)((Map.Entry)it.next()).getKey();
            ENTRIES.remove(id);
            List<ResourceLocation> owned;
            synchronized(TEXTURES) {
               owned = (List)TEXTURES.remove(id);
            }

            if (owned != null) {
               Minecraft.getInstance().execute(() -> owned.forEach((rl) -> Minecraft.getInstance().getTextureManager().release(rl)));
            }
         }

      }
   }

   public static void forget(String id) {
      ENTRIES.remove(id);
      List<ResourceLocation> owned;
      synchronized(TEXTURES) {
         owned = (List)TEXTURES.remove(id);
      }

      if (owned != null) {
         Minecraft.getInstance().execute(() -> owned.forEach((rl) -> Minecraft.getInstance().getTextureManager().release(rl)));
      }

   }

   private MediaCache() {
   }

   public static enum Status {
      LOADING,
      READY,
      FAILED;
   }

   public static record Frame(ResourceLocation texture, int delayMs) {
   }

   public static final class Entry {
      public volatile Status status;
      public volatile byte[] bytes;
      public volatile List<Frame> frames;
      public volatile int width;
      public volatile int height;
      public volatile String sender;
      public volatile String kind;
      public volatile int durationMs;
      public volatile String name;
      public volatile String error;
      volatile long lastTouched;
      final long created;
      volatile boolean inbound;

      public Entry() {
         this.status = MediaCache.Status.LOADING;
         this.frames = List.of();
         this.sender = "";
         this.kind = "";
         this.name = "";
         this.error = "";
         this.lastTouched = System.currentTimeMillis();
         this.created = System.currentTimeMillis();
      }

      public boolean ready() {
         return this.status == MediaCache.Status.READY;
      }

      public boolean isGif() {
         return this.frames.size() > 1;
      }
   }
}
