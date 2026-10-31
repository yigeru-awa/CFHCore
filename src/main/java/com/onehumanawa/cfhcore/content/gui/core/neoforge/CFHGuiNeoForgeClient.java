package com.onehumanawa.cfhcore.content.gui.core.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import com.onehumanawa.cfhcore.content.gui.core.client.CoreSettingsScreen;
import com.onehumanawa.cfhcore.content.gui.core.client.KeyBinds;
import com.onehumanawa.cfhcore.content.gui.core.client.CFHGuiClient;
import com.onehumanawa.cfhcore.content.gui.core.client.render.CFHGuiRenderEvents;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiKeys;
import com.onehumanawa.cfhcore.content.gui.core.screen.ContainerMotion;
import java.util.Objects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;

public final class CFHGuiNeoForgeClient {
   public static void init(IEventBus modBus, ModContainer container) {
      NeoForgeStartup.init(modBus);
      CFHGuiClient.init();
      modBus.addListener(RegisterKeyMappingsEvent.class, (e) -> {
         Objects.requireNonNull(e);
         CFHGuiKeys.install(e::register);
      });
      KeyBinds.install(new KeyBinds.Modifiers() {
         public String of(KeyMapping mapping) {
            return this.name(mapping.getKeyModifier());
         }

         public String defaultOf(KeyMapping mapping) {
            return this.name(mapping.getDefaultKeyModifier());
         }

         public void bind(KeyMapping mapping, InputConstants.Key key, String modifier) {
            KeyModifier var10000;
            switch (modifier) {
               case "shift" -> var10000 = KeyModifier.SHIFT;
               case "ctrl" -> var10000 = KeyModifier.CONTROL;
               case "alt" -> var10000 = KeyModifier.ALT;
               default -> var10000 = KeyModifier.NONE;
            }

            KeyModifier wanted = var10000;
            mapping.setKeyModifierAndCode(wanted, key);
         }

         private String name(KeyModifier modifier) {
            String var10000;
            switch (modifier) {
               case SHIFT -> var10000 = "shift";
               case CONTROL -> var10000 = "ctrl";
               case ALT -> var10000 = "alt";
               default -> var10000 = "";
            }

            return var10000;
         }
      });
      container.registerExtensionPoint(IConfigScreenFactory.class, (IConfigScreenFactory)(mod, parent) -> new CoreSettingsScreen(parent));
      NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, (e) -> CFHGuiEvents.CLIENT_TICK_END.invoke(Runnable::run));
      NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Post.class, (e) -> CFHGuiClient.onHudRendered(e.getGuiGraphics(), e.getPartialTick().getGameTimeDeltaPartialTick(false)));
      NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingIn.class, (e) -> CFHGuiEvents.CLIENT_JOINED_SERVER.invoke(Runnable::run));
      NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, (e) -> CFHGuiEvents.CLIENT_LEFT_SERVER.invoke(Runnable::run));
      NeoForge.EVENT_BUS.addListener(InputEvent.Key.class, (e) -> {
         if (e.getAction() == 1 && Minecraft.getInstance().screen == null) {
            CFHGuiEvents.KEY_PRESSED.invokeUntilConsumed((l) -> l.onKey(e.getKey(), e.getScanCode(), e.getModifiers()));
         }
      });
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, ScreenEvent.Render.Post.class, (e) -> ContainerMotion.begin(e.getScreen(), e.getGuiGraphics()));
      NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, ScreenEvent.Render.Post.class, (e) -> ContainerMotion.end(e.getScreen(), e.getGuiGraphics()));
      NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, (e) -> {
         if (e.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS && CFHGuiRenderEvents.hasAfterTranslucentListeners()) {
            CFHGuiRenderEvents.fireAfterTranslucent(new CFHGuiRenderEvents.WorldRenderContext(e.getPoseStack(), e.getModelViewMatrix(), e.getProjectionMatrix(), e.getCamera(), e.getPartialTick().getGameTimeDeltaPartialTick(false)));
         }
      });
   }

   private CFHGuiNeoForgeClient() {
   }
}
