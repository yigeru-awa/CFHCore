package com.onehumanawa.cfhcore.content.gui.config.page;

import com.onehumanawa.cfhcore.content.gui.config.ConfigSettings;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionResolvers;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionPageBase;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionRow;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

public final class FavouritesPage extends OptionPageBase {
   public FavouritesPage() {
      super("favourites", Component.translatable("cfhgui_config.page.favourites"), Icon.STAR);
      this.emptyText(Component.translatable("cfhgui_config.favourites.empty"));
      this.level(OptionPageBase.Level.ALL);
   }

   public static List<OptionBinding> resolved() {
      List<OptionBinding> out = new ArrayList();

      for(String id : new ArrayList<String>(ConfigSettings.get().favourites)) {
         out.add((OptionBinding)OptionResolvers.resolve(id).orElseGet(() -> Binding.of(id, OptionType.INFO, (Component)Component.literal(id)).getter(() -> Component.translatable("cfhgui_config.favourites.unavailable").getString())));
      }

      return out;
   }

   protected List<Section> sections() {
      return List.of(Section.of("pinned", Component.translatable("cfhgui_config.page.favourites"), resolved()).fixed());
   }

   protected void onRowChanged(OptionRow row) {
      super.onRowChanged(row);
      if (!ConfigSettings.isFavourite(row.binding().id())) {
         this.rebuild();
      }

   }
}
