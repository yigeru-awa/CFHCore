package com.onehumanawa.cfhcore.content.gui.config.ui;

import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfig;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class ApplyQueue {
   private static final Map<String, Pending> PENDING = new LinkedHashMap();

   public static synchronized void later(String key, int delayMs, Runnable run) {
      PENDING.put(key, new Pending(Clock.nowMs() + (long)delayMs, run, false, false));
   }

   public static void later(String key, Runnable run) {
      later(key, 350, run);
   }

   public static synchronized void afterDrag(String key, int delayMs, Runnable run) {
      PENDING.put(key, new Pending(Clock.nowMs() + (long)delayMs, run, true, false));
   }

   public static void tick() {
      List<Runnable> due = new ArrayList();
      synchronized(ApplyQueue.class) {
         if (PENDING.isEmpty()) {
            return;
         }

         long now = Clock.nowMs();
         Boolean down = null;
         Iterator<Map.Entry<String, Pending>> it = PENDING.entrySet().iterator();

         while(it.hasNext()) {
            Map.Entry<String, Pending> e = (Map.Entry)it.next();
            Pending p = (Pending)e.getValue();
            if (p.untilRelease) {
               if (down == null) {
                  down = mouseDown();
               }

               if (down) {
                  if (!p.held) {
                     e.setValue(new Pending(p.dueMs, p.run, true, true));
                  }
                  continue;
               }

               if (!p.held && p.dueMs > now) {
                  continue;
               }
            } else if (p.dueMs > now) {
               continue;
            }

            due.add(p.run);
            it.remove();
         }
      }

      run(due);
   }

   public static void flush() {
      List<Runnable> all = new ArrayList();
      synchronized(ApplyQueue.class) {
         for(Pending p : PENDING.values()) {
            all.add(p.run);
         }

         PENDING.clear();
      }

      run(all);
   }

   private static boolean mouseDown() {
      Minecraft mc = Minecraft.getInstance();
      return mc.getWindow() != null && GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), 0) == 1;
   }

   private static void run(List<Runnable> list) {
      for(Runnable r : list) {
         try {
            r.run();
         } catch (Exception e) {
            CFHGuiConfig.LOGGER.error("[CFHGui Config] deferred apply failed", e);
         }
      }

   }

   private ApplyQueue() {
   }

   private static record Pending(long dueMs, Runnable run, boolean untilRelease, boolean held) {
   }
}
