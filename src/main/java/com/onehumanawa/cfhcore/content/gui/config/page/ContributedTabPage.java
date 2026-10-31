package com.onehumanawa.cfhcore.content.gui.config.page;

import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfig;
import com.onehumanawa.cfhcore.content.gui.config.api.SettingsTabs;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionPageBase;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import java.util.List;

public final class ContributedTabPage extends OptionPageBase {
   private final SettingsTabs.Tab tab;

   public ContributedTabPage(SettingsTabs.Tab tab) {
      super(tab.id(), tab.title(), tab.icon());
      this.tab = tab;
      this.level(OptionPageBase.Level.ALL);
   }

   public SettingsTabs.Tab tab() {
      return this.tab;
   }

   protected List<Section> sections() {
      try {
         List<Section> s = (List)this.tab.sections().get();
         return s == null ? List.of() : s;
      } catch (Exception e) {
         CFHGuiConfig.LOGGER.warn("[CFHGui Config] settings tab {} failed to build: {}", this.tab.id(), e.toString());
         return List.of();
      }
   }
}
