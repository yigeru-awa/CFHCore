package com.onehumanawa.cfhcore.content.gui.core.screen.slot;

import java.util.List;
import net.minecraft.network.chat.Component;

public record MenuSlot(String id, Component name, String owner, boolean vanillaMenu, List<String> vanillaClasses) {
   public MenuSlot(String id, Component name, String owner, boolean vanillaMenu, List<String> vanillaClasses) {
      vanillaClasses = List.copyOf(vanillaClasses);
      this.id = id;
      this.name = name;
      this.owner = owner;
      this.vanillaMenu = vanillaMenu;
      this.vanillaClasses = vanillaClasses;
   }

   public static MenuSlot vanilla(String id, Component name, String owner, String... vanillaClasses) {
      return new MenuSlot(id, name, owner, true, List.of(vanillaClasses));
   }

   public static MenuSlot custom(String id, Component name, String owner) {
      return new MenuSlot(id, name, owner, false, List.of());
   }
}
