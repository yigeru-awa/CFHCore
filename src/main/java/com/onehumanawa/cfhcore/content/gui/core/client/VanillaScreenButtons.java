package com.onehumanawa.cfhcore.content.gui.core.client;

import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutApplier;
import com.onehumanawa.cfhcore.content.gui.core.layout.ScreenLayout;
import com.onehumanawa.cfhcore.content.gui.core.layout.WidgetKey;
import com.onehumanawa.cfhcore.content.gui.core.module.Features;
import com.onehumanawa.cfhcore.content.gui.core.screen.Reskin;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiIconButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;

public final class VanillaScreenButtons {
   private static final Set<String> FEEDBACK_KEYS = Set.of("menu.feedback", "menu.sendFeedback", "menu.reportBugs");
   private static final Set<String> ACCESSIBILITY_KEYS = Set.of("options.accessibility", "options.accessibility.title");
   private static final Set<String> LANGUAGE_KEYS = Set.of("options.language");
   private static final int ICON = 20;
   private static final int GAP = 4;
   private static final int ROW = 24;

   static void init() {
      CFHGuiEvents.SCREEN_INIT_POST.register(VanillaScreenButtons::onScreenInit);
   }

   private static void onScreenInit(Screen screen) {
      if (screen instanceof LayoutApplier.ScreenAccess access) {
         if (!Reskin.leftToOther(screen)) {
            if (screen.getClass() == TitleScreen.class) {
               title(screen, access);
            } else if (screen instanceof PauseScreen) {
               PauseScreen pause = (PauseScreen)screen;
               if (pause.showsPauseMenu()) {
                  pause(screen, access);
               }
            }

         }
      }
   }

   private static List<Extra> extras(Screen screen) {
      List<Extra> out = new ArrayList();
      out.add(new Extra(Icon.CFHGUI, Component.translatable("cfhgui.hub.title"), () -> CoreActions.openScreen("cfhgui:hub")));
      if (Features.present("cfhgui")) {
         out.add(new Extra(Icon.CAMERA, Component.translatable("cfhgui.slot.screenshots"), () -> MenuSlots.open("cfhgui:screenshots", screen)));
      }

      if (Features.present("cfhgui_multiplayer")) {
         out.add(new Extra(Icon.FRIENDS, Component.translatable("cfhgui.slot.friends"), () -> MenuSlots.open("cfhgui_multiplayer:hub", screen)));
      }

      if (Features.present("cfhgui_profile")) {
         out.add(new Extra(Icon.USER, Component.translatable("cfhgui.slot.profile"), () -> MenuSlots.open("cfhgui_profile:profile", screen)));
      }

      ScreenLayout layout = LayoutApplier.current(screen);
      out.removeIf((e) -> layout.hides(WidgetKey.first(e.label())));
      return out;
   }

   private static void title(Screen screen, LayoutApplier.ScreenAccess access) {
      AbstractWidget accessibility = find(screen, ACCESSIBILITY_KEYS);
      AbstractWidget language = find(screen, LANGUAGE_KEYS);
      int y = accessibility != null ? accessibility.getY() : (language != null ? language.getY() : screen.height / 4 + 48 + 72 + 12);
      int right = accessibility != null ? accessibility.getX() + accessibility.getWidth() + 4 : screen.width / 2 + 104 + 20 + 4;
      int left = language != null ? language.getX() - 4 : screen.width / 2 - 124 - 4;

      for(Extra e : extras(screen)) {
         int x;
         if (right + 20 <= screen.width - 4) {
            x = right;
            right += 24;
         } else {
            if (left - 20 < 4) {
               break;
            }

            left -= 20;
            x = left;
            left -= 4;
         }

         CFHGuiIconButton b = new CFHGuiIconButton(x, y, 20, e.icon(), e.label(), e.action());
         b.variant(CFHGuiButton.Variant.SECONDARY);
         access.cfhgui$add(b);
      }

   }

   private static void pause(Screen screen, LayoutApplier.ScreenAccess access) {
      int minX = Integer.MAX_VALUE;
      int maxRight = Integer.MIN_VALUE;
      int rowY = Integer.MIN_VALUE;
      int gridBottom = Integer.MIN_VALUE;
      int gridLeft = Integer.MAX_VALUE;
      int gridRight = Integer.MIN_VALUE;

      for(GuiEventListener child : screen.children()) {
         if (child instanceof AbstractWidget w) {
            gridBottom = Math.max(gridBottom, w.getY() + w.getHeight());
            gridLeft = Math.min(gridLeft, w.getX());
            gridRight = Math.max(gridRight, w.getX() + w.getWidth());
            if (is(w, FEEDBACK_KEYS)) {
               w.visible = false;
               minX = Math.min(minX, w.getX());
               maxRight = Math.max(maxRight, w.getX() + w.getWidth());
               rowY = Math.max(rowY, w.getY());
            }
         }
      }

      if (gridBottom != Integer.MIN_VALUE) {
         boolean haveRow = rowY != Integer.MIN_VALUE;
         int x0 = haveRow ? minX : gridLeft;
         int w = Math.max(98, haveRow ? maxRight - minX : gridRight - gridLeft);
         int half = (w - 4) / 2;
         List<Extra> extras = extras(screen);
         int i = 0;

         for(int y = haveRow ? rowY : gridBottom + 4; i < extras.size() && y + 20 <= screen.height - 4; y = Math.max(y + 24, gridBottom + 4)) {
            boolean pair = i + 1 < extras.size();

            for(int k = 0; k < (pair ? 2 : 1); ++i) {
               Extra e = (Extra)extras.get(i);
               int bw = pair ? half : w;
               access.cfhgui$add((new CFHGuiButton(x0 + k * (half + 4), y, bw, e.label(), e.action())).icon(e.icon()));
               ++k;
            }
         }

      }
   }

   private static @Nullable AbstractWidget find(Screen screen, Set<String> keys) {
      for(GuiEventListener child : screen.children()) {
         if (child instanceof AbstractWidget w) {
            if (is(w, keys)) {
               return w;
            }
         }
      }

      return null;
   }

   private static boolean is(AbstractWidget w, Set<String> keys) {
      Component msg = w.getMessage();
      boolean var10000;
      if (msg != null) {
         ComponentContents var4 = msg.getContents();
         if (var4 instanceof TranslatableContents) {
            TranslatableContents tc = (TranslatableContents)var4;
            if (keys.contains(tc.getKey())) {
               var10000 = true;
               return var10000;
            }
         }
      }

      var10000 = false;
      return var10000;
   }

   private VanillaScreenButtons() {
   }

   private static record Extra(Icon icon, Component label, Runnable action) {
   }
}
