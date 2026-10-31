package com.onehumanawa.cfhcore.content.gui.config.neoforge;

import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfig;
import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfigApi;
import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// Client bootstrap for the CFHGui config stack, driven by CFHClient.
public final class CFHGuiConfigNeoForge {
   private CFHGuiConfigNeoForge() {
   }

   public static void register(IEventBus modBus, ModContainer container) {
      Modules.register(CFHGuiConfig.MODULE);
      container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> CFHGuiConfigApi.hub(parent, (String)null));
   }
}
