package com.onehumanawa.cfhcore.content.gui.core.client;

import com.onehumanawa.cfhcore.content.gui.core.layout.LayoutIdProvider;
import com.onehumanawa.cfhcore.content.gui.core.screen.ScreenIds;
import com.onehumanawa.cfhcore.content.gui.core.screen.CFHGuiScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class CustomScreen extends CFHGuiScreen implements LayoutIdProvider {
   private final String id;

   public CustomScreen(String id, @Nullable Screen parent) {
      super(Component.literal(id), parent);
      this.id = id;
      this.showHeader = false;
   }

   public String id() {
      return this.id;
   }

   protected void build() {
   }

   public String layoutId() {
      return "custom:" + this.id;
   }

   static {
      ScreenIds.register(CustomScreen.class, "cfhgui:custom", "Custom screen");
   }
}
