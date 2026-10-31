package com.onehumanawa.cfhcore.content.gui.core.client.quick;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutApplier;
import com.onehumanawa.cfhcore.content.gui.core.mixin.ContainerScreenAccessor;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

public final class QuickActions {
   private static final long ASKED_MS = 6000L;
   private static final QuickAction.State[] ASKED = new QuickAction.State[QuickAction.values().length];
   private static final long[] ASKED_AT = new long[QuickAction.values().length];

   public static void init() {
      CFHGuiEvents.SCREEN_INIT_POST.register(QuickActions::onScreenInit);
   }

   private static void onScreenInit(Screen screen) {
      if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) {
         if (screen instanceof AbstractContainerScreen) {
            AbstractContainerScreen<?> container = (AbstractContainerScreen)screen;
            if (screen instanceof LayoutApplier.ScreenAccess) {
               LayoutApplier.ScreenAccess access = (LayoutApplier.ScreenAccess)screen;
               if (screen instanceof ContainerScreenAccessor) {
                  ContainerScreenAccessor place = (ContainerScreenAccessor)screen;

                  for(GuiEventListener child : screen.children()) {
                     if (child instanceof QuickBar) {
                        return;
                     }
                  }

                  access.cfhgui$add(new QuickBar(container, place));
                  return;
               }

               return;
            }
         }

      }
   }

   public static boolean enabled() {
      return CFHGui.config().quickActions;
   }

   public static int @Nullable [] bounds(Screen screen) {
      for(GuiEventListener child : screen.children()) {
         if (child instanceof QuickBar bar) {
            if (bar.visible && bar.getWidth() > 0 && bar.getHeight() > 0) {
               return new int[]{bar.getX(), bar.getY(), bar.getWidth(), bar.getHeight()};
            }
         }
      }

      return null;
   }

   static boolean allowed(QuickAction action) {
      LocalPlayer player = Minecraft.getInstance().player;
      return player != null && player.connection.getCommands().getRoot().getChild(action.command) != null;
   }

   static QuickAction.State state(QuickAction action) {
      QuickAction.State now = action.now(Minecraft.getInstance());
      int i = action.ordinal();
      QuickAction.State asked = ASKED[i];
      if (asked == null) {
         return now;
      } else if (asked != now && Util.getMillis() - ASKED_AT[i] <= 6000L) {
         return asked;
      } else {
         ASKED[i] = null;
         return now;
      }
   }

   static void run(QuickAction action) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         QuickAction.State next = action.after(state(action));
         if (next.command != null) {
            player.connection.sendCommand(next.command);
            ASKED[action.ordinal()] = next;
            ASKED_AT[action.ordinal()] = Util.getMillis();
         }
      }
   }

   private QuickActions() {
   }
}
