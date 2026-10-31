package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import com.onehumanawa.cfhcore.content.gui.menu.client.tablist.CFHGuiTabList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerTabOverlay.class})
public abstract class PlayerTabOverlayMixin {
   @Shadow
   private @Nullable Component header;
   @Shadow
   private @Nullable Component footer;
   @Shadow
   private boolean visible;

   @Shadow
   protected abstract List<PlayerInfo> getPlayerInfos();

   @Inject(
      method = {"setVisible"},
      at = {@At("HEAD")},
      require = 0
   )
   private void cfhgui$shown(boolean visible, CallbackInfo ci) {
      if (visible && !this.visible) {
         CFHGuiTabList.opened();
      }

   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void cfhgui$render(GuiGraphics g, int width, Scoreboard scoreboard, @Nullable Objective objective, CallbackInfo ci) {
      if (CFHGuiTabList.active()) {
         if (CFHGuiTabList.render(g, (PlayerTabOverlay)(Object)this, this.getPlayerInfos(), this.header, this.footer, scoreboard, objective)) {
            ci.cancel();
         }

      }
   }
}
