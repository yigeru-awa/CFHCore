package com.onehumanawa.cfhcore.content.gui.menu.client.worlds;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class PlayPlates {
   public static final int LARGE = 20;
   public static final int SMALL = 18;
   public static final int FADE_MS = 200;

   public static CFHGuiIconButton create(int size, Runnable onPress) {
      CFHGuiIconButton plate = new CFHGuiIconButton(0, 0, size, Icon.PLAY, Component.empty(), onPress);
      plate.variant(Variant.PRIMARY);
      plate.iconSize(size >= 18 ? 12 : 8);
      plate.visible = false;
      return plate;
   }

   public static boolean over(CFHGuiIconButton plate, double mouseX, double mouseY) {
      int s = plate.getWidth();
      return mouseX >= (double)(plate.getX() - 1) && mouseX < (double)(plate.getX() + s + 1) && mouseY >= (double)(plate.getY() - 1) && mouseY < (double)(plate.getY() + s + 1);
   }

   public static void label(CFHGuiIconButton plate, Component says) {
      plate.setMessage(says);
      plate.tip(says);
   }

   public static void draw(GuiGraphics g, CFHGuiIconButton plate, float shown, float alpha, float dx, float dy, int mouseX, int mouseY, float partialTick) {
      float k = Mth.clamp(shown, 0.0F, 1.0F);
      float a = Mth.clamp(alpha, 0.0F, 1.0F) * k;
      if (!(a <= 0.01F)) {
         int x = plate.getX();
         int y = plate.getY();
         int s = plate.getWidth();
         float scale = 0.75F + 0.25F * k;
         PoseStack pose = g.pose();
         pose.pushPose();
         pose.translate(dx, dy, 0.0F);
         if (scale < 0.999F) {
            float cx = (float)x + (float)s / 2.0F;
            float cy = (float)y + (float)s / 2.0F;
            pose.translate(cx, cy, 0.0F);
            pose.scale(scale, scale, 1.0F);
            pose.translate(-cx, -cy, 0.0F);
         }

         Theme t = Theme.current();
         if (!t.isVanilla()) {
            CFHGuiDraw.pixelRound(g, x - 1, y - 1, s + 2, s + 2, Colors.scaleAlpha(-1342177280, a), t.radius() + 1);
         }

         plate.variant(t.isVanilla() ? Variant.SECONDARY : Variant.PRIMARY);
         plate.setAlpha(a);
         plate.render(g, mouseX, mouseY, partialTick);
         pose.popPose();
      }
   }

   private PlayPlates() {
   }
}
