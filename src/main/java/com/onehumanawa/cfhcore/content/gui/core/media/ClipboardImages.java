package com.onehumanawa.cfhcore.content.gui.core.media;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public final class ClipboardImages {
   private static final int MAX_DIM = 3072;
   private static final float[] JPEG_QUALITIES = new float[]{0.92F, 0.85F, 0.78F, 0.7F};

   public static boolean hasImage() {
      try {
         return Toolkit.getDefaultToolkit().getSystemClipboard().isDataFlavorAvailable(DataFlavor.imageFlavor);
      } catch (Throwable var1) {
         return false;
      }
   }

   public static @Nullable byte[] readImage(int maxBytes) {
      try {
         Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.imageFlavor);
         if (data instanceof Image awt) {
            return encodeWithin(toBuffered(awt), maxBytes);
         } else {
            return null;
         }
      } catch (Throwable t) {
         CFHGui.LOGGER.debug("[CFHGui] clipboard image read failed: {}", t.toString());
         return null;
      }
   }

   public static boolean tryPaste(int maxBytes, Consumer<byte[]> onImage, @Nullable Runnable onFail) {
      if (!hasImage()) {
         return false;
      } else {
         Thread worker = new Thread(() -> {
            byte[] bytes = readImage(maxBytes);
            Minecraft.getInstance().execute(() -> {
               if (bytes != null) {
                  onImage.accept(bytes);
               } else if (onFail != null) {
                  onFail.run();
               }

            });
         }, "cfhgui-paste");
         worker.setDaemon(true);
         worker.start();
         return true;
      }
   }

   public static @Nullable byte[] shrinkToFit(byte[] raw, int maxBytes) {
      try {
         if (raw.length > maxBytes || !isImage(raw) && !isGif(raw)) {
            if (isGif(raw)) {
               return null;
            } else {
               BufferedImage img = ImageIO.read(new ByteArrayInputStream(raw));
               return img == null ? null : encodeWithin(img, maxBytes);
            }
         } else {
            return raw;
         }
      } catch (Exception var3) {
         return null;
      }
   }

   public static boolean isImage(byte[] b) {
      if (b != null && b.length >= 4) {
         boolean png = (b[0] & 255) == 137 && b[1] == 80 && b[2] == 78 && b[3] == 71;
         boolean jpg = (b[0] & 255) == 255 && (b[1] & 255) == 216;
         return png || jpg;
      } else {
         return false;
      }
   }

   public static boolean isGif(byte[] b) {
      return b != null && b.length > 3 && b[0] == 71 && b[1] == 73 && b[2] == 70;
   }

   public static String extensionFor(byte[] b) {
      if (isGif(b)) {
         return "gif";
      } else if (b != null && b.length >= 4 && (b[0] & 255) == 137) {
         return "png";
      } else {
         return isImage(b) ? "jpg" : "bin";
      }
   }

   public static @Nullable byte[] encodeWithin(BufferedImage source, int maxBytes) throws IOException {
      boolean hasAlpha = source.getColorModel().hasAlpha();
      float factor = Math.min(1.0F, Math.min(3072.0F / (float)source.getWidth(), 3072.0F / (float)source.getHeight()));

      for(int attempt = 0; attempt < 8; ++attempt) {
         BufferedImage img = factor >= 1.0F ? source : scaled(source, factor);
         byte[] best;
         if (hasAlpha) {
            best = encodePng(img);
         } else {
            best = null;

            for(float q : JPEG_QUALITIES) {
               best = encodeJpeg(img, q);
               if (best.length <= maxBytes) {
                  break;
               }
            }
         }

         if (best != null && best.length <= maxBytes) {
            return best;
         }

         if (img.getWidth() <= 96 || img.getHeight() <= 96) {
            return null;
         }

         double overshoot = best == null ? (double)2.0F : (double)best.length / (double)maxBytes;
         factor *= (float)(Math.sqrt((double)1.0F / overshoot) * 0.95);
      }

      return null;
   }

   private static byte[] encodeJpeg(BufferedImage img, float quality) throws IOException {
      BufferedImage rgb;
      if (img.getType() == 1) {
         rgb = img;
      } else {
         rgb = new BufferedImage(img.getWidth(), img.getHeight(), 1);
         Graphics2D g = rgb.createGraphics();
         g.drawImage(img, 0, 0, (ImageObserver)null);
         g.dispose();
      }

      ImageWriter writer = (ImageWriter)ImageIO.getImageWritersByFormatName("jpeg").next();
      ImageWriteParam param = writer.getDefaultWriteParam();
      param.setCompressionMode(2);
      param.setCompressionQuality(quality);
      ByteArrayOutputStream out = new ByteArrayOutputStream(262144);

      try {
         ImageOutputStream ios = ImageIO.createImageOutputStream(out);

         try {
            writer.setOutput(ios);
            writer.write((IIOMetadata)null, new IIOImage(rgb, (List)null, (IIOMetadata)null), param);
         } catch (Throwable var14) {
            if (ios != null) {
               try {
                  ios.close();
               } catch (Throwable var13) {
                  var14.addSuppressed(var13);
               }
            }

            throw var14;
         }

         if (ios != null) {
            ios.close();
         }
      } finally {
         writer.dispose();
      }

      return out.toByteArray();
   }

   private static BufferedImage toBuffered(Image awt) {
      if (awt instanceof BufferedImage b) {
         return b;
      } else {
         BufferedImage out = new BufferedImage(awt.getWidth((ImageObserver)null), awt.getHeight((ImageObserver)null), 2);
         Graphics2D g = out.createGraphics();
         g.drawImage(awt, 0, 0, (ImageObserver)null);
         g.dispose();
         return out;
      }
   }

   private static BufferedImage scaled(BufferedImage in, float factor) {
      int w = Math.max(1, Math.round((float)in.getWidth() * factor));
      int h = Math.max(1, Math.round((float)in.getHeight() * factor));
      BufferedImage out = new BufferedImage(w, h, 2);
      Graphics2D g = out.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.drawImage(in, 0, 0, w, h, (ImageObserver)null);
      g.dispose();
      return out;
   }

   private static byte[] encodePng(BufferedImage img) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream(262144);
      ImageIO.write(img, "png", out);
      return out.toByteArray();
   }

   public static void copy(Path file, Consumer<Boolean> onResult) {
      Thread worker = new Thread(() -> {
         boolean ok = false;

         try {
            if (!GraphicsEnvironment.isHeadless()) {
               BufferedImage read = ImageIO.read(file.toFile());
               if (read != null) {
                  BufferedImage rgb = new BufferedImage(read.getWidth(), read.getHeight(), 1);
                  Graphics2D g2 = rgb.createGraphics();
                  g2.drawImage(read, 0, 0, (ImageObserver)null);
                  g2.dispose();
                  Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageTransferable(rgb), (ClipboardOwner)null);
                  ok = true;
               }
            }
         } catch (Throwable t) {
            CFHGui.LOGGER.warn("[CFHGui] clipboard image copy failed: {}", t.toString());
         }

         boolean done = ok;
         Minecraft.getInstance().execute(() -> onResult.accept(done));
      }, "cfhgui-clipboard-copy");
      worker.setDaemon(true);
      worker.start();
   }

   private ClipboardImages() {
   }

   private static record ImageTransferable(BufferedImage image) implements Transferable {
      public DataFlavor[] getTransferDataFlavors() {
         return new DataFlavor[]{DataFlavor.imageFlavor};
      }

      public boolean isDataFlavorSupported(DataFlavor flavor) {
         return DataFlavor.imageFlavor.equals(flavor);
      }

      public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
         if (!this.isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
         } else {
            return this.image;
         }
      }
   }
}
