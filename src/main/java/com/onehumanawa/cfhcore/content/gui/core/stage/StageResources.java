package com.onehumanawa.cfhcore.content.gui.core.stage;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.StageModels;
import com.onehumanawa.cfhcore.content.gui.core.stage.scene.SceneLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public final class StageResources implements ResourceManagerReloadListener {
   private static volatile int generation;
   private static boolean registered;

   public static synchronized void ensureRegistered() {
      if (!registered) {
         ResourceManager rm = Minecraft.getInstance().getResourceManager();
         if (rm instanceof ReloadableResourceManager) {
            ReloadableResourceManager reloadable = (ReloadableResourceManager)rm;
            reloadable.registerReloadListener(new StageResources());
            registered = true;
         } else {
            CFHGui.LOGGER.warn("[CFHGui] stage: resource manager is not reloadable; stage caches will not follow resource reloads");
            registered = true;
         }

      }
   }

   public static int generation() {
      return generation;
   }

   public void onResourceManagerReload(ResourceManager manager) {
      ++generation;
      StageModels.clear();
      SceneLoader.clear();
      StagePost.reload();
      StageSoft.reload();
      CFHGui.LOGGER.debug("[CFHGui] stage: resources reloaded (generation {})", generation);
   }
}
