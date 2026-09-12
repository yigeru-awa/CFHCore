package com.onehumanawa.cfhcore.content.world.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.onehumanawa.cfhcore.content.world.preset.settings.Presets;
import com.onehumanawa.cfhcore.content.world.preset.settings.WorldSettings;
import com.onehumanawa.cfhcore.content.world.worldgen.CFHWorldGenRandomState;
import com.onehumanawa.cfhcore.content.world.worldgen.biome.CFHWorldGenClimateSampler;

@Mixin({MinecraftServer.class})
class MixinMinecraftServer {
   @Inject(
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/biome/Climate$Sampler;findSpawnPosition()Lnet/minecraft/core/BlockPos;"
      )},
      method = {"setInitialSpawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/storage/ServerLevelData;ZZ)V"}
   )
   private static void findSpawnPosition(ServerLevel serverLevel, ServerLevelData serverLevelData, boolean bl, boolean bl2, CallbackInfo callback) {
      RandomState randomState = serverLevel.getChunkSource().randomState();
      Sampler sampler = randomState.sampler();
      if (randomState instanceof CFHWorldGenRandomState cfhWorldGenRandomState
         && cfhWorldGenRandomState.generatorContext() != null
         // Sampler is a record, so it is final; the (Object) widening keeps the duck check legal.
         && (Object)sampler instanceof CFHWorldGenClimateSampler cfhWorldGenClimateSampler) {
         BlockPos searchCenter = WorldSettings.Properties.spawnType.getSearchCenter(cfhWorldGenRandomState.generatorContext(), Presets.DEFAULT.world().properties);
         cfhWorldGenClimateSampler.setSpawnSearchCenter(searchCenter);
      }
   }
}
