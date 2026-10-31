package com.onehumanawa.cfhcore.content.gui.core.stage.builds;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.Nullable;

public final class BuildFinder {
   private static final Map<Path, Known> KNOWN = new ConcurrentHashMap();
   private static final Set<Path> LOOKING = ConcurrentHashMap.newKeySet();
   private static final AtomicInteger CHANGES = new AtomicInteger();
   private static final Set<String> SAID = ConcurrentHashMap.newKeySet();

   private BuildFinder() {
   }

   public static int changes() {
      return CHANGES.get();
   }

   public static String extension(Path file) {
      String name = file.getFileName().toString();
      int dot = name.lastIndexOf(46);
      return dot <= 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
   }

   public static boolean buildFile(Path file) {
      return BuildSpec.EXTENSIONS.contains(extension(file));
   }

   static String plain(String name) {
      StringBuilder b = new StringBuilder(name.length());

      for(int i = 0; i < name.length(); ++i) {
         char c = Character.toLowerCase(name.charAt(i));
         if (Character.isLetterOrDigit(c)) {
            b.append(c);
         }
      }

      return b.toString();
   }

   public static boolean namedAs(BuildSpec spec, String fileName) {
      int dot = fileName.lastIndexOf(46);
      String base = plain(dot > 0 ? fileName.substring(0, dot) : fileName);
      if (base.isEmpty()) {
         return false;
      } else {
         List<String> names = new ArrayList();
         names.add(plain(spec.baseName()));
         String var10001 = spec.scene().getNamespace();
         names.add(plain(var10001 + spec.file()));
         String scenePath = spec.scene().getPath();
         names.add(plain(scenePath.substring(scenePath.lastIndexOf(47) + 1)));
         if (spec.download() != null) {
            for(String n : spec.download().names()) {
               names.add(plain(n));
            }
         }

         for(String n : names) {
            if (!n.isEmpty() && base.startsWith(n)) {
               String rest = base.substring(n.length());
               if (rest.isEmpty() || rest.chars().allMatch(Character::isDigit) && rest.length() <= 3) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public static List<Path> files(Path dir) {
      List<Path> out = new ArrayList();
      if (!Files.isDirectory(dir, new LinkOption[0])) {
         return out;
      } else {
         try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(dir);

            try {
               for(Path p : stream) {
                  if (buildFile(p) && Files.isRegularFile(p, new LinkOption[0])) {
                     out.add(p);
                  }
               }
            } catch (Throwable var6) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (stream != null) {
               stream.close();
            }
         } catch (RuntimeException | IOException var7) {
         }

         out.sort(Comparator.comparing(BuildFinder::time).reversed());
         return out;
      }
   }

   private static FileTime time(Path p) {
      try {
         return Files.getLastModifiedTime(p);
      } catch (IOException var2) {
         return FileTime.fromMillis(0L);
      }
   }

   public static @Nullable Path inFolder(BuildSpec spec, Path dir) {
      List<Path> files = files(dir);

      for(Path p : files) {
         if (namedAs(spec, p.getFileName().toString())) {
            return p;
         }
      }

      BuildSpec.Match match = spec.download() == null ? null : spec.download().match();
      if (match == null) {
         return null;
      } else {
         for(Path p : files) {
            BuildPeek.Signature s = known(p);
            if (s != null && match.matches(s)) {
               Set var10000 = SAID;
               String var10001 = spec.file();
               if (var10000.add(var10001 + "|" + String.valueOf(p))) {
                  CFHGui.LOGGER.info("[CFHGui] scene build {}: {} is it (known by what is in it: {})", new Object[]{spec.baseName(), p.getFileName(), s});
               }

               return p;
            }
         }

         return null;
      }
   }

   private static BuildPeek.@Nullable Signature known(Path file) {
      Known k = (Known)KNOWN.get(file);
      if (k != null && fresh(file, k)) {
         return k.signature();
      } else {
         if (LOOKING.add(file)) {
            BuildWork.POOL.execute(() -> {
               try {
                  BuildPeek.Signature s = signature(file);
                  if (s != null) {
                     CHANGES.incrementAndGet();
                  }
               } finally {
                  LOOKING.remove(file);
               }

            });
         }

         return null;
      }
   }

   private static boolean fresh(Path file, Known k) {
      try {
         return Files.size(file) == k.size() && Files.getLastModifiedTime(file).equals(k.time());
      } catch (IOException var3) {
         return false;
      }
   }

   public static BuildPeek.@Nullable Signature signature(Path file) {
      try {
         long size = Files.size(file);
         FileTime time = Files.getLastModifiedTime(file);
         Known k = (Known)KNOWN.get(file);
         if (k != null && k.size() == size && k.time().equals(time)) {
            return k.signature();
         } else {
            BuildPeek.Signature s = null;
            if (size > 0L && size <= 100663296L) {
               try {
                  s = BuildPeek.read(file);
               } catch (RuntimeException | IOException var7) {
               }
            }

            KNOWN.put(file, new Known(size, time, s));
            return s;
         }
      } catch (IOException var8) {
         return null;
      }
   }

   public static @Nullable BuildSpec identify(Path file, List<BuildSpec> specs) {
      if (buildFile(file) && Files.isRegularFile(file, new LinkOption[0])) {
         for(BuildSpec s : specs) {
            if (namedAs(s, file.getFileName().toString())) {
               return s;
            }
         }

         BuildPeek.Signature sig = signature(file);
         if (sig == null) {
            return null;
         } else {
            for(BuildSpec s : specs) {
               BuildSpec.Match m = s.download() == null ? null : s.download().match();
               if (m != null && m.matches(sig)) {
                  return s;
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   public static Path adopt(BuildSpec spec, Path source) throws IOException {
      Path dir = BuildSpec.configDir();
      Files.createDirectories(dir);
      String ext = extension(source);
      Path target = dir.resolve(spec.fileName(ext.isEmpty() ? "nbt" : ext));
      if (source.toAbsolutePath().normalize().equals(target.toAbsolutePath().normalize())) {
         return target;
      } else {
         Path part = dir.resolve(String.valueOf(target.getFileName()) + ".part");
         Files.copy(source, part, StandardCopyOption.REPLACE_EXISTING);

         try {
            Files.move(part, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (IOException var7) {
            Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
         }

         CHANGES.incrementAndGet();
         return target;
      }
   }

   private static record Known(long size, FileTime time, BuildPeek.@Nullable Signature signature) {
   }
}
