package com.onehumanawa.cfhcore.content.world.neoforge;

import com.mojang.serialization.MapCodec;
import com.onehumanawa.cfhcore.CFHCore;
import com.onehumanawa.cfhcore.content.world.CFHWorldGenCommon;
import com.onehumanawa.cfhcore.content.world.neoforge.data.CFHWorldGenDatapackProvider;
import com.onehumanawa.cfhcore.content.world.platform.RegistryUtil;
import com.onehumanawa.cfhcore.content.world.worldgen.biome.modifier.neoforge.AddModifier;
import com.onehumanawa.cfhcore.content.world.worldgen.biome.modifier.neoforge.ReplaceModifier;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * NeoForge entry point of the CFHWorldGen port.
 *
 * <p>Upstream CFHWorldGen was an {@code @Mod} of its own; inside CFHCore it is plain content,
 * so {@link CFHCore} calls {@link #register(IEventBus)} from its constructor and
 * {@link #gatherData(GatherDataEvent)} from its data listener.
 *
 * <p>The preset editor screen and the runtime preset datapack exporter are gone. The terrain is
 * described by {@link com.onehumanawa.cfhcore.content.world.preset.settings.Presets#DEFAULT}
 * and baked into a built-in datapack by {@link CFHWorldGenDatapackProvider}.
 */
public final class CFHWorldGenNeoForge {
   private CFHWorldGenNeoForge() {
   }

   public static void register(IEventBus modEventBus) {
      CFHWorldGenCommon.bootstrap();

      DeferredRegister<MapCodec<? extends BiomeModifier>> biomeModifierSerializers = DeferredRegister.create(
         NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, CFHCore.ID
      );
      biomeModifierSerializers.register("add", () -> AddModifier.CODEC);
      biomeModifierSerializers.register("replace", () -> ReplaceModifier.CODEC);
      biomeModifierSerializers.register(modEventBus);

      RegistryUtil.register(modEventBus);
   }

   public static void gatherData(GatherDataEvent event) {
      if (!event.includeServer()) {
         return;
      }

      DataGenerator generator = event.getGenerator();
      PackOutput output = generator.getPackOutput();
      generator.addProvider(true, new CFHWorldGenDatapackProvider(output, event.getLookupProvider()));
   }
}
