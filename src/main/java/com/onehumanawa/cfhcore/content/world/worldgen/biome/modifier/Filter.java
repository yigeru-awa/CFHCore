package com.onehumanawa.cfhcore.content.world.worldgen.biome.modifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biome;

public record Filter(HolderSet<Biome> biomes, Filter.Behavior behavior) {
   public static final Codec<Filter> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               Biome.LIST_CODEC.fieldOf("biomes").forGetter(Filter::biomes), Filter.Behavior.CODEC.fieldOf("behavior").forGetter(Filter::behavior)
            )
            .apply(instance, Filter::new)
   );

   public boolean test(Holder<Biome> biome) {
      return this.behavior.test(this.biomes, biome);
   }

   public static enum Behavior implements StringRepresentable {
      WHITELIST("whitelist") {
         @Override
         public boolean test(HolderSet<Biome> biomes, Holder<Biome> biome) {
            return biomes.contains(biome);
         }
      },
      BLACKLIST("blacklist") {
         @Override
         public boolean test(HolderSet<Biome> biomes, Holder<Biome> biome) {
            return !biomes.contains(biome);
         }
      };

      public static final Codec<Filter.Behavior> CODEC = StringRepresentable.fromEnum(Filter.Behavior::values);
      private String name;

      private Behavior(String name) {
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public abstract boolean test(HolderSet<Biome> var1, Holder<Biome> var2);
   }
}
