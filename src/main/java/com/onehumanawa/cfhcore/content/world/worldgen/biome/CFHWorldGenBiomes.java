package com.onehumanawa.cfhcore.content.world.worldgen.biome;

import net.minecraft.sounds.Music;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.BiomeBuilder;
import net.minecraft.world.level.biome.MobSpawnSettings.Builder;
import org.jetbrains.annotations.Nullable;

public class CFHWorldGenBiomes {
   @Nullable
   private static final Music NORMAL_MUSIC = null;

   public static Biome coldMarshland() {
      return null;
   }

   private static Biome biome(
      boolean hasPrecipitation,
      float skyColor,
      float downfall,
      Builder mobSpawnSettings,
      net.minecraft.world.level.biome.BiomeGenerationSettings.Builder generationSettings,
      @Nullable Music music
   ) {
      return biome(hasPrecipitation, skyColor, downfall, 4159204, 329011, null, null, mobSpawnSettings, generationSettings, music);
   }

   private static Biome biome(
      boolean hasPrecipitation,
      float skyColor,
      float downfall,
      int waterColor,
      int waterFogColor,
      @Nullable Integer grassColorOverride,
      @Nullable Integer foliageColorOverride,
      Builder mobSpawnSettings,
      net.minecraft.world.level.biome.BiomeGenerationSettings.Builder generationSettings,
      @Nullable Music music
   ) {
      net.minecraft.world.level.biome.BiomeSpecialEffects.Builder specialEffects = new net.minecraft.world.level.biome.BiomeSpecialEffects.Builder()
         .waterColor(waterColor)
         .waterFogColor(waterFogColor)
         .fogColor(12638463)
         .skyColor(calculateSkyColor(skyColor))
         .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
         .backgroundMusic(music);
      if (grassColorOverride != null) {
         specialEffects.grassColorOverride(grassColorOverride);
      }

      if (foliageColorOverride != null) {
         specialEffects.foliageColorOverride(foliageColorOverride);
      }

      return new BiomeBuilder()
         .hasPrecipitation(hasPrecipitation)
         .temperature(skyColor)
         .downfall(downfall)
         .specialEffects(specialEffects.build())
         .mobSpawnSettings(mobSpawnSettings.build())
         .generationSettings(generationSettings.build())
         .build();
   }

   private static int calculateSkyColor(float f) {
      float g = f / 3.0F;
      g = Mth.clamp(g, -1.0F, 1.0F);
      return Mth.hsvToRgb(0.62222224F - g * 0.05F, 0.5F + g * 0.1F, 1.0F);
   }
}
