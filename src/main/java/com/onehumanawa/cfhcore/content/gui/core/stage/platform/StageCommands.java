package com.onehumanawa.cfhcore.content.gui.core.stage.platform;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.platform.Services;
import com.onehumanawa.cfhcore.content.gui.core.stage.scene.SceneCommandHandler;
import java.util.ServiceConfigurationError;

public interface StageCommands {
   StageCommands NONE = (handler) -> CFHGui.LOGGER.info("[CFHGui] stage: no client command service on this loader; use the cfhgui:scene_capture action");

   static StageCommands get() {
      try {
         return (StageCommands)Services.load(StageCommands.class);
      } catch (ServiceConfigurationError | IllegalStateException var1) {
         return NONE;
      }
   }

   void register(SceneCommandHandler var1);
}
