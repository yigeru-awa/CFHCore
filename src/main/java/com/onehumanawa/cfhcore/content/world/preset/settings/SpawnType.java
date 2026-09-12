package com.onehumanawa.cfhcore.content.world.preset.settings;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Climate.Parameter;
import net.minecraft.world.level.biome.Climate.ParameterPoint;
import com.onehumanawa.cfhcore.content.world.worldgen.GeneratorContext;
import com.onehumanawa.cfhcore.content.world.worldgen.util.PosUtil;

public enum SpawnType implements StringRepresentable {
   CONTINENT_CENTER("CONTINENT_CENTER") {
      private static final Parameter FULL_RANGE = Parameter.span(-1.0F, 1.0F);
      private static final Parameter SURFACE_DEPTH = Parameter.point(0.0F);
      private static final Parameter INLAND_CONTINENTALNESS = Parameter.span(-0.11F, 0.55F);

      @Override
      public BlockPos getSearchCenter(GeneratorContext ctx, WorldSettings.Properties properties) {
         long center = ctx.generator.getHeightmap().continent().getNearestCenter(0.0F, 0.0F);
         return new BlockPos(PosUtil.unpackLeft(center), 0, PosUtil.unpackRight(center));
      }

      @Override
      public List<ParameterPoint> getParameterPoints() {
         return List.of(
            new ParameterPoint(FULL_RANGE, FULL_RANGE, Parameter.span(INLAND_CONTINENTALNESS, FULL_RANGE), FULL_RANGE, SURFACE_DEPTH, FULL_RANGE, 0L),
            new ParameterPoint(FULL_RANGE, FULL_RANGE, Parameter.span(INLAND_CONTINENTALNESS, FULL_RANGE), FULL_RANGE, SURFACE_DEPTH, FULL_RANGE, 0L)
         );
      }
   },
   ISLANDS("ISLANDS") {
      @Override
      public BlockPos getSearchCenter(GeneratorContext ctx, WorldSettings.Properties properties) {
         return BlockPos.ZERO;
      }

      @Override
      public List<ParameterPoint> getParameterPoints() {
         return ImmutableList.of();
      }
   },
   WORLD_ORIGIN("WORLD_ORIGIN") {
      @Override
      public BlockPos getSearchCenter(GeneratorContext ctx, WorldSettings.Properties properties) {
         return BlockPos.ZERO;
      }

      @Override
      public List<ParameterPoint> getParameterPoints() {
         return ImmutableList.of();
      }
   },
   USER_SELECTED("USER_SELECTED") {
      private static final Parameter FULL_RANGE = Parameter.span(-2.0F, 2.0F);

      @Override
      public BlockPos getSearchCenter(GeneratorContext ctx, WorldSettings.Properties properties) {
         return new BlockPos(WorldSettings.Properties.spawnX, 0, WorldSettings.Properties.spawnZ);
      }

      @Override
      public List<ParameterPoint> getParameterPoints() {
         return List.of(new ParameterPoint(FULL_RANGE, FULL_RANGE, FULL_RANGE, FULL_RANGE, FULL_RANGE, FULL_RANGE, 0L));
      }
   };

   public static final Codec<SpawnType> CODEC = StringRepresentable.fromEnum(SpawnType::values);
   private String name;

   private SpawnType(String name) {
      this.name = name;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public abstract BlockPos getSearchCenter(GeneratorContext var1, WorldSettings.Properties var2);

   public abstract List<ParameterPoint> getParameterPoints();
}
