package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

import com.onehumanawa.cfhcore.content.gui.core.client.settings.SettingRow;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiDropdown;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiKeybindButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSeparator;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSlider;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToggle;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public final class InfoHudOptions {
   private InfoHudOptions() {
   }

   public static List<AbstractWidget> rows(int width, Runnable rebuild) {
      List<AbstractWidget> out = new ArrayList();

      for(InfoHudSettings.Group group : InfoHudSettings.groups()) {
         out.add(new CFHGuiSeparator(0, 0, width, group.pageTitle()));

         for(InfoHudSettings.Setting s : group.settings()) {
            out.add(row(width, s, rebuild));
         }

         if (group.id().equals("overlay")) {
            out.add(key(width, InfoHud.TOGGLE_KEY));
            out.add(key(width, InfoHud.MODE_KEY));
         }
      }

      return out;
   }

   private static AbstractWidget row(int width, InfoHudSettings.Setting s, Runnable rebuild) {
      switch (s.type) {
         case BOOL:
            CFHGuiToggle toggle = new CFHGuiToggle(0, 0, width, s.label(), Boolean.TRUE.equals(s.get()), (v) -> {
               s.set(v);
               if (s.structural) {
                  rebuild.run();
               }

            });
            toggle.tip(s.tooltip());
            toggle.enabled(s.enabled());
            return toggle;
         case CHOICE:
            List<String> var10005 = s.choices;
            String var10006 = String.valueOf(s.get());
            Objects.requireNonNull(s);
            CFHGuiDropdown<String> dropdown = new CFHGuiDropdown<String>(0, 0, width, var10005, var10006, s::choiceLabel, (v) -> {
               s.set(v);
               if (s.structural) {
                  rebuild.run();
               }

            });
            dropdown.label(s.label());
            dropdown.tip(s.tooltip());
            dropdown.enabled(s.enabled());
            return dropdown;
         case INT:
         case DOUBLE:
            boolean whole = s.type == InfoHudSettings.Type.INT;
            CFHGuiSlider slider = new CFHGuiSlider(0, 0, width, s.label(), s.min, s.max, s.step, ((Number)s.get()).doubleValue(), (v) -> s.format != null ? ((Component)s.format.apply(whole ? (int)Math.round(v) : v)).getString() : Double.toString(v), (v) -> s.apply(whole ? (int)Math.round(v) : v));
            slider.onCommit((v) -> s.commit());
            slider.compact(true);
            slider.tip(s.tooltip());
            slider.enabled(s.enabled());
            return slider;
         default:
            return (new CFHGuiLabel(0, 0, width, s.label())).style(Style.MUTED).wrap(true);
      }
   }

   private static AbstractWidget key(int width, KeyMapping mapping) {
      CFHGuiKeybindButton button = new CFHGuiKeybindButton(0, 0, Math.min(120, SettingRow.controlWidth(width)), mapping, (changed) -> {
         KeyMapping.resetMapping();
         Minecraft.getInstance().options.save();
      });
      Component tip = Component.translatable(mapping.getName() + ".tip");
      button.tip(tip);
      return SettingRow.of(width, Component.translatable(mapping.getName()), tip, button);
   }
}
