package com.onehumanawa.cfhcore.content.gui.core.screen.reskin;

import com.mojang.blaze3d.platform.NativeImage;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageResources;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

public final class IconCell {
   private static final Map<String, Middle> MIDDLES = new HashMap();
   private static int generation = -1;

   public static void blitCentered(GuiGraphics g, ResourceLocation texture, float centerX, float centerY, int u, int v, int width, int height) {
      Middle m = middle(texture, u, v, width, height);
      float scale = (float)Minecraft.getInstance().getWindow().getGuiScale();
      float x = (float)Math.round((centerX - m.x()) * scale) / scale;
      float y = (float)Math.round((centerY - m.y()) * scale) / scale;
      g.pose().pushPose();
      g.pose().translate(x, y, 0.0F);
      g.blit(texture, 0, 0, u, v, width, height);
      g.pose().popPose();
   }

   private static Middle middle(ResourceLocation texture, int u, int v, int width, int height) {
      StageResources.ensureRegistered();
      if (generation != StageResources.generation()) {
         MIDDLES.clear();
         generation = StageResources.generation();
      }

      return (Middle)MIDDLES.computeIfAbsent(String.valueOf(texture) + "/" + u + "/" + v + "/" + width + "/" + height, (k) -> find(texture, u, v, width, height));
   }

   private static Middle find(ResourceLocation texture, int u, int v, int width, int height) {
      Middle cell = new Middle((float)width / 2.0F, (float)height / 2.0F);

      try {
         Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
         if (resource.isEmpty()) {
            return cell;
         } else {
            InputStream in = ((Resource)resource.get()).open();

            Middle var11;
            label102: {
               Middle var17;
               try {
                  NativeImage image = NativeImage.read(in);

                  label104: {
                     try {
                        if (u < 0 || v < 0 || u + width > image.getWidth() || v + height > image.getHeight()) {
                           var17 = cell;
                           break label104;
                        }

                        int[] light = bounds(image, u, v, width, height, true);
                        int[] box = light != null ? light : bounds(image, u, v, width, height, false);
                        var11 = box == null ? cell : new Middle((float)(box[0] + box[2] + 1) / 2.0F, (float)(box[1] + box[3] + 1) / 2.0F);
                     } catch (Throwable var14) {
                        if (image != null) {
                           try {
                              image.close();
                           } catch (Throwable var13) {
                              var14.addSuppressed(var13);
                           }
                        }

                        throw var14;
                     }

                     if (image != null) {
                        image.close();
                     }
                     break label102;
                  }

                  if (image != null) {
                     image.close();
                  }
               } catch (Throwable var15) {
                  if (in != null) {
                     try {
                        in.close();
                     } catch (Throwable var12) {
                        var15.addSuppressed(var12);
                     }
                  }

                  throw var15;
               }

               if (in != null) {
                  in.close();
               }

               return var17;
            }

            if (in != null) {
               in.close();
            }

            return var11;
         }
      } catch (Exception var16) {
         return cell;
      }
   }

   private static int[] bounds(NativeImage image, int u, int v, int width, int height, boolean lightOnly) {
      int minX = width;
      int minY = height;
      int maxX = -1;
      int maxY = -1;

      for(int y = 0; y < height; ++y) {
         for(int x = 0; x < width; ++x) {
            int abgr = image.getPixelRGBA(u + x, v + y);
            if (abgr >>> 24 > 40 && (!lightOnly || Math.max(abgr & 255, Math.max(abgr >> 8 & 255, abgr >> 16 & 255)) >= 128)) {
               minX = Math.min(minX, x);
               maxX = Math.max(maxX, x);
               minY = Math.min(minY, y);
               maxY = Math.max(maxY, y);
            }
         }
      }

      return maxX < 0 ? null : new int[]{minX, minY, maxX, maxY};
   }

   private IconCell() {
   }

   private static record Middle(float x, float y) {
   }
}
