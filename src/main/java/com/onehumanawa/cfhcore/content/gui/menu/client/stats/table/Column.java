package com.onehumanawa.cfhcore.content.gui.menu.client.stats.table;

import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.StatKind;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record Column(Component title, @Nullable Component tip, StatKind kind, boolean bar, int width, @Nullable ResourceLocation sprite) {
   public static Column count(Component title, @Nullable Component tip, int width) {
      return new Column(title, tip, StatKind.COUNT, false, width, (ResourceLocation)null);
   }

   public static Column count(Component title, @Nullable Component tip, int width, String sprite) {
      return new Column(title, tip, StatKind.COUNT, false, width, ResourceLocation.withDefaultNamespace(sprite));
   }
}
