package com.onehumanawa.cfhcore.content.gui.config.page;

import com.onehumanawa.cfhcore.content.gui.config.resolver.VanillaOptions;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionPageBase;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class EssentialsPage extends OptionPageBase {
   public EssentialsPage() {
      this("essentials", Component.translatable("cfhgui_config.page.essentials"), Icon.SPARKLE);
   }

   public EssentialsPage(String id, Component title, Icon icon) {
      super(id, title, icon);
      this.level(OptionPageBase.Level.ALL);
      this.indexed(false);
   }

   protected boolean pills(String tabKey) {
      return false;
   }

   protected List<Section> sections() {
      Section s = (new Section("essentials", Component.translatable("cfhgui_config.page.essentials"), Component.translatable("cfhgui_config.essentials.desc"))).fixed().basic();
      if (Minecraft.getInstance().level != null) {
         s.add(GameplayGeneralPage.difficultyBinding(this::rebuild));
      }

      s.addAll(VanillaOptions.all("fov", "renderDistance", "guiScale", "gamma", "fullscreen", "maxFps", "soundCategory_master"));
      return List.of(s);
   }
}
