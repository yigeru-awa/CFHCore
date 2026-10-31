package com.onehumanawa.cfhcore.content.gui.config.option;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

public record Choice(String id, Component label) {
   public static Choice of(String id) {
      return new Choice(id, Component.literal(id));
   }

   public static Choice of(String id, String label) {
      return new Choice(id, Component.literal(label));
   }

   public static List<Choice> ofEnum(Class<?> enumClass) {
      List<Choice> out = new ArrayList();
      Object[] constants = enumClass.getEnumConstants();
      if (constants == null) {
         return out;
      } else {
         for(Object c : constants) {
            String name = ((Enum)c).name();
            out.add(new Choice(name, Component.literal(Humanize.enumName(name))));
         }

         return out;
      }
   }

   public static List<Choice> ofStrings(List<String> ids) {
      List<Choice> out = new ArrayList();

      for(String s : ids) {
         out.add(new Choice(s, Component.literal(Humanize.enumName(s))));
      }

      return out;
   }
}
