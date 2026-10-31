package com.onehumanawa.cfhcore.content.gui.menu.client.loading.journey;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop.Kind;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class Backdrop {
   private static final long FRAME_MS = 33L;
   private static final long IDLE_FRAME_MS = 66L;
   private static final long UNUSED_MS = 120000L;
   private static final long RESTING_MS = 20000L;
   private static final long AWAY_MS = 300000L;
   private static final float RESOLUTION = 0.5F;
   private static @Nullable LandScene scene;
   private static @Nullable TextureTarget kept;
   private static long paintedAt;
   private static long drawnAt;
   private static long movedAt;
   private static double pointerX = Double.NaN;
   private static double pointerY;

   private Backdrop() {
   }

   public static void register() {
      CFHGuiEvents.CLIENT_TICK_END.register(Backdrop::tick);
   }

   public static boolean paint(GuiGraphics g, int width, int height, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      RenderTarget main = mc.getMainRenderTarget();
      if (GL11.glGetInteger(36006) != main.frameBufferId) {
         return false;
      } else {
         long now = Util.getMillis();
         if (mc.mouseHandler.xpos() != pointerX || mc.mouseHandler.ypos() != pointerY) {
            pointerX = mc.mouseHandler.xpos();
            pointerY = mc.mouseHandler.ypos();
            movedAt = now;
         }

         long resting = now - movedAt;
         boolean still = Theme.current().motion() <= 0.0F || resting > 300000L;
         long every = resting > 20000L ? 66L : 33L;
         boolean fresh = scene == null || kept == null || kept.width != main.width || kept.height != main.height;
         drawnAt = now;
         if (fresh || !still && now - paintedAt >= every) {
            if (scene == null) {
               scene = (new LandScene((Land)null)).looking(false);
               scene.stage().resolutionScale(0.5F);
            }

            scene.veiled(MenuBackdrop.veiled());
            scene.render(g, width, height, 0.0F, partialTick);
            keep(main);
            paintedAt = now;
            return true;
         } else {
            show(g, main);
            return true;
         }
      }
   }

   private static void keep(RenderTarget main) {
      if (kept == null) {
         kept = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
      } else if (kept.width != main.width || kept.height != main.height) {
         kept.resize(main.width, main.height, Minecraft.ON_OSX);
      }

      copy(main.frameBufferId, kept.frameBufferId, main.width, main.height, main);
   }

   private static void show(GuiGraphics g, RenderTarget main) {
      g.flush();
      copy(kept.frameBufferId, main.frameBufferId, main.width, main.height, main);
   }

   private static void copy(int from, int to, int w, int h, RenderTarget main) {
      boolean scissor = GL11.glIsEnabled(3089);
      if (scissor) {
         GL11.glDisable(3089);
      }

      GL30.glBindFramebuffer(36008, from);
      GL30.glBindFramebuffer(36009, to);
      GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, 16384, 9728);
      GL30.glBindFramebuffer(36160, main.frameBufferId);
      main.bindWrite(false);
      if (scissor) {
         GL11.glEnable(3089);
      }

   }

   private static void tick() {
      if (scene != null || kept != null) {
         if (!MenuBackdrop.active(Kind.SKY) || Util.getMillis() - drawnAt > 120000L) {
            release();
         }

      }
   }

   private static void release() {
      LandScene s = scene;
      scene = null;
      if (s != null) {
         try {
            s.close();
         } catch (RuntimeException e) {
            CFHGuiMenu.LOGGER.debug("[CFHGui Menu] the scene behind the menus could not be closed: {}", e.toString());
         }
      }

      if (kept != null) {
         kept.destroyBuffers();
         kept = null;
      }

   }

   static LandScene hand() {
      LandScene s = scene;
      if (s != null && !s.leaving() && s.land() == null) {
         scene = null;
         if (kept != null) {
            kept.destroyBuffers();
            kept = null;
         }

         s.stage().resolutionScale(LandScene.resolution());
         return s.veiled(false).looking(true);
      } else {
         return new LandScene((Land)null);
      }
   }

   static boolean keep(LandScene s) {
      if (MenuBackdrop.active(Kind.SKY) && s != scene) {
         release();
         scene = s.looking(false);
         s.stage().resolutionScale(0.5F);
         drawnAt = Util.getMillis();
         paintedAt = 0L;
         return true;
      } else {
         return s == scene;
      }
   }
}
