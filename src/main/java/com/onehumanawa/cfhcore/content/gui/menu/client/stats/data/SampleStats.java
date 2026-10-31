package com.onehumanawa.cfhcore.content.gui.menu.client.stats.data;

import com.onehumanawa.cfhcore.content.gui.core.client.DevHarness;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;

public final class SampleStats {
   private static final long HOUR = 3600000L;
   private static final long DAY = 86400000L;
   private static final Object[][] PEOPLE = new Object[][]{{"Mossback", 1.62, true}, {"Quillfeather", 1.18, true}, {"TinLantern", 0.74, true}, {"Brackenfoot", 0.52, false}, {"Saltwick", 0.41, false}, {"OldHarrow", 0.33, false}, {"Fennelseed", 0.21, true}, {"Cindermoth", 0.12, false}, {"Waxwing", 0.04, false}};
   private static boolean registered;
   private static final Map<String, Long> BASE = new LinkedHashMap();

   public static boolean on() {
      return DevHarness.sampleData() || Boolean.getBoolean("cfhgui.stats.sample");
   }

   public static boolean providerOn() {
      if (Boolean.getBoolean("cfhgui.stats.sampleProvider")) {
         return true;
      } else {
         return DevHarness.sampleData() && Minecraft.getInstance().level == null;
      }
   }

   public static synchronized void register() {
      if (!registered) {
         registered = true;
         StatsLink.provide(new Provider());
      }
   }

   public static UUID id(String name) {
      return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
   }

   public static StatsData self() {
      return of(StatsFeed.localId(), Minecraft.getInstance().getUser().getName(), (double)1.0F);
   }

   public static StatsData of(UUID player, String name, double scale) {
      Map<String, Long> stats = new HashMap();
      long who = player.getLeastSignificantBits();

      for(Map.Entry<String, Long> e : BASE.entrySet()) {
         String key = (String)e.getKey();
         boolean since = key.endsWith("time_since_death") || key.endsWith("time_since_rest");
         double nudge = scale == (double)1.0F ? (double)1.0F : 0.7 + (double)(((long)key.hashCode() ^ who) & 1023L) / (double)1023.0F * 0.6;
         long v = Math.round((double)(Long)e.getValue() * (since ? nudge : scale * nudge));
         if (v > 0L) {
            stats.put(key, v);
         }
      }

      long now = System.currentTimeMillis();
      Map<String, Long> extras = new LinkedHashMap();
      extras.put("afk_ticks", Math.round((double)1128000.0F * scale));
      extras.put("sessions", Math.max(1L, Math.round((double)42.0F * scale)));
      extras.put("first_seen_ms", now - Math.round((double)1.2096E10F * Math.min((double)1.0F, scale + 0.2)));
      extras.put("last_seen_ms", now - Math.round((double)1.08E7F / Math.max(0.05, scale)));
      return new StatsData(player, name, Map.copyOf(stats), extras, now);
   }

   private static void custom(String path, long value) {
      BASE.put(StatKeys.custom(path), value);
   }

   private static void put(String type, Object... idsAndValues) {
      for(int i = 0; i + 1 < idsAndValues.length; i += 2) {
         BASE.put(type + "|minecraft:" + String.valueOf(idsAndValues[i]), ((Number)idsAndValues[i + 1]).longValue());
      }

   }

   private SampleStats() {
   }

   static {
      custom("play_time", 5486400L);
      custom("total_world_time", 6104800L);
      custom("time_since_death", 187200L);
      custom("time_since_rest", 26400L);
      custom("sneak_time", 96240L);
      custom("leave_game", 41L);
      custom("deaths", 23L);
      custom("mob_kills", 2399L);
      custom("player_kills", 4L);
      custom("damage_dealt", 248600L);
      custom("damage_dealt_absorbed", 240L);
      custom("damage_dealt_resisted", 1220L);
      custom("damage_taken", 61240L);
      custom("damage_blocked_by_shield", 8420L);
      custom("damage_absorbed", 3960L);
      custom("damage_resisted", 12880L);
      custom("jump", 48213L);
      custom("drop", 612L);
      custom("walk_one_cm", 41256300L);
      custom("sprint_one_cm", 28731000L);
      custom("fly_one_cm", 9870400L);
      custom("aviate_one_cm", 6420000L);
      custom("boat_one_cm", 3310000L);
      custom("horse_one_cm", 2150000L);
      custom("swim_one_cm", 1204500L);
      custom("minecart_one_cm", 880000L);
      custom("crouch_one_cm", 640200L);
      custom("fall_one_cm", 512300L);
      custom("climb_one_cm", 301800L);
      custom("walk_on_water_one_cm", 210000L);
      custom("walk_under_water_one_cm", 96000L);
      custom("strider_one_cm", 12400L);
      custom("animals_bred", 184L);
      custom("fish_caught", 97L);
      custom("talked_to_villager", 412L);
      custom("traded_with_villager", 268L);
      custom("raid_trigger", 4L);
      custom("raid_win", 3L);
      custom("enchant_item", 76L);
      custom("sleep_in_bed", 142L);
      custom("open_chest", 5204L);
      custom("open_barrel", 1112L);
      custom("open_enderchest", 318L);
      custom("open_shulker_box", 206L);
      custom("interact_with_crafting_table", 2310L);
      custom("interact_with_furnace", 1490L);
      custom("interact_with_blast_furnace", 262L);
      custom("interact_with_smoker", 148L);
      custom("interact_with_anvil", 94L);
      custom("interact_with_grindstone", 37L);
      custom("interact_with_smithing_table", 12L);
      custom("interact_with_stonecutter", 221L);
      custom("interact_with_brewingstand", 83L);
      custom("interact_with_beacon", 9L);
      custom("interact_with_loom", 6L);
      custom("interact_with_cartography_table", 14L);
      custom("interact_with_lectern", 5L);
      custom("interact_with_campfire", 28L);
      custom("inspect_hopper", 141L);
      custom("inspect_dispenser", 22L);
      custom("inspect_dropper", 17L);
      custom("play_record", 12L);
      custom("play_noteblock", 64L);
      custom("tune_noteblock", 118L);
      custom("bell_ring", 31L);
      custom("target_hit", 18L);
      custom("eat_cake_slice", 9L);
      custom("pot_flower", 21L);
      custom("fill_cauldron", 7L);
      custom("use_cauldron", 11L);
      custom("clean_armor", 2L);
      custom("clean_shulker_box", 3L);
      custom("trigger_trapped_chest", 4L);
      put("minecraft:mined", "stone", 12430, "deepslate", 8214, "dirt", 4120, "netherrack", 3980, "grass_block", 2260, "oak_log", 2144, "cobblestone", 1876, "sand", 1420, "oak_leaves", 1310, "sugar_cane", 1012, "gravel", 986, "spruce_log", 930, "wheat", 844, "coal_ore", 742, "andesite", 688, "granite", 612, "diorite", 540, "iron_ore", 518, "tuff", 492, "deepslate_iron_ore", 364, "obsidian", 212, "copper_ore", 198, "deepslate_diamond_ore", 127, "redstone_ore", 96, "gold_ore", 88, "lapis_ore", 63, "diamond_ore", 41, "emerald_ore", 22, "ancient_debris", 19, "short_grass", 1604, "fire", 12);
      put("minecraft:used", "cobblestone", 6120, "oak_planks", 4480, "stone_bricks", 3902, "torch", 2214, "dirt", 1860, "glass", 940, "wheat_seeds", 844, "oak_stairs", 610, "rail", 480, "oak_slab", 402, "ladder", 220, "scaffolding", 188, "chest", 96, "furnace", 22, "crafting_table", 14, "diamond_pickaxe", 18420, "iron_pickaxe", 9210, "diamond_shovel", 5310, "diamond_sword", 4310, "diamond_axe", 3120, "firework_rocket", 1640, "bow", 1204, "bone_meal", 620, "golden_carrot", 502, "cooked_beef", 412, "bread", 388, "water_bucket", 214, "fishing_rod", 131, "ender_pearl", 87, "flint_and_steel", 44, "shears", 38);
      put("minecraft:crafted", "oak_planks", 5120, "stone_bricks", 4100, "stick", 2460, "torch", 2304, "firework_rocket", 1728, "iron_ingot", 1480, "glass", 980, "oak_stairs", 640, "rail", 512, "bread", 396, "cooked_beef", 420, "chest", 104, "hopper", 38, "piston", 24, "iron_pickaxe", 39, "diamond_pickaxe", 6, "shield", 5, "bow", 4, "enchanting_table", 1, "beacon", 1);
      put("minecraft:broken", "iron_pickaxe", 36, "stone_pickaxe", 14, "iron_sword", 11, "iron_shovel", 9, "shield", 4, "bow", 3, "diamond_pickaxe", 2, "fishing_rod", 2, "shears", 1);
      put("minecraft:picked_up", "cobblestone", 13902, "cobbled_deepslate", 8104, "dirt", 4300, "oak_log", 2160, "coal", 1620, "rotten_flesh", 1410, "arrow", 1240, "bone", 980, "raw_iron", 902, "wheat", 870, "string", 402, "gunpowder", 312, "diamond", 214, "ender_pearl", 96, "blaze_rod", 141, "oak_sapling", 264, "sand", 1420, "gravel", 990, "flint", 86);
      put("minecraft:dropped", "dirt", 612, "cobblestone", 420, "rotten_flesh", 388, "wheat_seeds", 240, "gravel", 96, "arrow", 12);
      put("minecraft:killed", "zombie", 412, "skeleton", 388, "creeper", 214, "spider", 196, "cow", 182, "enderman", 141, "chicken", 141, "blaze", 128, "slime", 96, "pig", 96, "sheep", 74, "drowned", 74, "wither_skeleton", 61, "zombified_piglin", 58, "pillager", 46, "piglin", 34, "witch", 22, "phantom", 17, "cod", 12, "player", 4, "ravager", 3, "ender_dragon", 1, "wither", 1, "iron_golem", 2);
      put("minecraft:killed_by", "creeper", 7, "skeleton", 4, "zombie", 3, "enderman", 2, "wither_skeleton", 2, "drowned", 1, "iron_golem", 1, "player", 1);
   }

   private static final class Provider implements StatsLink.Provider {
      private final Map<UUID, StatsData> people = new LinkedHashMap();
      private final Map<UUID, Boolean> online = new HashMap();

      private Map<UUID, StatsData> people() {
         UUID me = StatsFeed.localId();
         if (!this.people.containsKey(me)) {
            this.people.clear();
            this.online.clear();
            this.people.put(me, SampleStats.self());
            this.online.put(me, true);

            for(Object[] p : SampleStats.PEOPLE) {
               UUID id = SampleStats.id((String)p[0]);
               this.people.put(id, SampleStats.of(id, (String)p[0], (Double)p[1]));
               this.online.put(id, (Boolean)p[2]);
            }
         }

         return this.people;
      }

      public boolean available() {
         return SampleStats.providerOn();
      }

      public boolean ranksTotals() {
         return true;
      }

      public void players(Consumer<List<StatsLink.PlayerRef>> callback) {
         List<StatsLink.PlayerRef> out = new ArrayList();

         for(Map.Entry<UUID, StatsData> e : this.people().entrySet()) {
            out.add(new StatsLink.PlayerRef((UUID)e.getKey(), ((StatsData)e.getValue()).name(), (Boolean)this.online.getOrDefault(e.getKey(), false)));
         }

         out.sort(Comparator.comparing((StatsLink.PlayerRef p) -> !p.online()).thenComparing(StatsLink.PlayerRef::name, String.CASE_INSENSITIVE_ORDER));
         callback.accept(out);
      }

      public void stats(UUID player, Consumer<Optional<StatsLink.Snapshot>> callback) {
         StatsData d = (StatsData)this.people().get(player);
         callback.accept(d == null ? Optional.empty() : Optional.of(new StatsLink.Snapshot(player, d.name(), d.all(), d.extras(), System.currentTimeMillis())));
      }

      public void board(String key, int offset, int count, Consumer<StatsLink.Board> callback) {
         List<StatsLink.Row> all = new ArrayList();

         for(Map.Entry<UUID, StatsData> e : this.people().entrySet()) {
            long v = value((StatsData)e.getValue(), key);
            if (v > 0L) {
               all.add(new StatsLink.Row((UUID)e.getKey(), ((StatsData)e.getValue()).name(), v, 0));
            }
         }

         all.sort(Comparator.comparingLong(StatsLink.Row::value).reversed().thenComparing(StatsLink.Row::name, String.CASE_INSENSITIVE_ORDER));
         UUID me = StatsFeed.localId();
         List<StatsLink.Row> ranked = new ArrayList(all.size());
         StatsLink.Row self = null;

         for(int i = 0; i < all.size(); ++i) {
            StatsLink.Row r = new StatsLink.Row(((StatsLink.Row)all.get(i)).player(), ((StatsLink.Row)all.get(i)).name(), ((StatsLink.Row)all.get(i)).value(), i + 1);
            ranked.add(r);
            if (r.player().equals(me)) {
               self = r;
            }
         }

         int from = Math.max(0, Math.min(ranked.size(), offset));
         int to = Math.max(from, Math.min(ranked.size(), offset + count));
         callback.accept(new StatsLink.Board(key, List.copyOf(ranked.subList(from, to)), ranked.size(), self));
      }

      private static long value(StatsData d, String key) {
         StatKeys.Parsed parsed = StatKeys.parse(key);
         if (parsed == null) {
            return d.extra(key);
         } else {
            return parsed.total() ? d.total(parsed.type()) : d.get(key);
         }
      }
   }
}
