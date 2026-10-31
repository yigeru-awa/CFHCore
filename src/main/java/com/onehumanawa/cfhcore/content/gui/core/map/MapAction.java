package com.onehumanawa.cfhcore.content.gui.core.map;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public record MapAction(String id, Component label, @Nullable Icon icon, Runnable run, boolean danger, boolean enabled, @Nullable Component tip) {
   public static MapAction of(String id, Component label, @Nullable Icon icon, Runnable run) {
      return new MapAction(id, label, icon, run, false, true, (Component)null);
   }

   public static MapAction danger(String id, Component label, @Nullable Icon icon, Runnable run) {
      return new MapAction(id, label, icon, run, true, true, (Component)null);
   }

   public MapAction disabled(Component why) {
      return new MapAction(this.id, this.label, this.icon, this.run, this.danger, false, why);
   }
}
