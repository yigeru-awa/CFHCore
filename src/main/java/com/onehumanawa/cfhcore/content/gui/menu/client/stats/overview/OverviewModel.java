package com.onehumanawa.cfhcore.content.gui.menu.client.stats.overview;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.stats.StatsLink;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.AfkClock;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.StatFmt;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.StatNames;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.StatsTab;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.Rows;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.StatKeys;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.StatKind;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.StatsFeed;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.Summary;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.table.RowIcon;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.table.RowIcons;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public final class OverviewModel {
   public static final int WAYS = 4;
   public final Summary summary;
   public final List<Group> groups;
   public final List<Line> records;

   public OverviewModel(Summary s, StatsFeed feed) {
      this.summary = s;
      List<Group> out = new ArrayList(5);
      out.add(time(s, feed));
      out.add(survival(s));
      out.add(combat(s));
      out.add(travel(s));
      out.add(world(s));
      this.groups = List.copyOf(out);
      this.records = List.copyOf(records(s));
   }

   private static Component label(String name) {
      return Component.translatable("cfhgui.stats.o." + name);
   }

   private static @Nullable Component tip(String name) {
      String key = "cfhgui.stats.o." + name + ".tip";
      return I18n.exists(key) ? Component.translatable(key) : null;
   }

   private static StatFmt.Shown time(long ticks) {
      return StatFmt.of(StatKind.TIME, ticks, (ResourceLocation)null);
   }

   private static StatFmt.Shown hearts(long tenths) {
      return StatFmt.of(StatKind.TENTHS, tenths, ResourceLocation.withDefaultNamespace("damage_taken"));
   }

   private static StatFmt.Shown none(String key) {
      return StatFmt.words(I18n.get(key, new Object[0]), "");
   }

   private static Group time(Summary s, StatsFeed feed) {
      List<Line> lines = new ArrayList();
      if (s.away >= 0L) {
         lines.add(OverviewModel.Line.of("active", time(s.active()), (String)null));
         Component how = feed.ownAfk() ? Component.translatable("cfhgui.stats.o.away.tip.own", new Object[]{AfkClock.thresholdMinutes(), date(AfkClock.sinceMs())}) : Component.translatable("cfhgui.stats.o.away.tip.server");
         lines.add(OverviewModel.Line.of("away", time(s.away), "afk_ticks").tip(how));
      } else {
         lines.add(OverviewModel.Line.of("away", none("cfhgui.stats.unknown"), (String)null).tip(Component.translatable("cfhgui.stats.o.away.tip.unknown")).notKnown());
      }

      lines.add(OverviewModel.Line.of("since_death", time(s.sinceDeath), StatKeys.custom("time_since_death")));
      lines.add(OverviewModel.Line.of("since_rest", time(s.sinceRest), StatKeys.custom("time_since_rest")));
      if (s.sessions >= 0L) {
         lines.add(OverviewModel.Line.of("sessions", StatFmt.plain(s.sessions), (String)null));
      }

      if (s.firstSeenMs > 0L) {
         String when = date(s.firstSeenMs);
         lines.add(OverviewModel.Line.of("first_seen", StatFmt.words(when, when), (String)null));
      }

      return new Group("time", Component.translatable("cfhgui.stats.group.time"), OverviewModel.Line.of("played", time(s.played), StatKeys.custom("play_time")), lines, List.of(), 0L);
   }

   private static Group survival(Summary s) {
      List<Line> lines = new ArrayList();
      lines.add(top("killer", s.killer, "minecraft:killed_by", StatsTab.COMBAT));
      lines.add(OverviewModel.Line.of("damage_taken", hearts(s.damageTaken), StatKeys.custom("damage_taken")));
      lines.add(OverviewModel.Line.of("damage_dealt", hearts(s.damageDealt), StatKeys.custom("damage_dealt")));
      lines.add(OverviewModel.Line.of("damage_blocked", hearts(s.damageBlocked), StatKeys.custom("damage_blocked_by_shield")));
      lines.add(OverviewModel.Line.of("damage_absorbed", hearts(s.damageAbsorbed), StatKeys.custom("damage_absorbed")));
      lines.add(OverviewModel.Line.of("damage_resisted", hearts(s.damageResisted), StatKeys.custom("damage_resisted")));
      return new Group("survival", Component.translatable("cfhgui.stats.group.survival"), OverviewModel.Line.of("deaths", StatFmt.plain(s.deaths), StatKeys.custom("deaths")), lines, List.of(), 0L);
   }

   private static Group combat(Summary s) {
      List<Line> lines = new ArrayList();
      lines.add(OverviewModel.Line.of("hostile_kills", StatFmt.plain(s.hostileKills), (String)null).leadsTo(StatsTab.COMBAT));
      lines.add(OverviewModel.Line.of("player_kills", StatFmt.plain(s.playerKills), StatKeys.custom("player_kills")));
      long ratio = s.killsPerDeath();
      lines.add(ratio < 0L ? OverviewModel.Line.of("kd", none("cfhgui.stats.o.kd.none"), (String)null).tip(Component.translatable("cfhgui.stats.o.kd.tip.none")).notKnown() : OverviewModel.Line.of("kd", StatFmt.of(StatKind.RATIO, ratio, (ResourceLocation)null), (String)null));
      lines.add(top("prey", s.prey, "minecraft:killed", StatsTab.COMBAT));
      return new Group("combat", Component.translatable("cfhgui.stats.group.combat"), OverviewModel.Line.of("mob_kills", StatFmt.plain(s.mobKills), StatKeys.custom("mob_kills")), lines, List.of(), 0L);
   }

   private static Group travel(Summary s) {
      List<Line> lines = new ArrayList();
      List<Summary.Way> ways = new ArrayList();

      for(Summary.Way way : s.ways) {
         if (way.cm() <= 0L || ways.size() >= 4) {
            break;
         }

         ways.add(way);
         lines.add(new Line(StatNames.custom(way.id()), StatFmt.of(StatKind.DISTANCE, way.cm(), way.id()), StatKeys.custom(way.id()), (Component)null, RowIcon.NONE, StatsTab.TRAVEL, false));
      }

      lines.add(OverviewModel.Line.of("jumps", StatFmt.plain(s.jumps), StatKeys.custom("jump")));
      return new Group("travel", Component.translatable("cfhgui.stats.group.travel"), OverviewModel.Line.of("distance", StatFmt.of(StatKind.DISTANCE, s.distance, (ResourceLocation)null), (String)null).leadsTo(StatsTab.TRAVEL), lines, List.copyOf(ways), s.distance);
   }

   private static Group world(Summary s) {
      List<Line> lines = new ArrayList();
      lines.add(top("most_mined", s.mostMined, "minecraft:mined", StatsTab.BLOCKS));
      lines.add(OverviewModel.Line.of("placed", StatFmt.plain(s.placed), (String)null).leadsTo(StatsTab.BLOCKS));
      lines.add(OverviewModel.Line.of("crafted", StatFmt.plain(s.crafted), StatsLink.total("minecraft:crafted")).leadsTo(StatsTab.ITEMS));
      lines.add(OverviewModel.Line.of("broken", StatFmt.plain(s.broken), StatsLink.total("minecraft:broken")).leadsTo(StatsTab.ITEMS));
      lines.add(OverviewModel.Line.of("bred", StatFmt.plain(s.bred), StatKeys.custom("animals_bred")));
      lines.add(OverviewModel.Line.of("fish", StatFmt.plain(s.fish), StatKeys.custom("fish_caught")));
      lines.add(OverviewModel.Line.of("trades", StatFmt.plain(s.trades), StatKeys.custom("traded_with_villager")));
      lines.add(OverviewModel.Line.of("raids", StatFmt.plain(s.raidsWon), StatKeys.custom("raid_win")));
      lines.add(OverviewModel.Line.of("enchanted", StatFmt.plain(s.enchanted), StatKeys.custom("enchant_item")));
      lines.add(OverviewModel.Line.of("slept", StatFmt.plain(s.slept), StatKeys.custom("sleep_in_bed")));
      return new Group("world", Component.translatable("cfhgui.stats.group.world"), OverviewModel.Line.of("mined", StatFmt.plain(s.mined), StatsLink.total("minecraft:mined")).leadsTo(StatsTab.BLOCKS), lines, List.of(), 0L);
   }

   private static List<Line> records(Summary s) {
      List<Line> out = new ArrayList();
      addRecord(out, "most_mined", s.mostMined, "minecraft:mined", StatsTab.BLOCKS);
      addRecord(out, "most_placed", s.mostPlaced, "minecraft:used", StatsTab.BLOCKS);
      addRecord(out, "most_crafted", s.mostCrafted, "minecraft:crafted", StatsTab.ITEMS);
      addRecord(out, "most_used", s.mostUsed, "minecraft:used", StatsTab.ITEMS);
      addRecord(out, "most_picked_up", s.mostPickedUp, "minecraft:picked_up", StatsTab.ITEMS);
      addRecord(out, "prey", s.prey, "minecraft:killed", StatsTab.COMBAT);
      addRecord(out, "killer", s.killer, "minecraft:killed_by", StatsTab.COMBAT);
      Summary.Way way = s.longestWay();
      if (way != null) {
         StatFmt.Shown far = StatFmt.of(StatKind.DISTANCE, way.cm(), way.id());
         out.add(new Line(label("longest_way"), StatFmt.words(I18n.get("cfhgui.stats.thing_count", new Object[]{StatNames.custom(way.id()).getString(), far.text()}), far.exact()), StatKeys.custom(way.id()), (Component)null, Rows.wayIcon(way.id()), StatsTab.TRAVEL, false));
      }

      return out;
   }

   private static void addRecord(List<Line> out, String name, Summary.@Nullable Top top, String type, StatsTab tab) {
      if (top != null) {
         out.add(top(name, top, type, tab));
      }

   }

   private static Line top(String name, Summary.@Nullable Top top, String type, StatsTab tab) {
      if (top == null) {
         String nothing = I18n.get("cfhgui.stats.nothing_yet", new Object[0]);
         return new Line(label(name), StatFmt.words(nothing, ""), (String)null, tip(name), RowIcon.NONE, tab, true);
      } else {
         Component what = "minecraft:mined".equals(type) ? StatNames.block(top.id()) : (!"minecraft:killed".equals(type) && !"minecraft:killed_by".equals(type) ? StatNames.item(top.id()) : StatNames.entity(top.id()));
         String count = StatFmt.count(top.value());
         return new Line(label(name), StatFmt.words(I18n.get("cfhgui.stats.thing_count", new Object[]{what.getString(), count}), count), StatKeys.key(type, top.id()), tip(name), icon(type, top.id()), tab, false);
      }
   }

   public static RowIcon icon(String type, ResourceLocation id) {
      if (!"minecraft:killed".equals(type) && !"minecraft:killed_by".equals(type)) {
         Item item;
         if ("minecraft:mined".equals(type)) {
            Block block = (Block)BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
            item = block == null ? Items.AIR : block.asItem();
         } else {
            item = (Item)BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
         }

         return (RowIcon)(item == Items.AIR ? new RowIcon.Glyph(Icon.BLOCK) : new RowIcon.Stack(new ItemStack(item)));
      } else {
         EntityType<?> entity = (EntityType)BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
         if (entity == EntityType.PLAYER) {
            return new RowIcon.Glyph(Icon.USER);
         } else {
            SpawnEggItem egg = RowIcons.egg(entity);
            return new RowIcon.Mob(entity, egg == null ? ItemStack.EMPTY : new ItemStack(egg));
         }
      }
   }

   private static String date(long epochMs) {
      return DateFormat.getDateInstance(2, StatFmt.locale()).format(new Date(epochMs));
   }

   public String shape() {
      StringBuilder sb = new StringBuilder();

      for(Group g : this.groups) {
         sb.append(g.id()).append(g.lines().size()).append(g.ways().size()).append(';');
      }

      return sb.append(this.records.size()).toString();
   }

   public static record Line(Component label, StatFmt.Shown value, @Nullable String key, @Nullable Component tip, RowIcon icon, @Nullable StatsTab leadsTo, boolean unknown) {
      static Line of(String name, StatFmt.Shown value, @Nullable String key) {
         return new Line(OverviewModel.label(name), value, key, OverviewModel.tip(name), RowIcon.NONE, (StatsTab)null, false);
      }

      Line leadsTo(StatsTab tab) {
         return new Line(this.label, this.value, this.key, this.tip, this.icon, tab, this.unknown);
      }

      Line icon(RowIcon picture) {
         return new Line(this.label, this.value, this.key, this.tip, picture, this.leadsTo, this.unknown);
      }

      Line tip(@Nullable Component text) {
         return new Line(this.label, this.value, this.key, text, this.icon, this.leadsTo, this.unknown);
      }

      Line notKnown() {
         return new Line(this.label, this.value, this.key, this.tip, this.icon, this.leadsTo, true);
      }
   }

   public static record Group(String id, Component title, Line hero, List<Line> lines, List<Summary.Way> ways, long waysTotal) {
   }
}
