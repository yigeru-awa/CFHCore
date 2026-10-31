package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.create;

import com.onehumanawa.cfhcore.content.gui.core.stage.node.TerrainNode;
import com.onehumanawa.cfhcore.content.gui.menu.CFHGuiMenu;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.jetbrains.annotations.Nullable;

final class WorldPreview {
   static final int WIDTH = 192;
   static final int DEPTH = 96;
   private static final AtomicInteger THREADS = new AtomicInteger();
   private static final int WORKERS = Math.max(1, Math.min(3, Runtime.getRuntime().availableProcessors() - 2));
   private static final ExecutorService POOL;
   private static final int GRAIN = 2;
   private static final int BLOCKS_X = 96;
   private static final int BLOCKS_Z = 48;
   private static final int[] ORDER;
   private static final int UNKNOWN = Integer.MIN_VALUE;
   private final TerrainNode.Column[] columns = new TerrainNode.Column[18432];
   private final AtomicInteger generation = new AtomicInteger();
   private final AtomicInteger running = new AtomicInteger();
   private volatile State state;
   private volatile @Nullable TerrainNode sink;
   private volatile @Nullable Run current;
   private @Nullable Object key;
   private long seed;
   private Span span;

   WorldPreview() {
      this.state = WorldPreview.State.IDLE;
      this.span = WorldPreview.Span.NEAR;
   }

   private static int[] order() {
      List<Integer> blocks = new ArrayList(4608);

      for(int i = 0; i < 4608; ++i) {
         blocks.add(i);
      }

      blocks.sort(Comparator.comparingDouble((ix) -> {
         double x = (double)(ix % 96) - (double)48.0F + (double)0.5F;
         double z = (double)(ix / 96) - (double)24.0F + (double)0.5F;
         return x * x + z * z * 1.6;
      }));
      int[] out = new int[blocks.size()];

      for(int i = 0; i < out.length; ++i) {
         out[i] = (Integer)blocks.get(i);
      }

      return out;
   }

   State state() {
      return this.state;
   }

   float progress() {
      Run run = this.current;
      return run == null ? 0.0F : (float)run.done.get() / 18432.0F;
   }

   long seed() {
      return this.seed;
   }

   Span span() {
      return this.span;
   }

   void show(@Nullable TerrainNode node) {
      this.sink = node;
      if (node != null) {
         synchronized(this.columns) {
            for(int i = 0; i < this.columns.length; ++i) {
               if (this.columns[i] != null) {
                  node.put(i % 192, i / 192, this.columns[i]);
               }
            }
         }

         if (this.state == WorldPreview.State.WORKING) {
            this.start();
         }

      }
   }

   private void start() {
      for(int w = this.running.get(); w < WORKERS; ++w) {
         this.running.incrementAndGet();
         POOL.execute(() -> {
            try {
               this.work();
            } finally {
               this.running.decrementAndGet();
            }

            if (this.wanted()) {
               this.start();
            }

         });
      }

   }

   private boolean wanted() {
      Run run = this.current;
      return run != null && this.sink != null && this.state == WorldPreview.State.WORKING && this.generation.get() == run.generation && run.next.get() < ORDER.length;
   }

   void update(WorldCreationContext context, Span span) {
      long newSeed = context.options().seed();

      ChunkGenerator generator;
      try {
         generator = context.selectedDimensions().overworld();
      } catch (Exception e) {
         this.fail(e);
         return;
      }

      boolean debug = context.selectedDimensions().isDebug();
      Object newKey = List.of(System.identityHashCode(generator), newSeed, debug, span);
      if (!newKey.equals(this.key)) {
         this.key = newKey;
         this.seed = newSeed;
         this.span = span;
         int run = this.generation.incrementAndGet();
         synchronized(this.columns) {
            Arrays.fill(this.columns, null);
         }

         TerrainNode node = this.sink;
         if (node != null) {
            node.clear();
         }

         this.current = null;
         if (debug) {
            this.state = WorldPreview.State.FAILED;
         } else {
            LandSampler sampler;
            try {
               sampler = LandSampler.of(context, generator, newSeed, span.rise);
            } catch (Exception e) {
               this.fail(e);
               return;
            }

            this.current = new Run(run, sampler, span.step);
            this.state = WorldPreview.State.WORKING;
            this.start();
         }
      }
   }

   private void fail(Exception e) {
      CFHGuiMenu.LOGGER.warn("[CFHGui Menu] no preview for this world type: {}", e.toString());
      this.generation.incrementAndGet();
      this.state = WorldPreview.State.FAILED;
   }

   private void work() {
      Run run = null;

      try {
         while(true) {
            run = this.current;
            if (run == null || this.sink == null || this.state != WorldPreview.State.WORKING) {
               return;
            }

            int n = run.next.getAndIncrement();
            if (n >= ORDER.length) {
               return;
            }

            this.block(run, ORDER[n]);
         }
      } catch (Throwable t) {
         if (run != null && this.generation.compareAndSet(run.generation, run.generation + 1)) {
            CFHGuiMenu.LOGGER.warn("[CFHGui Menu] the world preview stopped: {}", t.toString());
            this.state = WorldPreview.State.FAILED;
         }

      }
   }

   private void block(Run run, int block) {
      int bx = block % 96;
      int bz = block / 96;
      boolean quick = run.sampler.quick();
      int h00 = quick ? 0 : run.height(bx, bz);
      int h10 = quick ? 0 : run.height(bx + 1, bz);
      int h01 = quick ? 0 : run.height(bx, bz + 1);
      int h11 = quick ? 0 : run.height(bx + 1, bz + 1);

      for(int dz = 0; dz < 2; ++dz) {
         for(int dx = 0; dx < 2; ++dx) {
            if (this.generation.get() != run.generation) {
               return;
            }

            int cx = bx * 2 + dx;
            int cz = bz * 2 + dz;
            int wx = (cx - 96) * run.step + run.step / 2;
            int wz = (cz - 48) * run.step + run.step / 2;
            float u = ((float)dx + 0.5F) / 2.0F;
            float v = ((float)dz + 0.5F) / 2.0F;
            float floor = quick ? (float)run.sampler.floor(wx, wz) : ((float)h00 * (1.0F - u) + (float)h10 * u) * (1.0F - v) + ((float)h01 * (1.0F - u) + (float)h11 * u) * v;
            TerrainNode.Column column = run.sampler.column(wx, wz, Math.round(floor));
            synchronized(this.columns) {
               if (this.generation.get() != run.generation) {
                  return;
               }

               this.columns[cz * 192 + cx] = column;
            }

            TerrainNode node = this.sink;
            if (node != null) {
               node.put(cx, cz, column);
            }
         }
      }

      if (run.done.addAndGet(4) >= 18432 && this.generation.get() == run.generation) {
         this.state = WorldPreview.State.DONE;
      }

   }

   static {
      POOL = Executors.newFixedThreadPool(WORKERS, (r) -> {
         Thread t = new Thread(r, "cfhgui-world-preview-" + THREADS.incrementAndGet());
         t.setDaemon(true);
         t.setPriority(1);
         return t;
      });
      ORDER = order();
   }

   static enum State {
      IDLE,
      WORKING,
      DONE,
      FAILED;
   }

   static enum Span {
      NEAR(4, 2),
      WIDER(8, 4),
      WIDEST(16, 7);

      final int step;
      final int rise;

      private Span(final int step, final int rise) {
         this.step = step;
         this.rise = rise;
      }

      int blocksWide() {
         return 192 * this.step;
      }

      int blocksDeep() {
         return 96 * this.step;
      }

      @Nullable Span wider() {
         return this.ordinal() + 1 < values().length ? values()[this.ordinal() + 1] : null;
      }

      @Nullable Span narrower() {
         return this.ordinal() > 0 ? values()[this.ordinal() - 1] : null;
      }

      static Span of(int blocksWide) {
         Span best = NEAR;

         for(Span s : values()) {
            if (Math.abs(s.blocksWide() - blocksWide) < Math.abs(best.blocksWide() - blocksWide)) {
               best = s;
            }
         }

         return best;
      }
   }

   private static final class Run {
      final int generation;
      final LandSampler sampler;
      final int step;
      final AtomicInteger next = new AtomicInteger();
      final AtomicInteger done = new AtomicInteger();
      final AtomicIntegerArray heights = new AtomicIntegerArray(4753);

      Run(int generation, LandSampler sampler, int step) {
         this.generation = generation;
         this.sampler = sampler;
         this.step = step;

         for(int i = 0; i < this.heights.length(); ++i) {
            this.heights.set(i, Integer.MIN_VALUE);
         }

      }

      int height(int gx, int gz) {
         int i = gz * 97 + gx;
         int h = this.heights.get(i);
         if (h == Integer.MIN_VALUE) {
            h = this.sampler.floor((gx * 2 - 96) * this.step, (gz * 2 - 48) * this.step);
            this.heights.set(i, h);
         }

         return h;
      }
   }
}
