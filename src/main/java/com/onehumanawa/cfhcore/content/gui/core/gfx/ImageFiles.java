package com.onehumanawa.cfhcore.content.gui.core.gfx;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public final class ImageFiles {
   private static final long BUDGET = 67108864L;
   private static final long MAX_FILE = 67108864L;
   private static final int MIN_SIDE = 480;
   private static final int MAX_FRAMES = 1000;
   private static final long UPLOAD_PER_DRAW = 8388608L;
   private static final int DEFAULT_DELAY_MS = 100;
   private static final long IDLE_MS = 120000L;
   private static final ExecutorService POOL = Executors.newSingleThreadExecutor((r) -> {
      Thread t = new Thread(r, "cfhgui-images");
      t.setDaemon(true);
      return t;
   });
   private static final Map<String, Entry> ENTRIES = new HashMap();
   private static long lastSweep;
   private static int counter;

   public static @Nullable Picture get(Path file) {
      String key = file.toAbsolutePath().toString();
      long now = Util.getMillis();
      Entry e = (Entry)ENTRIES.get(key);
      if (e == null) {
         e = new Entry();
         ENTRIES.put(key, e);
         read(file, key, e);
      }

      e.lastUsed = now;
      if (now - lastSweep > 10000L) {
         sweep(now);
      }

      return e.picture;
   }

   public static void invalidate(Path file) {
      Entry e = (Entry)ENTRIES.remove(file.toAbsolutePath().toString());
      if (e != null && e.picture != null) {
         e.picture.release();
      }

   }

   private static void read(Path file, String key, Entry e) {
      CompletableFuture.supplyAsync(() -> {
         try {
            return decode(file);
         } catch (OutOfMemoryError | Exception ex) {
            CFHGui.LOGGER.warn("[CFHGui] cannot read the picture {}: {}", file, ((Throwable)ex).toString());
            return null;
         }
      }, POOL).thenAcceptAsync((p) -> {
         if (ENTRIES.get(key) != e) {
            if (p != null) {
               p.release();
            }

         } else {
            if (p == null) {
               e.failed = true;
            } else {
               e.picture = p;
            }

         }
      }, Minecraft.getInstance());
   }

   private static void sweep(long now) {
      lastSweep = now;
      Iterator<Entry> it = ENTRIES.values().iterator();

      while(it.hasNext()) {
         Entry e = (Entry)it.next();
         if (now - e.lastUsed >= 120000L && (e.picture != null || e.failed)) {
            if (e.picture != null) {
               e.picture.release();
            }

            it.remove();
         }
      }

   }

   private static Picture decode(Path file) throws IOException {
      long size = Files.size(file);
      if (size > 67108864L) {
         throw new IOException("the file is " + (size >> 20) + " MB, more than the 64 MB a picture may be");
      } else {
         byte[] data = Files.readAllBytes(file);
         if (startsWith(data, 137, 80, 78, 71)) {
            NativeImage img = NativeImage.read(new ByteArrayInputStream(data));
            return new Picture(img.getWidth(), img.getHeight(), List.of(img), List.of(1), false);
         } else if (startsWith(data, 71, 73, 70, 56)) {
            return gif(data);
         } else if (startsWith(data, 82, 73, 70, 70) && data.length > 11 && data[8] == 87 && data[9] == 69 && data[10] == 66 && data[11] == 80) {
            throw new IOException("WebP is not a format CFHGui can read: save the picture as PNG, JPEG or GIF");
         } else {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) {
               throw new IOException("not a picture CFHGui can read (PNG, JPEG, GIF, BMP or TIFF)");
            } else if ((long)image.getWidth() * (long)image.getHeight() * 4L > 67108864L) {
               double s = Math.sqrt((double)1.6777216E7F / ((double)image.getWidth() * (double)image.getHeight()));
               BufferedImage small = scaled(image, Math.max(1, (int)((double)image.getWidth() * s)), Math.max(1, (int)((double)image.getHeight() * s)));
               return new Picture(image.getWidth(), image.getHeight(), List.of(toNative(small)), List.of(1), true);
            } else {
               return new Picture(image.getWidth(), image.getHeight(), List.of(toNative(image)), List.of(1), false);
            }
         }
      }
   }

   private static Picture gif(byte[] data) throws IOException {
      Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
      if (!readers.hasNext()) {
         throw new IOException("this Java has no GIF reader");
      } else {
         ImageReader reader = (ImageReader)readers.next();
         List<NativeImage> out = new ArrayList();
         List<Integer> delays = new ArrayList();

         Picture var46;
         try {
            ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data));

            try {
               reader.setInput(in, false);
               int count = reader.getNumImages(true);
               if (count <= 0) {
                  throw new IOException("no frames in the GIF");
               }

               int sw = 0;
               int sh = 0;

               try {
                  Node root = reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0");

                  for(Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
                     if (n.getNodeName().equals("LogicalScreenDescriptor")) {
                        NamedNodeMap a = n.getAttributes();
                        sw = Integer.parseInt(a.getNamedItem("logicalScreenWidth").getNodeValue());
                        sh = Integer.parseInt(a.getNamedItem("logicalScreenHeight").getNodeValue());
                     }
                  }
               } catch (Exception var38) {
               }

               if (sw <= 0 || sh <= 0) {
                  sw = reader.getWidth(0);
                  sh = reader.getHeight(0);
               }

               double pixels = (double)sw * (double)sh;
               float scale = (float)Math.min((double)1.0F, Math.sqrt((double)1.6777216E7F / (pixels * (double)count)));
               float floor = Math.min(1.0F, 480.0F / (float)Math.max(sw, sh));
               if (scale < floor) {
                  scale = floor;
               }

               int kw = Math.max(1, (int)((float)sw * scale));
               int kh = Math.max(1, (int)((float)sh * scale));
               long perFrame = (long)kw * (long)kh * 4L;
               int step = Math.max((int)Math.ceil((double)count / (double)1000.0F), (int)Math.ceil((double)((long)count * perFrame) / (double)6.7108864E7F));
               if (scale < 1.0F || step > 1) {
                  CFHGui.LOGGER.info("[CFHGui] a GIF of {} frames at {} x {} is kept at {} x {}{} to stay within {} MB", new Object[]{count, sw, sh, kw, kh, step > 1 ? ", every " + step + " frames as one" : "", 64L});
               }

               BufferedImage canvas = new BufferedImage(sw, sh, 2);
               int pending = 0;

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
                        int hundredths = Integer.parseInt(a.getNamedItem("delayTime").getNodeValue());
                        delay = hundredths <= 1 ? 100 : hundredths * 10;
                        disposal = a.getNamedItem("disposalMethod").getNodeValue();
                     } else if (n.getNodeName().equals("ImageDescriptor")) {
                        NamedNodeMap a = n.getAttributes();
                        x = Integer.parseInt(a.getNamedItem("imageLeftPosition").getNodeValue());
                        y = Integer.parseInt(a.getNamedItem("imageTopPosition").getNodeValue());
                     }
                  }

                  BufferedImage previous = disposal.equals("restoreToPrevious") ? copy(canvas) : null;
                  Graphics2D g = canvas.createGraphics();
                  g.drawImage(frame, x, y, (ImageObserver)null);
                  g.dispose();
                  pending += delay;
                  if (i % step == step - 1 || i == count - 1) {
                     out.add(toNative(scale < 1.0F ? scaled(canvas, kw, kh) : canvas));
                     delays.add(pending);
                     pending = 0;
                  }

                  if (disposal.equals("restoreToBackgroundColor")) {
                     Graphics2D g2 = canvas.createGraphics();
                     g2.setComposite(AlphaComposite.Clear);
                     g2.fillRect(x, y, frame.getWidth(), frame.getHeight());
                     g2.dispose();
                  } else if (previous != null) {
                     canvas = previous;
                  }
               }

               var46 = new Picture(sw, sh, out, delays, scale < 1.0F);
            } catch (Throwable var39) {
               if (in != null) {
                  try {
                     in.close();
                  } catch (Throwable var37) {
                     var39.addSuppressed(var37);
                  }
               }

               throw var39;
            }

            if (in != null) {
               in.close();
            }
         } catch (RuntimeException | OutOfMemoryError | IOException e) {
            for(NativeImage img : out) {
               img.close();
            }

            throw e;
         } finally {
            reader.dispose();
         }

         return var46;
      }
   }

   private static boolean startsWith(byte[] data, int... head) {
      if (data.length < head.length) {
         return false;
      } else {
         for(int i = 0; i < head.length; ++i) {
            if ((data[i] & 255) != head[i]) {
               return false;
            }
         }

         return true;
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

   private static BufferedImage scaled(BufferedImage src, int w, int h) {
      BufferedImage out = new BufferedImage(w, h, 2);
      Graphics2D g = out.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.setComposite(AlphaComposite.Src);
      g.drawImage(src, 0, 0, w, h, (ImageObserver)null);
      g.dispose();
      return out;
   }

   private static NativeImage toNative(BufferedImage img) {
      int w = img.getWidth();
      int h = img.getHeight();
      int[] row = new int[w];
      NativeImage ni = new NativeImage(w, h, false);

      for(int y = 0; y < h; ++y) {
         img.getRGB(0, y, w, 1, row, 0, w);

         for(int x = 0; x < w; ++x) {
            int argb = row[x];
            ni.setPixelRGBA(x, y, argb & -16711936 | (argb & 16711680) >>> 16 | (argb & 255) << 16);
         }
      }

      return ni;
   }

   private ImageFiles() {
   }

   private static final class Entry {
      @Nullable Picture picture;
      boolean failed;
      long lastUsed;
   }

   public static final class Picture {
      private final int width;
      private final int height;
      private final int[] delays;
      private final int total;
      private final boolean smooth;
      private final ResourceLocation[] frames;
      private @Nullable NativeImage[] waiting;
      private int uploaded;
      private boolean released;

      private Picture(int width, int height, List<NativeImage> images, List<Integer> delays, boolean smooth) {
         this.width = width;
         this.height = height;
         this.smooth = smooth;
         this.waiting = (NativeImage[])images.toArray(new NativeImage[0]);
         this.frames = new ResourceLocation[this.waiting.length];
         this.delays = new int[this.waiting.length];
         int sum = 0;

         for(int i = 0; i < this.delays.length; ++i) {
            this.delays[i] = Math.max(1, (Integer)delays.get(i));
            sum += this.delays[i];
         }

         this.total = Math.max(1, sum);
      }

      public int width() {
         return this.width;
      }

      public int height() {
         return this.height;
      }

      public boolean animated() {
         return this.frames.length > 1;
      }

      public @Nullable ResourceLocation frame() {
         if (this.released) {
            return null;
         } else {
            this.upload();
            if (this.uploaded == 0) {
               return null;
            } else if (this.uploaded >= this.frames.length && this.frames.length != 1) {
               long at = Util.getMillis() % (long)this.total;

               for(int i = 0; i < this.frames.length; ++i) {
                  at -= (long)this.delays[i];
                  if (at < 0L) {
                     return this.frames[i];
                  }
               }

               return this.frames[0];
            } else {
               return this.frames[0];
            }
         }
      }

      private void upload() {
         NativeImage[] w = this.waiting;
         if (w != null) {
            ResourceLocation id;
            for(long spent = 0L; this.uploaded < w.length && (spent == 0L || spent < 8388608L); this.frames[this.uploaded++] = id) {
               NativeImage img = w[this.uploaded];
               spent += (long)img.getWidth() * (long)img.getHeight() * 4L;
               int var10000 = ImageFiles.counter++;
               id = CFHGui.id("dyn/picture/" + var10000);
               Minecraft.getInstance().getTextureManager().register(id, new FrameTexture(img, this.smooth));
               w[this.uploaded] = null;
            }

            if (this.uploaded == w.length) {
               this.waiting = null;
            }

         }
      }

      public void release() {
         if (!this.released) {
            this.released = true;

            for(int i = 0; i < this.uploaded; ++i) {
               Minecraft.getInstance().getTextureManager().release(this.frames[i]);
            }

            NativeImage[] w = this.waiting;
            this.waiting = null;
            if (w != null) {
               for(NativeImage img : w) {
                  if (img != null) {
                     img.close();
                  }
               }
            }

         }
      }
   }

   private static final class FrameTexture extends AbstractTexture {
      FrameTexture(NativeImage image, boolean smooth) {
         TextureUtil.prepareImage(this.getId(), image.getWidth(), image.getHeight());
         image.upload(0, 0, 0, 0, 0, image.getWidth(), image.getHeight(), smooth, true, false, true);
      }

      public void load(ResourceManager manager) {
      }
   }
}
