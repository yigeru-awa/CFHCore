package com.onehumanawa.cfhcore.content.gui.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class JsonConfig<T> {
   public static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
   private final Path file;
   private final Class<T> type;
   private final Supplier<T> defaults;
   private T value;

   private JsonConfig(Path file, Class<T> type, Supplier<T> defaults) {
      this.file = file;
      this.type = type;
      this.defaults = defaults;
      this.load();
   }

   public static <T> JsonConfig<T> of(String name, Class<T> type, Supplier<T> defaults) {
      return new JsonConfig<T>(dir().resolve(name + ".json"), type, defaults);
   }

   public static <T> JsonConfig<T> at(Path file, Class<T> type, Supplier<T> defaults) {
      return new JsonConfig<T>(file, type, defaults);
   }

   public static Path dir() {
      return CFHGuiPlatform.get().configDir().resolve("cfhgui");
   }

   public T get() {
      return this.value;
   }

   public Path file() {
      return this.file;
   }

   public synchronized boolean fileHas(String key) {
      if (!Files.isRegularFile(this.file, new LinkOption[0])) {
         return false;
      } else {
         try {
            Reader r = Files.newBufferedReader(this.file, StandardCharsets.UTF_8);

            boolean var4;
            try {
               JsonElement root = JsonParser.parseReader(r);
               var4 = root != null && root.isJsonObject() && root.getAsJsonObject().has(key);
            } catch (Throwable var6) {
               if (r != null) {
                  try {
                     r.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (r != null) {
               r.close();
            }

            return var4;
         } catch (Exception var7) {
            return false;
         }
      }
   }

   public synchronized void load() {
      T loaded = null;
      if (Files.isRegularFile(this.file, new LinkOption[0])) {
         try {
            Reader r = Files.newBufferedReader(this.file, StandardCharsets.UTF_8);

            try {
               loaded = (T)GSON.fromJson(r, this.type);
            } catch (Throwable var6) {
               if (r != null) {
                  try {
                     r.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (r != null) {
               r.close();
            }
         } catch (Exception e) {
            CFHGui.LOGGER.warn("[CFHGui] could not read {} - using defaults ({})", this.file, e.toString());
         }
      }

      this.value = (T)(loaded != null ? loaded : this.defaults.get());
      if (loaded == null) {
         this.save();
      }

   }

   public synchronized void save() {
      try {
         Files.createDirectories(this.file.getParent());
         Path tmp = this.file.resolveSibling(String.valueOf(this.file.getFileName()) + ".tmp");
         Files.writeString(tmp, GSON.toJson(this.value), StandardCharsets.UTF_8);
         Files.move(tmp, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException e) {
         CFHGui.LOGGER.error("[CFHGui] could not write {}", this.file, e);
      }

   }

   public synchronized void update(Consumer<T> edit) {
      edit.accept(this.value);
      this.save();
   }

   public synchronized void reset() {
      this.value = (T)this.defaults.get();
      this.save();
   }
}
