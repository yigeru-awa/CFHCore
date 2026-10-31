package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class PlayIo {
   private static final AtomicInteger N = new AtomicInteger();
   public static final ExecutorService POOL = Executors.newFixedThreadPool(2, (r) -> {
      Thread t = new Thread(r, "cfhgui-play-io-" + N.incrementAndGet());
      t.setDaemon(true);
      return t;
   });

   private PlayIo() {
   }
}
