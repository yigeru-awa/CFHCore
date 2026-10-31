package com.onehumanawa.cfhcore.content.gui.core.module;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType;
import java.util.List;
import net.minecraft.network.chat.Component;

public interface CFHGuiModule {
   String id();

   Component displayName();

   Icon icon();

   default void init() {
   }

   default void initClient() {
   }

   default List<HubEntry> hubEntries() {
      return List.of();
   }

   default List<ActionType> actions() {
      return List.of();
   }

   public static record HubEntry(Component label, Icon icon, Runnable onClick) {
   }
}
