package com.onehumanawa.cfhcore.content.gui.core.compat.maps;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Linkage {
   public static boolean complete(Class<?> type) {
      Set<String> bodies = new HashSet();

      for(Class<?> c = type; c != null; c = c.getSuperclass()) {
         for(Method m : c.getDeclaredMethods()) {
            String key = key(m);
            if (!Modifier.isAbstract(m.getModifiers())) {
               bodies.add(key);
            } else if (!bodies.contains(key)) {
               return false;
            }
         }
      }

      for(Method m : type.getMethods()) {
         if (Modifier.isAbstract(m.getModifiers())) {
            return false;
         }
      }

      return true;
   }

   private static String key(Method m) {
      String var10000 = m.getName();
      return var10000 + Arrays.toString(m.getParameterTypes());
   }

   private Linkage() {
   }
}
