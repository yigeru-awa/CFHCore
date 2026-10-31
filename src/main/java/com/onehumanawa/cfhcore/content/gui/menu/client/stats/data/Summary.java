package com.onehumanawa.cfhcore.content.gui.menu.client.stats.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public final class Summary {
   private static final TagKey<EntityType<?>> BOSSES;
   public final StatsData data;
   public final long played;
   public final long away;
   public final long sinceDeath;
   public final long sinceRest;
   public final long sessions;
   public final long firstSeenMs;
   public final long lastSeenMs;
   public final long deaths;
   public final long damageTaken;
   public final long damageDealt;
   public final long damageBlocked;
   public final long damageAbsorbed;
   public final long damageResisted;
   public final @Nullable Top killer;
   public final long mobKills;
   public final long playerKills;
   public final long hostileKills;
   public final @Nullable Top prey;
   public final List<Way> ways;
   public final long distance;
   public final long jumps;
   public final long mined;
   public final long placed;
   public final long crafted;
   public final long used;
   public final long broken;
   public final long pickedUp;
   public final long dropped;
   public final @Nullable Top mostMined;
   public final @Nullable Top mostPlaced;
   public final @Nullable Top mostCrafted;
   public final @Nullable Top mostUsed;
   public final @Nullable Top mostBroken;
   public final @Nullable Top mostPickedUp;
   public final long bred;
   public final long fish;
   public final long trades;
   public final long raidsWon;
   public final long enchanted;
   public final long slept;

   public Summary(StatsData d) {
      this.data = d;
      this.played = d.custom("play_time");
      this.sinceDeath = d.custom("time_since_death");
      this.sinceRest = d.custom("time_since_rest");
      this.away = d.hasExtra("afk_ticks") ? Math.min(this.played, Math.max(0L, d.extra("afk_ticks"))) : -1L;
      this.sessions = d.hasExtra("sessions") ? d.extra("sessions") : -1L;
      this.firstSeenMs = d.extra("first_seen_ms");
      this.lastSeenMs = d.extra("last_seen_ms");
      this.deaths = d.custom("deaths");
      this.damageTaken = d.custom("damage_taken");
      this.damageDealt = d.custom("damage_dealt");
      this.damageBlocked = d.custom("damage_blocked_by_shield");
      this.damageAbsorbed = d.custom("damage_absorbed");
      this.damageResisted = d.custom("damage_resisted");
      this.killer = top(d.type("minecraft:killed_by"));
      this.mobKills = d.custom("mob_kills");
      this.playerKills = d.custom("player_kills");
      long hostile = 0L;

      for(Map.Entry<ResourceLocation, Long> e : d.type("minecraft:killed").entrySet()) {
         if (hostile((ResourceLocation)e.getKey())) {
            hostile += (Long)e.getValue();
         }
      }

      this.hostileKills = hostile;
      this.prey = top(d.type("minecraft:killed"));
      List<Way> all = new ArrayList();
      long far = 0L;

      for(ResourceLocation id : distanceStats(d)) {
         long cm = d.get(StatKeys.custom(id));
         all.add(new Way(id, cm));
         far += cm;
      }

      all.sort(Comparator.comparingLong(Way::cm).reversed().thenComparing((w) -> w.id().toString()));
      this.ways = List.copyOf(all);
      this.distance = far;
      this.jumps = d.custom("jump");
      this.mined = d.total("minecraft:mined");
      this.mostMined = top(d.type("minecraft:mined"));
      long blocksPlaced = 0L;
      long itemsUsed = 0L;
      Top topPlaced = null;
      Top topUsed = null;

      for(Map.Entry<ResourceLocation, Long> e : d.type("minecraft:used").entrySet()) {
         Item item = (Item)BuiltInRegistries.ITEM.getOptional((ResourceLocation)e.getKey()).orElse(null);
         if (blockOf(item) != null) {
            blocksPlaced += (Long)e.getValue();
            if (topPlaced == null || (Long)e.getValue() > topPlaced.value()) {
               topPlaced = new Top((ResourceLocation)e.getKey(), (Long)e.getValue());
            }
         } else {
            itemsUsed += (Long)e.getValue();
            if (topUsed == null || (Long)e.getValue() > topUsed.value()) {
               topUsed = new Top((ResourceLocation)e.getKey(), (Long)e.getValue());
            }
         }
      }

      this.placed = blocksPlaced;
      this.used = itemsUsed;
      this.mostPlaced = topPlaced;
      this.mostUsed = topUsed;
      this.crafted = d.total("minecraft:crafted");
      this.mostCrafted = top(d.type("minecraft:crafted"));
      this.broken = d.total("minecraft:broken");
      this.mostBroken = top(d.type("minecraft:broken"));
      this.pickedUp = d.total("minecraft:picked_up");
      this.mostPickedUp = top(d.type("minecraft:picked_up"));
      this.dropped = d.total("minecraft:dropped");
      this.bred = d.custom("animals_bred");
      this.fish = d.custom("fish_caught");
      this.trades = d.custom("traded_with_villager");
      this.raidsWon = d.custom("raid_win");
      this.enchanted = d.custom("enchant_item");
      this.slept = d.custom("sleep_in_bed");
   }

   public long active() {
      return this.away < 0L ? this.played : Math.max(0L, this.played - this.away);
   }

   public long killsPerDeath() {
      return this.deaths <= 0L ? -1L : Math.round((double)(this.mobKills + this.playerKills) * (double)100.0F / (double)this.deaths);
   }

   public @Nullable Way longestWay() {
      return !this.ways.isEmpty() && ((Way)this.ways.get(0)).cm() > 0L ? (Way)this.ways.get(0) : null;
   }

   private static @Nullable Top top(Map<ResourceLocation, Long> values) {
      Top best = null;

      for(Map.Entry<ResourceLocation, Long> e : values.entrySet()) {
         if ((Long)e.getValue() > 0L && (best == null || (Long)e.getValue() > best.value() || (Long)e.getValue() == best.value() && ((ResourceLocation)e.getKey()).compareTo(best.id()) < 0)) {
            best = new Top((ResourceLocation)e.getKey(), (Long)e.getValue());
         }
      }

      return best;
   }

   public static boolean hostile(ResourceLocation entityId) {
      EntityType<?> type = (EntityType)BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
      if (type == null) {
         return false;
      } else if (type.getCategory() == MobCategory.MONSTER) {
         return true;
      } else {
         try {
            return type.is(BOSSES);
         } catch (RuntimeException var3) {
            return false;
         }
      }
   }

   public static @Nullable Block blockOf(@Nullable Item item) {
      if (item instanceof BlockItem blockItem) {
         Block block = blockItem.getBlock();
         if (block != null && item.getDescriptionId().equals(block.getDescriptionId())) {
            return item.components().has(DataComponents.FOOD) ? null : block;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static Set<ResourceLocation> distanceStats(StatsData d) {
      Set<ResourceLocation> out = new LinkedHashSet();

      for(ResourceLocation id : BuiltInRegistries.CUSTOM_STAT) {
         if (StatKind.ofCustom(id) == StatKind.DISTANCE) {
            out.add(id);
         }
      }

      for(ResourceLocation id : d.type("minecraft:custom").keySet()) {
         if (!BuiltInRegistries.CUSTOM_STAT.containsKey(id) && id.getPath().endsWith("_one_cm")) {
            out.add(id);
         }
      }

      return out;
   }

   static {
      BOSSES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses"));
   }

   public static record Top(ResourceLocation id, long value) {
   }

   public static record Way(ResourceLocation id, long cm) {
   }
}
