package com.onehumanawa.cfhcore.content.gui.core.layout;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public interface ElementType {
   String id();

   Component label();

   Icon icon();

   default int[] defaultSize() {
      return new int[]{100, 20};
   }

   default List<ActionType.Arg> props() {
      return List.of();
   }

   default boolean clickable() {
      return true;
   }

   AbstractWidget create(Screen var1, ScreenLayout.Element var2, int var3, int var4, int var5, int var6, Runnable var7);
}
