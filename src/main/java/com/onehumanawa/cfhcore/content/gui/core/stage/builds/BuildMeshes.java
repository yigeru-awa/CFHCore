package com.onehumanawa.cfhcore.content.gui.core.stage.builds;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageResources;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageTrouble;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.BlockAndTintGetter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public final class BuildMeshes {
   private static final long UPLOAD_BUDGET_NS = 4000000L;
   private static final int IN_FLIGHT;
   private static final int EMPTY = 0;
   private static final int WANTED = 1;
   private static final int QUEUED = 2;
   private static final int MESHING = 3;
   private static final int READY = 4;
   final BuildSpec spec;
   private final boolean soft;
   private final BuildCamera camera;
   final CompletableFuture<BuildVolume> volume;
   private @Nullable BuildVolume loaded;
   private boolean failed;
   private int piecesX;
   private int piecesY;
   private int piecesZ;
   private int[] state = new int[0];
   private VertexBuffer[][] buffers = new VertexBuffer[0][];
   private final Set<TextureAtlasSprite> animated = new HashSet();
   private int[] order = new int[0];
   private int cursor;
   private final ConcurrentLinkedQueue<Meshed> done = new ConcurrentLinkedQueue();
   private final AtomicInteger inFlight = new AtomicInteger();
   private volatile boolean closed;
   private int generation = -1;
   private final AtomicInteger epoch = new AtomicInteger();
   private int quads;
   private long startedMs;
   int users;
   long idleSinceMs;
   final String key;
   private final String name;
   private final @Nullable float[] rim;
   private @Nullable BlockAndTintGetter view;
   private @Nullable boolean[] restVisible;

   BuildMeshes(String key, BuildSpec spec, String name, boolean soft, BuildCamera camera, CompletableFuture<BuildVolume> volume, @Nullable float[] rim) {
      this.key = key;
      this.spec = spec;
      this.name = name;
      this.soft = soft;
      this.camera = camera;
      this.volume = volume;
      this.rim = rim;
      this.startedMs = System.currentTimeMillis();
   }

   public boolean soft() {
      return this.soft;
   }

   public @Nullable BuildVolume loaded() {
      return this.loaded;
   }

   public boolean failed() {
      return this.failed;
   }

   public int quads() {
      return this.quads;
   }

   public int piecesX() {
      return this.piecesX;
   }

   public int piecesY() {
      return this.piecesY;
   }

   public int piecesZ() {
      return this.piecesZ;
   }

   public boolean ready(int piece) {
      return piece >= 0 && piece < this.state.length && (this.state[piece] == 4 || this.state[piece] == 0);
   }

   public boolean hasBlocks(int piece) {
      return piece >= 0 && piece < this.state.length && this.state[piece] != 0;
   }

   public @Nullable VertexBuffer buffer(int piece, int layer) {
      VertexBuffer[] b = piece >= 0 && piece < this.buffers.length ? this.buffers[piece] : null;
      return b == null ? null : b[layer];
   }

   public Set<TextureAtlasSprite> animated() {
      return this.animated;
   }

   public void pump() {
      if (!this.closed && !this.failed) {
         if (this.loaded == null) {
            if (!this.volume.isDone()) {
               return;
            }

            try {
               this.loaded = (BuildVolume)this.volume.join();
            } catch (RuntimeException e) {
               this.failed = true;
               CFHGui.LOGGER.warn("[CFHGui] build {} could not be read: {}", this.name, e.getCause() != null ? e.getCause().toString() : e.toString());
               return;
            }

            this.plan(this.loaded);
         }

         if (!(Minecraft.getInstance().getOverlay() instanceof LoadingOverlay)) {
            int gen = StageResources.generation();
            if (gen != this.generation) {
               if (this.generation != -1) {
                  this.forget();
               }

               this.generation = gen;
            }

            long start = System.nanoTime();

            Meshed m;
            while(System.nanoTime() - start < 4000000L && (m = (Meshed)this.done.poll()) != null) {
               this.upload(m);
            }

            this.schedule();
         }
      }
   }

   public void want(int piece) {
      if (piece >= 0 && piece < this.state.length && this.state[piece] == 1) {
         this.state[piece] = 2;
         int[] next = new int[this.order.length - this.cursor + 1];
         next[0] = piece;
         System.arraycopy(this.order, this.cursor, next, 1, this.order.length - this.cursor);
         this.order = next;
         this.cursor = 0;
      }

   }

   public boolean restReady() {
      if (this.loaded == null) {
         return false;
      } else {
         for(int i = 0; i < this.state.length; ++i) {
            if ((this.state[i] == 2 || this.state[i] == 3) && this.restVisible != null && this.restVisible[i]) {
               return false;
            }
         }

         return true;
      }
   }

   private void plan(BuildVolume v) {
      int n = 32;
      this.piecesX = (v.sizeX() + 32 - 1) / 32;
      this.piecesY = (v.sizeY() + 32 - 1) / 32;
      this.piecesZ = (v.sizeZ() + 32 - 1) / 32;
      int count = this.piecesX * this.piecesY * this.piecesZ;
      this.state = new int[count];
      this.buffers = new VertexBuffer[count][];
      this.restVisible = new boolean[count];
      List<Integer> queue = new ArrayList();
      float[] distance = new float[count];
      Vector3f rest = this.camera.rest();
      int wanted = 0;

      for(int i = 0; i < count; ++i) {
         int px = i % this.piecesX;
         int pz = i / this.piecesX % this.piecesZ;
         int py = i / (this.piecesX * this.piecesZ);
         int x0 = px * 32;
         int y0 = py * 32;
         int z0 = pz * 32;
         if (v.emptyBetween(x0, y0, z0, x0 + 32, y0 + 32, z0 + 32)) {
            this.state[i] = 0;
         } else {
            float x1 = (float)Math.min(v.sizeX(), x0 + 32);
            float y1 = (float)Math.min(v.sizeY(), y0 + 32);
            float z1 = (float)Math.min(v.sizeZ(), z0 + 32);
            if (this.rim != null && this.pastRim(v, (float)x0, (float)z0, x1, z1)) {
               this.state[i] = 0;
            } else {
               float cx = clamp(rest.x, (float)x0, x1);
               float cy = clamp(rest.y, (float)y0, y1);
               float cz = clamp(rest.z, (float)z0, z1);
               distance[i] = (cx - rest.x) * (cx - rest.x) + (cy - rest.y) * (cy - rest.y) + (cz - rest.z) * (cz - rest.z);
               this.restVisible[i] = this.camera.seenAtRest((float)x0, (float)y0, (float)z0, x1, y1, z1);
               if (!(distance[i] > this.camera.far() * this.camera.far()) && this.camera.sees((float)x0, (float)y0, (float)z0, x1, y1, z1)) {
                  this.state[i] = 2;
                  queue.add(i);
                  ++wanted;
               } else {
                  this.state[i] = 1;
               }
            }
         }
      }

      queue.sort(Comparator.comparingInt((Integer ix) -> this.restVisible[ix] ? 0 : 1).thenComparingDouble((Integer ix) -> (double)distance[ix]));
      this.order = queue.stream().mapToInt(Integer::intValue).toArray();
      this.cursor = 0;
      CFHGui.LOGGER.info("[CFHGui] build {}: {}x{}x{}, {} {}, {} of {} pieces to mesh{}", new Object[]{this.name, v.sizeX(), v.sizeY(), v.sizeZ(), v.blocks(), v.isLand() ? "columns" : "blocks", wanted, count, this.soft ? "" : " (the game's own light)"});
   }

   private static float clamp(float v, float lo, float hi) {
      return v < lo ? lo : Math.min(v, hi);
   }

   private boolean pastRim(BuildVolume v, float x0, float z0, float x1, float z1) {
      float hx = (float)v.sizeX() / 2.0F;
      float hz = (float)v.sizeZ() / 2.0F;
      double qx = (double)(Math.abs(clamp(hx, x0, x1) - hx) / Math.max(1.0E-4F, hx));
      double qz = (double)(Math.abs(clamp(hz, z0, z1) - hz) / Math.max(1.0E-4F, hz));
      double r = (double)Math.max(1.0F, this.rim == null ? 3.0F : this.rim[1]);
      return Math.pow(Math.pow(qx, r) + Math.pow(qz, r), (double)1.0F / r) >= (double)1.0F;
   }

   private void schedule() {
      BuildVolume v = this.loaded;
      if (v != null) {
         if (this.view == null) {
            this.view = v.view(this.soft);
         }

         BlockAndTintGetter w = this.view;
         BuildMesher.Cull cull = Boolean.getBoolean("cfhgui.buildNoCull") ? null : this.camera.cull();
         Vector3f sortFrom = new Vector3f(this.camera.rest());

         while(this.inFlight.get() < IN_FLIGHT && this.cursor < this.order.length) {
            int piece = this.order[this.cursor++];
            if (this.state[piece] == 2) {
               this.state[piece] = 3;
               this.inFlight.incrementAndGet();
               int n = 32;
               int x0 = piece % this.piecesX * 32;
               int z0 = piece / this.piecesX % this.piecesZ * 32;
               int y0 = piece / (this.piecesX * this.piecesZ) * 32;
               int at = this.epoch.get();
               BuildWork.POOL.execute(() -> {
                  BuildMesher.Piece result = null;

                  try {
                     if (!this.closed && at == this.epoch.get()) {
                        result = BuildMesher.mesh(v, w, piece, x0, y0, z0, this.soft, cull, sortFrom);
                     }
                  } catch (Throwable t) {
                     StageTrouble.once("a piece of the build " + this.name, "left empty", t);
                  } finally {
                     this.inFlight.decrementAndGet();
                  }

                  if (result == null) {
                     this.done.add(new Meshed(at, new BuildMesher.Piece(piece, List.of(), Set.of(), 0)));
                  } else if (!this.closed && at == this.epoch.get()) {
                     this.done.add(new Meshed(at, result));
                     Meshed left;
                     if (this.closed) {
                        while((left = (Meshed)this.done.poll()) != null) {
                           left.piece().close();
                        }
                     }
                  } else {
                     result.close();
                  }

               });
            }
         }

      }
   }

   private void upload(Meshed m) {
      BuildMesher.Piece p = m.piece();
      if (!this.closed && m.epoch() == this.epoch.get() && p.index() < this.state.length) {
         VertexBuffer[] layers = new VertexBuffer[BuildMesher.LAYERS.size()];

         for(BuildMesher.Layer l : p.layers()) {
            int i = BuildMesher.LAYERS.indexOf(l.type());

            try {
               VertexBuffer vbo = new VertexBuffer(Usage.STATIC);
               vbo.bind();
               vbo.upload(l.data());
               VertexBuffer.unbind();
               if (i >= 0) {
                  layers[i] = vbo;
               } else {
                  vbo.close();
               }
            } catch (RuntimeException e) {
               StageTrouble.once("putting a piece of the build " + this.name + " on the card", "that piece is left empty", e);
            } finally {
               for(ByteBufferBuilder b : l.memory()) {
                  b.close();
               }

            }
         }

         VertexBuffer[] old = this.buffers[p.index()];
         if (old != null) {
            for(VertexBuffer b : old) {
               if (b != null) {
                  b.close();
               }
            }
         }

         this.buffers[p.index()] = layers;
         this.state[p.index()] = 4;
         this.animated.addAll(p.animated());
         this.quads += p.quads();
         if (this.cursor >= this.order.length && this.inFlight.get() == 0 && this.done.isEmpty()) {
            CFHGui.LOGGER.info("[CFHGui] build {}: meshed in {} ms, {} faces", new Object[]{this.name, System.currentTimeMillis() - this.startedMs, this.quads});
         }

      } else {
         p.close();
      }
   }

   private void forget() {
      this.epoch.incrementAndGet();

      Meshed m;
      while((m = (Meshed)this.done.poll()) != null) {
         m.piece().close();
      }

      for(VertexBuffer[] layers : this.buffers) {
         if (layers != null) {
            for(VertexBuffer b : layers) {
               if (b != null) {
                  b.close();
               }
            }
         }
      }

      Arrays.fill(this.buffers, null);
      this.animated.clear();
      this.quads = 0;
      this.startedMs = System.currentTimeMillis();
      if (this.loaded != null) {
         this.plan(this.loaded);
      }

   }

   void close() {
      if (!this.closed) {
         this.closed = true;
         this.epoch.incrementAndGet();

         Meshed m;
         while((m = (Meshed)this.done.poll()) != null) {
            m.piece().close();
         }

         for(VertexBuffer[] layers : this.buffers) {
            if (layers != null) {
               for(VertexBuffer b : layers) {
                  if (b != null) {
                     b.close();
                  }
               }
            }
         }

         this.buffers = new VertexBuffer[0][];
         this.state = new int[0];
      }
   }

   boolean closed() {
      return this.closed;
   }

   static {
      IN_FLIGHT = BuildWork.THREADS;
   }

   private static record Meshed(int epoch, BuildMesher.Piece piece) {
   }
}
