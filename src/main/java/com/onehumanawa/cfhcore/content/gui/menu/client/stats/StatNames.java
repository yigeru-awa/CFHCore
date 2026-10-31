package com.onehumanawa.cfhcore.content.gui.menu.client.stats;

import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.StatKeys;
import com.onehumanawa.cfhcore.content.gui.menu.client.stats.data.StatKind;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatType;
import org.jetbrains.annotations.Nullable;

public final class StatNames {
   private static final Map<String, String> MOD_NAMES = new ConcurrentHashMap();

   public static Component custom(ResourceLocation id) {
      return Component.translatableWithFallback("stat." + id.getNamespace() + "." + id.getPath(), pretty(strip(id.getPath())));
   }

   public static Component block(ResourceLocation id) {
      return (Component)BuiltInRegistries.BLOCK.getOptional(id).map((b) -> b.getName()).orElseGet(() -> Component.literal(pretty(id.getPath())));
   }

   public static Component item(ResourceLocation id) {
      return (Component)BuiltInRegistries.ITEM.getOptional(id).map((i) -> i.getDescription()).orElseGet(() -> Component.literal(pretty(id.getPath())));
   }

   public static Component entity(ResourceLocation id) {
      return (Component)BuiltInRegistries.ENTITY_TYPE.getOptional(id).map((t) -> t.getDescription()).orElseGet(() -> Component.literal(pretty(id.getPath())));
   }

   public static Component ofKey(String key) {
      StatKeys.Parsed parsed = StatKeys.parse(key);
      if (parsed == null) {
         return Component.translatableWithFallback("cfhgui.stats.extra." + key, pretty(key));
      } else if (parsed.total()) {
         ResourceLocation type = ResourceLocation.tryParse(parsed.type());
         return Component.translatableWithFallback("cfhgui.stats.total." + (type == null ? parsed.type() : type.getPath()), pretty(parsed.type()));
      } else {
         ResourceLocation id = parsed.id();
         if (id == null) {
            return Component.literal(key);
         } else if ("minecraft:custom".equals(parsed.type())) {
            return custom(id);
         } else {
            Component var10000;
            switch (parsed.type()) {
               case "minecraft:mined":
                  var10000 = block(id);
                  break;
               case "minecraft:killed":
               case "minecraft:killed_by":
                  var10000 = entity(id);
                  break;
               default:
                  var10000 = item(id);
            }

            Component what = var10000;
            return Component.translatable("cfhgui.stats.of_type", new Object[]{what, typeName(parsed.type())});
         }
      }
   }

   public static Component typeName(String typeId) {
      if ("minecraft:killed".equals(typeId)) {
         return Component.translatable("cfhgui.stats.col.kills.tip");
      } else if ("minecraft:killed_by".equals(typeId)) {
         return Component.translatable("cfhgui.stats.col.deaths.tip");
      } else {
         ResourceLocation id = ResourceLocation.tryParse(typeId);
         StatType<?> type = id == null ? null : (StatType)BuiltInRegistries.STAT_TYPE.get(id);
         return (Component)(type != null ? type.getDisplayName() : Component.literal(pretty(id == null ? typeId : id.getPath())));
      }
   }

   public static StatKind kindOfKey(String key) {
      if ("afk_ticks".equals(key)) {
         return StatKind.TIME;
      } else {
         StatKeys.Parsed parsed = StatKeys.parse(key);
         if (parsed != null && !parsed.total() && "minecraft:custom".equals(parsed.type())) {
            ResourceLocation id = parsed.id();
            return id == null ? StatKind.COUNT : StatKind.ofCustom(id);
         } else {
            return StatKind.COUNT;
         }
      }
   }

   public static @Nullable ResourceLocation customIdOfKey(String key) {
      StatKeys.Parsed parsed = StatKeys.parse(key);
      return parsed != null && !parsed.total() && "minecraft:custom".equals(parsed.type()) ? parsed.id() : null;
   }

   public static String modName(String namespace) {
      return (String)MOD_NAMES.computeIfAbsent(namespace, (ns) -> {
         try {
            return (String)CFHGuiPlatform.get().modInfo(ns).map((info) -> info.name() != null && !info.name().isBlank() ? info.name() : pretty(ns)).orElseGet(() -> pretty(ns));
         } catch (RuntimeException var2) {
            return pretty(ns);
         }
      });
   }

   public static String pretty(String path) {
      StringBuilder sb = new StringBuilder(path.length());
      boolean up = true;

      for(char c : path.toCharArray()) {
         if (c != '_' && c != '/' && c != ':' && c != '.') {
            sb.append(up ? Character.toUpperCase(c) : c);
            up = false;
         } else {
            sb.append(' ');
            up = true;
         }
      }

      return sb.toString();
   }

   private static String strip(String path) {
      return path.endsWith("_one_cm") ? path.substring(0, path.length() - "_one_cm".length()) : path;
   }

   private StatNames() {
   }
}
