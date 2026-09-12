package com.onehumanawa.cfhcore.content.world.worldgen.feature.template.paste;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PasteConfig(int baseDepth, boolean pasteAir, boolean checkBounds, boolean replaceSolid, boolean updatePostPaste) {
   public static final Codec<PasteConfig> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               Codec.INT.optionalFieldOf("base_depth", Integer.valueOf(0)).forGetter(PasteConfig::baseDepth),
               Codec.BOOL.optionalFieldOf("paste_air", Boolean.valueOf(false)).forGetter(PasteConfig::pasteAir),
               Codec.BOOL.optionalFieldOf("check_bounds", Boolean.valueOf(false)).forGetter(PasteConfig::checkBounds),
               Codec.BOOL.optionalFieldOf("replace_solid", Boolean.valueOf(false)).forGetter(PasteConfig::replaceSolid),
               Codec.BOOL.optionalFieldOf("update_post_paste", Boolean.valueOf(false)).forGetter(PasteConfig::updatePostPaste)
            )
            .apply(instance, PasteConfig::new)
   );
   public static final PasteConfig DEFAULT = new PasteConfig(0, false, false, false, false);
}
