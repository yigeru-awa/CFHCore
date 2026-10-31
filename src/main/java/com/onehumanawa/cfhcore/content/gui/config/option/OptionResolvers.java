package com.onehumanawa.cfhcore.content.gui.config.option;

import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfig;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OptionResolvers {
   private static final Map<String, OptionResolver> RESOLVERS = Collections.synchronizedMap(new LinkedHashMap());
   private static final Map<String, OptionBinding> PUBLISHED = Collections.synchronizedMap(new LinkedHashMap());

   public static void register(OptionResolver r) {
      RESOLVERS.put(r.prefix(), r);
   }

   public static void publish(OptionBinding b) {
      PUBLISHED.put(b.id(), b);
   }

   public static void publishAll(Iterable<? extends OptionBinding> bindings) {
      for(OptionBinding b : bindings) {
         publish(b);
      }

   }

   public static List<String> prefixes() {
      synchronized(RESOLVERS) {
         return List.copyOf(RESOLVERS.keySet());
      }
   }

   public static Optional<OptionBinding> resolve(String path) {
      if (path != null && !path.isBlank()) {
         OptionBinding published = (OptionBinding)PUBLISHED.get(path);
         if (published != null) {
            return Optional.of(published);
         } else {
            int colon = path.indexOf(58);
            if (colon <= 0) {
               return Optional.empty();
            } else {
               OptionResolver r = (OptionResolver)RESOLVERS.get(path.substring(0, colon));
               if (r == null) {
                  return Optional.empty();
               } else {
                  try {
                     Optional<OptionBinding> b = r.resolve(path.substring(colon + 1));
                     b.ifPresent((x) -> PUBLISHED.putIfAbsent(path, x));
                     return b;
                  } catch (Exception e) {
                     CFHGuiConfig.LOGGER.warn("[CFHGui Config] cannot resolve {}: {}", path, e.toString());
                     return Optional.empty();
                  }
               }
            }
         }
      } else {
         return Optional.empty();
      }
   }

   public static void invalidate(String prefix) {
      PUBLISHED.keySet().removeIf((k) -> k.startsWith(prefix + ":"));
   }

   private OptionResolvers() {
   }
}
