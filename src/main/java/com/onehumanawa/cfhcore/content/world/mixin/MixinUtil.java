package com.onehumanawa.cfhcore.content.world.mixin;

import java.util.concurrent.ExecutorService;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.onehumanawa.cfhcore.content.world.concurrent.ThreadPools;
import com.onehumanawa.cfhcore.content.world.concurrent.cache.Cache;

@Mixin({Util.class})
public class MixinUtil {
   @Inject(
      method = {"shutdownExecutors()V"},
      at = {@At("TAIL")}
   )
   private static void shutdownExecutors(CallbackInfo callback) {
      shutdownExecutor(ThreadPools.WORLD_GEN);
      shutdownExecutor(Cache.SCHEDULER);
   }

   @Shadow
   private static void shutdownExecutor(ExecutorService executorService) {
      throw new UnsupportedOperationException();
   }
}
