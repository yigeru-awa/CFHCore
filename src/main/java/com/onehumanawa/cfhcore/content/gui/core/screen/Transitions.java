package com.onehumanawa.cfhcore.content.gui.core.screen;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class Transitions {
   private static final int DURATION = 140;
   private static long startMs;
   private static @Nullable Screen forScreen;

   public static void onScreenChanged(@Nullable Screen previous, @Nullable Screen next) {
      forScreen = null;
      if (next != null && previous != null && Theme.current().transitions()) {
         if (!(next instanceof ChatScreen) && !(previous instanceof ChatScreen)) {
            if (Minecraft.getInstance().level == null || ScreenIds.isCFHGui(next)) {
               startMs = Clock.nowMs();
               forScreen = next;
            }
         }
      }
   }

   public static void render(GuiGraphics g, Screen screen, int w, int h) {
      if (forScreen == screen) {
         float t = (float)(Clock.nowMs() - startMs) / (float)Theme.current().ms(140);
         if (!(t >= 1.0F) && !(Theme.current().motion() <= 0.0F)) {
            float a = (1.0F - t) * (1.0F - t) * 0.55F;
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 500.0F);
            g.fill(0, 0, w, h, Colors.withAlpha(Theme.current().isVanilla() ? 0 : 921101, Math.round(a * 255.0F)));
            g.pose().popPose();
         } else {
            forScreen = null;
         }
      }
   }

   private Transitions() {
   }
}
