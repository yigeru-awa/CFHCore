package com.onehumanawa.cfhcore.content.gui.menu.client.stats.data;

import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.AfkClock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action;
import net.minecraft.stats.StatsCounter;
import org.jetbrains.annotations.Nullable;

public final class StatsFeed {
   public static final long REFRESH_MS = 3000L;
   public static final long SLOW_MS = 5000L;
   private final boolean preview;
   private final StatsCounter counter;
   private final UUID selfId;
   private final String selfName;
   private StatsData self;
   private boolean answered;
   private long lastAskMs;
   private long waitingSinceMs;
   private @Nullable Map<String, Long> serverExtras;
   private long extrasAskedMs;
   private boolean selfAfkLocal;
   private StatsLink.@Nullable PlayerRef subject;
   private StatsData other;
   private boolean otherAnswered;
   private boolean otherMissing;
   private long otherAskMs;
   private int subjectToken;
   private int version;
   private boolean closed;

   public StatsFeed(StatsCounter counter, boolean preview) {
      this.self = StatsData.EMPTY;
      this.other = StatsData.EMPTY;
      Minecraft mc = Minecraft.getInstance();
      this.counter = counter;
      this.preview = preview;
      this.selfId = localId();
      this.selfName = mc.player != null ? mc.player.getGameProfile().getName() : mc.getUser().getName();
      if (preview) {
         this.self = SampleStats.self();
         this.answered = true;
      } else {
         this.self = this.read();
      }

   }

   public static UUID localId() {
      Minecraft mc = Minecraft.getInstance();
      return mc.player != null ? mc.player.getUUID() : mc.getUser().getProfileId();
   }

   public void open() {
      this.waitingSinceMs = System.currentTimeMillis();
      this.ask();
   }

   public void tick() {
      if (!this.closed) {
         long now = System.currentTimeMillis();
         if (now - this.lastAskMs >= 3000L) {
            this.ask();
         }

         if (this.subject != null && now - this.otherAskMs >= 3000L) {
            this.askOther();
         }

      }
   }

   private void ask() {
      this.lastAskMs = System.currentTimeMillis();
      if (!this.preview) {
         ClientPacketListener connection = Minecraft.getInstance().getConnection();
         if (connection != null) {
            connection.send(new ServerboundClientCommandPacket(Action.REQUEST_STATS));
         }

         provider().ifPresent((p) -> {
            if (this.extrasAskedMs == 0L) {
               this.extrasAskedMs = this.lastAskMs;
            }

            try {
               p.stats(this.selfId, (answer) -> {
                  if (!this.closed) {
                     if (!answer.isEmpty() || this.serverExtras == null) {
                        Map<String, Long> extras = (Map<String, Long>)answer.map(StatsLink.Snapshot::extras).orElse(null);
                        this.serverExtras = (Map<String, Long>)(extras == null ? Map.of() : new LinkedHashMap<>(extras));
                        this.refreshSelf();
                     }
                  }
               });
            } catch (RuntimeException e) {
               CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the server's statistics could not be asked for: {}", e.toString());
            }

         });
      }
   }

   public void onGameStats() {
      if (!this.preview && !this.closed) {
         boolean first = !this.answered;
         this.answered = true;
         this.refreshSelf();
         if (first && this.subject == null) {
            ++this.version;
         }

      }
   }

   private void refreshSelf() {
      StatsData fresh = this.read();
      boolean changed = !fresh.sameNumbers(this.self);
      this.self = fresh;
      if (changed && this.subject == null) {
         ++this.version;
      }

   }

   private StatsData read() {
      Map<String, Long> extras = new LinkedHashMap();
      if (this.serverExtras != null) {
         extras.putAll(this.serverExtras);
      }

      this.selfAfkLocal = this.localAfk();
      if (this.selfAfkLocal) {
         extras.put("afk_ticks", AfkClock.awayTicks());
      }

      StatsData data = StatsData.of(this.counter, this.selfId, this.selfName, extras);
      if (!extras.containsKey("sessions") && !data.isEmpty()) {
         extras.put("sessions", data.custom("leave_game") + 1L);
         return data.withExtras(extras);
      } else {
         return data;
      }
   }

   private boolean localAfk() {
      if (!this.preview && AfkClock.counting()) {
         if (this.serverExtras != null) {
            return !this.serverExtras.containsKey("afk_ticks");
         } else {
            return provider().isEmpty() || this.extrasAskedMs > 0L && System.currentTimeMillis() - this.extrasAskedMs >= 5000L;
         }
      } else {
         return false;
      }
   }

   public void view(StatsLink.@Nullable PlayerRef player) {
      StatsLink.PlayerRef next = player != null && player.id().equals(this.selfId) ? null : player;
      if (next != null || this.subject != null) {
         if (next == null || this.subject == null || !next.id().equals(this.subject.id())) {
            this.subject = next;
            ++this.subjectToken;
            this.other = StatsData.EMPTY;
            this.otherAnswered = false;
            this.otherMissing = false;
            this.waitingSinceMs = System.currentTimeMillis();
            ++this.version;
            if (next != null) {
               this.askOther();
            }

         }
      }
   }

   private void askOther() {
      this.otherAskMs = System.currentTimeMillis();
      StatsLink.PlayerRef who = this.subject;
      if (who != null) {
         int token = this.subjectToken;
         Optional<StatsLink.Provider> provider = provider();
         if (provider.isEmpty()) {
            this.otherAnswered = true;
            this.otherMissing = true;
            ++this.version;
         } else {
            try {
               ((StatsLink.Provider)provider.get()).stats(who.id(), (answer) -> {
                  if (!this.closed && token == this.subjectToken) {
                     boolean was = this.otherAnswered;
                     this.otherAnswered = true;
                     if (answer.isEmpty()) {
                        if (!this.otherMissing || !was) {
                           ++this.version;
                        }

                        this.otherMissing = true;
                     } else {
                        StatsData fresh = StatsData.of((StatsLink.Snapshot)answer.get());
                        if (!was || this.otherMissing || !fresh.sameNumbers(this.other)) {
                           ++this.version;
                        }

                        this.otherMissing = false;
                        this.other = fresh;
                     }
                  }
               });
            } catch (RuntimeException e) {
               CFHGuiMenu.LOGGER.warn("[CFHGui Menu] a player's statistics could not be asked for: {}", e.toString());
               this.otherAnswered = true;
               this.otherMissing = true;
               ++this.version;
            }

         }
      }
   }

   public boolean preview() {
      return this.preview;
   }

   public static Optional<StatsLink.Provider> provider() {
      try {
         return StatsLink.provider();
      } catch (RuntimeException var1) {
         return Optional.empty();
      }
   }

   public UUID selfId() {
      return this.selfId;
   }

   public String selfName() {
      return this.selfName;
   }

   public StatsLink.@Nullable PlayerRef subject() {
      return this.subject;
   }

   public String shownName() {
      return this.subject != null ? this.subject.name() : this.selfName;
   }

   public UUID shownId() {
      return this.subject != null ? this.subject.id() : this.selfId;
   }

   public StatsData shown() {
      return this.subject != null ? this.other : this.self;
   }

   public StatsData self() {
      return this.self;
   }

   public boolean loading() {
      if (this.subject != null) {
         return !this.otherAnswered;
      } else {
         return !this.preview && !this.answered && this.self.isEmpty();
      }
   }

   public boolean missing() {
      return this.subject != null && this.otherAnswered && this.otherMissing;
   }

   public boolean slow() {
      return this.loading() && System.currentTimeMillis() - this.waitingSinceMs >= 5000L;
   }

   public boolean answered() {
      return this.preview || this.answered;
   }

   public int version() {
      return this.version;
   }

   public boolean ownAfk() {
      return this.subject == null && this.selfAfkLocal;
   }

   public void close() {
      this.closed = true;
   }

   public void resume() {
      if (this.closed) {
         this.closed = false;
         this.waitingSinceMs = System.currentTimeMillis();
         this.ask();
         if (this.subject != null) {
            this.askOther();
         }

      }
   }
}
