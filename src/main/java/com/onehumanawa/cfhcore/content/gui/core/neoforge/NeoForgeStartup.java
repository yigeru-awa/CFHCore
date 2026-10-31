package com.onehumanawa.cfhcore.content.gui.core.neoforge;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.client.StartupTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.ImmediateWindowHandler;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

final class NeoForgeStartup {
   private static final PreparableReloadListener SLOW_RELOAD = (barrier, manager, prepare, apply, background, game) -> {
      CompletableFuture var10000 = CompletableFuture.runAsync(() -> StartupTime.slow("reload"), background);
      Objects.requireNonNull(barrier);
      return var10000.thenCompose(barrier::wait);
   };

   static void init(IEventBus modBus) {
      StartupTime.slow("construct");
      modBus.addListener(EventPriority.HIGHEST, false, RegisterEvent.class, (e) -> {
         tick();
         StartupTime.slow("registry", 150L);
         tick();
      });
      modBus.addListener(EventPriority.HIGHEST, false, RegisterColorHandlersEvent.Block.class, (e) -> {
         tick();
         StartupTime.slow("colors");
         tick();
      });
      modBus.addListener(EventPriority.HIGHEST, false, RegisterColorHandlersEvent.Item.class, (e) -> tick());
      modBus.addListener(EventPriority.HIGHEST, false, RegisterParticleProvidersEvent.class, (e) -> tick());
      modBus.addListener(EventPriority.HIGHEST, false, RegisterClientReloadListenersEvent.class, (e) -> {
         tick();
         if (System.getProperty("cfhgui.startupSlow") != null) {
            e.registerReloadListener(SLOW_RELOAD);
         }

      });
      modBus.addListener(EventPriority.HIGHEST, false, EntityRenderersEvent.RegisterLayerDefinitions.class, (e) -> tick());
      modBus.addListener(EventPriority.HIGHEST, false, EntityRenderersEvent.RegisterRenderers.class, (e) -> tick());
      modBus.addListener(EventPriority.HIGHEST, false, RegisterKeyMappingsEvent.class, (e) -> tick());
      modBus.addListener(EventPriority.HIGHEST, false, RegisterGuiLayersEvent.class, (e) -> tick());
      modBus.addListener(EventPriority.NORMAL, false, FMLClientSetupEvent.class, (e) -> StartupTime.slow("setup"));
   }

   private static void tick() {
      try {
         if (!StartupTime.running() || !RenderSystem.isOnRenderThread()) {
            return;
         }

         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.getOverlay() != null) {
            return;
         }

         ImmediateWindowHandler.renderTick();
      } catch (LinkageError | RuntimeException var1) {
      }

   }

   private NeoForgeStartup() {
   }
}
