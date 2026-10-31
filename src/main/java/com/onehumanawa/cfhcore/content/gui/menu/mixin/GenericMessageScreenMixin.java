package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import com.onehumanawa.cfhcore.content.gui.menu.client.loading.LoadingScreens;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({GenericMessageScreen.class})
public abstract class GenericMessageScreenMixin extends Screen {
   protected GenericMessageScreenMixin(Component title) {
      super(title);
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      if (!LoadingScreens.active()) {
         super.render(g, mouseX, mouseY, partialTick);
      } else {
         LoadingScreens.render(this, g, mouseX, mouseY, partialTick, this.getTitle(), (Component)null, -1.0F, (StoringChunkProgressListener)null, List.of());
      }
   }
}
