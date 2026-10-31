package com.onehumanawa.cfhcore.content.gui.config.page;

import com.onehumanawa.cfhcore.content.gui.config.ui.OptionPageBase;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

public final class CompositePage extends OptionPageBase {
   private final List<OptionPageBase> parts;

   public CompositePage(String id, Component title, Icon icon, List<OptionPageBase> parts) {
      super(id, title, icon);
      this.parts = List.copyOf(parts);
   }

   public OptionPageBase level(OptionPageBase.Level l) {
      for(OptionPageBase p : this.parts) {
         p.level(l);
      }

      return super.level(OptionPageBase.Level.ALL);
   }

   protected List<Section> sections() {
      List<Section> out = new ArrayList();

      for(OptionPageBase p : this.parts) {
         for(Section s : p.sectionsNow()) {
            out.add(s.tab(p.id(), p.title()));
         }
      }

      return out;
   }
}
