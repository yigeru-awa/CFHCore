package com.onehumanawa.cfhcore.content.world.worldgen.feature.template.placement;

import com.mojang.serialization.MapCodec;
import com.onehumanawa.cfhcore.content.world.platform.RegistryUtil;
import com.onehumanawa.cfhcore.content.world.registries.CFHWorldGenBuiltInRegistries;

public class TemplatePlacements {
   public static void bootstrap() {
      register("any", AnyPlacement.CODEC);
      register("tree", TreePlacement.CODEC);
   }

   public static AnyPlacement any() {
      return new AnyPlacement();
   }

   public static TreePlacement tree() {
      return new TreePlacement();
   }

   private static void register(String name, MapCodec<? extends TemplatePlacement<?>> placement) {
      RegistryUtil.register(CFHWorldGenBuiltInRegistries.TEMPLATE_PLACEMENT_TYPE, name, placement);
   }
}
