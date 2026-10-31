package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

public class CFHGuiSearchField extends CFHGuiTextField {
   public CFHGuiSearchField(int x, int y, int width, Consumer<String> onChange) {
      super(x, y, width, 20, Component.translatable("cfhgui.search"));
      this.icon(Icon.SEARCH);
      this.clearButton(true);
      this.placeholder(Component.translatable("cfhgui.search.placeholder"));
      this.onChange(onChange);
      this.onEscape(() -> {
         if (!this.getValue().isEmpty()) {
            this.setValue("");
         } else {
            this.setFocused(false);
         }

      });
   }
}
