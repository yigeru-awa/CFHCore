package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play.map;

import com.mojang.blaze3d.platform.NativeImage;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Textures;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.play.PlayIo;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public final class MapTiles implements AutoCloseable {
   static final int KEEP = 48;
   private final Source source;
   private final Map<Long, Tile> tiles = new HashMap();
   private boolean closed;
   private long clock;
   private int loaded;

   private MapTiles(Source source) {
      this.source = source;
   }

   public static MapTiles ofWorld(Path worldFolder) {
      Path regions = worldFolder.resolve("region");
      return new MapTiles((x, z) -> {
         RegionMap.Picture p = RegionMap.render(regions.resolve("r." + x + "." + z + ".mca"));
         if (p == null) {
            return null;
         } else {
            NativeImage img = new NativeImage(512, 512, false);
            int[] px = p.abgr();

            for(int y = 0; y < 512; ++y) {
               for(int i = 0; i < 512; ++i) {
                  img.setPixelRGBA(i, y, px[y * 512 + i]);
               }
            }

            return img;
         }
      });
   }

   public static @Nullable MapTiles journeyMap(Path dayFolder) {
      return !Files.isDirectory(dayFolder, new LinkOption[0]) ? null : new MapTiles((x, z) -> {
         Path file = dayFolder.resolve(x + "," + z + ".png");
         if (!Files.isRegularFile(file, new LinkOption[0])) {
            return null;
         } else {
            try {
               InputStream in = Files.newInputStream(file);

               NativeImage var5;
               try {
                  var5 = NativeImage.read(in);
               } catch (Throwable var8) {
                  if (in != null) {
                     try {
                        in.close();
                     } catch (Throwable var7) {
                        var8.addSuppressed(var7);
                     }
                  }

                  throw var8;
               }

               if (in != null) {
                  in.close();
               }

               return var5;
            } catch (Exception var9) {
               return null;
            }
         }
      });
   }

   public Textures.@Nullable Loaded tile(int x, int z) {
      if (this.closed) {
         return null;
      } else {
         long key = (long)x << 32 ^ (long)z & 4294967295L;
         Tile t = (Tile)this.tiles.get(key);
         if (t == null) {
            t = new Tile();
            this.tiles.put(key, t);
            this.request(key, x, z, t);
            this.trim();
         }

         t.usedAt = ++this.clock;
         return t.state == MapTiles.State.READY ? t.texture : null;
      }
   }

   public boolean loading(int x, int z) {
      Tile t = (Tile)this.tiles.get((long)x << 32 ^ (long)z & 4294967295L);
      return t != null && t.state == MapTiles.State.LOADING;
   }

   public int loadedCount() {
      return this.loaded;
   }

   public boolean anyLoading() {
      for(Tile t : this.tiles.values()) {
         if (t.state == MapTiles.State.LOADING) {
            return true;
         }
      }

      return false;
   }

   private void request(long key, int x, int z, Tile tile) {
      CompletableFuture.supplyAsync(() -> {
         try {
            return this.source.load(x, z);
         } catch (Exception e) {
            CFHGuiMenu.LOGGER.debug("[CFHGui Menu] map tile {},{} failed: {}", new Object[]{x, z, e.toString()});
            return null;
         }
      }, PlayIo.POOL).thenAcceptAsync((img) -> {
         if (!this.closed && this.tiles.get(key) == tile) {
            if (img == null) {
               tile.state = MapTiles.State.EMPTY;
            } else {
               tile.texture = Textures.register(img, "map");
               tile.state = MapTiles.State.READY;
               ++this.loaded;
            }
         } else {
            if (img != null) {
               img.close();
            }

         }
      }, Minecraft.getInstance());
   }

   private void trim() {
      int ready = 0;

      for(Tile t : this.tiles.values()) {
         if (t.state == MapTiles.State.READY) {
            ++ready;
         }
      }

      if (ready > 48) {
         List<Map.Entry<Long, Tile>> byAge = new ArrayList(this.tiles.entrySet());
         byAge.sort((a, b) -> Long.compare(((Tile)a.getValue()).usedAt, ((Tile)b.getValue()).usedAt));

         for(Map.Entry<Long, Tile> e : byAge) {
            if (ready <= 48) {
               break;
            }

            Tile t = (Tile)e.getValue();
            if (t.state == MapTiles.State.READY) {
               if (t.texture != null) {
                  Textures.release(t.texture);
               }

               this.tiles.remove(e.getKey());
               --ready;
            }
         }

      }
   }

   public void close() {
      this.closed = true;

      for(Tile t : this.tiles.values()) {
         if (t.texture != null) {
            Textures.release(t.texture);
         }
      }

      this.tiles.clear();
   }

   private static enum State {
      LOADING,
      READY,
      EMPTY;
   }

   private static final class Tile {
      State state;
      Textures.@Nullable Loaded texture;
      long usedAt;

      private Tile() {
         this.state = MapTiles.State.LOADING;
      }
   }

   private interface Source {
      @Nullable NativeImage load(int var1, int var2);
   }
}
