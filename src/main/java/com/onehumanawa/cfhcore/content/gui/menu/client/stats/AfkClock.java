package com.onehumanawa.cfhcore.content.gui.menu.client.stats;

import com.mojang.blaze3d.platform.InputConstants;
import com.onehumanawa.cfhcore.content.gui.core.config.JsonConfig;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.MenuIo;
import com.onehumanawa.cfhcore.content.gui.menu.client.play.model.ServerActions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

public final class AfkClock {
   private static final long SAVE_EVERY_MS = 300000L;
   private static final int KEEP_PLACES = 200;
   private static final int[] KEYS = keys();
   private static @Nullable JsonConfig<Store> file;
   private static boolean started;
   private static @Nullable String place;
   private static long awayTicks;
   private static long sinceMs;
   private static int idleTicks;
   private static boolean dirty;
   private static long lastSaveMs;
   private static double mouseX;
   private static double mouseY;
   private static float yaw;
   private static float pitch;

   public static synchronized void init() {
      if (!started) {
         started = true;
         CFHGuiEvents.CLIENT_JOINED_SERVER.register(AfkClock::joined);
         CFHGuiEvents.CLIENT_LEFT_SERVER.register(AfkClock::left);
         CFHGuiEvents.CLIENT_TICK_END.register(AfkClock::tick);
      }
   }

   public static boolean counting() {
      return place != null;
   }

   public static long awayTicks() {
      return awayTicks;
   }

   public static long sinceMs() {
      return sinceMs;
   }

   public static boolean away() {
      return place != null && idleTicks >= thresholdTicks();
   }

   public static int thresholdMinutes() {
      return Mth.clamp(CFHGuiMenu.config().statsAfkMinutes, 1, 60);
   }

   private static int thresholdTicks() {
      return thresholdMinutes() * 1200;
   }

   private static void tick() {
      if (place != null) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer player = mc.player;
         if (player != null && mc.level != null) {
            if (!mc.isPaused()) {
               if (input(mc, player)) {
                  idleTicks = 0;
               } else {
                  int threshold = thresholdTicks();
                  ++idleTicks;
                  if (idleTicks == threshold) {
                     awayTicks += (long)threshold;
                     dirty = true;
                  } else if (idleTicks > threshold) {
                     ++awayTicks;
                     dirty = true;
                  }
               }

               if (dirty && System.currentTimeMillis() - lastSaveMs >= 300000L) {
                  save(true);
               }

            }
         }
      }
   }

   private static boolean input(Minecraft mc, LocalPlayer player) {
      MouseHandler mouse = mc.mouseHandler;
      boolean any = mouse.xpos() != mouseX || mouse.ypos() != mouseY || mouse.isLeftPressed() || mouse.isRightPressed() || mouse.isMiddlePressed();
      mouseX = mouse.xpos();
      mouseY = mouse.ypos();
      if (player.getYRot() != yaw || player.getXRot() != pitch) {
         any = true;
      }

      yaw = player.getYRot();
      pitch = player.getXRot();
      if (any) {
         return true;
      } else {
         Input in = player.input;
         if (in == null || in.forwardImpulse == 0.0F && in.leftImpulse == 0.0F && !in.jumping && !in.shiftKeyDown) {
            if (!mc.isWindowActive()) {
               return false;
            } else {
               long window = mc.getWindow().getWindow();

               for(int key : KEYS) {
                  if (InputConstants.isKeyDown(window, key)) {
                     return true;
                  }
               }

               return false;
            }
         } else {
            return true;
         }
      }
   }

   private static int[] keys() {
      int[][] ranges = new int[][]{{32, 32}, {39, 39}, {44, 57}, {59, 59}, {61, 61}, {65, 93}, {96, 96}, {161, 162}, {256, 269}, {280, 284}, {290, 314}, {320, 336}, {340, 348}};
      int n = 0;

      for(int[] r : ranges) {
         n += r[1] - r[0] + 1;
      }

      int[] out = new int[n];
      int i = 0;

      for(int[] r : ranges) {
         for(int k = r[0]; k <= r[1]; out[i++] = k++) {
         }
      }

      return out;
   }

   private static void joined() {
      Minecraft mc = Minecraft.getInstance();
      boolean firstVisit = false;

      try {
         place = placeKey(mc);
         JsonConfig<Store> f = file();
         Place known;
         synchronized(f) {
            known = ((Store)f.get()).places == null ? null : (Place)((Store)f.get()).places.get(place);
         }

         firstVisit = known == null;
         awayTicks = known == null ? 0L : Math.max(0L, known.awayTicks);
         sinceMs = known != null && known.sinceMs > 0L ? known.sinceMs : System.currentTimeMillis();
      } catch (RuntimeException e) {
         CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the time away could not be read, counting from nothing: {}", e.toString());
         awayTicks = 0L;
         sinceMs = System.currentTimeMillis();
      }

      idleTicks = 0;
      dirty = firstVisit;
      lastSaveMs = System.currentTimeMillis();
   }

   private static void left() {
      if (place != null) {
         if (dirty) {
            save(false);
         }

         place = null;
         idleTicks = 0;
      }
   }

   private static String placeKey(Minecraft mc) {
      if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
         String folder = "";

         try {
            folder = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString();
         } catch (RuntimeException var3) {
         }

         return "world:" + folder;
      } else {
         ServerData server = mc.getCurrentServer();
         if (server == null) {
            return "server:";
         } else {
            String var10000 = server.isRealm() ? "realm:" : (server.isLan() ? "lan:" : "server:");
            return var10000 + ServerActions.normalize(server.isRealm() ? server.name : server.ip);
         }
      }
   }

   private static synchronized JsonConfig<Store> file() {
      if (file == null) {
         file = JsonConfig.at(JsonConfig.dir().resolve("menu").resolve("afk.json"), Store.class, Store::new);
      }

      return file;
   }

   private static void save(boolean later) {
      String where = place;
      if (where != null) {
         long ticks = awayTicks;
         long since = sinceMs;
         long now = System.currentTimeMillis();
         dirty = false;
         lastSaveMs = now;
         Runnable write = () -> {
            try {
               file().update((store) -> {
                  if (store.places == null) {
                     store.places = new LinkedHashMap();
                  }

                  Place p = (Place)store.places.computeIfAbsent(where, (k) -> new Place());
                  p.awayTicks = ticks;
                  p.sinceMs = since;
                  p.updatedMs = now;
                  if (store.places.size() > 200) {
                     List<Map.Entry<String, Place>> byAge = new ArrayList(store.places.entrySet());
                     byAge.sort(Comparator.comparingLong((e) -> e.getValue() == null ? 0L : ((Place)e.getValue()).updatedMs));

                     for(int i = 0; i < byAge.size() - 200; ++i) {
                        store.places.remove(((Map.Entry)byAge.get(i)).getKey());
                     }
                  }

               });
            } catch (RuntimeException e) {
               CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the time away could not be saved: {}", e.toString());
            }

         };
         if (later) {
            MenuIo.POOL.execute(write);
         } else {
            write.run();
         }

      }
   }

   private AfkClock() {
   }

   public static final class Store {
      public Map<String, Place> places = new LinkedHashMap();
   }

   public static final class Place {
      public long awayTicks;
      public long sinceMs;
      public long updatedMs;
   }
}
