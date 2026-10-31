package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop;
import com.onehumanawa.cfhcore.content.gui.core.screen.MenuBackdrop.Kind;
import com.onehumanawa.cfhcore.content.gui.core.stage.Stage;
import com.onehumanawa.cfhcore.content.gui.core.stage.node.MotesNode;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.menu.MenuConfig;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.set.TitleScene;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class TitleBackdrop {
   private static final long FRAME_MS = 33L;
   private static final long IDLE_FRAME_MS = 66L;
   private static final long UNUSED_MS = 120000L;
   private static final long RESTING_MS = 20000L;
   private static final long AWAY_MS = 300000L;
   private static final float RESOLUTION = 0.5F;
   private static @Nullable Stage stage;
   private static @Nullable String built;
   private static @Nullable MotesNode dust;
   private static int dustColor;
   private static final float VEILED_DUST_SIZE = 1.7F;
   private static final float VEILED_DUST_STEADY = 0.5F;
   private static @Nullable TextureTarget kept;
   private static long paintedAt;
   private static long drawnAt;
   private static long movedAt;
   private static double pointerX = Double.NaN;
   private static double pointerY;

   private TitleBackdrop() {
   }

   public static void register() {
      CFHGuiEvents.CLIENT_TICK_END.register(TitleBackdrop::tick);
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

         MenuConfig cfg = CFHGuiMenu.config();
         TitleScene which = TitleScene.of(cfg.titleScene);
         String var10000 = which.id();
         String wanted = var10000 + "/" + TitleStyle.of(cfg.titleTextures).id();
         if (stage != null && !wanted.equals(built)) {
            release();
         }

         long resting = now - movedAt;
         boolean still = Theme.current().motion() <= 0.0F || resting > 300000L;
         long every = resting > 20000L ? 66L : 33L;
         boolean fresh = stage == null || kept == null || kept.width != main.width || kept.height != main.height;
         drawnAt = now;
         if (fresh || !still && now - paintedAt >= every) {
            if (stage == null) {
               stage = build(which);
               built = wanted;
            }

            if (dust != null) {
               boolean veiled = MenuBackdrop.veiled();
               dust.color(veiled ? Colors.withAlpha(Colors.brighten(dustColor, 0.6F), 255) : dustColor).grow(veiled ? 1.7F : 1.0F).steady(veiled ? 0.5F : 0.0F);
            }

            stage.render(g, 0, 0, width, height, (double)-1.0F, (double)-1.0F, partialTick);
            keep(main);
            paintedAt = now;
            return true;
         } else {
            show(g, main);
            return true;
         }
      }
   }

   private static Stage build(TitleScene which) {
      Stage s = new Stage();
      OverhaulTitleScreen.Dressed dressed = OverhaulTitleScreen.dress(s, which);
      s.camera().timeline().skip(s.camera().pose());
      s.resolutionScale(0.5F);
      Vector3f middle = OverhaulTitleScreen.middle(dressed.scene());
      dustColor = dressed.set() != null ? dressed.set().dust() : 1728047042;
      dust = dustColor == 0 ? null : new MotesNode(54, 5.2F, 2.6F, 2.6F, 0.02F, dustColor, 4L);
      if (dust != null) {
         ((MotesNode)s.add(dust)).at(middle.x, middle.y + 0.1F, middle.z + 0.4F);
      }

      return s;
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
      if (stage != null || kept != null) {
         if (!MenuBackdrop.active(Kind.TITLE) || Util.getMillis() - drawnAt > 120000L) {
            release();
         }

      }
   }

   private static void release() {
      Stage s = stage;
      stage = null;
      built = null;
      dust = null;
      if (s != null) {
         s.close();
      }

      if (kept != null) {
         kept.destroyBuffers();
         kept = null;
      }

   }
}
