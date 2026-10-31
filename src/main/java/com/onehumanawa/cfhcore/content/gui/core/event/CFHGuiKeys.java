package com.onehumanawa.cfhcore.content.gui.core.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;

public final class CFHGuiKeys {
   public static final String CATEGORY = "key.categories.cfhgui";
   private static final List<KeyMapping> PENDING = new ArrayList();
   private static Consumer<KeyMapping> sink;

   public static synchronized void register(KeyMapping mapping) {
      if (sink != null) {
         sink.accept(mapping);
      } else {
         PENDING.add(mapping);
      }

   }

   public static synchronized void install(Consumer<KeyMapping> registrar) {
      sink = registrar;

      for(KeyMapping m : PENDING) {
         registrar.accept(m);
      }

      PENDING.clear();
   }

   public static synchronized List<KeyMapping> drain() {
      List<KeyMapping> out = List.copyOf(PENDING);
      PENDING.clear();
      return out;
   }

   private CFHGuiKeys() {
   }
}
