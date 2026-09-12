package com.onehumanawa.cfhcore.content.world.mixin;

import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.Beardifier.Rigid;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Beardifier.class})
public interface BeardifierAccessor {
   @Accessor("pieceIterator")
   ObjectListIterator<Rigid> getPieceIterator();

   @Accessor("junctionIterator")
   ObjectListIterator<JigsawJunction> getJunctionIterator();
}
