package com.onehumanawa.cfhcore.content.gui.core.layout.editor;

import com.onehumanawa.cfhcore.content.gui.core.client.CoreActions;
import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutStore;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

final class EditorForms {
   static final int FIELD_H = 20;
   static final int MULTILINE_H = 56;
   static final int ACTION_LIST_H = 44;

   static int height(ActionType.Arg.Kind kind) {
      byte var10000;
      switch (kind) {
         case MULTILINE -> var10000 = 56;
         case ACTION_LIST -> var10000 = 44;
         default -> var10000 = 20;
      }

      return var10000;
   }

   static boolean captioned(ActionType.Arg.Kind kind) {
      return kind != ActionType.Arg.Kind.BOOL;
   }

   static AbstractWidget field(int x, int y, int w, ActionType.Arg arg, String value, Consumer<String> onChange) {
      String v = value == null ? "" : value;
      Object var10000;
      switch (arg.kind()) {
         case MULTILINE:
            var10000 = (new MultiLineField(x, y, w, 56, arg.label())).text(v).onChange(onChange);
            break;
         case ACTION_LIST:
            var10000 = (new MultiLineField(x, y, w, 44, arg.label())).text(v).placeholder(EditorText.t("props.action_list_hint")).onChange(onChange);
            break;
         case BOOL:
            var10000 = new CFHGuiToggle(x, y, w, arg.label(), Boolean.parseBoolean(v), (b) -> onChange.accept(Boolean.toString(b)));
            break;
         case NUMBER:
            var10000 = (new EditorTextField(x, y, w, arg.label())).decimal().text(v).onChange(onChange);
            break;
         case CHOICE:
            List<String> opts = new ArrayList(arg.choices());
            if (!v.isEmpty() && !opts.contains(v)) {
               opts.add(0, v);
            }

            String cur = opts.contains(v) ? v : (opts.isEmpty() ? v : (String)opts.get(0));
            var10000 = new CFHGuiDropdown<String>(x, y, w, opts, cur, (s) -> Component.literal(s), onChange);
            break;
         case SCREEN_ID:
            var10000 = new CFHGuiDropdown<String>(x, y, w, screenIds(v), v, EditorForms::screenLabel, onChange);
            break;
         default:
            var10000 = (new EditorTextField(x, y, w, arg.label())).text(v).onChange(onChange);
      }

      return (AbstractWidget)var10000;
   }

   static List<String> customScreens() {
      List<String> out = new ArrayList();

      try {
         for(String id : LayoutStore.saved()) {
            if (id.startsWith("custom:")) {
               out.add(id.substring(7));
            }
         }
      } catch (Exception var3) {
      }

      out.sort((Comparator)null);
      return out;
   }

   static List<String> screenIds(String include) {
      Set<String> ids = new LinkedHashSet();
      if (include != null && !include.isBlank()) {
         ids.add(include);
      }

      try {
         synchronized(CoreActions.SCREEN_FACTORIES) {
            ids.addAll(CoreActions.SCREEN_FACTORIES.keySet());
         }
      } catch (Exception var6) {
      }

      for(String c : customScreens()) {
         ids.add("custom:" + c);
      }

      try {
         ids.addAll(ScreenIds.known());
      } catch (Exception var4) {
      }

      ids.remove("cfhgui:custom");
      return new ArrayList(ids);
   }

   static Component screenLabel(String id) {
      if (id != null && !id.isEmpty()) {
         if (id.startsWith("custom:")) {
            return EditorText.t("props.custom_screen", id.substring(7));
         } else {
            String display = ScreenIds.display(id);
            String path = id.substring(id.indexOf(58) + 1);
            return Component.literal(display.equals(path) ? id : display + "  (" + id + ")");
         }
      } else {
         return Component.literal("-");
      }
   }

   private EditorForms() {
   }
}
