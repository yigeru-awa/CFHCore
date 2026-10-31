package com.onehumanawa.cfhcore.content.gui.menu.client.lookat;

import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfigApi;
import com.onehumanawa.cfhcore.content.gui.config.api.SettingsTabs;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionValues;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

final class LookAtSettingsTab {
   static final String TAB_ID = "look_at";

   private LookAtSettingsTab() {
   }

   static void register() {
      SettingsTabs.register("interface", new SettingsTabs.Tab("look_at", Component.translatable("cfhgui_lookat.settings.tab"), Icon.EYE, LookAtSettingsTab::sections, 145));
      CFHGuiConfigApi.registerReloadHook("lookat", LookAtConfig::reload);
   }

   static Screen screen(@Nullable Screen parent) {
      return CFHGuiConfigApi.hub(parent, "interface/look_at");
   }

   private static List<Section> sections() {
      List<Section> out = new ArrayList();

      for(LookAtSettings.Group group : LookAtSettings.groups()) {
         Section section = new Section(group.id(), group.title(), group.about());

         for(LookAtSettings.Setting setting : group.settings()) {
            section.add(binding(setting));
         }

         out.add(section);
      }

      return out;
   }

   private static Binding binding(LookAtSettings.Setting s) {
      Binding var10000 = Binding.of("cfhgui_lookat:" + s.id, OptionType.BOOLEAN, s.label()).tooltip(s.tooltip());
      Objects.requireNonNull(s);
      return var10000.getter(s::get).setter((v) -> s.set(OptionValues.asBoolean(v, s.def()))).def(s.def()).enabledIf(s.enabledIf()).searchWords("look at plate tooltip mob entity target health armour armor name owner jade " + s.id);
   }
}
