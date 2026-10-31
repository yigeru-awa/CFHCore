package com.onehumanawa.cfhcore.content.gui.config.page;

import com.onehumanawa.cfhcore.content.gui.config.curated.CuratedPages;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionBinding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionResolvers;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionPageBase;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;

public final class CuratedPage extends OptionPageBase {
   private final CuratedPages.PageDef def;

   public CuratedPage(CuratedPages.PageDef def) {
      super("curated:" + def.id(), Component.literal(def.title()), def.icon());
      this.def = def;
      this.level(OptionPageBase.Level.ALL);
   }

   protected boolean showSingleHeader() {
      return true;
   }

   protected List<Section> sections() {
      List<Section> out = new ArrayList();
      int i = 0;

      for(CuratedPages.SectionDef sd : this.def.sections()) {
         int var10002 = i++;
         Section s = new Section("s" + var10002, Component.literal(sd.title()), sd.description() == null ? null : Component.literal(sd.description()));

         for(CuratedPages.OptionDef od : sd.options()) {
            Optional<OptionBinding> var10000 = OptionResolvers.resolve(od.path());
            Objects.requireNonNull(od);
            OptionBinding b = (OptionBinding)var10000.map(od::apply).orElseGet(() -> Binding.of(od.path(), OptionType.INFO, (Component)Component.literal(od.label() != null ? od.label() : od.path())).tooltip((Component)Component.translatable("cfhgui_config.curated.unavailable.tip", new Object[]{od.path()})).getter(() -> Component.translatable("cfhgui_config.curated.unavailable").getString()));
            s.add(b);
         }

         out.add(s);
      }

      return out;
   }
}
