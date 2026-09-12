package com.onehumanawa.cfhcore.content.world.preset.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class FilterSettings {
   public static final Codec<FilterSettings> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               FilterSettings.Erosion.CODEC.fieldOf("erosion").forGetter(o -> o.erosion),
               FilterSettings.Smoothing.CODEC.fieldOf("smoothing").forGetter(o -> o.smoothing)
            )
            .apply(instance, FilterSettings::new)
   );
   public FilterSettings.Erosion erosion;
   public FilterSettings.Smoothing smoothing;

   public FilterSettings(FilterSettings.Erosion erosion, FilterSettings.Smoothing smoothing) {
      this.erosion = erosion;
      this.smoothing = smoothing;
   }

   public FilterSettings copy() {
      return new FilterSettings(this.erosion.copy(), this.smoothing.copy());
   }

   public static class Erosion {
      public static final Codec<FilterSettings.Erosion> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  Codec.INT.fieldOf("dropletsPerChunk").forGetter(o -> o.dropletsPerChunk),
                  Codec.INT.fieldOf("dropletLifetime").forGetter(o -> o.dropletLifetime),
                  Codec.FLOAT.fieldOf("dropletVolume").forGetter(o -> o.dropletVolume),
                  Codec.FLOAT.fieldOf("dropletVelocity").forGetter(o -> o.dropletVelocity),
                  Codec.FLOAT.fieldOf("erosionRate").forGetter(o -> o.erosionRate),
                  Codec.FLOAT.fieldOf("depositeRate").forGetter(o -> o.depositeRate)
               )
               .apply(instance, FilterSettings.Erosion::new)
      );
      public int dropletsPerChunk;
      public int dropletLifetime;
      public float dropletVolume;
      public float dropletVelocity;
      public float erosionRate;
      public float depositeRate;

      public Erosion(int dropletsPerChunk, int dropletsLifetime, float dropletVolume, float dropletVelocity, float erosionRate, float depositeRate) {
         this.dropletsPerChunk = dropletsPerChunk;
         this.dropletLifetime = dropletsLifetime;
         this.dropletVolume = dropletVolume;
         this.dropletVelocity = dropletVelocity;
         this.erosionRate = erosionRate;
         this.depositeRate = depositeRate;
      }

      public FilterSettings.Erosion copy() {
         return new FilterSettings.Erosion(
            this.dropletsPerChunk, this.dropletLifetime, this.dropletVolume, this.dropletVelocity, this.erosionRate, this.depositeRate
         );
      }
   }

   public static class Smoothing {
      public static final Codec<FilterSettings.Smoothing> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  Codec.INT.fieldOf("iterations").forGetter(o -> o.iterations),
                  Codec.FLOAT.fieldOf("smoothingRadius").forGetter(o -> o.smoothingRadius),
                  Codec.FLOAT.fieldOf("smoothingRate").forGetter(o -> o.smoothingRate)
               )
               .apply(instance, FilterSettings.Smoothing::new)
      );
      public int iterations;
      public float smoothingRadius;
      public float smoothingRate;

      public Smoothing(int iterations, float smoothingRadius, float smoothingRate) {
         this.iterations = iterations;
         this.smoothingRadius = smoothingRadius;
         this.smoothingRate = smoothingRate;
      }

      public FilterSettings.Smoothing copy() {
         return new FilterSettings.Smoothing(this.iterations, this.smoothingRadius, this.smoothingRate);
      }
   }
}
