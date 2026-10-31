package com.onehumanawa.cfhcore.content.gui.core.layout.action;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Actions {
   private static final Map<String, ActionType> TYPES = Collections.synchronizedMap(new LinkedHashMap());

   public static void register(ActionType type) {
      if (TYPES.putIfAbsent(type.id(), type) != null) {
         CFHGui.LOGGER.warn("[CFHGui] action {} registered twice; keeping the first", type.id());
      }

   }

   public static Optional<ActionType> get(String id) {
      return Optional.ofNullable((ActionType)TYPES.get(id));
   }

   public static List<ActionType> all() {
      synchronized(TYPES) {
         return List.copyOf(TYPES.values());
      }
   }

   public static void run(String id, Map<String, String> args) {
      ActionType t = (ActionType)TYPES.get(id);
      if (t == null) {
         CFHGui.LOGGER.warn("[CFHGui] unknown action {}", id);
      } else {
         try {
            t.run(args == null ? Map.of() : args);
         } catch (Exception e) {
            CFHGui.LOGGER.error("[CFHGui] action {} failed", id, e);
         }

      }
   }

   private Actions() {
   }
}
