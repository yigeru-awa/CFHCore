package com.onehumanawa.cfhcore.content.gui.config.mods;

import com.onehumanawa.cfhcore.content.gui.config.ConfigPlatform;
import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfig;
import com.onehumanawa.cfhcore.content.gui.core.platform.ModInfo;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class ModLibraries {
   private static final Set<String> KNOWN = Set.of("architectury", "athena", "badpackets", "balm", "bookshelf", "collective", "creativecore", "cupboard", "framework", "fzzy_config", "geckolib", "glitchcore", "glodium", "iceberg", "konkrete", "kotlinforforge", "lithostitched", "moonlight", "placebo", "playeranimator", "prickle", "resourcefulconfig", "terrablender", "zeta");
   private static final Set<String> NEVER = Set.of("minecraft", "neoforge", "forge");
   private static final Set<String> NAME_WORDS = Set.of("lib", "libs", "library", "api");
   private static final Set<String> DESCRIPTION_WORDS = Set.of("lib", "library", "api");
   private static final int DESCRIPTION_OPENING = 160;
   private static @Nullable Map<String, Reason> found;

   public static synchronized Map<String, Reason> all() {
      if (found == null) {
         ModFacts facts = ModFacts.NONE;

         try {
            facts = ConfigPlatform.get().modFacts();
         } catch (Throwable t) {
            CFHGuiConfig.LOGGER.warn("[CFHGui Config] the loader could not say which mods are nested or required: {}", t.toString());
         }

         found = Map.copyOf(find(CFHGuiPlatform.get().allMods(), facts, contentNamespaces()));
      }

      return found;
   }

   public static boolean isLibrary(String modId) {
      return all().containsKey(modId);
   }

   public static Map<String, Reason> find(List<ModInfo> mods, ModFacts facts, Set<String> content) {
      Set<String> required = new HashSet();

      for(Map.Entry<String, Set<String>> e : facts.requires().entrySet()) {
         for(String needed : e.getValue()) {
            if (!needed.equals(e.getKey())) {
               required.add(needed);
            }
         }
      }

      Map<String, Reason> out = new LinkedHashMap();

      for(ModInfo m : mods) {
         Reason reason = reason(m, facts, content, required);
         if (reason != null) {
            out.put(m.id(), reason);
         }
      }

      return out;
   }

   private static @Nullable Reason reason(ModInfo m, ModFacts facts, Set<String> content, Set<String> required) {
      String id = m.id();
      if (facts.declaredLibraries().contains(id)) {
         return ModLibraries.Reason.DECLARED;
      } else if (NEVER.contains(id)) {
         return null;
      } else if (KNOWN.contains(id)) {
         return ModLibraries.Reason.KNOWN;
      } else if (content.contains(id)) {
         return null;
      } else if (facts.nested().contains(id)) {
         return ModLibraries.Reason.NESTED;
      } else if (named(id, m.name())) {
         return ModLibraries.Reason.NAMED;
      } else {
         return required.contains(id) && described(m.description()) ? ModLibraries.Reason.DESCRIBED : null;
      }
   }

   private static boolean named(String id, @Nullable String name) {
      if (!hasWord(name, NAME_WORDS) && !hasWord(id, NAME_WORDS)) {
         String low = id.toLowerCase(Locale.ROOT);
         return low.length() > 5 && (low.endsWith("lib") || low.endsWith("api"));
      } else {
         return true;
      }
   }

   private static boolean described(@Nullable String description) {
      if (description == null) {
         return false;
      } else {
         String opening = description.strip();
         int line = opening.indexOf(10);
         if (line >= 0) {
            opening = opening.substring(0, line);
         }

         if (opening.length() > 160) {
            opening = opening.substring(0, 160);
         }

         return hasWord(opening, DESCRIPTION_WORDS);
      }
   }

   private static boolean hasWord(@Nullable String text, Set<String> words) {
      if (text == null) {
         return false;
      } else {
         for(String word : words(text)) {
            if (words.contains(word)) {
               return true;
            }
         }

         return false;
      }
   }

   private static List<String> words(String text) {
      List<String> out = new ArrayList();
      StringBuilder word = new StringBuilder();
      char before = 0;

      for(int i = 0; i < text.length(); ++i) {
         char c = text.charAt(i);
         boolean part = Character.isLetterOrDigit(c);
         if (word.length() > 0 && (!part || Character.isUpperCase(c) && (Character.isLowerCase(before) || Character.isDigit(before)))) {
            out.add(word.toString().toLowerCase(Locale.ROOT));
            word.setLength(0);
         }

         if (part) {
            word.append(c);
         }

         before = c;
      }

      if (word.length() > 0) {
         out.add(word.toString().toLowerCase(Locale.ROOT));
      }

      return out;
   }

   private static Set<String> contentNamespaces() {
      Set<String> out = new HashSet();

      for(ResourceLocation key : BuiltInRegistries.ITEM.keySet()) {
         out.add(key.getNamespace());
      }

      for(ResourceLocation key : BuiltInRegistries.BLOCK.keySet()) {
         out.add(key.getNamespace());
      }

      for(ResourceLocation key : BuiltInRegistries.ENTITY_TYPE.keySet()) {
         out.add(key.getNamespace());
      }

      return out;
   }

   private ModLibraries() {
   }

   public static enum Reason {
      DECLARED,
      KNOWN,
      NESTED,
      NAMED,
      DESCRIBED;
   }
}
