package com.onehumanawa.cfhcore.content.world.worldgen.feature.template;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.structure.Structure;

public class StructureUtils {
   public static boolean hasOvergroundStructure(HolderLookup<Structure> structures, ChunkAccess chunk) {
      Map<Structure, LongSet> references = chunk.getAllReferences();

      for (Structure structure : structures.listElements().map(Holder::value).filter(structurex -> structurex.step() == Decoration.SURFACE_STRUCTURES).toList()) {
         LongSet refs = references.get(structure);
         if (refs != null && refs.size() > 0) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasStructure(ChunkAccess chunk, Structure structure) {
      LongSet refs = chunk.getAllReferences().get(structure);
      return refs != null && refs.size() > 0;
   }
}
