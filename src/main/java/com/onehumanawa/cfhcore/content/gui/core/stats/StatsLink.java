package com.onehumanawa.cfhcore.content.gui.core.stats;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

public final class StatsLink {
   public static final String EXTRA_AFK_TICKS = "afk_ticks";
   public static final String EXTRA_FIRST_SEEN_MS = "first_seen_ms";
   public static final String EXTRA_LAST_SEEN_MS = "last_seen_ms";
   public static final String EXTRA_SESSIONS = "sessions";
   public static final String ALL = "*";
   private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList();
   private static volatile @Nullable UUID focus;

   public static String key(String type, String value) {
      return type + "|" + value;
   }

   public static String total(String type) {
      return key(type, "*");
   }

   public static void provide(Provider provider) {
      PROVIDERS.add(provider);
   }

   public static Optional<Provider> provider() {
      for(Provider p : PROVIDERS) {
         if (p.available()) {
            return Optional.of(p);
         }
      }

      return Optional.empty();
   }

   public static void focus(@Nullable UUID player) {
      focus = player;
   }

   public static @Nullable UUID takeFocus() {
      UUID f = focus;
      focus = null;
      return f;
   }

   private StatsLink() {
   }

   public static record PlayerRef(UUID id, String name, boolean online) {
   }

   public static record Snapshot(UUID player, String name, Map<String, Long> stats, Map<String, Long> extras, long atMs) {
      public long stat(String key) {
         return (Long)this.stats.getOrDefault(key, 0L);
      }

      public long extra(String key) {
         return (Long)this.extras.getOrDefault(key, 0L);
      }
   }

   public static record Row(UUID player, String name, long value, int rank) {
   }

   public static record Board(String key, List<Row> rows, int total, @Nullable Row self) {
   }

   public interface Provider {
      boolean available();

      void players(Consumer<List<PlayerRef>> var1);

      void stats(UUID var1, Consumer<Optional<Snapshot>> var2);

      void board(String var1, int var2, int var3, Consumer<Board> var4);

      default boolean ranksTotals() {
         return false;
      }
   }
}
