package com.onehumanawa.cfhcore.content.gui.menu.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;

public final class MenuIo {
   private static final AtomicInteger N = new AtomicInteger();
   public static final ExecutorService POOL = Executors.newFixedThreadPool(4, (r) -> {
      Thread t = new Thread(r, "cfhgui-menu-io-" + N.incrementAndGet());
      t.setDaemon(true);
      return t;
   });

   public static void onClient(Runnable r) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.isSameThread()) {
         r.run();
      } else {
         mc.execute(r);
      }

   }

   private MenuIo() {
   }
}
