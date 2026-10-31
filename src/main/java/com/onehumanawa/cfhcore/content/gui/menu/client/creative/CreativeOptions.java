package com.onehumanawa.cfhcore.content.gui.menu.client.creative;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSeparator;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public final class CreativeOptions {
   private CreativeOptions() {
   }

   public static List<AbstractWidget> rows(int width) {
      CFHGuiButton reset = (new CFHGuiButton(0, 0, width, Component.translatable("cfhgui.creative.order.reset"), (Runnable)null)).icon(Icon.REFRESH);
      reset.onPress(() -> {
         TabOrder.reset();
         reset.enabled(false);
      });
      reset.tip(Component.translatable("cfhgui.creative.order.reset.tip"));
      reset.enabled(TabOrder.custom());
      return List.of(new CFHGuiSeparator(0, 0, width, Component.translatable("cfhgui.creative.order.section")), reset);
   }
}
