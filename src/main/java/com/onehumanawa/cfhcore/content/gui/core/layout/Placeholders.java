package com.onehumanawa.cfhcore.content.gui.core.layout;

import com.onehumanawa.cfhcore.content.gui.core.module.Modules;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;

public final class Placeholders {
   private static final Map<String, Supplier<String>> SUPPLIERS = new LinkedHashMap();
   private static final Pattern PATTERN = Pattern.compile("\\{([a-z_]+)}");

   public static void register(String key, Supplier<String> supplier) {
      SUPPLIERS.put(key, supplier);
   }

   public static String apply(String text) {
      if (text != null && text.indexOf(123) >= 0) {
         Matcher m = PATTERN.matcher(text);

         StringBuilder sb;
         String v;
         for(sb = new StringBuilder(); m.find(); m.appendReplacement(sb, Matcher.quoteReplacement(v == null ? "" : v))) {
            Supplier<String> s = (Supplier)SUPPLIERS.get(m.group(1));

            try {
               v = s == null ? m.group() : (String)s.get();
            } catch (Exception var6) {
               v = "";
            }
         }

         m.appendTail(sb);
         return sb.toString();
      } else {
         return text == null ? "" : text;
      }
   }

   public static Set<String> keys() {
      return SUPPLIERS.keySet();
   }

   private Placeholders() {
   }

   static {
      register("player", () -> Minecraft.getInstance().getUser().getName());
      register("uuid", () -> Minecraft.getInstance().getUser().getProfileId().toString());
      register("mc_version", () -> SharedConstants.getCurrentVersion().getName());
      register("loader", () -> CFHGuiPlatform.get().loader().displayName);
      register("mod_count", () -> Integer.toString(CFHGuiPlatform.get().allMods().size()));
      register("fps", () -> Integer.toString(Minecraft.getInstance().getFps()));
      register("time", () -> LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
      register("date", () -> LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
      register("server", () -> {
         Minecraft mc = Minecraft.getInstance();
         if (mc.getCurrentServer() != null) {
            return mc.getCurrentServer().name;
         } else {
            return mc.hasSingleplayerServer() ? "Singleplayer" : "";
         }
      });
      register("world", () -> {
         Minecraft mc = Minecraft.getInstance();
         return mc.getSingleplayerServer() != null ? mc.getSingleplayerServer().getWorldData().getLevelName() : "";
      });
      register("cfhgui_modules", () -> Integer.toString(Modules.all().size()));
   }
}
