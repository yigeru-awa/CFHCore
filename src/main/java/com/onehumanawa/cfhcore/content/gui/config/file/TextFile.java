package com.onehumanawa.cfhcore.content.gui.config.file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;

public final class TextFile {
   public static String read(Path path) throws IOException {
      String s = Files.readString(path, StandardCharsets.UTF_8);
      if (!s.isEmpty() && s.charAt(0) == '\ufeff') {
         s = s.substring(1);
      }

      return s;
   }

   public static void writeAtomic(Path path, String content) throws IOException {
      if (path.getParent() != null) {
         Files.createDirectories(path.getParent());
      }

      Path tmp = path.resolveSibling(String.valueOf(path.getFileName()) + ".cfhgui-tmp");
      Files.writeString(tmp, content, StandardCharsets.UTF_8);

      try {
         Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException var4) {
         Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
      }

   }

   public static long mtime(Path path) {
      try {
         FileTime t = Files.getLastModifiedTime(path);
         return t.toMillis();
      } catch (IOException var2) {
         return 0L;
      }
   }

   public static String lineSeparator(String text) {
      int i = text.indexOf(10);
      return i > 0 && text.charAt(i - 1) == '\r' ? "\r\n" : "\n";
   }

   private TextFile() {
   }
}
