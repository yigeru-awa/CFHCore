package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfigApi;
import com.onehumanawa.cfhcore.content.gui.config.api.SettingsTabs;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.Choice;
import com.onehumanawa.cfhcore.content.gui.config.option.KeyBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiLabel.Style;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class InfoHudSettingsTab {
   static final String TAB_ID = "info_overlay";

   private InfoHudSettingsTab() {
   }

   static void register() {
      SettingsTabs.register("interface", new SettingsTabs.Tab("info_overlay", Component.translatable("cfhgui_infohud.settings.tab"), Icon.MONITOR, InfoHudSettingsTab::sections, 140));
      CFHGuiConfigApi.registerReloadHook("infohud", InfoHudConfig::reload);
   }

   static Screen screen(@Nullable Screen parent) {
      return CFHGuiConfigApi.hub(parent, "interface/info_overlay");
   }

   private static List<Section> sections() {
      List<Section> out = new ArrayList();

      for(InfoHudSettings.Group group : InfoHudSettings.groups()) {
         Section section = new Section(group.id(), group.title(), group.about());

         for(InfoHudSettings.Setting setting : group.settings()) {
            if (setting.type == InfoHudSettings.Type.NOTE) {
               section.custom((width) -> (new CFHGuiLabel(0, 0, width, setting.label())).style(Style.MUTED).wrap(true));
            } else {
               section.add(binding(setting));
            }
         }

         if (group.id().equals("overlay")) {
            section.add(key(InfoHud.TOGGLE_KEY));
            section.add(key(InfoHud.MODE_KEY));
         }

         out.add(section);
      }

      return out;
   }

   private static KeyBinding key(KeyMapping mapping) {
      KeyBinding binding = new KeyBinding(mapping);
      binding.tooltip(Component.translatable(mapping.getName() + ".tip"));
      return binding;
   }

   private static Binding binding(InfoHudSettings.Setting s) {
      OptionType var10000;
      switch (s.type) {
         case BOOL -> var10000 = OptionType.BOOLEAN;
         case INT -> var10000 = OptionType.INT;
         case DOUBLE -> var10000 = OptionType.DOUBLE;
         case CHOICE -> var10000 = OptionType.CHOICE;
         case NOTE -> throw new IllegalArgumentException(s.id);
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      OptionType type = var10000;
      Binding var6 = Binding.of("cfhgui_infohud:" + s.id, type, s.label());
      Objects.requireNonNull(s);
      var6 = var6.getter(s::get);
      Objects.requireNonNull(s);
      Binding b = var6.setter(s::set).def(s.def()).enabledIf(s.enabledIf()).searchWords("info overlay hud f3 debug coordinates fps ping latency ram vram memory " + s.id);
      b.tooltip(s.tooltip());
      if (s.type == InfoHudSettings.Type.INT || s.type == InfoHudSettings.Type.DOUBLE) {
         b.range(s.min, s.max, s.step);
      }

      if (s.type == InfoHudSettings.Type.CHOICE) {
         List<Choice> choices = new ArrayList();

         for(String id : s.choices) {
            choices.add(new Choice(id, s.choiceLabel(id)));
         }

         b.choices(choices);
      }

      if (s.format != null) {
         b.format(s.format);
      }

      return b;
   }
}
