package com.onehumanawa.cfhcore.content.world.neoforge.mixin;

import com.mojang.datafixers.DataFixer;
import java.net.Proxy;
import java.util.Collection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.MinecraftServer.ReloadableResources;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.onehumanawa.cfhcore.content.world.server.CFHWorldGenMinecraftServer;
import com.onehumanawa.cfhcore.content.world.worldgen.feature.template.template.FeatureTemplateManager;

@Implements({@Interface(
      iface = CFHWorldGenMinecraftServer.class,
      prefix = "cfhcore$CFHWorldGenMinecraftServer$"
   )})
@Mixin({MinecraftServer.class})
public class MixinMinecraftServer {
   private FeatureTemplateManager templateManager;

   @Inject(
      method = {"<init>(Ljava/lang/Thread;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/server/WorldStem;Ljava/net/Proxy;Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/server/Services;Lnet/minecraft/server/level/progress/ChunkProgressListenerFactory;)V"},
      at = {@At("TAIL")}
   )
   public void MinecraftServer(
      Thread thread,
      LevelStorageAccess arg2,
      PackRepository arg22,
      WorldStem arg3,
      Proxy proxy,
      DataFixer dataFixer,
      Services arg4,
      ChunkProgressListenerFactory arg5,
      CallbackInfo callback
   ) {
      this.templateManager = new FeatureTemplateManager(this.getResourceManager());
   }

   public FeatureTemplateManager cfhcore$CFHWorldGenMinecraftServer$getFeatureTemplateManager() {
      return this.templateManager;
   }

   @Inject(
      method = {"lambda$reloadResources$30(Ljava/util/Collection;Lnet/minecraft/server/MinecraftServer$ReloadableResources;)V"},
      require = 0,
      at = {@At("TAIL")}
   )
   private void lambda$reloadResources$26(Collection collection, ReloadableResources arg, CallbackInfo callback) {
      this.templateManager.onReload(this.getResourceManager());
   }

   @Shadow
   private ResourceManager getResourceManager() {
      throw new UnsupportedOperationException();
   }
}
