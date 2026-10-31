package com.onehumanawa.cfhcore.content.gui.core.layout.editor;

import com.onehumanawa.cfhcore.content.gui.core.client.DevModeButton;
import com.onehumanawa.cfhcore.content.gui.core.layout.ElementType;
import com.onehumanawa.cfhcore.content.gui.core.layout.ElementTypes;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutApplier;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutAware;
import com.onehumanawa.cfhcore.content.gui.core.layout.ScreenLayout;
import com.onehumanawa.cfhcore.content.gui.core.layout.WidgetKey;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Anchor;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

final class EditorItems {
   static List<EditorItem> bind(Screen screen, ScreenLayout layout) {
      List<EditorItem> out = new ArrayList();
      List<AbstractWidget> added = LayoutApplier.addedTo(screen);
      Set<AbstractWidget> addedSet = Collections.newSetFromMap(new IdentityHashMap());
      addedSet.addAll(added);
      Set<String> seen = new HashSet();
      if (screen instanceof LayoutAware aware) {
         List<LayoutAware.Part> parts;
         try {
            parts = aware.layoutParts();
         } catch (RuntimeException var15) {
            parts = List.of();
         }

         for(LayoutAware.Part p : parts) {
            if (p != null && seen.add(p.key())) {
               out.add(EditorItem.part(p, !layout.hides(p.key())));
            }
         }
      }

      Map<AbstractWidget, String> keys = WidgetKey.all(screen.children());

      for(GuiEventListener l : screen.children()) {
         if (l instanceof AbstractWidget w) {
            if (!addedSet.contains(w) && !(w instanceof DevModeButton.Pencil)) {
               String key = (String)keys.get(w);
               if (key != null && seen.add(key)) {
                  out.add(EditorItem.vanilla(key, w, backdrop(w, screen)));
               }
            }
         }
      }

      for(String key : layout.hidden) {
         if (key != null && seen.add(key)) {
            out.add(EditorItem.ghost(key));
         }
      }

      List<ScreenLayout.Element> els = layout.elements;
      int j = 0;

      for(int i = 0; i < els.size(); ++i) {
         ScreenLayout.Element e = (ScreenLayout.Element)els.get(i);
         ElementType type = (ElementType)ElementTypes.get(e.type).orElse(null);
         Rect expected = expectedRect(e, type, screen.width, screen.height);
         AbstractWidget w = null;
         if (e.visible && type != null && j < added.size()) {
            AbstractWidget cand = (AbstractWidget)added.get(j);
            if (at(cand, expected)) {
               w = cand;
               ++j;
            } else if (!fitsLater(cand, els, i + 1, screen)) {
               w = cand;
               ++j;
            }
         }

         Rect rect = w != null ? Rect.of(w.getX(), w.getY(), w.getWidth(), w.getHeight()) : expected;
         out.add(EditorItem.custom(e, type, w, rect));
      }

      return out;
   }

   private static boolean backdrop(AbstractWidget w, Screen screen) {
      return (double)w.getWidth() >= (double)screen.width * 0.9 && (double)w.getHeight() >= (double)screen.height * 0.9;
   }

   private static boolean at(AbstractWidget w, Rect r) {
      return w.getX() == r.x() && w.getY() == r.y();
   }

   private static boolean fitsLater(AbstractWidget cand, List<ScreenLayout.Element> els, int from, Screen screen) {
      for(int k = from; k < els.size(); ++k) {
         ScreenLayout.Element e2 = (ScreenLayout.Element)els.get(k);
         if (e2.visible) {
            ElementType t2 = (ElementType)ElementTypes.get(e2.type).orElse(null);
            if (t2 != null && at(cand, expectedRect(e2, t2, screen.width, screen.height))) {
               return true;
            }
         }
      }

      return false;
   }

   static Rect expectedRect(ScreenLayout.Element e, @Nullable ElementType type, int sw, int sh) {
      int[] def = type == null ? new int[]{100, 20} : type.defaultSize();
      ScreenLayout.Placement p = e.place;
      int w = p.w > 0 ? p.w : def[0];
      int h = p.h > 0 ? p.h : def[1];
      Anchor a = Anchor.parse(p.anchor, Anchor.TOP_LEFT);
      return Rect.of(a.x(sw, p.x, w), a.y(sh, p.y, h), w, h);
   }

   private EditorItems() {
   }
}
