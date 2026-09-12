package com.onehumanawa.cfhcore.content.world.worldgen.feature.template.paste;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import com.onehumanawa.cfhcore.content.world.worldgen.feature.template.placement.TemplatePlacement;
import com.onehumanawa.cfhcore.content.world.worldgen.feature.template.template.TemplateContext;

public interface Paste {
   <T extends TemplateContext> boolean apply(LevelAccessor var1, T var2, BlockPos var3, Mirror var4, Rotation var5, TemplatePlacement<T> var6, PasteConfig var7);
}
