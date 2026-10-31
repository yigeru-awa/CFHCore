package com.onehumanawa.cfhcore.content.gui.core.module;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.ActionType;
import com.onehumanawa.cfhcore.content.gui.core.layout.action.Actions;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Modules {
   private static final Map<String, CFHGuiModule> MODULES = Collections.synchronizedMap(new LinkedHashMap());
   private static final List<CFHGuiModule> CLIENT_INITIALISED = new ArrayList();

   public static void register(CFHGuiModule module) {
      CFHGui.init();
      if (MODULES.putIfAbsent(module.id(), module) == null) {
         CFHGui.LOGGER.info("[CFHGui] module {} ({})", module.id(), module.displayName().getString());
         module.init();

         for(ActionType action : module.actions()) {
            Actions.register(action);
         }

         if (CFHGuiPlatform.get().isClient()) {
            module.initClient();
            synchronized(CLIENT_INITIALISED) {
               CLIENT_INITIALISED.add(module);
            }
         }

      }
   }

   public static boolean isLoaded(String id) {
      return MODULES.containsKey(id);
   }

   public static Optional<CFHGuiModule> get(String id) {
      return Optional.ofNullable((CFHGuiModule)MODULES.get(id));
   }

   public static List<CFHGuiModule> all() {
      synchronized(MODULES) {
         return List.copyOf(MODULES.values());
      }
   }

   private Modules() {
   }
}
