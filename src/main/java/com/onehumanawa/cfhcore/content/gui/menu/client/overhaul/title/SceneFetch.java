package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.stage.builds.BuildCache;
import com.onehumanawa.cfhcore.content.gui.core.stage.builds.BuildFinder;
import com.onehumanawa.cfhcore.content.gui.core.stage.builds.BuildSpec;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiToasts;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.title.set.TitleScene;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class SceneFetch {
   private static final long POLL_MS = 1500L;
   private static final long DOWNLOADS_MS = 600000L;
   private static final long SCENES_MS = 4000L;
   private static final long WANTED_MS = 1000L;
   private static final Object LOCK = new Object();
   private static long downloadsUntil;
   private static long scenesUntil;
   private static long wantedAt;
   private static boolean wantedOnce;
   private static List<BuildSpec> wanted = List.of();
   private static @Nullable Thread thread;
   private static final Set<String> MISSING = new HashSet();
   private static final Set<String> BRINGING = new HashSet();
   private static final Map<Path, Long> LAST = new HashMap();
   private static final Map<Path, Long> DONE = new HashMap();
   private static @Nullable String folderPrint;
   private static final boolean DEV_WATCH = Boolean.getBoolean("cfhgui.sceneFetch.watch");
   private static boolean devWatched;

   private SceneFetch() {
   }

   public static Path downloads() {
      String dev = System.getProperty("cfhgui.sceneFetch.downloads");
      return dev != null && !dev.isBlank() ? Path.of(dev) : Path.of(System.getProperty("user.home"), "Downloads");
   }

   public static boolean watchingDownloads() {
      synchronized(LOCK) {
         return Util.getMillis() < downloadsUntil;
      }
   }

   public static void pageOpened() {
      synchronized(LOCK) {
         downloadsUntil = Util.getMillis() + 600000L;
      }

      recheck();
   }

   public static void asking() {
      synchronized(LOCK) {
         scenesUntil = Util.getMillis() + 4000L;
         if (DEV_WATCH && !devWatched) {
            devWatched = true;
            downloadsUntil = Util.getMillis() + 600000L;
         }
      }

      renew(false);
   }

   public static void recheck() {
      renew(true);
   }

   private static void renew(boolean now) {
      long t = Util.getMillis();
      boolean fresh;
      synchronized(LOCK) {
         fresh = now || !wantedOnce || t - wantedAt >= 1000L;
         if (fresh) {
            wantedAt = t;
            wantedOnce = true;
         }
      }

      if (fresh) {
         List<BuildSpec> missing = new ArrayList();
         TitleScene came = null;
         BuildSpec cameSpec = null;

         for(TitleScene s : TitleScene.fetchable()) {
            BuildSpec b = s.build();
            if (b != null) {
               boolean bringing;
               synchronized(LOCK) {
                  bringing = BRINGING.contains(b.key());
               }

               if (!s.missing() && s.problem() == null) {
                  if (!bringing && MISSING.remove(b.key()) && came == null) {
                     came = s;
                     cameSpec = b;
                  }
               } else {
                  missing.add(b);
                  MISSING.add(b.key());
               }
            }
         }

         synchronized(LOCK) {
            wanted = List.copyOf(missing);
         }

         if (came != null) {
            BuildSpec.Found found = cameSpec.find();
            arrived(came, cameSpec, found != null && found.path() != null ? found.path().getFileName().toString() : cameSpec.fileName("nbt"), SceneFetch.Where.SCENES);
         }
      }

      synchronized(LOCK) {
         long n = Util.getMillis();
         if (n < downloadsUntil || n < scenesUntil) {
            if (thread == null || !thread.isAlive()) {
               Thread th = new Thread(SceneFetch::watch, "cfhgui-scene-fetch");
               th.setDaemon(true);
               th.setPriority(1);
               thread = th;
               th.start();
            }
         }
      }
   }

   private static void watch() {
      while(true) {
         List<BuildSpec> specs;
         boolean downloads;
         synchronized(LOCK) {
            long now = Util.getMillis();
            downloads = now < downloadsUntil;
            specs = wanted;
            if (!downloads && now >= scenesUntil) {
               thread = null;
               return;
            }
         }

         try {
            lookAtScenesFolder();
            if (downloads && !specs.isEmpty()) {
               lookAtDownloads(specs);
            }
         } catch (RuntimeException var9) {
         }

         try {
            Thread.sleep(1500L);
         } catch (InterruptedException var8) {
            synchronized(LOCK) {
               thread = null;
               return;
            }
         }
      }
   }

   private static void lookAtScenesFolder() {
      StringBuilder print = new StringBuilder();

      for(Path p : BuildFinder.files(BuildSpec.configDir())) {
         print.append(p.getFileName()).append(':').append(stamp(p)).append(';');
      }

      String now = print.toString();
      boolean changed = folderPrint != null && !now.equals(folderPrint);
      folderPrint = now;
      if (changed) {
         Minecraft.getInstance().execute(SceneFetch::recheck);
      }

   }

   private static void lookAtDownloads(List<BuildSpec> specs) {
      Map<Path, Long> seen = new HashMap();

      for(Path p : BuildFinder.files(downloads())) {
         long stamp = stamp(p);
         seen.put(p, stamp);
         if (stamp != 0L && !Long.valueOf(stamp).equals(DONE.get(p))) {
            Long before = (Long)LAST.get(p);
            if (before != null && before == stamp && !recent(p)) {
               BuildSpec spec = BuildFinder.identify(p, specs);
               DONE.put(p, stamp);
               if (spec != null) {
                  String name = p.getFileName().toString();
                  synchronized(LOCK) {
                     BRINGING.add(spec.key());
                  }

                  try {
                     Path to = BuildFinder.adopt(spec, p);
                     CFHGui.LOGGER.info("[CFHGui] scene build {} brought in from {} as {}", new Object[]{spec.baseName(), p, to.getFileName()});
                     synchronized(LOCK) {
                        List<BuildSpec> left = new ArrayList(wanted);
                        left.removeIf((w) -> w.key().equals(spec.key()));
                        wanted = List.copyOf(left);
                     }

                     Minecraft.getInstance().execute(() -> {
                        TitleScene scene = TitleScene.of(spec);
                        if (scene != null) {
                           arrived(scene, spec, name, SceneFetch.Where.DOWNLOADS);
                        } else {
                           brought(spec);
                        }

                     });
                  } catch (RuntimeException | IOException e) {
                     brought(spec);
                     CFHGui.LOGGER.warn("[CFHGui] scene build {}: {} could not be copied into {}: {}", new Object[]{spec.baseName(), p, BuildSpec.configDir(), ((Exception)e).toString()});
                  }
                  break;
               }
            }
         }
      }

      LAST.clear();
      LAST.putAll(seen);
   }

   private static boolean recent(Path p) {
      try {
         return System.currentTimeMillis() - Files.getLastModifiedTime(p).toMillis() < 1000L;
      } catch (IOException var2) {
         return true;
      }
   }

   private static long stamp(Path p) {
      try {
         long size = Files.size(p);
         return size == 0L ? 0L : size * 1000003L ^ Files.getLastModifiedTime(p).toMillis();
      } catch (IOException var3) {
         return 0L;
      }
   }

   public static void dropped(List<Path> files) {
      List<BuildSpec> specs = new ArrayList();

      for(TitleScene s : TitleScene.fetchable()) {
         BuildSpec b = s.build();
         if (b != null) {
            specs.add(b);
         }
      }

      if (!specs.isEmpty() && !files.isEmpty()) {
         Util.backgroundExecutor().execute(() -> {
            List<String> none = new ArrayList();

            for(Path p : files) {
               BuildSpec spec = BuildFinder.identify(p, specs);
               if (spec == null) {
                  none.add(p.getFileName().toString());
               } else {
                  String name = p.getFileName().toString();
                  synchronized(LOCK) {
                     BRINGING.add(spec.key());
                  }

                  try {
                     Path to = BuildFinder.adopt(spec, p);
                     CFHGui.LOGGER.info("[CFHGui] scene build {} brought in from {} (dropped onto the window) as {}", new Object[]{spec.baseName(), p, to.getFileName()});
                     Minecraft.getInstance().execute(() -> {
                        TitleScene scene = TitleScene.of(spec);
                        if (scene != null) {
                           arrived(scene, spec, name, SceneFetch.Where.DROPPED);
                        } else {
                           brought(spec);
                        }

                     });
                  } catch (RuntimeException | IOException e) {
                     brought(spec);
                     CFHGui.LOGGER.warn("[CFHGui] scene build {}: {} could not be copied into {}: {}", new Object[]{spec.baseName(), p, BuildSpec.configDir(), ((Exception)e).toString()});
                     String why = ((Exception)e).getMessage() == null ? ((Exception)e).toString() : ((Exception)e).getMessage();
                     Minecraft.getInstance().execute(() -> CFHGuiToasts.show(Component.translatable("cfhgui.overhaul.fetch.copy_failed", new Object[]{name}), Component.literal(why), Icon.ERROR));
                  }
               }
            }

            if (!none.isEmpty()) {
               Minecraft.getInstance().execute(() -> CFHGuiToasts.show(Component.translatable("cfhgui.overhaul.fetch.not_a_build"), Component.translatable("cfhgui.overhaul.fetch.not_a_build.body", new Object[]{String.join(", ", none)}), Icon.WARNING));
            }

         });
      }
   }

   private static void brought(BuildSpec spec) {
      synchronized(LOCK) {
         BRINGING.remove(spec.key());
      }
   }

   private static void arrived(TitleScene scene, BuildSpec spec, String file, Where from) {
      MISSING.remove(spec.key());
      brought(spec);
      BuildCache.retry(spec);
      Component body = Component.translatable("cfhgui.overhaul.fetch.arrived." + from.name().toLowerCase(Locale.ROOT), new Object[]{file});
      CFHGuiToasts.show(Component.translatable("cfhgui.overhaul.fetch.arrived", new Object[]{scene.title()}), body, Icon.CHECK);
      Screen var6 = Minecraft.getInstance().screen;
      if (var6 instanceof OverhaulTitleScreen title) {
         title.buildArrived(scene);
      } else {
         CFHGuiMenu.configFile().update((c) -> c.titleScene = scene.id());
      }

   }

   private static enum Where {
      DOWNLOADS,
      SCENES,
      DROPPED;
   }
}
