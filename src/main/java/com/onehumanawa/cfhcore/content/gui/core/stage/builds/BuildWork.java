package com.onehumanawa.cfhcore.content.gui.core.stage.builds;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

final class BuildWork {
   private static final AtomicInteger N = new AtomicInteger();
   static final int THREADS = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() / 4));
   static final ExecutorService POOL;

   private BuildWork() {
   }

   static {
      ThreadPoolExecutor pool = new ThreadPoolExecutor(THREADS, THREADS, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue(), (r) -> {
         Thread t = new Thread(r, "cfhgui-build-" + N.incrementAndGet());
         t.setDaemon(true);
         t.setPriority(Math.max(1, 3));
         return t;
      });
      pool.allowCoreThreadTimeOut(true);
      POOL = pool;
   }
}
