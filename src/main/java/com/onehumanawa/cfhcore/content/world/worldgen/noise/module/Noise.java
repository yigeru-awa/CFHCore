package com.onehumanawa.cfhcore.content.world.worldgen.noise.module;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Direct;
import net.minecraft.resources.RegistryFileCodec;
import com.onehumanawa.cfhcore.content.world.registries.CFHWorldGenRegistries;

public interface Noise {
   Codec<Noise> DIRECT_CODEC = Noises.DIRECT_CODEC;
   Codec<Holder<Noise>> CODEC = RegistryFileCodec.create(CFHWorldGenRegistries.NOISE, DIRECT_CODEC);
   Codec<Noise> HOLDER_HELPER_CODEC = CODEC.xmap(
      Noises.HolderHolder::new, noise -> (Holder<Noise>)(noise instanceof Noises.HolderHolder holderHolder ? holderHolder.holder() : new Direct<>(noise))
   );

   float compute(float var1, float var2, int var3);

   float minValue();

   float maxValue();

   Noise mapAll(Noise.Visitor var1);

   MapCodec<? extends Noise> codec();

   public interface Visitor {
      Noise apply(Noise var1);
   }
}
