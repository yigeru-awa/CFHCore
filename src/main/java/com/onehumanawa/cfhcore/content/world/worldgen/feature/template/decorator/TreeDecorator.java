package com.onehumanawa.cfhcore.content.world.worldgen.feature.template.decorator;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator.Context;

record TreeDecorator(
   net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator decorator,
   net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator modifiedDecorator
) implements TemplateDecorator<TreeContext> {
   public static final MapCodec<TreeDecorator> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator.CODEC.fieldOf("decorator").forGetter(TreeDecorator::decorator),
               net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator.CODEC
                  .fieldOf("modified_decorator")
                  .forGetter(TreeDecorator::modifiedDecorator)
            )
            .apply(instance, TreeDecorator::new)
   );

   public net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator getDecorator(boolean modified) {
      return modified ? this.modifiedDecorator : this.decorator;
   }

   public void apply(LevelAccessor level, TreeContext buffer, RandomSource random, boolean modified) {
      Set<BlockPos> logs = buffer.logs();
      Set<BlockPos> leaves = buffer.leaves();
      if (!logs.isEmpty() && !leaves.isEmpty()) {
         Context ctx = new Context(level, (pos, state) -> level.setBlock(pos, state, 19), random, logs, leaves, ImmutableSet.of());
         this.getDecorator(modified).place(ctx);
      }
   }

   @Override
   public MapCodec<TreeDecorator> codec() {
      return CODEC;
   }
}
