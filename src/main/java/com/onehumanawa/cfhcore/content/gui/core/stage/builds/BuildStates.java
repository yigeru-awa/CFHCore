package com.onehumanawa.cfhcore.content.gui.core.stage.builds;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class BuildStates {
   private BuildStates() {
   }

   public static CompoundTag toTag(String text) {
      String s = text.trim();
      int open = s.indexOf(91);
      String name = open < 0 ? s : s.substring(0, open);
      if (!name.contains(":")) {
         name = "minecraft:" + name;
      }

      CompoundTag tag = new CompoundTag();
      tag.putString("Name", name);
      if (open >= 0) {
         int close = s.lastIndexOf(93);
         String inner = s.substring(open + 1, close > open ? close : s.length());
         CompoundTag props = new CompoundTag();

         for(String pair : inner.split(",")) {
            int eq = pair.indexOf(61);
            if (eq > 0) {
               props.putString(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
            }
         }

         if (!props.isEmpty()) {
            tag.put("Properties", props);
         }
      }

      return tag;
   }

   public static @Nullable BlockState parse(String text) {
      CompoundTag tag = toTag(text);
      BlockState s = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag);
      return s.isAir() && !tag.getString("Name").endsWith("air") ? null : s;
   }
}
