package com.onehumanawa.cfhcore.content.world.preset.settings;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public class StructureSettings {
   public static final Codec<StructureSettings> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               Codec.unboundedMap(ResourceKey.codec(Registries.STRUCTURE_SET), StructureSettings.StructureSetEntry.CODEC)
                  .fieldOf("structures")
                  .forGetter(s -> s.entries)
            )
            .apply(instance, StructureSettings::new)
   );
   public Map<ResourceKey<StructureSet>, StructureSettings.StructureSetEntry> entries;

   public StructureSettings(Map<ResourceKey<StructureSet>, StructureSettings.StructureSetEntry> entries) {
      this.entries = new HashMap<>(entries);
   }

   public StructureSettings() {
      this(ImmutableMap.of());
   }

   public StructureSettings copy() {
      Map<ResourceKey<StructureSet>, StructureSettings.StructureSetEntry> entries = new HashMap<>();

      for (Entry<ResourceKey<StructureSet>, StructureSettings.StructureSetEntry> entry : this.entries.entrySet()) {
         entries.put(entry.getKey(), entry.getValue().copy());
      }

      return new StructureSettings(entries);
   }

   public static class StructureSetEntry {
      public static final Codec<StructureSettings.StructureSetEntry> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  Codec.INT.fieldOf("spacing").forGetter(s -> s.spacing),
                  Codec.INT.fieldOf("separation").forGetter(s -> s.separation),
                  Codec.INT.fieldOf("salt").forGetter(s -> s.salt),
                  Codec.BOOL.fieldOf("disabled").forGetter(s -> s.disabled)
               )
               .apply(instance, StructureSettings.StructureSetEntry::new)
      );
      public int spacing;
      public int separation;
      public int salt;
      public boolean disabled;

      public StructureSetEntry(int spacing, int separation, int salt, boolean disabled) {
         this.spacing = spacing;
         this.separation = separation;
         this.salt = salt;
         this.disabled = disabled;
      }

      public StructureSettings.StructureSetEntry copy() {
         return new StructureSettings.StructureSetEntry(this.spacing, this.separation, this.salt, this.disabled);
      }
   }
}
