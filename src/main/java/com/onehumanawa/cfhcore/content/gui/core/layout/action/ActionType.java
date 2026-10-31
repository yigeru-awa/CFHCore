package com.onehumanawa.cfhcore.content.gui.core.layout.action;

import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;

public record ActionType(String id, Component label, List<Arg> args, Runner runner) {
   public void run(Map<String, String> values) {
      this.runner.run(values);
   }

   public static record Arg(String key, Component label, Kind kind, String defaultValue, List<String> choices) {
      public static Arg text(String key, Component label, String def) {
         return new Arg(key, label, ActionType.Arg.Kind.TEXT, def, List.of());
      }

      public static Arg multiline(String key, Component label, String def) {
         return new Arg(key, label, ActionType.Arg.Kind.MULTILINE, def, List.of());
      }

      public static Arg bool(String key, Component label, boolean def) {
         return new Arg(key, label, ActionType.Arg.Kind.BOOL, Boolean.toString(def), List.of());
      }

      public static Arg number(String key, Component label, String def) {
         return new Arg(key, label, ActionType.Arg.Kind.NUMBER, def, List.of());
      }

      public static Arg choice(String key, Component label, String def, List<String> choices) {
         return new Arg(key, label, ActionType.Arg.Kind.CHOICE, def, choices);
      }

      public static Arg screen(String key, Component label) {
         return new Arg(key, label, ActionType.Arg.Kind.SCREEN_ID, "minecraft:title", List.of());
      }

      public static enum Kind {
         TEXT,
         MULTILINE,
         NUMBER,
         BOOL,
         SCREEN_ID,
         ACTION_LIST,
         CHOICE;
      }
   }

   @FunctionalInterface
   public interface Runner {
      void run(Map<String, String> var1);
   }
}
