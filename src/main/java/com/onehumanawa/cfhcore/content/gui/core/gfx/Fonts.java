package com.onehumanawa.cfhcore.content.gui.core.gfx;

import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class Fonts {
   public static MutableComponent heading(Component text) {
      return text.copy().withStyle(Theme.current().headingStyle());
   }

   public static MutableComponent heading(String text) {
      return heading((Component)Component.literal(text));
   }

   private Fonts() {
   }
}
