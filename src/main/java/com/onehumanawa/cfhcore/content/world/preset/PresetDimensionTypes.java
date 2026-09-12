package com.onehumanawa.cfhcore.content.world.preset;

import java.util.OptionalLong;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.DimensionType.MonsterSettings;
import com.onehumanawa.cfhcore.content.world.preset.settings.Preset;
import com.onehumanawa.cfhcore.content.world.preset.settings.WorldSettings;

public final class PresetDimensionTypes {
   public static void bootstrap(Preset preset, BootstrapContext<DimensionType> ctx) {
      WorldSettings worldSettings = preset.world();
      WorldSettings.Properties properties = worldSettings.properties;
      int worldHeight = properties.worldHeight;
      int worldDepth = properties.worldDepth;
      int totalHeight = worldDepth + worldHeight;
      ctx.register(
         BuiltinDimensionTypes.OVERWORLD,
         new DimensionType(
            OptionalLong.empty(),
            true,
            false,
            false,
            true,
            1.0,
            true,
            false,
            -worldDepth,
            totalHeight,
            totalHeight,
            BlockTags.INFINIBURN_OVERWORLD,
            BuiltinDimensionTypes.OVERWORLD_EFFECTS,
            0.0F,
            new MonsterSettings(false, true, UniformInt.of(0, 7), 0)
         )
      );
   }
}
