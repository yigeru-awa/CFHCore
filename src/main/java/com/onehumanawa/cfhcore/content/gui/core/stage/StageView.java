package com.onehumanawa.cfhcore.content.gui.core.stage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public final class StageView implements BlockAndTintGetter {
   private final StageLevel level;

   public StageView(StageLevel level) {
      this.level = level;
   }

   public float getShade(Direction direction, boolean shade) {
      return this.level.getShade(direction, shade);
   }

   public LevelLightEngine getLightEngine() {
      return this.level.getLightEngine();
   }

   public int getBrightness(LightLayer layer, BlockPos pos) {
      return this.level.getBrightness(layer, pos);
   }

   public int getRawBrightness(BlockPos pos, int amount) {
      return this.level.getRawBrightness(pos, amount);
   }

   public boolean canSeeSky(BlockPos pos) {
      return this.level.canSeeSky(pos);
   }

   public int getBlockTint(BlockPos pos, ColorResolver resolver) {
      return this.level.getBlockTint(pos, resolver);
   }

   public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
      return this.level.getBlockEntity(pos);
   }

   public BlockState getBlockState(BlockPos pos) {
      return this.level.getBlockState(pos);
   }

   public FluidState getFluidState(BlockPos pos) {
      return this.level.getFluidState(pos);
   }

   public int getHeight() {
      return this.level.getHeight();
   }

   public int getMinBuildHeight() {
      return this.level.getMinBuildHeight();
   }
}
