package com.onehumanawa.cfhcore.content.gui.core.layout;

import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public interface LayoutAware {
   default List<Part> layoutParts() {
      return List.of();
   }

   public static record Part(String key, Component name, @Nullable Rect rect) {
   }
}
