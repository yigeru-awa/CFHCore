package com.onehumanawa.cfhcore.content.gui.menu.client.loading.journey;

import com.onehumanawa.cfhcore.content.gui.core.stage.node.TerrainNode;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.create.LandSampler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import org.jetbrains.annotations.Nullable;

final class Land {
   static final int WIDTH = 160;
   static final int DEPTH = 128;
   static final int STEP = 4;
   static final int TILE = 8;
   static final int TILES_X = 20;
   static final int TILES_Z = 16;
   static final int SUNK = 11;
   static final int NATURAL = 3;
   static final int BUILT = 9999;
   private static final AtomicInteger THREADS = new AtomicInteger();
   private static final int WORKERS = Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() - 3));
   private static final ExecutorService POOL;
   private static final int GRAIN = 2;
   private static final int BLOCKS_X = 80;
   private static final int BLOCKS_Z = 64;
   private static final int[] ORDER;
   private static final int UNKNOWN = Integer.MIN_VALUE;
   private final AtomicReferenceArray<Cell> cells = new AtomicReferenceArray(20480);
   private final AtomicIntegerArray looked = new AtomicIntegerArray(320);
   private final AtomicInteger filled = new AtomicInteger();
   private volatile boolean closed;
   private volatile boolean asking;
   private volatile boolean dropped;
   private int lift;
   private boolean compared;
   final int centerX;
   final int centerZ;
   volatile int dayTime;
   volatile int sea = sunk(Math.round(19.5F));

   static int sunk(int cells) {
      return Math.max(1, cells - 11);
   }

   Land(int centerX, int centerZ) {
      this.centerX = Math.floorDiv(centerX, 4) * 4;
      this.centerZ = Math.floorDiv(centerZ, 4) * 4;
   }

   private static int[] order() {
      List<Integer> blocks = new ArrayList(5120);

      for(int i = 0; i < 5120; ++i) {
         blocks.add(i);
      }

      blocks.sort(Comparator.comparingDouble((ix) -> {
         double x = (double)(ix % 80) - (double)40.0F + (double)0.5F;
         double z = (double)(ix / 80) - (double)32.0F + (double)0.5F;
         return x * x + z * z;
      }));
      int[] out = new int[blocks.size()];

      for(int i = 0; i < out.length; ++i) {
         out[i] = (Integer)blocks.get(i);
      }

      return out;
   }

   int blockX(int x) {
      return this.centerX + (x - 80) * 4 + 2;
   }

   int blockZ(int z) {
      return this.centerZ + (z - 64) * 4 + 2;
   }

   @Nullable Cell at(int x, int z) {
      return x >= 0 && z >= 0 && x < 160 && z < 128 ? (Cell)this.cells.get(z * 160 + x) : null;
   }

   void lay(int x, int z, @Nullable Cell cell) {
      if (x >= 0 && z >= 0 && x < 160 && z < 128) {
         int i = z * 160 + x;
         if (cell != null && this.cells.compareAndSet(i, null, cell.shallow())) {
            this.filled.incrementAndGet();
         }

      }
   }

   private void seen(int x, int z) {
      this.looked.incrementAndGet(z / 8 * 20 + x / 8);
   }

   boolean ready(int tx, int tz) {
      return this.closed || this.looked.get(tz * 20 + tx) >= 64;
   }

   boolean any() {
      return this.filled.get() > 0;
   }

   void close() {
      this.closed = true;
   }

   boolean closed() {
      return this.closed;
   }

   void drop() {
      this.dropped = true;
   }

   void ask(LandSampler sampler) {
      if (!this.asking && !this.closed) {
         this.asking = true;
         this.sea = sunk(sampler.cells(sampler.sea()));
         AtomicInteger next = new AtomicInteger();
         AtomicInteger running = new AtomicInteger(WORKERS);
         AtomicIntegerArray heights = new AtomicIntegerArray(5265);

         for(int i = 0; i < heights.length(); ++i) {
            heights.set(i, Integer.MIN_VALUE);
         }

         for(int w = 0; w < WORKERS; ++w) {
            POOL.execute(() -> {
               try {
                  this.work(sampler, next, heights);
               } catch (Throwable t) {
                  CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the land of the world could not be worked out: {}", t.toString());
                  this.closed = true;
               } finally {
                  if (running.decrementAndGet() == 0) {
                     this.closed = true;
                  }

               }

            });
         }

      }
   }

   private int height(LandSampler sampler, AtomicIntegerArray heights, int gx, int gz) {
      int i = gz * 81 + gx;
      int h = heights.get(i);
      if (h == Integer.MIN_VALUE) {
         h = sampler.floor(this.centerX + (gx * 2 - 80) * 4, this.centerZ + (gz * 2 - 64) * 4);
         heights.set(i, h);
      }

      return h;
   }

   private int compare(LandSampler sampler) {
      List<Integer> off = new ArrayList();

      for(int z = 3; z < 128 && off.size() < 120; z += 7) {
         for(int x = 3; x < 160 && off.size() < 120; x += 7) {
            Cell seen = (Cell)this.cells.get(z * 160 + x);
            if (seen != null && seen.water() <= 0 && seen.strata() == 3) {
               int wx = this.blockX(x);
               int wz = this.blockZ(z);
               Cell said = Land.Cell.of(sampler.column(wx, wz, sampler.floor(wx, wz)));
               if (said.water() <= 0) {
                  off.add(seen.ground() - said.ground());
               }
            }
         }
      }

      if (off.size() < 8) {
         return 0;
      } else {
         off.sort(Comparator.naturalOrder());
         return Math.max(-8, Math.min(8, (Integer)off.get(off.size() / 2)));
      }
   }

   private void work(LandSampler sampler, AtomicInteger next, AtomicIntegerArray heights) {
      boolean quick = sampler.quick();
      int raise;
      synchronized(this) {
         if (!this.compared) {
            this.lift = this.filled.get() > 0 ? this.compare(sampler) : 0;
            this.compared = true;
         }

         raise = this.lift;
      }

      while(!this.dropped) {
         int n = next.getAndIncrement();
         if (n >= ORDER.length) {
            return;
         }

         int block = ORDER[n];
         int bx = block % 80;
         int bz = block / 80;
         boolean missing = false;

         for(int dz = 0; dz < 2 && !missing; ++dz) {
            for(int dx = 0; dx < 2; ++dx) {
               if (this.cells.get((bz * 2 + dz) * 160 + bx * 2 + dx) == null) {
                  missing = true;
                  break;
               }
            }
         }

         if (missing) {
            int h00 = quick ? 0 : this.height(sampler, heights, bx, bz);
            int h10 = quick ? 0 : this.height(sampler, heights, bx + 1, bz);
            int h01 = quick ? 0 : this.height(sampler, heights, bx, bz + 1);
            int h11 = quick ? 0 : this.height(sampler, heights, bx + 1, bz + 1);

            for(int dz = 0; dz < 2; ++dz) {
               for(int dx = 0; dx < 2; ++dx) {
                  int cx = bx * 2 + dx;
                  int cz = bz * 2 + dz;
                  if (this.cells.get(cz * 160 + cx) == null) {
                     int wx = this.blockX(cx);
                     int wz = this.blockZ(cz);
                     float u = ((float)dx + 0.5F) / 2.0F;
                     float v = ((float)dz + 0.5F) / 2.0F;
                     float floor = quick ? (float)sampler.floor(wx, wz) : ((float)h00 * (1.0F - u) + (float)h10 * u) * (1.0F - v) + ((float)h01 * (1.0F - u) + (float)h11 * u) * v;
                     Cell cell = Land.Cell.of(sampler.column(wx, wz, Math.round(floor)));
                     this.lay(cx, cz, raise != 0 && cell.water() <= 0 ? cell.raised(raise) : cell);
                  }
               }
            }
         }

         for(int dz = 0; dz < 2; ++dz) {
            for(int dx = 0; dx < 2; ++dx) {
               this.seen(bx * 2 + dx, bz * 2 + dz);
            }
         }
      }

   }

   static {
      POOL = Executors.newFixedThreadPool(WORKERS, (r) -> {
         Thread t = new Thread(r, "cfhgui-land-" + THREADS.incrementAndGet());
         t.setDaemon(true);
         t.setPriority(1);
         return t;
      });
      ORDER = order();
   }

   static record Cell(int ground, String top, int topTint, String under, int underTint, int strata, int water, int waterTint, boolean frozen, @Nullable String trunk, @Nullable String canopy, int canopyTint) {
      static Cell of(TerrainNode.Column c) {
         int ground = Land.sunk(c.ground());
         int level = Land.sunk(c.ground() + Math.max(0, c.water()));
         return new Cell(ground, c.top(), c.topTint(), c.under(), -1, 3, c.water() > 0 ? Math.max(1, level - ground) : 0, c.waterTint(), c.frozen(), c.trunk(), c.canopy(), c.canopyTint());
      }

      boolean tree() {
         return this.trunk != null && this.canopy != null && this.water <= 0;
      }

      Cell raised(int by) {
         return new Cell(Math.max(1, this.ground + by), this.top, this.topTint, this.under, this.underTint, this.strata, this.water, this.waterTint, this.frozen, this.trunk, this.canopy, this.canopyTint);
      }

      Cell withTree(String bark, String leaves, int leafTint) {
         return new Cell(this.ground, this.top, this.topTint, this.under, this.underTint, this.strata, this.water, this.waterTint, this.frozen, bark, leaves, leafTint);
      }

      Cell shallow() {
         return this.water <= 4 ? this : new Cell(this.ground + this.water - 4, this.top, this.topTint, this.under, this.underTint, this.strata, 4, this.waterTint, this.frozen, this.trunk, this.canopy, this.canopyTint);
      }

      int level() {
         return this.ground + this.water;
      }
   }
}
