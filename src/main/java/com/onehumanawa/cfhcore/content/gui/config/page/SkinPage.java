package com.onehumanawa.cfhcore.content.gui.config.page;

import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionValues;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionPageBase;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelPart;

public final class SkinPage extends OptionPageBase {
   public SkinPage() {
      super("skin", Component.translatable("cfhgui_config.page.skin"), Icon.USER);
   }

   protected List<Section> sections() {
      Section s = Section.of("layers", Component.translatable("cfhgui_config.skin.layers")).fixed();

      for(PlayerModelPart part : PlayerModelPart.values()) {
         s.add(Binding.of("skin:" + part.getId(), OptionType.BOOLEAN, part.getName()).getter(() -> Minecraft.getInstance().options.isModelPartEnabled(part)).setter((v) -> {
            Minecraft.getInstance().options.toggleModelPart(part, OptionValues.asBoolean(v, true));
            Minecraft.getInstance().options.save();
         }).def(Boolean.TRUE).searchWords("skin layer model part " + part.getId()));
      }

      return List.of(s);
   }
}
