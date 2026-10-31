package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import com.onehumanawa.cfhcore.content.gui.menu.client.loading.LoadingScreens;
import com.onehumanawa.cfhcore.content.gui.menu.client.loading.journey.Journey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ConnectScreen.class})
public abstract class ConnectScreenMixin extends Screen {
   @Shadow
   private Component status;
   @Shadow
   private long lastNarration;
   @Unique
   private @Nullable Component cfhgui$server;

   protected ConnectScreenMixin(Component title) {
      super(title);
   }

   @Inject(
      method = {"connect"},
      at = {@At("HEAD")}
   )
   private void cfhgui$remember(Minecraft mc, ServerAddress address, @Nullable ServerData data, @Nullable TransferState transfer, CallbackInfo ci) {
      this.cfhgui$server = Component.literal(data != null && data.name != null && !data.name.isBlank() ? data.name : address.getHost());
      Journey.joining(data, address.getHost());
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cfhgui$render(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (LoadingScreens.active()) {
         ci.cancel();
         long now = Util.getMillis();
         if (now - this.lastNarration > 2000L) {
            this.lastNarration = now;
            this.minecraft.getNarrator().sayNow(Component.translatable("narrator.joining"));
         }

         List<AbstractWidget> buttons = new ArrayList();

         for(GuiEventListener c : this.children()) {
            if (c instanceof AbstractWidget) {
               AbstractWidget w = (AbstractWidget)c;
               buttons.add(w);
            }
         }

         Component title = this.cfhgui$server != null ? Component.translatable("cfhgui.loading.joining", new Object[]{this.cfhgui$server}) : Component.translatable("cfhgui.loading.joining_server");
         LoadingScreens.render(this, g, mouseX, mouseY, partialTick, title, this.status, -1.0F, (StoringChunkProgressListener)null, buttons);
      }
   }
}
