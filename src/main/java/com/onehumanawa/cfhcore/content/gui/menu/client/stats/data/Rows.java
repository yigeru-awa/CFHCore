package com.onehumanawa.cfhcore.content.gui.menu.client.stats.data;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.StatNames;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.table.RowIcon;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.table.RowIcons;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.table.TableRow;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;

public final class Rows {
   public static List<TableRow> mobs(StatsData d) {
      Map<ResourceLocation, Long> killed = d.type("minecraft:killed");
      Map<ResourceLocation, Long> killedBy = d.type("minecraft:killed_by");
      Set<ResourceLocation> ids = new LinkedHashSet(killed.keySet());
      ids.addAll(killedBy.keySet());
      List<TableRow> out = new ArrayList(ids.size());

      for(ResourceLocation id : ids) {
         EntityType<?> type = (EntityType)BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
         RowIcon icon;
         if (type == EntityType.PLAYER) {
            icon = new RowIcon.Glyph(Icon.USER);
         } else {
            SpawnEggItem egg = RowIcons.egg(type);
            icon = new RowIcon.Mob(type, egg == null ? ItemStack.EMPTY : new ItemStack(egg));
         }

         TableRow row = TableRow.of(id, StatNames.entity(id), icon, new long[]{(Long)killed.getOrDefault(id, 0L), (Long)killedBy.getOrDefault(id, 0L)}, new String[]{StatKeys.key("minecraft:killed", id), StatKeys.key("minecraft:killed_by", id)});
         if (Summary.hostile(id)) {
            row.note(Component.translatable("cfhgui.stats.note.hostile"));
         }

         out.add(row);
      }

      return out;
   }

   public static List<TableRow> ways(Summary s) {
      List<TableRow> out = new ArrayList(s.ways.size());

      for(Summary.Way way : s.ways) {
         out.add(TableRow.of(way.id(), StatNames.custom(way.id()), wayIcon(way.id()), new long[]{way.cm()}, new String[]{StatKeys.custom(way.id())}).kind(StatKind.DISTANCE, way.id()));
      }

      return out;
   }

   public static RowIcon wayIcon(ResourceLocation id) {
      if (!"minecraft".equals(id.getNamespace())) {
         return new RowIcon.Glyph(Icon.MAP);
      } else {
         Item var10000;
         switch (id.getPath()) {
            case "walk_one_cm" -> var10000 = Items.IRON_BOOTS;
            case "sprint_one_cm" -> var10000 = Items.GOLDEN_BOOTS;
            case "crouch_one_cm" -> var10000 = Items.LEATHER_BOOTS;
            case "walk_on_water_one_cm" -> var10000 = Items.LILY_PAD;
            case "walk_under_water_one_cm" -> var10000 = Items.TURTLE_HELMET;
            case "swim_one_cm" -> var10000 = Items.COD;
            case "climb_one_cm" -> var10000 = Items.LADDER;
            case "fall_one_cm" -> var10000 = Items.ANVIL;
            case "fly_one_cm" -> var10000 = Items.FEATHER;
            case "aviate_one_cm" -> var10000 = Items.ELYTRA;
            case "boat_one_cm" -> var10000 = Items.OAK_BOAT;
            case "minecart_one_cm" -> var10000 = Items.MINECART;
            case "horse_one_cm" -> var10000 = Items.SADDLE;
            case "pig_one_cm" -> var10000 = Items.CARROT_ON_A_STICK;
            case "strider_one_cm" -> var10000 = Items.WARPED_FUNGUS_ON_A_STICK;
            default -> var10000 = Items.AIR;
         }

         Item item = var10000;
         return (RowIcon)(item == Items.AIR ? new RowIcon.Glyph(Icon.MAP) : new RowIcon.Stack(new ItemStack(item)));
      }
   }

   public static List<TableRow> blocks(StatsData d) {
      Map<ResourceLocation, long[]> rows = new LinkedHashMap();
      Map<ResourceLocation, Set<ResourceLocation>> counted = new HashMap();

      for(Map.Entry<ResourceLocation, Long> e : d.type("minecraft:mined").entrySet()) {
         ((long[])rows.computeIfAbsent((ResourceLocation)e.getKey(), (k) -> new long[5]))[0] = (Long)e.getValue();
      }

      String[] types = new String[]{"minecraft:used", "minecraft:crafted", "minecraft:picked_up", "minecraft:dropped"};

      for(int t = 0; t < types.length; ++t) {
         for(Map.Entry<ResourceLocation, Long> e : d.type(types[t]).entrySet()) {
            Block asBlock = Summary.blockOf((Item)BuiltInRegistries.ITEM.getOptional((ResourceLocation)e.getKey()).orElse(null));
            if (asBlock != null) {
               ResourceLocation block = BuiltInRegistries.BLOCK.getKey(asBlock);
               long[] var10000 = (long[])rows.computeIfAbsent(block, (k) -> new long[5]);
               var10000[t + 1] += (Long)e.getValue();
               ((Set)counted.computeIfAbsent(block, (k) -> new LinkedHashSet(2))).add((ResourceLocation)e.getKey());
            }
         }
      }

      List<TableRow> out = new ArrayList(rows.size());

      for(Map.Entry<ResourceLocation, long[]> e : rows.entrySet()) {
         ResourceLocation id = (ResourceLocation)e.getKey();
         long[] values = (long[])e.getValue();
         Block block = (Block)BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
         Item held = block == null ? Items.AIR : block.asItem();
         RowIcon icon = (RowIcon)(held == Items.AIR ? new RowIcon.Glyph(Icon.BLOCK) : new RowIcon.Stack(new ItemStack(held)));
         boolean ownItem = block != null && Summary.blockOf(held) == block;
         Set<ResourceLocation> items = (Set)counted.getOrDefault(id, Set.of());
         ResourceLocation item = items.size() == 1 ? (ResourceLocation)items.iterator().next() : (items.isEmpty() && ownItem ? BuiltInRegistries.ITEM.getKey(held) : null);
         String[] keys = new String[5];
         keys[0] = StatKeys.key("minecraft:mined", id);

         for(int i = 1; i < 5; ++i) {
            if (item != null) {
               keys[i] = StatKeys.key(types[i - 1], item);
            } else if (items.isEmpty()) {
               values[i] = Long.MIN_VALUE;
            }
         }

         TableRow row = TableRow.of(id, StatNames.block(id), icon, values, keys);
         if (held != Items.AIR && !ownItem) {
            String var10001 = held.getDescription().getString();
            row.found(var10001 + " " + String.valueOf(BuiltInRegistries.ITEM.getKey(held)));
         }

         for(ResourceLocation other : items) {
            if (!other.equals(id)) {
               row.found(other.toString());
            }
         }

         out.add(row);
      }

      return out;
   }

   public static List<TableRow> items(StatsData d) {
      String[] types = new String[]{"minecraft:used", "minecraft:crafted", "minecraft:broken", "minecraft:picked_up", "minecraft:dropped"};
      Map<ResourceLocation, long[]> rows = new LinkedHashMap();

      for(int t = 0; t < types.length; ++t) {
         for(Map.Entry<ResourceLocation, Long> e : d.type(types[t]).entrySet()) {
            Item item = (Item)BuiltInRegistries.ITEM.getOptional((ResourceLocation)e.getKey()).orElse(null);
            if (Summary.blockOf(item) == null) {
               ((long[])rows.computeIfAbsent((ResourceLocation)e.getKey(), (k) -> new long[5]))[t] = (Long)e.getValue();
            }
         }
      }

      List<TableRow> out = new ArrayList(rows.size());

      for(Map.Entry<ResourceLocation, long[]> e : rows.entrySet()) {
         Item item = (Item)BuiltInRegistries.ITEM.getOptional((ResourceLocation)e.getKey()).orElse(null);
         RowIcon icon = (RowIcon)(item != null && item != Items.AIR ? new RowIcon.Stack(new ItemStack(item)) : new RowIcon.Glyph(Icon.PACK));
         String[] rowKeys = new String[types.length];

         for(int t = 0; t < types.length; ++t) {
            rowKeys[t] = StatKeys.key(types[t], (ResourceLocation)e.getKey());
         }

         out.add(TableRow.of((ResourceLocation)e.getKey(), StatNames.item((ResourceLocation)e.getKey()), icon, (long[])e.getValue(), rowKeys));
      }

      return out;
   }

   public static List<TableRow> general(StatsData d) {
      Set<ResourceLocation> ids = new LinkedHashSet();

      for(ResourceLocation id : BuiltInRegistries.CUSTOM_STAT) {
         ids.add(id);
      }

      ids.addAll(d.type("minecraft:custom").keySet());
      Map<String, List<TableRow>> sections = new LinkedHashMap();
      Map<String, Component> titles = new LinkedHashMap();

      for(Section s : Rows.Section.values()) {
         String key = s.name().toLowerCase(Locale.ROOT);
         sections.put(key, new ArrayList());
         titles.put(key, Component.translatable("cfhgui.stats.section." + key));
      }

      List<String> mods = new ArrayList();

      for(ResourceLocation id : ids) {
         StatKind kind = StatKind.ofCustom(id);
         String section;
         if ("minecraft".equals(id.getNamespace())) {
            section = section(id.getPath(), kind).name().toLowerCase(Locale.ROOT);
         } else {
            section = "mod:" + id.getNamespace();
            if (!sections.containsKey(section)) {
               sections.put(section, new ArrayList());
               titles.put(section, Component.literal(StatNames.modName(id.getNamespace())));
               mods.add(section);
            }
         }

         String key = StatKeys.custom(id);
         sections.get(section).add(TableRow.of(id, StatNames.custom(id), RowIcon.NONE, new long[]{d.get(key)}, new String[]{key}).kind(kind, id));
      }

      List<TableRow> out = new ArrayList(ids.size() + sections.size());

      for(Map.Entry<String, List<TableRow>> e : sections.entrySet()) {
         if (!e.getValue().isEmpty()) {
            e.getValue().sort(Comparator.comparing((TableRow r) -> r.label, String.CASE_INSENSITIVE_ORDER));
            out.add(TableRow.section(e.getKey(), titles.get(e.getKey())));
            out.addAll(e.getValue());
         }
      }

      return out;
   }

   private static Section section(String path, StatKind kind) {
      if (kind == StatKind.TIME) {
         return Rows.Section.TIME;
      } else if (kind != StatKind.DISTANCE && !path.equals("jump")) {
         if (path.startsWith("damage_")) {
            return Rows.Section.DAMAGE;
         } else {
            switch (path) {
               case "deaths":
               case "mob_kills":
               case "player_kills":
               case "raid_trigger":
               case "raid_win":
               case "target_hit":
                  return Rows.Section.COMBAT;
               case "animals_bred":
               case "fish_caught":
               case "talked_to_villager":
               case "traded_with_villager":
               case "enchant_item":
               case "sleep_in_bed":
               case "eat_cake_slice":
               case "pot_flower":
               case "bell_ring":
               case "play_noteblock":
               case "tune_noteblock":
               case "play_record":
                  return Rows.Section.WORLD;
               case "leave_game":
               case "drop":
                  return Rows.Section.GAME;
               default:
                  return !path.startsWith("interact_with_") && !path.startsWith("open_") && !path.startsWith("inspect_") && !path.startsWith("clean_") && !path.endsWith("_cauldron") && !path.startsWith("trigger_") ? Rows.Section.GAME : Rows.Section.USE;
            }
         }
      } else {
         return Rows.Section.TRAVEL;
      }
   }

   private Rows() {
   }

   private static enum Section {
      TIME,
      TRAVEL,
      COMBAT,
      DAMAGE,
      WORLD,
      USE,
      GAME;
   }
}
