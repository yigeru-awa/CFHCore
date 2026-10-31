package com.onehumanawa.cfhcore.content.gui.menu.client.screenshots;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Textures;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.MenuIo;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;

public final class Screenshots {
   public static Path dir() {
      return (new File(Minecraft.getInstance().gameDirectory, "screenshots")).toPath();
   }

   public static boolean isImage(Path p) {
      String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
      return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg");
   }

   public static CompletableFuture<List<Shot>> scan() {
      return CompletableFuture.supplyAsync(() -> {
         List<Shot> out = new ArrayList();
         Path d = dir();
         if (!Files.isDirectory(d, new LinkOption[0])) {
            return out;
         } else {
            try {
               Stream<Path> s = Files.list(d);

               try {
                  Objects.requireNonNull(s);

                  for(Path p : (Iterable<Path>)s::iterator) {
                     if (Files.isRegularFile(p, new LinkOption[0]) && isImage(p)) {
                        String file = p.getFileName().toString();
                        int dot = file.lastIndexOf(46);
                        long mod = 0L;
                        long size = 0L;

                        try {
                           mod = Files.getLastModifiedTime(p).toMillis();
                           size = Files.size(p);
                        } catch (IOException var13) {
                        }

                        out.add(new Shot(p, dot > 0 ? file.substring(0, dot) : file, file, mod, size));
                     }
                  }
               } catch (Throwable var14) {
                  if (s != null) {
                     try {
                        s.close();
                     } catch (Throwable var12) {
                        var14.addSuppressed(var12);
                     }
                  }

                  throw var14;
               }

               if (s != null) {
                  s.close();
               }
            } catch (IOException e) {
               CFHGuiMenu.LOGGER.warn("[CFHGui Menu] cannot list screenshots: {}", e.toString());
            }

            out.sort((a, b) -> Long.compare(b.modified(), a.modified()));
            return out;
         }
      }, MenuIo.POOL);
   }

   public static boolean rename(Shot shot, String newName) {
      if (newName == null) {
         return false;
      } else {
         String clean = newName.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
         if (clean.isEmpty()) {
            return false;
         } else {
            String file = shot.fileName();
            int dot = file.lastIndexOf(46);
            String ext = dot > 0 ? file.substring(dot) : ".png";
            Path target = shot.path().resolveSibling(clean + ext);
            if (Files.exists(target, new LinkOption[0])) {
               return false;
            } else {
               try {
                  Files.move(shot.path(), target, StandardCopyOption.ATOMIC_MOVE);
                  Textures.invalidate(shot.path());
                  return true;
               } catch (IOException e) {
                  CFHGuiMenu.LOGGER.warn("[CFHGui Menu] rename failed: {}", e.toString());
                  return false;
               }
            }
         }
      }
   }

   public static boolean delete(Shot shot) {
      try {
         boolean ok = Files.deleteIfExists(shot.path());
         Textures.invalidate(shot.path());
         return ok;
      } catch (IOException e) {
         CFHGuiMenu.LOGGER.warn("[CFHGui Menu] delete failed: {}", e.toString());
         return false;
      }
   }

   private Screenshots() {
   }

   public static record Shot(Path path, String name, String fileName, long modified, long size) {
   }
}
