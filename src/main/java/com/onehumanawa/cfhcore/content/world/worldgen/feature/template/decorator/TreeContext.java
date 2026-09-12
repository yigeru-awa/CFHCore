package com.onehumanawa.cfhcore.content.world.worldgen.feature.template.decorator;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import com.onehumanawa.cfhcore.content.world.worldgen.feature.template.template.TemplateContext;

public class TreeContext implements TemplateContext {
   private Set<BlockPos> logs;
   private Set<BlockPos> leaves;

   public Set<BlockPos> logs() {
      return (Set<BlockPos>)(this.logs != null ? this.logs : ImmutableSet.of());
   }

   public Set<BlockPos> leaves() {
      return (Set<BlockPos>)(this.leaves != null ? this.leaves : ImmutableSet.of());
   }

   @Override
   public void recordState(BlockPos pos, BlockState state) {
      if (state.is(BlockTags.LEAVES)) {
         this.leaves = safeAdd(this.leaves, pos);
      } else {
         if (state.is(BlockTags.LOGS)) {
            this.logs = safeAdd(this.logs, pos);
         }
      }
   }

   private static Set<BlockPos> safeAdd(Set<BlockPos> list, BlockPos pos) {
      if (list == null) {
         list = new HashSet<>();
      }

      list.add(pos.immutable());
      return list;
   }
}
