package com.onehumanawa.cfhcore.content.gui.core.media;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public final class FilePicker {
   public static final List<String> IMAGE_PATTERNS = List.of("*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp");
   private static volatile boolean open;
   private static Path lastDir;

   public static boolean available() {
      try {
         return !open && !Minecraft.getInstance().getWindow().isFullscreen();
      } catch (Throwable var1) {
         return false;
      }
   }

   public static boolean isOpen() {
      return open;
   }

   public static void pickImage(Consumer<Path> onPicked) {
      pickFile("Send an image", IMAGE_PATTERNS, "Images", onPicked);
   }

   public static void pickFile(String title, List<String> patterns, @Nullable String description, Consumer<Path> onPicked) {
      if (!open) {
         open = true;
         Thread worker = new Thread(() -> {
            String picked = null;

            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  PointerBuffer filters = null;
                  if (patterns != null && !patterns.isEmpty()) {
                     filters = stack.mallocPointer(patterns.size());

                     for(String p : patterns) {
                        filters.put(stack.UTF8(p));
                     }

                     filters.flip();
                  }

                  String start = lastDir != null && Files.isDirectory(lastDir, new LinkOption[0]) ? String.valueOf(lastDir.toAbsolutePath()) + File.separator : defaultDir();
                  picked = TinyFileDialogs.tinyfd_openFileDialog(title, start, filters, description, false);
               } catch (Throwable var15) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var14) {
                        var15.addSuppressed(var14);
                     }
                  }

                  throw var15;
               }

               if (stack != null) {
                  stack.close();
               }
            } catch (Throwable t) {
               CFHGui.LOGGER.warn("[CFHGui] file dialog failed: {}", t.toString());
            } finally {
               open = false;
            }

            if (picked != null && !picked.isBlank()) {
               Path path = Path.of(picked.trim());
               if (path.getParent() != null) {
                  lastDir = path.getParent();
               }

               Minecraft.getInstance().execute(() -> onPicked.accept(path));
            }
         }, "cfhgui-filepicker");
         worker.setDaemon(true);
         worker.start();
      }
   }

   public static void saveFile(String title, String suggestedName, List<String> patterns, @Nullable String description, Consumer<Path> onPicked) {
      if (!open) {
         open = true;
         Thread worker = new Thread(() -> {
            String picked = null;

            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  PointerBuffer filters = null;
                  if (patterns != null && !patterns.isEmpty()) {
                     filters = stack.mallocPointer(patterns.size());

                     for(String p : patterns) {
                        filters.put(stack.UTF8(p));
                     }

                     filters.flip();
                  }

                  String var10000 = lastDir != null && Files.isDirectory(lastDir, new LinkOption[0]) ? String.valueOf(lastDir.toAbsolutePath()) + File.separator : defaultDir();
                  String start = var10000 + suggestedName;
                  picked = TinyFileDialogs.tinyfd_saveFileDialog(title, start, filters, description);
               } catch (Throwable var16) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var15) {
                        var16.addSuppressed(var15);
                     }
                  }

                  throw var16;
               }

               if (stack != null) {
                  stack.close();
               }
            } catch (Throwable t) {
               CFHGui.LOGGER.warn("[CFHGui] save dialog failed: {}", t.toString());
            } finally {
               open = false;
            }

            if (picked != null && !picked.isBlank()) {
               Path path = Path.of(picked.trim());
               if (path.getParent() != null) {
                  lastDir = path.getParent();
               }

               Minecraft.getInstance().execute(() -> onPicked.accept(path));
            }
         }, "cfhgui-filepicker");
         worker.setDaemon(true);
         worker.start();
      }
   }

   private static String defaultDir() {
      try {
         Path pictures = Path.of(System.getProperty("user.home"), "Pictures");
         if (Files.isDirectory(pictures, new LinkOption[0])) {
            String var2 = String.valueOf(pictures.toAbsolutePath());
            return var2 + File.separator;
         } else {
            String var10000 = String.valueOf(Path.of(System.getProperty("user.home")).toAbsolutePath());
            return var10000 + File.separator;
         }
      } catch (Exception var1) {
         return "";
      }
   }

   public static @Nullable Path place(String name) {
      try {
         return name.equals("Screenshots") ? Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots") : Path.of(System.getProperty("user.home"), name);
      } catch (Exception var2) {
         return null;
      }
   }

   public static List<Path> openFiles(Component title, Path startDir, @Nullable String description, String... patterns) {
      if (open) {
         return List.of();
      } else {
         open = true;

         List<Path> out;
         try {
            MemoryStack stack;
            label212: {
               stack = MemoryStack.stackPush();

               Object var28;
               try {
                  PointerBuffer filters = null;
                  if (patterns.length > 0) {
                     filters = stack.mallocPointer(patterns.length);

                     for(String p : patterns) {
                        filters.put(stack.UTF8(p));
                     }

                     filters.flip();
                  }

                  String var10000 = String.valueOf(startDir.toAbsolutePath());
                  String start = var10000 + File.separator;
                  String result = TinyFileDialogs.tinyfd_openFileDialog(title.getString(), start, filters, description, true);
                  if (result == null || result.isBlank()) {
                     out = List.of();
                     break label212;
                  }

                  out = new ArrayList();

                  for(String part : result.split("\\|")) {
                     if (!part.isBlank()) {
                        out.add(Path.of(part.trim()));
                     }
                  }

                  if (!out.isEmpty() && ((Path)out.get(0)).getParent() != null) {
                     lastDir = ((Path)out.get(0)).getParent();
                  }

                  var28 = out;
               } catch (Throwable var19) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var18) {
                        var19.addSuppressed(var18);
                     }
                  }

                  throw var19;
               }

               if (stack != null) {
                  stack.close();
               }

               return (List<Path>)var28;
            }

            if (stack != null) {
               stack.close();
            }
         } catch (Throwable t) {
            CFHGui.LOGGER.warn("[CFHGui] no file dialog available: {}", t.toString());
            List filters = List.of();
            return filters;
         } finally {
            open = false;
         }

         return out;
      }
   }

   public static List<String> copyInto(List<Path> files, Path dir) {
      List<String> names = new ArrayList();

      try {
         Files.createDirectories(dir);
      } catch (IOException e) {
         CFHGui.LOGGER.warn("[CFHGui] cannot create {}: {}", dir, e.toString());
         return names;
      }

      for(Path file : files) {
         if (Files.isRegularFile(file, new LinkOption[0])) {
            String name = file.getFileName().toString();
            Path target = dir.resolve(name);
            if (Files.exists(target, new LinkOption[0]) && !sameFile(file, target)) {
               int dot = name.lastIndexOf(46);
               String stem = dot > 0 ? name.substring(0, dot) : name;
               String ext = dot > 0 ? name.substring(dot) : "";

               for(int i = 2; Files.exists(target, new LinkOption[0]); ++i) {
                  target = dir.resolve(stem + " (" + i + ")" + ext);
               }
            }

            try {
               if (!sameFile(file, target)) {
                  Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
               }

               names.add(target.getFileName().toString());
            } catch (IOException e) {
               CFHGui.LOGGER.warn("[CFHGui] cannot copy {} into {}: {}", new Object[]{file, dir, e.toString()});
            }
         }
      }

      return names;
   }

   private static boolean sameFile(Path a, Path b) {
      try {
         return Files.exists(b, new LinkOption[0]) && Files.isSameFile(a, b);
      } catch (IOException var3) {
         return false;
      }
   }

   private FilePicker() {
   }
}
