package com.onehumanawa.cfhcore.content.gui.core.module;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public record KnownModule(String id, Component name, Icon icon, Component pitch, @Nullable String url) {
   public boolean installed() {
      return Modules.isLoaded(this.id);
   }
}
