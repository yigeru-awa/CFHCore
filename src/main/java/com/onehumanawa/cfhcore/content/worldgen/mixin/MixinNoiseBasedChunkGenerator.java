package com.onehumanawa.cfhcore.content.worldgen.mixin;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import com.onehumanawa.cfhcore.content.worldgen.ActiveChunk;
import com.onehumanawa.cfhcore.content.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.worldgen.MaxHeightUtil;
import com.onehumanawa.cfhcore.content.worldgen.CFHChunk;
import com.onehumanawa.cfhcore.content.worldgen.CFHRandomState;
import com.onehumanawa.cfhcore.content.worldgen.densityfunction.tile.Tile;

@Mixin(NoiseBasedChunkGenerator.class)
abstract class MixinNoiseBasedChunkGenerator extends ChunkGenerator {
	@Shadow
	@Final
    private Holder<NoiseGeneratorSettings> settings;

	public MixinNoiseBasedChunkGenerator(BiomeSource biomeSource, Function<Holder<Biome>, BiomeGenerationSettings> settingsGetter) {
		super(biomeSource, settingsGetter);
	}

	@Inject(
		method = "doCreateBiomes",
		at = @At("HEAD")
	)
    public void doCreateBiomes(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk, CallbackInfo callback) {
		CFHRandomState rtfRandomState = (CFHRandomState) (Object) randomState;
		GeneratorContext generatorContext = rtfRandomState.generatorContext();
		
		if(generatorContext == null) {
			return;
		}
		
		CFHChunk rtfChunk = (CFHChunk) chunk;
		
		ChunkPos chunkPos = chunk.getPos();
		Tile tile = generatorContext.cache.provideAtChunk(chunkPos.x, chunkPos.z);

		Tile.Chunk tileChunk = tile.getChunkReader(chunkPos.x, chunkPos.z);
		float maxHeight = Float.MIN_VALUE;
		for(int x = 0; x < 16; x++) {
			for(int z = 0; z < 16; z++) {
				float cellHeight = tileChunk.getCell(x, z).height * 256.0F;
				if(cellHeight > maxHeight) {
					maxHeight = cellHeight;
				}
			}	
		}
		
		rtfChunk.setMaxHeight(Mth.ceil(maxHeight));
    }
    
	@Redirect(
		method = { "fillFromNoise", "populateNoise" },
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/NoiseSettings;height()I"
		)
	)
    public int fillFromNoise(NoiseSettings settings, Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {

		CFHRandomState rtfRandomState = (CFHRandomState) (Object) randomState;
		if (rtfRandomState.generatorContext() == null) {
			return settings.height();
		}

		CFHChunk rtfChunk = (CFHChunk) chunk;
		int maxHeight = rtfChunk.getMaxHeight().orElseGet(settings::height);
		maxHeight = MaxHeightUtil.getMaxHeight(chunk.getPos(), maxHeight, this.settings.value(), settings, structureManager);
		return maxHeight;
    }
	
	@Inject(
		method = "createNoiseChunk",	
		at = @At("HEAD")
	)
    private void createNoiseChunk(ChunkAccess chunkAccess, StructureManager structureManager, Blender blender, RandomState randomState, CallbackInfoReturnable<NoiseChunk> callback) {

		CFHRandomState rtfRandomState = (CFHRandomState) (Object) randomState;
		if (rtfRandomState.generatorContext() == null) {
			return;
		}

		ActiveChunk.set(chunkAccess);
	}

}
