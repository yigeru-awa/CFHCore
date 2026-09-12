package com.onehumanawa.cfhcore.content.world.preset.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.function.DistanceFunction;

public class WorldSettings {
   public static final Codec<WorldSettings> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               WorldSettings.Continent.CODEC.fieldOf("continent").forGetter(o -> o.continent),
               WorldSettings.ControlPoints.CODEC.fieldOf("controlPoints").forGetter(o -> o.controlPoints),
               WorldSettings.Properties.CODEC.fieldOf("properties").forGetter(o -> o.properties)
            )
            .apply(instance, WorldSettings::new)
   );
   public WorldSettings.Continent continent;
   public WorldSettings.ControlPoints controlPoints;
   public WorldSettings.Properties properties;

   public WorldSettings(WorldSettings.Continent continent, WorldSettings.ControlPoints controlPoints, WorldSettings.Properties properties) {
      this.continent = continent;
      this.controlPoints = controlPoints;
      this.properties = properties;
   }

   public WorldSettings copy() {
      return new WorldSettings(this.continent.copy(), this.controlPoints.copy(), this.properties.copy());
   }

   public static class Continent {
      public static final Codec<WorldSettings.Continent> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  ContinentType.CODEC.fieldOf("continentType").forGetter(o -> o.continentType),
                  DistanceFunction.CODEC.optionalFieldOf("continentShape", DistanceFunction.EUCLIDEAN).forGetter(o -> o.continentShape),
                  Codec.INT.fieldOf("continentScale").forGetter(o -> o.continentScale),
                  Codec.FLOAT.fieldOf("continentJitter").forGetter(o -> o.continentJitter),
                  Codec.FLOAT.optionalFieldOf("continentSkipping", Float.valueOf(0.25F)).forGetter(o -> o.continentSkipping),
                  Codec.FLOAT.optionalFieldOf("continentSizeVariance", Float.valueOf(0.25F)).forGetter(o -> o.continentSizeVariance),
                  Codec.INT.optionalFieldOf("continentNoiseOctaves", Integer.valueOf(5)).forGetter(o -> o.continentNoiseOctaves),
                  Codec.FLOAT.optionalFieldOf("continentNoiseGain", Float.valueOf(0.26F)).forGetter(o -> o.continentNoiseGain),
                  Codec.FLOAT.optionalFieldOf("continentNoiseLacunarity", Float.valueOf(4.33F)).forGetter(o -> o.continentNoiseLacunarity)
               )
               .apply(instance, WorldSettings.Continent::new)
      );
      public ContinentType continentType;
      public DistanceFunction continentShape;
      public int continentScale;
      public float continentJitter;
      public float continentSkipping;
      public float continentSizeVariance;
      public int continentNoiseOctaves;
      public float continentNoiseGain;
      public float continentNoiseLacunarity;

      public Continent(
         ContinentType continentType,
         DistanceFunction continentShape,
         int continentScale,
         float continentJitter,
         float continentSkipping,
         float continentSizeVariance,
         int continentNoiseOctaves,
         float continentNoiseGain,
         float continentNoiseLacunarity
      ) {
         this.continentType = continentType;
         this.continentShape = continentShape;
         this.continentScale = continentScale;
         this.continentJitter = continentJitter;
         this.continentSkipping = continentSkipping;
         this.continentSizeVariance = continentSizeVariance;
         this.continentNoiseOctaves = continentNoiseOctaves;
         this.continentNoiseGain = continentNoiseGain;
         this.continentNoiseLacunarity = continentNoiseLacunarity;
      }

      public WorldSettings.Continent copy() {
         return new WorldSettings.Continent(
            this.continentType,
            this.continentShape,
            this.continentScale,
            this.continentJitter,
            this.continentSkipping,
            this.continentSizeVariance,
            this.continentNoiseOctaves,
            this.continentNoiseGain,
            this.continentNoiseLacunarity
         );
      }
   }

   public static class ControlPoints {
      public static final Codec<WorldSettings.ControlPoints> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  Codec.FLOAT.fieldOf("islandInland").forGetter(o -> o.islandInland),
                  Codec.FLOAT.fieldOf("islandCoast").forGetter(o -> o.islandCoast),
                  Codec.FLOAT.fieldOf("deepOcean").forGetter(o -> o.deepOcean),
                  Codec.FLOAT.fieldOf("shallowOcean").forGetter(o -> o.shallowOcean),
                  Codec.FLOAT.fieldOf("beach").forGetter(o -> o.beach),
                  Codec.FLOAT.fieldOf("coast").forGetter(o -> o.coast),
                  Codec.FLOAT.fieldOf("inland").forGetter(o -> o.inland)
               )
               .apply(instance, WorldSettings.ControlPoints::new)
      );
      public float islandInland;
      public float islandCoast;
      public float deepOcean;
      public float shallowOcean;
      public float beach;
      public float coast;
      public float inland;

      public ControlPoints(float islandInland, float islandCoast, float deepOcean, float shallowOcean, float beach, float coast, float inland) {
         this.islandInland = islandInland;
         this.islandCoast = islandCoast;
         this.deepOcean = deepOcean;
         this.shallowOcean = shallowOcean;
         this.beach = beach;
         this.coast = coast;
         this.inland = inland;
      }

      public float coastMarker() {
         return this.coast + (this.inland - this.coast) / 2.0F;
      }

      public WorldSettings.ControlPoints copy() {
         return new WorldSettings.ControlPoints(this.islandInland, this.islandCoast, this.deepOcean, this.shallowOcean, this.beach, this.coast, this.inland);
      }
   }

   public static class Properties {
      public static final Codec<WorldSettings.Properties> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  SpawnType.CODEC.fieldOf("spawnType").forGetter(o -> WorldSettings.Properties.spawnType),
                  Codec.INT.fieldOf("worldHeight").forGetter(o -> o.worldHeight),
                  Codec.INT.optionalFieldOf("worldDepth", Integer.valueOf(64)).forGetter(o -> o.worldDepth),
                  Codec.INT.fieldOf("seaLevel").forGetter(o -> o.seaLevel),
                  Codec.INT.optionalFieldOf("lavaLevel", Integer.valueOf(-54)).forGetter(o -> o.lavaLevel),
                  Codec.INT.optionalFieldOf("spawnX", Integer.valueOf(0)).forGetter(o -> WorldSettings.Properties.spawnX),
                  Codec.INT.optionalFieldOf("spawnZ", Integer.valueOf(0)).forGetter(o -> WorldSettings.Properties.spawnZ)
               )
               .apply(instance, WorldSettings.Properties::new)
      );
      public static SpawnType spawnType;
      public int worldHeight;
      public int worldDepth;
      public int seaLevel;
      public int lavaLevel;
      public static int spawnX;
      public static int spawnZ;

      public Properties(SpawnType spawnType, int worldHeight, int worldDepth, int seaLevel, int lavaLevel, int spawnX, int spawnZ) {
         WorldSettings.Properties.spawnType = spawnType;
         this.worldHeight = worldHeight;
         this.worldDepth = worldDepth;
         this.seaLevel = seaLevel;
         this.lavaLevel = lavaLevel;
         WorldSettings.Properties.spawnX = spawnX;
         WorldSettings.Properties.spawnZ = spawnZ;
      }

      public WorldSettings.Properties copy() {
         return new WorldSettings.Properties(spawnType, this.worldHeight, this.worldDepth, this.seaLevel, this.lavaLevel, spawnX, spawnZ);
      }

      @Deprecated
      public int terrainScaler() {
         return Math.min(this.worldHeight, 256);
      }
   }
}
