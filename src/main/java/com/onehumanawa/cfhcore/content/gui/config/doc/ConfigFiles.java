package com.onehumanawa.cfhcore.content.gui.config.doc;

import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.Util;
import org.jetbrains.annotations.Nullable;

public final class ConfigFiles {
   private static final long SNAPSHOT_TTL_MS = 2000L;
   private static @Nullable Snapshot snapshot;

   public static List<Path> forMod(String modId) {
      List<Path> out = new ArrayList();
      String norm = normalize(modId);
      if (norm.isEmpty()) {
         return out;
      } else {
         for(Entry e : entries()) {
            if (e.directory()) {
               if (e.stem().equals(norm)) {
                  out.addAll(listDir(e.path(), 2));
               }
            } else if (e.supported() && (e.stem().equals(norm) || e.stem().startsWith(norm + "client") || e.stem().startsWith(norm + "common") || e.stem().startsWith(norm + "server") || e.stem().startsWith(norm + "startup") || e.stem().startsWith(norm) && e.stem().length() - norm.length() <= 8)) {
               out.add(e.path());
            }
         }

         return out;
      }
   }

   private static synchronized List<Entry> entries() {
      Path cfg = CFHGuiPlatform.get().configDir();
      long now = Util.getMillis();
      Snapshot s = snapshot;
      if (s != null && s.dir().equals(cfg) && now - s.takenAtMs() < 2000L) {
         return s.entries();
      } else {
         List<Entry> entries = new ArrayList();
         if (Files.isDirectory(cfg, new LinkOption[0])) {
            try {
               Stream<Path> list = Files.list(cfg);

               try {
                  for(Path p : list.sorted().toList()) {
                     entries.add(new Entry(p, normalize(stripExtension(p.getFileName().toString())), Files.isDirectory(p, new LinkOption[0]), Documents.isSupported(p)));
                  }
               } catch (Throwable var9) {
                  if (list != null) {
                     try {
                        list.close();
                     } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                     }
                  }

                  throw var9;
               }

               if (list != null) {
                  list.close();
               }
            } catch (IOException var10) {
            }
         }

         snapshot = new Snapshot(cfg, List.copyOf(entries), now);
         return snapshot.entries();
      }
   }

   private static List<Path> listDir(Path dir, int depth) {
      List<Path> out = new ArrayList();
      if (depth <= 0) {
         return out;
      } else {
         try {
            Stream<Path> s = Files.list(dir);

            try {
               for(Path p : s.sorted().toList()) {
                  if (Files.isDirectory(p, new LinkOption[0])) {
                     out.addAll(listDir(p, depth - 1));
                  } else if (Documents.isSupported(p) && Files.size(p) < 4000000L) {
                     out.add(p);
                  }
               }
            } catch (Throwable var7) {
               if (s != null) {
                  try {
                     s.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (s != null) {
               s.close();
            }
         } catch (IOException var8) {
         }

         return out;
      }
   }

   private static String stripExtension(String name) {
      int dot = name.lastIndexOf(46);
      return dot > 0 ? name.substring(0, dot) : name;
   }

   private static String normalize(String s) {
      return s.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").replace(" ", "");
   }

   private ConfigFiles() {
   }

   private static record Entry(Path path, String stem, boolean directory, boolean supported) {
   }

   private static record Snapshot(Path dir, List<Entry> entries, long takenAtMs) {
   }
}
