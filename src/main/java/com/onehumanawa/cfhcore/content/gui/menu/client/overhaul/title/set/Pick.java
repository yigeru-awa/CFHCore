package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.set;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public final class Pick {
   private static final Map<String, Optional<BlockState>> CACHE = new HashMap();

   public static BlockState of(String... names) {
      for(String name : names) {
         BlockState s = state(name);
         if (s != null) {
            return s;
         }
      }

      return Blocks.AIR.defaultBlockState();
   }

   public static boolean has(String name) {
      return state(name) != null;
   }

   public static synchronized @Nullable BlockState state(String name) {
      Optional<BlockState> cached = (Optional)CACHE.get(name);
      if (cached != null) {
         return (BlockState)cached.orElse(null);
      } else {
         BlockState parsed = parse(name);
         CACHE.put(name, Optional.ofNullable(parsed));
         return parsed;
      }
   }

   private static @Nullable BlockState parse(String name) {
      String text = name.trim();
      int open = text.indexOf(91);
      String id = open < 0 ? text : text.substring(0, open);
      ResourceLocation key = ResourceLocation.tryParse(id.contains(":") ? id : "minecraft:" + id);
      if (key == null) {
         return null;
      } else {
         Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(key);
         if (block.isEmpty()) {
            CFHGui.LOGGER.debug("[CFHGui] title scene: no block {} here, its stand-in is used", key);
            return null;
         } else {
            BlockState state = ((Block)block.get()).defaultBlockState();
            if (open >= 0) {
               int close = text.lastIndexOf(93);
               String props = text.substring(open + 1, close > open ? close : text.length());

               for(String pair : props.split(",")) {
                  int eq = pair.indexOf(61);
                  if (eq > 0) {
                     Property<?> property = state.getBlock().getStateDefinition().getProperty(pair.substring(0, eq).trim());
                     if (property != null) {
                        state = with(state, property, pair.substring(eq + 1).trim().toLowerCase(Locale.ROOT));
                     }
                  }
               }
            }

            return state;
         }
      }
   }

   private static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> property, String value) {
      return (BlockState)property.getValue(value).map((v) -> (BlockState)state.setValue(property, v)).orElse(state);
   }

   static synchronized void clear() {
      CACHE.clear();
   }

   private Pick() {
   }
}
