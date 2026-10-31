package com.onehumanawa.cfhcore.content.gui.menu.mixin;

import com.onehumanawa.cfhcore.content.gui.menu.client.infohud.LatencyProbe;
import net.minecraft.client.multiplayer.PingDebugMonitor;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PingDebugMonitor.class})
public abstract class PingDebugMonitorMixin {
   @Inject(
      method = {"onPongReceived"},
      at = {@At("HEAD")},
      require = 0
   )
   private void cfhgui$pong(ClientboundPongResponsePacket packet, CallbackInfo ci) {
      LatencyProbe.pong(packet.time());
   }
}
