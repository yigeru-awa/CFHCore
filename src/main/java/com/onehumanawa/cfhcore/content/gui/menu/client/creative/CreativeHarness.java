package com.onehumanawa.cfhcore.content.gui.menu.client.creative;

import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.mixin.MouseHandlerAccessor;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiScrollPanel;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.options.CFHGuiOptionsScreen;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

public final class CreativeHarness {
   private static Act act;
   private static long due;
   private static double pointerX;
   private static double pointerY;
   private static double targetY;
   private static int steps;
   private static double menuX;
   private static double menuY;

   private CreativeHarness() {
   }

   public static void register() {
      entry("click", CreativeHarness.Act.CLICK);
      entry("carry", CreativeHarness.Act.LIFT);
      entry("carry_far", CreativeHarness.Act.LIFT_FAR);
      entry("drop", CreativeHarness.Act.DROP);
      entry("menu", CreativeHarness.Act.MENU);
      entry("reset", CreativeHarness.Act.RESET);
      CoreActions.SCREEN_FACTORIES.put("cfhgui:creative/options", (parent) -> {
         act = CreativeHarness.Act.OPTIONS;
         due = Util.getMillis() + 250L;
         return new CFHGuiOptionsScreen(parent);
      });
      CFHGuiEvents.SCREEN_RENDER_POST.register((CFHGuiEvents.ScreenRender)(screen, g, mouseX, mouseY, partialTick) -> frame(screen));
   }

   private static void entry(String id, Act what) {
      CoreActions.SCREEN_FACTORIES.put("cfhgui:creative/" + id, (parent) -> {
         act = what;
         due = Util.getMillis() + 250L;
         return parent;
      });
   }

   private static void frame(Screen screen) {
      if (act != CreativeHarness.Act.NONE && Util.getMillis() >= due) {
         if (act == CreativeHarness.Act.OPTIONS) {
            act = CreativeHarness.Act.NONE;
            if (screen instanceof CFHGuiOptionsScreen) {
               CFHGuiOptionsScreen options = (CFHGuiOptionsScreen)screen;
               turnTo(options);
            }

         } else {
            if (screen instanceof CFHGuiCreativeScreen) {
               CFHGuiCreativeScreen creative = (CFHGuiCreativeScreen)screen;
               if (creative.laidOut()) {
                  List<double[]> rows = rows(creative);
                  switch (act.ordinal()) {
                     case 1:
                        if (rows.size() > 3) {
                           pointerX = ((double[])rows.get(3))[0];
                           pointerY = ((double[])rows.get(3))[1];
                           point();
                           button(0, true);
                           button(0, false);
                        }

                        act = CreativeHarness.Act.NONE;
                        break;
                     case 2:
                     case 3:
                        if (rows.size() < 4) {
                           CFHGuiMenu.LOGGER.warn("[CFHGui Menu] harness: too few tabs in sight to move one");
                           act = CreativeHarness.Act.NONE;
                           return;
                        }

                        boolean far = act == CreativeHarness.Act.LIFT_FAR;
                        double[] from = (double[])rows.get(far ? 0 : 1);
                        double[] last = (double[])rows.get(rows.size() - 1);
                        pointerX = from[0];
                        pointerY = from[1];
                        targetY = far ? last[1] + (double)4.0F : ((double[])rows.get(2))[1] + (double)2.0F;
                        steps = 10;
                        point();
                        button(0, true);
                        act = CreativeHarness.Act.CARRY;
                        break;
                     case 4:
                        double step = (targetY - pointerY) / (double)steps;
                        pointerY += step;
                        point();
                        creative.mouseDragged(pointerX, pointerY, 0, (double)0.0F, step);
                        if (--steps <= 0) {
                           act = CreativeHarness.Act.NONE;
                        }
                        break;
                     case 5:
                        button(0, false);
                        act = CreativeHarness.Act.NONE;
                        break;
                     case 6:
                        if (rows.isEmpty()) {
                           act = CreativeHarness.Act.NONE;
                           return;
                        }

                        menuX = pointerX = ((double[])rows.get(0))[0];
                        menuY = pointerY = ((double[])rows.get(0))[1];
                        point();
                        button(1, true);
                        button(1, false);
                        act = CreativeHarness.Act.NONE;
                        break;
                     case 7:
                        pointerX = menuX + (double)30.0F;
                        pointerY = menuY + (double)3.0F + (double)14.0F + (double)5.0F + (double)7.0F;
                        point();
                        button(0, true);
                        button(0, false);
                        act = CreativeHarness.Act.NONE;
                  }

                  return;
               }
            }

            CFHGuiMenu.LOGGER.warn("[CFHGui Menu] harness: the creative inventory is not open, nothing is moved");
            act = CreativeHarness.Act.NONE;
         }
      }
   }

   private static void turnTo(CFHGuiOptionsScreen options) {
      options.showPage("menu");
      String reset = Component.translatable("cfhgui.creative.order.reset").getString();

      for(GuiEventListener child : options.children()) {
         if (child instanceof CFHGuiScrollPanel panel) {
            for(GuiEventListener row : panel.children()) {
               if (row instanceof AbstractWidget widget) {
                  if (widget.getMessage().getString().equals(reset)) {
                     panel.ensureVisible(widget);
                  }
               }
            }
         }
      }

   }

   private static List<double[]> rows(CFHGuiCreativeScreen screen) {
      List<double[]> out = new ArrayList();
      int left = screen.getGuiLeft();
      int top = screen.getGuiTop();
      CreativeModeTab last = null;
      int start = 0;

      for(int y = 0; y <= screen.height; ++y) {
         CreativeModeTab tab = y < screen.height ? screen.tabAt(14, y - top) : null;
         if (tab != last) {
            if (last != null && TabOrder.movable(last)) {
               out.add(new double[]{(double)(left + 40), (double)(start + y) / (double)2.0F, (double)start});
            }

            last = tab;
            start = y;
         }
      }

      return out;
   }

   private static void point() {
      Minecraft mc = Minecraft.getInstance();
      MouseHandlerAccessor mouse = (MouseHandlerAccessor)mc.mouseHandler;
      mouse.cfhgui$setXpos(pointerX * (double)mc.getWindow().getScreenWidth() / (double)mc.getWindow().getGuiScaledWidth());
      mouse.cfhgui$setYpos(pointerY * (double)mc.getWindow().getScreenHeight() / (double)mc.getWindow().getGuiScaledHeight());
   }

   private static void button(int which, boolean down) {
      Minecraft mc = Minecraft.getInstance();
      ((MouseHandlerAccessor)mc.mouseHandler).cfhgui$onPress(mc.getWindow().getWindow(), which, down ? 1 : 0, 0);
   }

   static {
      act = CreativeHarness.Act.NONE;
   }

   private static enum Act {
      NONE,
      CLICK,
      LIFT,
      LIFT_FAR,
      CARRY,
      DROP,
      MENU,
      RESET,
      OPTIONS;
   }
}
