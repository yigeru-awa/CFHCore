package com.onehumanawa.cfhcore.content.gui.core.gfx;

import com.mojang.blaze3d.platform.NativeImage;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

public final class Textures {
   private static final Map<String, Loaded> CACHE = new ConcurrentHashMap();
   private static final Map<String, CompletableFuture<Loaded>> PENDING = new ConcurrentHashMap();
   private static final ExecutorService POOL = Executors.newFixedThreadPool(2, (r) -> {
      Thread t = new Thread(r, "cfhgui-textures");
      t.setDaemon(true);
      return t;
   });
   private static int counter;

   public static Optional<Loaded> cached(Path file) {
      return Optional.ofNullable((Loaded)CACHE.get(file.toAbsolutePath().toString()));
   }

   public static void load(Path file, Consumer<Loaded> onReady) {
      String key = file.toAbsolutePath().toString();
      Loaded c = (Loaded)CACHE.get(key);
      if (c != null) {
         onReady.accept(c);
      } else {
         ((CompletableFuture)PENDING.computeIfAbsent(key, (k) -> CompletableFuture.supplyAsync(() -> {
               try {
                  InputStream in = Files.newInputStream(file);

                  NativeImage var2;
                  try {
                     var2 = NativeImage.read(in);
                  } catch (Throwable var5) {
                     if (in != null) {
                        try {
                           in.close();
                        } catch (Throwable var4) {
                           var5.addSuppressed(var4);
                        }
                     }

                     throw var5;
                  }

                  if (in != null) {
                     in.close();
                  }

                  return var2;
               } catch (Exception e) {
                  CFHGui.LOGGER.warn("[CFHGui] cannot read image {}: {}", file, e.toString());
                  return null;
               }
            }, POOL).thenApplyAsync((img) -> {
               PENDING.remove(k);
               if (img == null) {
                  return null;
               } else {
                  Loaded l = register(img, "file");
                  CACHE.put(k, l);
                  return l;
               }
            }, Minecraft.getInstance()))).thenAccept((l) -> {
            if (l != null) {
               onReady.accept((Loaded) l);
            }

         });
      }
   }

   public static void loadThumbnail(Path file, int maxSize, Consumer<Loaded> onReady) {
      String var10000 = String.valueOf(file.toAbsolutePath());
      String key = var10000 + "@" + maxSize;
      Loaded c = (Loaded)CACHE.get(key);
      if (c != null) {
         onReady.accept(c);
      } else {
         ((CompletableFuture)PENDING.computeIfAbsent(key, (k) -> CompletableFuture.supplyAsync(() -> {
               try {
                  InputStream in = Files.newInputStream(file);

                  NativeImage var17;
                  try {
                     NativeImage full = NativeImage.read(in);

                     try {
                        int w = full.getWidth();
                        int h = full.getHeight();
                        float s = Math.min(1.0F, (float)maxSize / (float)Math.max(w, h));
                        int tw = Math.max(1, Math.round((float)w * s));
                        int th = Math.max(1, Math.round((float)h * s));
                        NativeImage small = new NativeImage(tw, th, false);

                        for(int y = 0; y < th; ++y) {
                           for(int x = 0; x < tw; ++x) {
                              small.setPixelRGBA(x, y, full.getPixelRGBA(Math.min(w - 1, (int)((float)x / s)), Math.min(h - 1, (int)((float)y / s))));
                           }
                        }

                        var17 = small;
                     } catch (Throwable var14) {
                        if (full != null) {
                           try {
                              full.close();
                           } catch (Throwable var13) {
                              var14.addSuppressed(var13);
                           }
                        }

                        throw var14;
                     }

                     if (full != null) {
                        full.close();
                     }
                  } catch (Throwable var15) {
                     if (in != null) {
                        try {
                           in.close();
                        } catch (Throwable var12) {
                           var15.addSuppressed(var12);
                        }
                     }

                     throw var15;
                  }

                  if (in != null) {
                     in.close();
                  }

                  return var17;
               } catch (Exception e) {
                  CFHGui.LOGGER.warn("[CFHGui] cannot thumbnail {}: {}", file, e.toString());
                  return null;
               }
            }, POOL).thenApplyAsync((img) -> {
               PENDING.remove(k);
               if (img == null) {
                  return null;
               } else {
                  Loaded l = register(img, "thumb");
                  CACHE.put(k, l);
                  return l;
               }
            }, Minecraft.getInstance()))).thenAccept((l) -> {
            if (l != null) {
               onReady.accept((Loaded) l);
            }

         });
      }
   }

   public static Optional<Loaded> fromBytes(byte[] bytes, String cacheKey) {
      if (cacheKey != null) {
         Loaded c = (Loaded)CACHE.get(cacheKey);
         if (c != null) {
            return Optional.of(c);
         }
      }

      try {
         InputStream in = new ByteArrayInputStream(bytes);

         Optional var5;
         try {
            NativeImage img = NativeImage.read(in);
            Loaded l = register(img, "bytes");
            if (cacheKey != null) {
               CACHE.put(cacheKey, l);
            }

            var5 = Optional.of(l);
         } catch (Throwable var7) {
            try {
               in.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         in.close();
         return var5;
      } catch (Exception var8) {
         return Optional.empty();
      }
   }

   public static Loaded register(NativeImage img, String prefix) {
      ResourceLocation id = CFHGui.id("dyn/" + prefix + "/" + counter++);
      Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(img));
      return new Loaded(id, img.getWidth(), img.getHeight());
   }

   public static void release(Loaded loaded) {
      if (loaded != null) {
         Minecraft.getInstance().getTextureManager().release(loaded.id());
         CACHE.values().removeIf((l) -> l.id().equals(loaded.id()));
      }
   }

   public static void invalidate(Path file) {
      String key = file.toAbsolutePath().toString();
      CACHE.keySet().removeIf((k) -> k.equals(key) || k.startsWith(key + "@"));
   }

   private Textures() {
   }

   public static record Loaded(ResourceLocation id, int width, int height) {
   }
}
