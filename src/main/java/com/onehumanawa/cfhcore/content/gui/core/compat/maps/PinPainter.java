package com.onehumanawa.cfhcore.content.gui.core.compat.maps;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

public final class PinPainter {
   public static final int LEFT = -8;
   public static final int RIGHT = 8;
   public static final int TOP = -18;
   public static final int BOTTOM = 1;

   public static void pin(GuiGraphics g, MapPins.Pin pin, boolean lit, float alpha) {
      if (!(alpha <= 0.03F)) {
         RenderSystem.enableBlend();
         g.setColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, alpha));
         g.blit(pin.picture(lit), -7, -17, 14, 17, 0.0F, 0.0F, 28, 34, 28, 34);
         g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
      }
   }

   public static void label(GuiGraphics g, String name, @Nullable Component distance, float scale, float alpha) {
      if (!name.isEmpty() && !(alpha <= 0.03F)) {
         Font font = Minecraft.getInstance().font;
         Theme theme = Theme.current();
         boolean vanilla = theme.isVanilla();
         MutableComponent text = Component.literal(name);
         int nameColor = Colors.scaleAlpha(vanilla ? -1 : theme.text(), alpha);
         int restColor = Colors.scaleAlpha(vanilla ? -4671304 : theme.muted(), alpha);
         Component rest = distance == null ? null : Component.translatable("cfhgui.maps.label.rest", new Object[]{distance});
         int nameWidth = font.width(text);
         int w = nameWidth + (rest == null ? 0 : font.width(rest));
         PoseStack pose = g.pose();
         pose.pushPose();
         pose.translate(0.0F, 3.0F, 0.0F);
         pose.scale(scale, scale, 1.0F);
         int x = -w / 2;
         if (vanilla) {
            g.fill(x - 2, 0, x + w + 2, 10, Colors.scaleAlpha(-1610612736, alpha));
            g.drawString(font, text, x, 1, nameColor, true);
            if (rest != null) {
               g.drawString(font, rest, x + nameWidth, 1, restColor, true);
            }
         } else {
            int plate = Colors.scaleAlpha(Colors.withAlpha(theme.bg(), 216), alpha);
            g.drawManaged(() -> CFHGuiDraw.pixelRound(g, x - 3, 0, w + 6, 11, plate, 1));
            g.drawString(font, text, x, 2, nameColor, false);
            if (rest != null) {
               g.drawString(font, rest, x + nameWidth, 2, restColor, false);
            }
         }

         pose.popPose();
      }
   }

   private PinPainter() {
   }
}
