package com.onehumanawa.cfhcore.content.world.preset.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class TerrainSettings {
   public static final Codec<TerrainSettings> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               TerrainSettings.General.CODEC.fieldOf("general").forGetter(o -> o.general),
               TerrainSettings.Terrain.CODEC.fieldOf("steppe").forGetter(o -> o.steppe),
               TerrainSettings.Terrain.CODEC.fieldOf("plains").forGetter(o -> o.plains),
               TerrainSettings.Terrain.CODEC.fieldOf("hills").forGetter(o -> o.hills),
               TerrainSettings.Terrain.CODEC.fieldOf("dales").forGetter(o -> o.dales),
               TerrainSettings.Terrain.CODEC.fieldOf("plateau").forGetter(o -> o.plateau),
               TerrainSettings.Terrain.CODEC.fieldOf("badlands").forGetter(o -> o.badlands),
               TerrainSettings.Terrain.CODEC.fieldOf("torridonian").forGetter(o -> o.torridonian),
               TerrainSettings.Terrain.CODEC.fieldOf("mountains").forGetter(o -> o.mountains),
               TerrainSettings.Terrain.CODEC.fieldOf("volcano").forGetter(o -> o.volcano)
            )
            .apply(instance, TerrainSettings::new)
   );
   public TerrainSettings.General general;
   public TerrainSettings.Terrain steppe;
   public TerrainSettings.Terrain plains;
   public TerrainSettings.Terrain hills;
   public TerrainSettings.Terrain dales;
   public TerrainSettings.Terrain plateau;
   public TerrainSettings.Terrain badlands;
   public TerrainSettings.Terrain torridonian;
   public TerrainSettings.Terrain mountains;
   public TerrainSettings.Terrain volcano;

   public TerrainSettings(
      TerrainSettings.General general,
      TerrainSettings.Terrain steppe,
      TerrainSettings.Terrain plains,
      TerrainSettings.Terrain hills,
      TerrainSettings.Terrain dales,
      TerrainSettings.Terrain plateau,
      TerrainSettings.Terrain badlands,
      TerrainSettings.Terrain torridonian,
      TerrainSettings.Terrain mountains,
      TerrainSettings.Terrain volcano
   ) {
      this.general = general;
      this.steppe = steppe;
      this.plains = plains;
      this.hills = hills;
      this.dales = dales;
      this.plateau = plateau;
      this.badlands = badlands;
      this.torridonian = torridonian;
      this.mountains = mountains;
      this.volcano = volcano;
   }

   public TerrainSettings copy() {
      return new TerrainSettings(
         this.general.copy(),
         this.steppe.copy(),
         this.plains.copy(),
         this.hills.copy(),
         this.dales.copy(),
         this.plateau.copy(),
         this.badlands.copy(),
         this.torridonian.copy(),
         this.mountains.copy(),
         this.volcano.copy()
      );
   }

   public static class General {
      public static final Codec<TerrainSettings.General> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  Codec.INT.fieldOf("terrainSeedOffset").forGetter(o -> o.terrainSeedOffset),
                  Codec.INT.fieldOf("terrainRegionSize").forGetter(o -> o.terrainRegionSize),
                  Codec.FLOAT.fieldOf("globalVerticalScale").forGetter(o -> o.globalVerticalScale),
                  Codec.FLOAT.fieldOf("globalHorizontalScale").forGetter(o -> o.globalHorizontalScale),
                  Codec.BOOL.fieldOf("fancyMountains").forGetter(o -> o.fancyMountains),
                  Codec.BOOL.optionalFieldOf("legacyMountainScaling", Boolean.valueOf(true)).forGetter(o -> o.legacyMountainScaling)
               )
               .apply(instance, TerrainSettings.General::new)
      );
      public int terrainSeedOffset;
      public int terrainRegionSize;
      public float globalVerticalScale;
      public float globalHorizontalScale;
      public boolean fancyMountains;
      public boolean legacyMountainScaling;

      public General(
         int terrainSeedOffset,
         int terrainRegionSize,
         float globalVerticalScale,
         float globalHorizontalScale,
         boolean fancyMountains,
         boolean legacyMountainScaling
      ) {
         this.terrainSeedOffset = terrainSeedOffset;
         this.terrainRegionSize = terrainRegionSize;
         this.globalVerticalScale = globalVerticalScale;
         this.globalHorizontalScale = globalHorizontalScale;
         this.fancyMountains = fancyMountains;
         this.legacyMountainScaling = legacyMountainScaling;
      }

      public TerrainSettings.General copy() {
         return new TerrainSettings.General(
            this.terrainSeedOffset,
            this.terrainRegionSize,
            this.globalVerticalScale,
            this.globalHorizontalScale,
            this.fancyMountains,
            this.legacyMountainScaling
         );
      }
   }

   public static class Terrain {
      public static final Codec<TerrainSettings.Terrain> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  Codec.FLOAT.fieldOf("weight").forGetter(o -> o.weight),
                  Codec.FLOAT.fieldOf("baseScale").forGetter(o -> o.baseScale),
                  Codec.FLOAT.fieldOf("verticalScale").forGetter(o -> o.verticalScale),
                  Codec.FLOAT.fieldOf("horizontalScale").forGetter(o -> o.horizontalScale)
               )
               .apply(instance, TerrainSettings.Terrain::new)
      );
      public float weight;
      public float baseScale;
      public float verticalScale;
      public float horizontalScale;

      public Terrain(float weight, float baseScale, float verticalScale, float horizontalScale) {
         this.weight = weight;
         this.baseScale = baseScale;
         this.verticalScale = verticalScale;
         this.horizontalScale = horizontalScale;
      }

      public TerrainSettings.Terrain copy() {
         return new TerrainSettings.Terrain(this.weight, this.baseScale, this.verticalScale, this.horizontalScale);
      }
   }
}
