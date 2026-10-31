package com.onehumanawa.cfhcore.content.gui.core.layout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;

public final class WidgetKey {
   public static String of(AbstractWidget widget, List<? extends GuiEventListener> siblings) {
      String base = base(widget);
      int index = 0;

      for(GuiEventListener s : siblings) {
         if (s == widget) {
            break;
         }

         if (s instanceof AbstractWidget w) {
            if (base(w).equals(base)) {
               ++index;
            }
         }
      }

      return base + "#" + index;
   }

   public static Map<AbstractWidget, String> all(List<? extends GuiEventListener> widgets) {
      Map<String, Integer> counts = new HashMap();
      Map<AbstractWidget, String> out = new HashMap();

      for(GuiEventListener l : widgets) {
         if (l instanceof AbstractWidget w) {
            String base = base(w);
            int i = (Integer)counts.merge(base, 1, Integer::sum) - 1;
            out.put(w, base + "#" + i);
         }
      }

      return out;
   }

   public static String first(Component label) {
      return base(label, (Class)null) + "#0";
   }

   public static Component name(String key) {
      int hash = key.lastIndexOf(35);
      String base = hash > 0 ? key.substring(0, hash) : key;
      return I18n.exists(base) ? Component.translatable(base) : Component.literal(base.startsWith("#") ? base.substring(1) : base);
   }

   private static String base(AbstractWidget w) {
      return base(w.getMessage(), w.getClass());
   }

   private static String base(@Nullable Component msg, @Nullable Class<?> type) {
      String text = "";
      if (msg != null) {
         ComponentContents var4 = msg.getContents();
         if (var4 instanceof TranslatableContents) {
            TranslatableContents tc = (TranslatableContents)var4;
            text = tc.getKey();
         } else {
            text = msg.getString();
         }
      }

      text = text.replace('#', '_').trim();
      if (text.isEmpty()) {
         String var10000 = type == null ? "Widget" : type.getSimpleName();
         return "#" + var10000;
      } else {
         if (text.length() > 48) {
            text = text.substring(0, 48);
         }

         return text;
      }
   }

   private WidgetKey() {
   }
}
