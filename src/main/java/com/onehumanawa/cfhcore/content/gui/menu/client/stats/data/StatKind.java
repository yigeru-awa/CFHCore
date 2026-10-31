package com.onehumanawa.cfhcore.content.gui.menu.client.stats.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

public enum StatKind {
   COUNT,
   TIME,
   DISTANCE,
   TENTHS,
   RATIO,
   OWN;

   private static final int PROBE_A = 72000;
   private static final int PROBE_B = 1234;
   private static final Map<ResourceLocation, StatKind> KINDS = new ConcurrentHashMap();

   public static StatKind ofCustom(ResourceLocation id) {
      StatKind known = (StatKind)KINDS.get(id);
      if (known != null) {
         return known;
      } else {
         StatKind kind = probe(id);
         if (BuiltInRegistries.CUSTOM_STAT.containsKey(id)) {
            KINDS.put(id, kind);
         }

         return kind;
      }
   }

   private static StatKind probe(ResourceLocation id) {
      ResourceLocation registered = (ResourceLocation)BuiltInRegistries.CUSTOM_STAT.get(id);
      if (registered != null && Stats.CUSTOM.contains(registered)) {
         try {
            Stat<ResourceLocation> stat = Stats.CUSTOM.get(registered);
            String a = stat.format(72000);
            String b = stat.format(1234);
            if (same(StatFormatter.DEFAULT, a, b)) {
               return COUNT;
            } else if (same(StatFormatter.TIME, a, b)) {
               return TIME;
            } else if (same(StatFormatter.DISTANCE, a, b)) {
               return DISTANCE;
            } else {
               return same(StatFormatter.DIVIDE_BY_TEN, a, b) ? TENTHS : OWN;
            }
         } catch (RuntimeException var5) {
            return COUNT;
         }
      } else {
         return COUNT;
      }
   }

   private static boolean same(StatFormatter formatter, String a, String b) {
      return formatter.format(72000).equals(a) && formatter.format(1234).equals(b);
   }

   public static String own(ResourceLocation id, long value) {
      try {
         ResourceLocation registered = (ResourceLocation)BuiltInRegistries.CUSTOM_STAT.get(id);
         if (registered != null && Stats.CUSTOM.contains(registered)) {
            return Stats.CUSTOM.get(registered).format((int)Math.max(-2147483648L, Math.min(2147483647L, value)));
         }
      } catch (RuntimeException var4) {
      }

      return Long.toString(value);
   }
}
