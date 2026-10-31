package com.onehumanawa.cfhcore.content.gui.menu.client.stats.data;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class StatKeys {
   public static final String CUSTOM = "minecraft:custom";
   public static final String MINED = "minecraft:mined";
   public static final String CRAFTED = "minecraft:crafted";
   public static final String USED = "minecraft:used";
   public static final String BROKEN = "minecraft:broken";
   public static final String PICKED_UP = "minecraft:picked_up";
   public static final String DROPPED = "minecraft:dropped";
   public static final String KILLED = "minecraft:killed";
   public static final String KILLED_BY = "minecraft:killed_by";

   public static String key(String type, ResourceLocation value) {
      return type + "|" + String.valueOf(value);
   }

   public static String custom(String path) {
      return "minecraft:custom|minecraft:" + path;
   }

   public static String custom(ResourceLocation id) {
      return "minecraft:custom|" + String.valueOf(id);
   }

   public static @Nullable Parsed parse(String key) {
      int bar = key.indexOf(124);
      return bar > 0 && bar != key.length() - 1 ? new Parsed(key.substring(0, bar), key.substring(bar + 1)) : null;
   }

   private StatKeys() {
   }

   public static record Parsed(String type, String value) {
      public @Nullable ResourceLocation id() {
         return ResourceLocation.tryParse(this.value);
      }

      public boolean total() {
         return "*".equals(this.value);
      }
   }
}
