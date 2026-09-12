package com.onehumanawa.cfhcore.content.world.mixin;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Climate.ParameterPoint;
import net.minecraft.world.level.biome.Climate.Sampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.onehumanawa.cfhcore.content.world.preset.settings.SpawnType;
import com.onehumanawa.cfhcore.content.world.preset.settings.WorldSettings;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.biome.spawn.SpawnFinderFix;

@Mixin({Climate.class})
public class MixinSpawnFinder {
   @Inject(
      method = {"findSpawnPosition(Ljava/util/List;Lnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/BlockPos;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void findSpawnPosition(List<ParameterPoint> list, Sampler sampler, CallbackInfoReturnable<BlockPos> cir) {
      if (WorldSettings.Properties.spawnType == SpawnType.USER_SELECTED) {
         int x = WorldSettings.Properties.spawnX;
         int z = WorldSettings.Properties.spawnZ;
         cir.setReturnValue(new BlockPos(x, 0, z));
      } else if (WorldSettings.Properties.spawnType == SpawnType.WORLD_ORIGIN) {
         int x = 0;
         int z = 0;
         cir.setReturnValue(new BlockPos(x, 0, z));
      } else {
         cir.setReturnValue((new SpawnFinderFix(list, sampler)).result.location());
      }
   }
}
