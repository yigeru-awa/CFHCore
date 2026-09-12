package com.onehumanawa.cfhcore.content.world.neoforge.data;

import com.onehumanawa.cfhcore.content.world.preset.settings.Presets;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

/**
 * Bakes the hardcoded CFHWorldGen preset into a datapack at datagen time.
 *
 * <p>Upstream CFHWorldGen let the player export an arbitrary preset to a datapack from its
 * preset screen. CFHCore has no such screen, so there is exactly one preset
 * ({@link Presets#DEFAULT}) and it is generated once, into {@code src/generated/resources},
 * where it ships as CFHCore's always-enabled built-in datapack.
 *
 * <p>{@code modIds} is left {@code null} on purpose: the generated patch overrides
 * {@code minecraft:overworld}'s dimension type and noise settings, and filtering to the
 * {@code cfhcore} namespace would drop exactly those entries.
 */
public class CFHWorldGenDatapackProvider extends DatapackBuiltinEntriesProvider {
   public CFHWorldGenDatapackProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
      super(output, registries.thenApply(Presets.DEFAULT::buildPatch), (Set<String>)null);
   }

   @Override
   public String getName() {
      return "CFHCore CFHWorldGen worldgen";
   }
}
