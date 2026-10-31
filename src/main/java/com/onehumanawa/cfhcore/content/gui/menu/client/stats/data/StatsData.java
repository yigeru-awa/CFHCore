package com.onehumanawa.cfhcore.content.gui.menu.client.stats.data;

import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.StatsCounter;
import org.jetbrains.annotations.Nullable;

public final class StatsData {
   public static final StatsData EMPTY = new StatsData((UUID)null, "", Map.of(), Map.of(), 0L);
   private final @Nullable UUID player;
   private final String name;
   private final Map<String, Long> stats;
   private final Map<String, Long> extras;
   private final long atMs;
   private final Map<String, Map<ResourceLocation, Long>> byType = new HashMap();

   public StatsData(@Nullable UUID player, String name, Map<String, Long> stats, Map<String, Long> extras, long atMs) {
      this.player = player;
      this.name = name == null ? "" : name;
      this.stats = stats;
      this.extras = extras;
      this.atMs = atMs;
   }

   public static StatsData of(StatsCounter counter, @Nullable UUID player, String name, Map<String, Long> extras) {
      Map<String, Long> out = new HashMap(512);

      for(StatType<?> type : BuiltInRegistries.STAT_TYPE) {
         ResourceLocation typeId = BuiltInRegistries.STAT_TYPE.getKey(type);
         if (typeId != null) {
            collect(type, counter, typeId.toString(), out);
         }
      }

      return new StatsData(player, name, out, Map.copyOf(extras), System.currentTimeMillis());
   }

   private static <T> void collect(StatType<T> type, StatsCounter counter, String typeId, Map<String, Long> out) {
      Registry<T> registry = type.getRegistry();

      try {
         for(Stat<T> stat : type) {
            if (stat != null) {
               put(registry, stat.getValue(), counter.getValue(stat), typeId, out);
            }
         }
      } catch (ConcurrentModificationException var8) {
         for(T value : registry) {
            if (type.contains(value)) {
               put(registry, value, counter.getValue(type.get(value)), typeId, out);
            }
         }
      }

   }

   private static <T> void put(Registry<T> registry, T value, int n, String typeId, Map<String, Long> out) {
      if (n != 0) {
         ResourceLocation id = registry.getKey(value);
         if (id != null) {
            out.put(typeId + "|" + String.valueOf(id), (long)n);
         }

      }
   }

   public static StatsData of(StatsLink.Snapshot snapshot) {
      return new StatsData(snapshot.player(), snapshot.name(), Map.copyOf(snapshot.stats()), Map.copyOf(snapshot.extras()), snapshot.atMs());
   }

   public StatsData withExtras(Map<String, Long> newExtras) {
      return new StatsData(this.player, this.name, this.stats, Map.copyOf(newExtras), this.atMs);
   }

   public @Nullable UUID player() {
      return this.player;
   }

   public String name() {
      return this.name;
   }

   public long atMs() {
      return this.atMs;
   }

   public boolean isEmpty() {
      return this.stats.isEmpty();
   }

   public Map<String, Long> all() {
      return this.stats;
   }

   public Map<String, Long> extras() {
      return this.extras;
   }

   public long get(String key) {
      Long v = (Long)this.stats.get(key);
      return v == null ? 0L : v;
   }

   public long custom(String path) {
      return this.get(StatKeys.custom(path));
   }

   public boolean hasExtra(String name) {
      return this.extras.containsKey(name);
   }

   public long extra(String name) {
      Long v = (Long)this.extras.get(name);
      return v == null ? 0L : v;
   }

   public Map<ResourceLocation, Long> type(String typeId) {
      Map<ResourceLocation, Long> m = (Map)this.byType.get(typeId);
      if (m != null) {
         return m;
      } else {
         m = new LinkedHashMap();
         String prefix = typeId + "|";

         for(Map.Entry<String, Long> e : this.stats.entrySet()) {
            if (((String)e.getKey()).startsWith(prefix)) {
               ResourceLocation id = ResourceLocation.tryParse(((String)e.getKey()).substring(prefix.length()));
               if (id != null && (Long)e.getValue() != 0L) {
                  m.put(id, (Long)e.getValue());
               }
            }
         }

         this.byType.put(typeId, m);
         return m;
      }
   }

   public long total(String typeId) {
      long sum = 0L;

      for(long v : this.type(typeId).values()) {
         sum += v;
      }

      return sum;
   }

   public boolean sameNumbers(StatsData other) {
      return other != null && this.stats.equals(other.stats) && this.extras.equals(other.extras) && this.name.equals(other.name) && Objects.equals(this.player, other.player);
   }
}
