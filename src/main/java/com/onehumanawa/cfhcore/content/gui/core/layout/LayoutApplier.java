package com.onehumanawa.cfhcore.content.gui.core.layout;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.Actions;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Anchor;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;

public final class LayoutApplier {
   private static final Map<Screen, List<AbstractWidget>> ADDED = new WeakHashMap();
   private static final Map<Screen, Map<AbstractWidget, int[]>> ORIGINAL = new WeakHashMap();
   private static boolean suspended;

   public static void setSuspended(boolean s) {
      suspended = s;
   }

   public static String layoutId(Screen screen) {
      if (screen instanceof LayoutIdProvider p) {
         try {
            String id = p.layoutId();
            if (id != null && !id.isBlank()) {
               return id;
            }
         } catch (Exception var3) {
         }
      }

      return ScreenIds.of(screen);
   }

   public static ScreenLayout current(Screen screen) {
      String id = layoutId(screen);
      if (LayoutStore.refresh(id)) {
         LayoutBackground.forgetImages();
      }

      return LayoutStore.get(id);
   }

   public static void apply(Screen screen) {
      if (!suspended && screen != null && !ScreenIds.isContainer(screen)) {
         String id = layoutId(screen);
         if (LayoutStore.refresh(id)) {
            LayoutBackground.forgetImages();
         }

         ScreenLayout layout = LayoutStore.get(id);
         remove(screen);
         if (!layout.isEmpty()) {
            LayoutBackground.checkImages();

            try {
               applyVanilla(screen, layout);
               applyElements(screen, layout);
            } catch (Exception e) {
               CFHGui.LOGGER.error("[CFHGui] applying layout {} failed", id, e);
            }

         }
      }
   }

   public static void refresh(Screen screen) {
      if (screen instanceof LayoutAware && screen instanceof ScreenAccess) {
         ScreenAccess access = (ScreenAccess)screen;
         access.cfhgui$rebuild();
      } else {
         apply(screen);
      }
   }

   public static void reposition(Screen screen) {
      Map<AbstractWidget, int[]> orig = (Map)ORIGINAL.get(screen);
      if (orig != null) {
         orig.forEach((w, r) -> {
            r[0] = w.getX();
            r[1] = w.getY();
            r[2] = w.getWidth();
            r[3] = w.getHeight();
         });
      }

      apply(screen);
   }

   public static void remove(Screen screen) {
      List<AbstractWidget> added = (List)ADDED.remove(screen);
      if (added != null && screen instanceof ScreenAccess acc) {
         for(AbstractWidget w : added) {
            acc.cfhgui$remove(w);
         }
      }

      Map<AbstractWidget, int[]> orig = (Map)ORIGINAL.remove(screen);
      if (orig != null) {
         orig.forEach((wx, r) -> {
            wx.setX(r[0]);
            wx.setY(r[1]);
            wx.setWidth(r[2]);
            wx.setHeight(r[3]);
            wx.visible = r[4] != 0;
         });
      }

   }

   private static void applyVanilla(Screen screen, ScreenLayout layout) {
      if (!layout.hidden.isEmpty() || !layout.moved.isEmpty()) {
         Map<AbstractWidget, String> keys = WidgetKey.all(screen.children());
         Map<AbstractWidget, int[]> orig = (Map)ORIGINAL.computeIfAbsent(screen, (s) -> new HashMap());
         keys.forEach((w, key) -> {
            ScreenLayout.Placement p = (ScreenLayout.Placement)layout.moved.get(key);
            boolean hide = layout.hidden.contains(key);
            if (p != null || hide) {
               orig.putIfAbsent(w, new int[]{w.getX(), w.getY(), w.getWidth(), w.getHeight(), w.visible ? 1 : 0});
               if (hide) {
                  w.visible = false;
               } else {
                  int w2 = p.w > 0 ? p.w : w.getWidth();
                  int h2 = p.h > 0 ? p.h : w.getHeight();
                  Anchor a = Anchor.parse(p.anchor, Anchor.TOP_LEFT);
                  w.setX(a.x(screen.width, p.x, w2));
                  w.setY(a.y(screen.height, p.y, h2));
                  w.setWidth(w2);
                  w.setHeight(h2);
               }
            }
         });
      }
   }

   private static void applyElements(Screen screen, ScreenLayout layout) {
      if (!layout.elements.isEmpty() && screen instanceof ScreenAccess acc) {
         ArrayList added = new ArrayList();

         for(ScreenLayout.Element e : layout.elements) {
            if (e.visible) {
               ElementType type = (ElementType)ElementTypes.get(e.type).orElse(null);
               if (type == null) {
                  CFHGui.LOGGER.warn("[CFHGui] unknown element type {} in layout for {}", e.type, layoutId(screen));
               } else {
                  int[] def = type.defaultSize();
                  int w = e.place.w > 0 ? e.place.w : def[0];
                  int h = e.place.h > 0 ? e.place.h : def[1];
                  Anchor a = Anchor.parse(e.place.anchor, Anchor.TOP_LEFT);
                  int x = a.x(screen.width, e.place.x, w);
                  int y = a.y(screen.height, e.place.y, h);
                  Runnable run = () -> {
                     for(ScreenLayout.Action act : e.actions) {
                        Actions.run(act.type, act.args);
                     }

                  };
                  AbstractWidget widget = type.create(screen, e, x, y, w, h, run);
                  if (widget != null) {
                     acc.cfhgui$add(widget);
                     added.add(widget);
                  }
               }
            }
         }

         if (!added.isEmpty()) {
            ADDED.put(screen, added);
         }

      }
   }

   public static List<AbstractWidget> addedTo(Screen screen) {
      List<AbstractWidget> l = (List)ADDED.get(screen);
      return l == null ? List.of() : l;
   }

   private LayoutApplier() {
   }

   public interface ScreenAccess {
      <T extends GuiEventListener & Renderable & NarratableEntry> T cfhgui$add(T var1);

      void cfhgui$remove(GuiEventListener var1);

      void cfhgui$rebuild();
   }
}
