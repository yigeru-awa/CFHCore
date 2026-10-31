package com.onehumanawa.cfhcore.content.gui.core.stage.builds;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.event.CFHGuiEvents;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public final class BuildCache {
   private static final long KEEP_MS = 120000L;
   private static final long OTHERS_MS = 15000L;
   private static final long IN_WORLD_MS = 5000L;
   private static final long TAKEN_UP_MS = 10000L;
   private static final Map<String, CompletableFuture<BuildVolume>> VOLUMES = new HashMap();
   private static final Map<String, Long> ASKED = new HashMap();
   private static final Map<String, BuildMeshes> MESHES = new HashMap();
   private static final Map<String, CompletableFuture<BuildLand.Placed>> LANDS = new HashMap();
   private static boolean sweeping;
   private static final Set<String> MISSING_SAID = ConcurrentHashMap.newKeySet();
   private static final Map<String, Problem> PROBLEMS = new ConcurrentHashMap();

   private BuildCache() {
   }

   public static synchronized BuildMeshes acquire(BuildSpec spec, boolean soft, BuildCamera camera) {
      if (!sweeping) {
         sweeping = true;
         CFHGuiEvents.CLIENT_TICK_END.register(BuildCache::sweep);
      }

      String var10000 = spec.key();
      String key = var10000 + "|" + (soft ? "soft" : "game") + "|" + camera.key();
      BuildMeshes m = (BuildMeshes)MESHES.get(key);
      if (m == null || m.closed()) {
         m = new BuildMeshes(key, spec, spec.file(), soft, camera, volume(spec), spec.fadeRound() > 0.0F ? new float[]{spec.fadeFrom(), spec.fadeRound()} : null);
         MESHES.put(key, m);
      }

      ++m.users;
      return m;
   }

   public static synchronized BuildMeshes acquireLand(BuildSpec spec, int ring, boolean soft, BuildCamera camera, boolean outermost) {
      String var10000 = landKey(spec, ring);
      String key = var10000 + "|" + (soft ? "soft" : "game") + "|" + camera.key();
      BuildMeshes m = (BuildMeshes)MESHES.get(key);
      if (m == null || m.closed()) {
         String var10004 = spec.file();
         m = new BuildMeshes(key, spec, var10004 + " (land " + ring + ")", soft, camera, landOf(spec, ring).thenApply(BuildLand.Placed::volume), outermost ? new float[]{0.78F, 2.6F} : null);
         MESHES.put(key, m);
      }

      ++m.users;
      return m;
   }

   public static synchronized CompletableFuture<BuildLand.Placed> landOf(BuildSpec spec, int ring) {
      String key = landKey(spec, ring);
      ASKED.put(key, Util.getMillis());
      CompletableFuture<BuildLand.Placed> known = (CompletableFuture)LANDS.get(key);
      if (known != null && !known.isCompletedExceptionally()) {
         return known;
      } else {
         CompletableFuture<BuildLand.Placed> f = volume(spec).thenApplyAsync((v) -> {
            long t0 = System.currentTimeMillis();

            BuildLand.Placed var7;
            try {
               BuildLand.Placed p = BuildLand.generate(spec, v, ring);
               CFHGui.LOGGER.info("[CFHGui] build {}: land ring {} ({} x {} columns of {} blocks) made in {} ms", new Object[]{spec.file(), ring, p.ring().columnsX(), p.ring().columnsZ(), p.ring().cell(), System.currentTimeMillis() - t0});
               var7 = p;
            } finally {
               synchronized(BuildCache.class) {
                  ASKED.put(key, Util.getMillis());
               }
            }

            return var7;
         }, BuildWork.POOL);
         LANDS.put(key, f);
         return f;
      }
   }

   private static String landKey(BuildSpec spec, int ring) {
      String var10000 = spec.key();
      return var10000 + "|land:" + (spec.land() == null ? "-" : spec.land().key()) + "|" + ring;
   }

   public static synchronized CompletableFuture<BuildVolume> volumeOf(BuildSpec spec) {
      return volume(spec);
   }

   public static synchronized void release(BuildMeshes meshes) {
      meshes.users = Math.max(0, meshes.users - 1);
      if (meshes.users == 0) {
         meshes.idleSinceMs = Util.getMillis();
      }

   }

   private static CompletableFuture<BuildVolume> volume(BuildSpec spec) {
      String key = spec.key();
      ASKED.put(key, Util.getMillis());
      CompletableFuture<BuildVolume> known = (CompletableFuture)VOLUMES.get(key);
      if (known != null && !known.isCompletedExceptionally()) {
         return known;
      } else {
         BuildSpec.Found found = spec.find();
         CompletableFuture<BuildVolume> f;
         if (found == null) {
            String var10002 = spec.file();
            f = CompletableFuture.failedFuture(new FileNotFoundException("no file " + var10002 + " (." + String.join(", .", BuildSpec.EXTENSIONS) + ")"));
            if (MISSING_SAID.add(key)) {
               String page = spec.download() != null ? spec.download().page() : null;
               CFHGui.LOGGER.info("[CFHGui] scene build {} is not here: {}", spec.baseName(), page != null ? "the player fetches it from " + page + " (any name; it is found in config/cfhgui/scenes or the Downloads folder)" : "no file " + spec.file() + " in config/cfhgui/scenes or the resources");
            }
         } else {
            f = CompletableFuture.supplyAsync(() -> {
               long t0 = System.currentTimeMillis();

               BuildVolume var19;
               try {
                  BuildVolume v = BuildFiles.read(spec, found);
                  if (spec.grass() != 0 || spec.foliage() != 0 || spec.water() != 0) {
                     v.tint(spec.grass(), spec.foliage(), spec.water());
                  }

                  if (spec.light()) {
                     v.bakeLight();
                  }

                  CFHGui.LOGGER.info("[CFHGui] build {} read from {} in {} ms", new Object[]{spec.file(), found, System.currentTimeMillis() - t0});
                  PROBLEMS.remove(key);
                  var19 = v;
               } catch (Exception e) {
                  String why = why(e);
                  Problem was = (Problem)PROBLEMS.put(key, new Problem(found.path(), stamp(found.path()), why));
                  if (was == null || !why.equals(was.message())) {
                     CFHGui.LOGGER.warn("[CFHGui] build {} could not be read from {}: {}", new Object[]{spec.baseName(), found, why});
                  }

                  throw new CompletionException(e);
               } finally {
                  synchronized(BuildCache.class) {
                     ASKED.put(key, Util.getMillis());
                  }
               }

               return var19;
            }, BuildWork.POOL);
         }

         VOLUMES.put(key, f);
         return f;
      }
   }

   public static boolean present(BuildSpec spec) {
      return spec.find() != null;
   }

   public static @Nullable String problem(BuildSpec spec) {
      Problem p = (Problem)PROBLEMS.get(spec.key());
      if (p == null) {
         return null;
      } else {
         if (p.file() != null) {
            BuildSpec.Found now = spec.find();
            if (now == null || !p.file().equals(now.path()) || stamp(p.file()) != p.stamp()) {
               return null;
            }
         }

         return p.message();
      }
   }

   private static long stamp(@Nullable Path file) {
      if (file == null) {
         return 0L;
      } else {
         try {
            return Files.getLastModifiedTime(file).toMillis() ^ Files.size(file) * 31L;
         } catch (IOException var2) {
            return -1L;
         }
      }
   }

   private static String why(Throwable error) {
      Throwable e;
      for(e = error; (e instanceof CompletionException || e instanceof ExecutionException) && e.getCause() != null; e = e.getCause()) {
      }

      if (!(e instanceof EOFException) && (!(e instanceof ZipException) || !String.valueOf(e.getMessage()).contains("end"))) {
         if (e instanceof ZipException) {
            return "the file is damaged (" + e.getMessage() + ")";
         } else if (e instanceof OutOfMemoryError) {
            return "the build is too large to show";
         } else {
            String m = e.getMessage();
            return m != null && !m.isBlank() ? m : e.getClass().getSimpleName();
         }
      } else {
         return "the file ends too soon (a download cut short?)";
      }
   }

   public static synchronized void retry(BuildSpec spec) {
      CompletableFuture<BuildVolume> f = (CompletableFuture)VOLUMES.get(spec.key());
      if (f != null && f.isCompletedExceptionally()) {
         VOLUMES.remove(spec.key());
      }

      Iterator<BuildMeshes> it = MESHES.values().iterator();

      while(it.hasNext()) {
         BuildMeshes m = (BuildMeshes)it.next();
         if (m.spec.key().equals(spec.key()) && m.failed() && m.users == 0) {
            m.close();
            it.remove();
         }
      }

   }

   private static synchronized void sweep() {
      if (!MESHES.isEmpty() || !VOLUMES.isEmpty() || !LANDS.isEmpty()) {
         long now = Util.getMillis();
         boolean playing = Minecraft.getInstance().level != null;
         BuildMeshes last = null;

         for(BuildMeshes m : MESHES.values()) {
            if (m.users == 0 && (last == null || m.idleSinceMs > last.idleSinceMs)) {
               last = m;
            }
         }

         String lastBuild = last == null ? null : last.spec.key();
         List<String> gone = new ArrayList();

         for(Map.Entry<String, BuildMeshes> e : MESHES.entrySet()) {
            BuildMeshes m = (BuildMeshes)e.getValue();
            long keep = playing ? 5000L : (m.spec.key().equals(lastBuild) ? 120000L : 15000L);
            if (m.users == 0 && now - m.idleSinceMs > keep) {
               gone.add((String)e.getKey());
            }
         }

         for(String k : gone) {
            ((BuildMeshes)MESHES.remove(k)).close();
         }

         LANDS.entrySet().removeIf((ex) -> ((CompletableFuture)ex.getValue()).isDone() && now - (Long)ASKED.getOrDefault(ex.getKey(), 0L) > 10000L && MESHES.keySet().stream().noneMatch((k) -> k.startsWith((String)ex.getKey() + "|")));
         VOLUMES.entrySet().removeIf((ex) -> ((CompletableFuture)ex.getValue()).isDone() && now - (Long)ASKED.getOrDefault(ex.getKey(), 0L) > 10000L && MESHES.values().stream().noneMatch((m) -> m.spec.key().equals(ex.getKey())) && LANDS.keySet().stream().noneMatch((k) -> k.startsWith((String)ex.getKey() + "|land:")));
         ASKED.keySet().removeIf((kx) -> !VOLUMES.containsKey(kx) && !LANDS.containsKey(kx));
      }
   }

   private static record Problem(@Nullable Path file, long stamp, String message) {
   }
}
