package com.onehumanawa.cfhcore.content.gui.core.compat.maps;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.map.MapPoint;
import com.onehumanawa.cfhcore.content.gui.core.map.MapPoints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class MapPins {
   private static final Map<String, Pin> BY_ID = new HashMap();
   private static List<Pin> pins = List.of();
   private static List<String> groups = List.of();
   private static List<Hidden> hidden = List.of();
   private static int seen = -1;
   private static boolean choiceChanged;
   private static int version;

   public static List<Pin> pins() {
      return pins;
   }

   public static List<String> groups() {
      return groups;
   }

   public static List<Hidden> hiddenPlaces() {
      if (hidden.isEmpty()) {
         return hidden;
      } else {
         List<String> ones = MapPlacesConfig.get().hiddenPlaces;
         List<Hidden> now = new ArrayList(hidden.size());

         for(Hidden one : hidden) {
            if (ones.contains(one.id())) {
               now.add(one);
            }
         }

         return now;
      }
   }

   public static int version() {
      return version;
   }

   public static boolean any() {
      return !groups.isEmpty();
   }

   static boolean update() {
      int revision = MapPoints.revision();
      if (revision == seen && !choiceChanged) {
         return false;
      } else {
         seen = revision;
         choiceChanged = false;
         List<MapPoint> points = MapPoints.all();
         List<String> known = new ArrayList();
         List<String> own = new ArrayList();
         List<Pin> next = new ArrayList(points.size());
         Map<String, Pin> keep = new HashMap();
         MapPlacesConfig choice = points.isEmpty() ? null : MapPlacesConfig.get();
         List<String> hiddenGroups = choice == null ? List.of() : choice.hiddenGroups;
         List<String> hiddenOnes = choice == null ? List.of() : choice.hiddenPlaces;
         List<Hidden> single = new ArrayList();
         boolean changed = false;

         for(MapPoint point : points) {
            if (point != null && point.id() != null && !keep.containsKey(point.id())) {
               String group = point.group();
               if (!known.contains(group) && !own.contains(group)) {
                  (order(group) < 4 ? known : own).add(group);
               }

               if (!hiddenGroups.contains(group)) {
                  if (hiddenOnes.contains(point.id())) {
                     single.add(new Hidden(point.id(), point.label().getString(), group));
                  } else {
                     Pin pin = (Pin)BY_ID.get(point.id());
                     if (pin == null) {
                        pin = new Pin(point);
                        changed = true;
                     } else if (!pin.point.equals(point)) {
                        pin.set(point);
                        changed = true;
                     }

                     keep.put(pin.id, pin);
                     next.add(pin);
                  }
               }
            }
         }

         known.sort((a, b) -> Integer.compare(order(a), order(b)));
         known.addAll(own);
         changed |= next.size() != pins.size() || !known.equals(groups) || !single.equals(hidden);

         for(int i = 0; !changed && i < next.size(); ++i) {
            changed = next.get(i) != pins.get(i);
         }

         BY_ID.clear();
         BY_ID.putAll(keep);
         pins = List.copyOf(next);
         groups = List.copyOf(known);
         hidden = List.copyOf(single);
         if (changed) {
            ++version;
         }

         return changed;
      }
   }

   private static int order(String group) {
      byte var10000;
      switch (group) {
         case "homes" -> var10000 = 0;
         case "warps" -> var10000 = 1;
         case "spawn" -> var10000 = 2;
         case "deaths" -> var10000 = 3;
         default -> var10000 = 4;
      }

      return var10000;
   }

   public static boolean shown(String group) {
      return !MapPlacesConfig.get().hiddenGroups.contains(group);
   }

   public static void setShown(String group, boolean shown) {
      MapPlacesConfig config = MapPlacesConfig.get();
      if (shown != !config.hiddenGroups.contains(group)) {
         if (shown) {
            config.hiddenGroups.remove(group);
         } else {
            config.hiddenGroups.add(group);
         }

         MapPlacesConfig.save();
         choiceChanged = true;
      }
   }

   public static boolean placeShown(String id) {
      return !MapPlacesConfig.get().hiddenPlaces.contains(id);
   }

   public static void setPlaceShown(String id, boolean shown) {
      MapPlacesConfig config = MapPlacesConfig.get();
      if (shown != !config.hiddenPlaces.contains(id)) {
         if (shown) {
            config.hiddenPlaces.remove(id);
         } else {
            config.hiddenPlaces.add(id);
         }

         MapPlacesConfig.save();
         choiceChanged = true;
      }
   }

   public static boolean inWorld() {
      return !any() || MapPlacesConfig.get().inWorld;
   }

   public static void setInWorld(boolean on) {
      MapPlacesConfig config = MapPlacesConfig.get();
      if (config.inWorld != on) {
         config.inWorld = on;
         MapPlacesConfig.save();
      }
   }

   public static Component groupName(String group) {
      Component named = MapPoints.groupName(group);
      ComponentContents var3 = named.getContents();
      if (var3 instanceof PlainTextContents plain) {
         if (plain.text().equals(group) && named.getSiblings().isEmpty()) {
            if (order(group) < 4) {
               return Component.translatable("cfhgui.maps.group." + group);
            }

            String word = group.substring(group.lastIndexOf(58) + 1).replace('_', ' ');
            return Component.literal(word.isEmpty() ? group : word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1));
         }
      }

      return named;
   }

   public static Icon groupIcon(String group) {
      Icon var10000;
      switch (group) {
         case "homes" -> var10000 = Icon.HOME;
         case "warps" -> var10000 = Icon.COMPASS;
         case "spawn" -> var10000 = Icon.WORLD;
         case "deaths" -> var10000 = Icon.CLOSE;
         default -> var10000 = Icon.PIN;
      }

      return var10000;
   }

   public static double distance(Pin pin) {
      Minecraft mc = Minecraft.getInstance();
      Entity from = (Entity)(mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player);
      if (from != null && mc.level != null && mc.level.dimension().location().toString().equals(pin.dimension())) {
         double dx = pin.x() - from.getX();
         double dy = pin.y() - from.getY();
         double dz = pin.z() - from.getZ();
         return Math.sqrt(dx * dx + dy * dy + dz * dz);
      } else {
         return (double)-1.0F;
      }
   }

   public static @Nullable Component distanceText(Pin pin) {
      double d = distance(pin);
      if (d < (double)0.0F) {
         return null;
      } else {
         return d < (double)999.5F ? Component.translatable("cfhgui.maps.distance.m", new Object[]{Math.round(d)}) : Component.translatable("cfhgui.maps.distance.km", new Object[]{String.format(Locale.ROOT, "%.1f", d / (double)1000.0F)});
      }
   }

   public static Component describe(Pin pin) {
      Component distance = distanceText(pin);
      return distance == null ? Component.literal(pin.name()) : Component.translatable("cfhgui.maps.label", new Object[]{pin.name(), distance});
   }

   public static @Nullable Pin byId(String id) {
      return (Pin)BY_ID.get(id);
   }

   private MapPins() {
   }

   public static final class Pin {
      public final String id;
      private MapPoint point;
      private Icon icon;
      private String name;
      private @Nullable ResourceLocation rest;
      private @Nullable ResourceLocation lit;
      private int pictures = -1;

      private Pin(MapPoint point) {
         this.id = point.id();
         this.set(point);
      }

      private void set(MapPoint point) {
         this.point = point;
         this.icon = point.icon() != null ? point.icon() : MapPins.groupIcon(point.group());
         this.name = point.label().getString();
         this.rest = null;
         this.lit = null;
      }

      ResourceLocation picture(boolean pointedAt) {
         if (this.pictures != PinImages.generation()) {
            this.pictures = PinImages.generation();
            this.rest = null;
            this.lit = null;
         }

         if (pointedAt) {
            return this.lit != null ? this.lit : (this.lit = PinImages.of(this.icon, this.rgb(), true));
         } else {
            return this.rest != null ? this.rest : (this.rest = PinImages.of(this.icon, this.rgb(), false));
         }
      }

      public MapPoint point() {
         return this.point;
      }

      public String name() {
         return this.name;
      }

      public String group() {
         return this.point.group();
      }

      public String dimension() {
         return this.point.dimension();
      }

      public double x() {
         return this.point.x();
      }

      public double y() {
         return this.point.y();
      }

      public double z() {
         return this.point.z();
      }

      public int rgb() {
         return this.point.rgb() & 16777215;
      }

      public Icon icon() {
         return this.icon;
      }
   }

   public static record Hidden(String id, String name, String group) {
   }
}
