package com.onehumanawa.cfhcore.content.gui.core.platform;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

public final class Services {
   private static final Map<Class<?>, Object> CACHE = new ConcurrentHashMap();

   public static <T> T load(Class<T> type) {
      return (T)CACHE.computeIfAbsent(type, (t) -> ServiceLoader.load(t, Services.class.getClassLoader()).findFirst().orElseThrow(() -> new IllegalStateException("No CFHGui service implementation for " + t.getName() + " - the loader jar is missing its META-INF/services entry")));
   }

   private Services() {
   }
}
