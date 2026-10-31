package com.onehumanawa.cfhcore.content.gui.core.screen;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.layout.ui.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public abstract class SidebarPage {
   private final String id;
   private final Component title;
   private final Icon icon;

   protected SidebarPage(String id, Component title, Icon icon) {
      this.id = id;
      this.title = title;
      this.icon = icon;
   }

   public String id() {
      return this.id;
   }

   public Component title() {
      return this.title;
   }

   public Icon icon() {
      return this.icon;
   }

   public int badge() {
      return 0;
   }

   public abstract void build(SidebarScreen var1, Rect var2);

   public void render(SidebarScreen screen, GuiGraphics g, Rect area, int mouseX, int mouseY, float partialTick) {
   }

   public void tick() {
   }

   public void onHide() {
   }
}
