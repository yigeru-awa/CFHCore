package com.onehumanawa.cfhcore.content.world.worldgen.biome.modifier.neoforge;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo.BiomeInfo.Builder;
import com.onehumanawa.cfhcore.content.world.worldgen.biome.modifier.BiomeModifier;

interface ForgeBiomeModifier extends BiomeModifier, net.neoforged.neoforge.common.world.BiomeModifier {
   @Override
   void modify(Holder<Biome> var1, Phase var2, Builder var3);

   @Override
   MapCodec<? extends ForgeBiomeModifier> codec();
}
