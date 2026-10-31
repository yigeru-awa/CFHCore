package com.onehumanawa.cfhcore.content.gui.core.stage.mesh;

import com.onehumanawa.cfhcore.content.gui.core.stage.StageTrouble;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

final class StageColors extends BlockColors {
   private static final Set<Block> BROKEN = ConcurrentHashMap.newKeySet();
   private static volatile boolean tintsBroken;
   private static final int WATER = -12618012;
   private final BlockColors game;

   StageColors(BlockColors game) {
      this.game = game;
   }

   static boolean broken(Block block) {
      return BROKEN.contains(block);
   }

   public int getColor(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
      Block block = state.getBlock();
      if (!BROKEN.contains(block)) {
         try {
            return this.game.getColor(state, level, pos, tintIndex);
         } catch (RuntimeException e) {
            BROKEN.add(block);
            StageTrouble.once("the colour of " + String.valueOf(BuiltInRegistries.BLOCK.getKey(block)), "coloured with the scene's own tints", e);
         }
      }

      return fallback(state, level, pos);
   }

   static int fallback(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
      Block b = state.getBlock();
      if (b == Blocks.BIRCH_LEAVES) {
         return FoliageColor.getBirchColor();
      } else if (b == Blocks.SPRUCE_LEAVES) {
         return FoliageColor.getEvergreenColor();
      } else if (b == Blocks.LILY_PAD) {
         return level != null && pos != null ? 2129968 : 7455580;
      } else {
         MapColor map = b.defaultMapColor();
         ColorResolver resolver;
         int plain;
         if (!state.getFluidState().is(FluidTags.WATER) && b != Blocks.WATER_CAULDRON && map != MapColor.WATER) {
            if (!state.is(BlockTags.LEAVES) && !(b instanceof LeavesBlock) && !(b instanceof VineBlock)) {
               if (map != MapColor.GRASS && map != MapColor.PLANT) {
                  return -1;
               }

               resolver = BiomeColors.GRASS_COLOR_RESOLVER;
               plain = GrassColor.getDefaultColor();
            } else {
               resolver = BiomeColors.FOLIAGE_COLOR_RESOLVER;
               plain = FoliageColor.getDefaultColor();
            }
         } else {
            resolver = BiomeColors.WATER_COLOR_RESOLVER;
            plain = -12618012;
         }

         if (level != null && pos != null && !tintsBroken) {
            try {
               return level.getBlockTint(pos, resolver);
            } catch (RuntimeException e) {
               tintsBroken = true;
               StageTrouble.once("the biome colours", "the game's default colours of grass, leaves and water", e);
               return plain;
            }
         } else {
            return plain;
         }
      }
   }
}
