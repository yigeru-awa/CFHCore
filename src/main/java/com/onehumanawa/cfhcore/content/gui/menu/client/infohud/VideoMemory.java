package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;

final class VideoMemory {
   private static final int UNASKED = 0;
   private static final int NVIDIA = 1;
   private static final int AMD = 2;
   private static final int NOBODY = 3;
   private static final long EVERY_MS = 2000L;
   private static int source = 0;
   private static String reason = "cfhgui_infohud.vram.unknown";
   private static long totalKb;
   private static long usedKb;
   private static long askedAtMs;
   private static final int[] four = new int[4];

   private VideoMemory() {
   }

   static void probe() {
      if (source == 0 && RenderSystem.isOnRenderThread()) {
         try {
            GLCapabilities caps = GL.getCapabilities();
            if (caps.GL_NVX_gpu_memory_info) {
               source = 1;
            } else if (caps.GL_ATI_meminfo) {
               long total = AmdCard.totalKb();
               if (total > 0L) {
                  totalKb = total;
                  source = 2;
               } else {
                  source = 3;
                  reason = "cfhgui_infohud.vram.unknown.total";
               }
            } else {
               source = 3;
            }

            if (source != 3) {
               ask();
               if (totalKb <= 0L) {
                  source = 3;
               }
            }
         } catch (Throwable t) {
            source = 3;
            CFHGuiMenu.LOGGER.debug("[CFHGui Menu] the graphics driver could not be asked about video memory: {}", t.toString());
         }

         if (source == 3) {
            CFHGuiMenu.LOGGER.info("[CFHGui Menu] info overlay: this graphics driver does not report video memory; its bar is left out");
         }

      }
   }

   static void refresh(long nowMs) {
      if (source == 0) {
         probe();
      }

      if (source != 3 && nowMs - askedAtMs >= 2000L) {
         askedAtMs = nowMs;

         try {
            long began = InfoHudHarness.PROFILE ? System.nanoTime() : 0L;
            ask();
            if (InfoHudHarness.PROFILE) {
               InfoHudHarness.driver(System.nanoTime() - began);
            }
         } catch (Throwable t) {
            source = 3;
            CFHGuiMenu.LOGGER.debug("[CFHGui Menu] the graphics driver stopped answering about video memory: {}", t.toString());
         }

      }
   }

   private static void ask() {
      if (source == 1) {
         if (totalKb <= 0L) {
            long total = (long)GL11.glGetInteger(36935);
            if (total <= 0L) {
               total = (long)GL11.glGetInteger(36936);
            }

            totalKb = Math.max(0L, total);
         }

         long free = (long)GL11.glGetInteger(36937);
         usedKb = clamp(totalKb - free, totalKb);
      } else if (source == 2) {
         GL11.glGetIntegerv(34812, four);
         usedKb = clamp(totalKb - (long)four[0], totalKb);
      }

   }

   private static long clamp(long used, long total) {
      return Math.max(0L, Math.min(total, used));
   }

   static boolean known() {
      if (source == 0) {
         probe();
      }

      return source == 1 || source == 2;
   }

   static long usedBytes() {
      return usedKb * 1024L;
   }

   static long totalBytes() {
      return totalKb * 1024L;
   }

   static String reasonKey() {
      return reason;
   }
}
