package com.onehumanawa.cfhcore.content.gui.core.layout;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ElementTypes {
   private static final Map<String, ElementType> TYPES = Collections.synchronizedMap(new LinkedHashMap());

   public static void register(ElementType type) {
      TYPES.put(type.id(), type);
   }

   public static Optional<ElementType> get(String id) {
      return Optional.ofNullable((ElementType)TYPES.get(id));
   }

   public static List<ElementType> all() {
      synchronized(TYPES) {
         return List.copyOf(TYPES.values());
      }
   }

   private ElementTypes() {
   }
}
