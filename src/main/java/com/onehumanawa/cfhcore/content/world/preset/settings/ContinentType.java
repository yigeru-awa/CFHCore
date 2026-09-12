package com.onehumanawa.cfhcore.content.world.preset.settings;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import com.onehumanawa.cfhcore.content.world.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.continent.Continent;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.continent.advanced.AdvancedContinentGenerator;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.continent.simple.MultiContinentGenerator;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.continent.simple.SingleContinentGenerator;
import com.onehumanawa.cfhcore.content.world.worldgen.cell.continent.uplift.UpliftContinentGenerator;
import com.onehumanawa.cfhcore.content.world.worldgen.util.Seed;

public enum ContinentType implements StringRepresentable {
   MULTI {
      public MultiContinentGenerator create(Seed seed, GeneratorContext context) {
         return new MultiContinentGenerator(seed, context);
      }
   },
   SINGLE {
      public SingleContinentGenerator create(Seed seed, GeneratorContext context) {
         return new SingleContinentGenerator(seed, context);
      }
   },
   MULTI_IMPROVED {
      public AdvancedContinentGenerator create(Seed seed, GeneratorContext context) {
         return new AdvancedContinentGenerator(seed, context);
      }
   },
   UPLIFT {
      public UpliftContinentGenerator create(Seed seed, GeneratorContext context) {
         return new UpliftContinentGenerator(seed, context);
      }
   };

   public static final Codec<ContinentType> CODEC = StringRepresentable.fromEnum(ContinentType::values);

   public abstract Continent create(Seed var1, GeneratorContext var2);

   @Override
   public String getSerializedName() {
      return this.name();
   }
}
