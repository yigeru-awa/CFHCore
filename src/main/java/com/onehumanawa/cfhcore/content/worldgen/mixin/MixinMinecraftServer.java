package com.onehumanawa.cfhcore.content.worldgen.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.ServerLevelData;
import com.onehumanawa.cfhcore.content.worldgen.data.preset.settings.Preset;
import com.onehumanawa.cfhcore.content.worldgen.registries.CFHRegistries;
import com.onehumanawa.cfhcore.content.worldgen.CFHRandomState;
import com.onehumanawa.cfhcore.content.worldgen.biome.CFHClimateSampler;

@Mixin(MinecraftServer.class)
class MixinMinecraftServer {

	@Inject(
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Climate$Sampler;findSpawnPosition()Lnet/minecraft/core/BlockPos;"
		),
		method = "setInitialSpawn"
	)
    private static void findSpawnPosition(ServerLevel serverLevel, ServerLevelData serverLevelData, boolean bl, boolean bl2, CallbackInfo callback) {
		RandomState randomState = serverLevel.getChunkSource().randomState();
		Climate.Sampler sampler = randomState.sampler();
		serverLevel.registryAccess().lookup(CFHRegistries.PRESET).flatMap((registry) -> {
			return registry.get(Preset.KEY);
		}).ifPresent((preset) -> {
			if((Object) randomState instanceof CFHRandomState rtfRandomState && (Object) sampler instanceof CFHClimateSampler rtfClimateSampler) {
				BlockPos searchCenter = preset.value().world().properties.spawnType.getSearchCenter(rtfRandomState.generatorContext(), preset.value().world().properties);
				rtfClimateSampler.setSpawnSearchCenter(searchCenter);
			} else {
				throw new IllegalStateException();
			}
		});
    }
}
