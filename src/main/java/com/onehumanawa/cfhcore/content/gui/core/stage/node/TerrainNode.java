package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageResources;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageSoft;
import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.QuadMesh;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public final class TerrainNode extends StageNode {
   private static final int TEXELS = 8;
   private static final int TILE = 32;
   private static final long REMESH_MS = 280L;
   private static final long BUDGET_NS = 4000000L;
   private static final int WHITE = -1;
   private final int width;
   private final int depth;
   private final int tilesX;
   private final int tilesZ;
   private final AtomicReferenceArray<Column> cells;
   private final BitSet holes = new BitSet();
   private final AtomicInteger filled = new AtomicInteger();
   private final AtomicIntegerArray changes;
   private final int[] meshedChanges;
   private final long[] meshedMs;
   private final QuadMesh[] meshes;
   private final int[] order;
   private final Map<String, TextureAtlasSprite> sprites = new HashMap();
   private int meshResources = -1;
   private boolean broken;
   private boolean shown;
   private final Anim rise;
   private boolean still;
   private float gloss;
   private float edgeFrom;
   private float edgeRound;
   private volatile int shelfRows;
   private volatile int shelfLevel;
   private final float[] shades;
   private final QuadMesh.Shade shade;
   private final Vector3f normal;

   public TerrainNode(int width, int depth) {
      this.rise = new Anim(0.0F, 900, Ease.OUT_CUBIC);
      this.edgeFrom = 1.0F;
      this.shades = new float[6];
      this.shade = (d) -> this.shades[d.ordinal()];
      this.normal = new Vector3f();
      this.width = Math.max(1, width);
      this.depth = Math.max(1, depth);
      this.cells = new AtomicReferenceArray(this.width * this.depth);
      this.tilesX = (this.width + 32 - 1) / 32;
      this.tilesZ = (this.depth + 32 - 1) / 32;
      int tiles = this.tilesX * this.tilesZ;
      this.changes = new AtomicIntegerArray(tiles);
      this.meshedChanges = new int[tiles];
      this.meshedMs = new long[tiles];
      this.meshes = new QuadMesh[tiles];
      List<Integer> byDistance = new ArrayList(tiles);

      for(int t = 0; t < tiles; ++t) {
         byDistance.add(t);
      }

      byDistance.sort(Comparator.comparingDouble((tx) -> {
         double x = ((double)(tx % this.tilesX) + (double)0.5F) * (double)32.0F - (double)this.width / (double)2.0F;
         double z = ((double)(tx / this.tilesX) + (double)0.5F) * (double)32.0F - (double)this.depth / (double)2.0F;
         return x * x + z * z;
      }));
      this.order = byDistance.stream().mapToInt(Integer::intValue).toArray();
      this.bounds((float)(-this.width) / 2.0F, 0.0F, (float)(-this.depth) / 2.0F, (float)this.width / 2.0F, 40.0F, (float)this.depth / 2.0F);
      this.hoverFeel(0.0F, 1.0F);
      this.pickable(false);
      this.named("terrain");
   }

   public int width() {
      return this.width;
   }

   public int depth() {
      return this.depth;
   }

   public TerrainNode still() {
      this.still = true;
      this.gloss = 1.0F;
      return this;
   }

   public TerrainNode edgeFade(float from, float roundness) {
      this.edgeFrom = Mth.clamp(from, 0.0F, 0.999F);
      this.edgeRound = Math.max(0.0F, roundness);
      return this;
   }

   public TerrainNode shelf(int rows, int level) {
      int r = Math.max(0, rows);
      if (r == this.shelfRows && level == this.shelfLevel) {
         return this;
      } else {
         int from = Math.max(0, this.depth - 1 - Math.max(r, this.shelfRows)) / 32;
         this.shelfRows = r;
         this.shelfLevel = level;

         for(int t = from * this.tilesX; t < this.tilesX * this.tilesZ; ++t) {
            this.changes.incrementAndGet(t);
         }

         return this;
      }
   }

   public void hole(int x, int z) {
      if (x >= 0 && z >= 0 && x < this.width && z < this.depth) {
         this.holes.set(z * this.width + x);
         this.touch(x, z);
      }
   }

   private boolean isHole(int x, int z) {
      return x >= 0 && z >= 0 && x < this.width && z < this.depth && this.holes.get(z * this.width + x);
   }

   public void put(int x, int z, Column column) {
      if (x >= 0 && z >= 0 && x < this.width && z < this.depth) {
         if (this.cells.getAndSet(z * this.width + x, column) == null) {
            this.filled.incrementAndGet();
         }

         this.touch(x, z);
      }
   }

   public void clear() {
      for(int i = 0; i < this.cells.length(); ++i) {
         this.cells.set(i, null);
      }

      this.filled.set(0);

      for(int t = 0; t < this.changes.length(); ++t) {
         this.changes.incrementAndGet(t);
      }

   }

   private void touch(int x, int z) {
      int tx = x / 32;
      int tz = z / 32;
      int lx = x - tx * 32;
      int lz = z - tz * 32;
      this.changes.incrementAndGet(tz * this.tilesX + tx);
      if (lx == 0 && tx > 0) {
         this.changes.incrementAndGet(tz * this.tilesX + tx - 1);
      }

      if (lx == 31 && tx < this.tilesX - 1) {
         this.changes.incrementAndGet(tz * this.tilesX + tx + 1);
      }

      if (lz == 0 && tz > 0) {
         this.changes.incrementAndGet((tz - 1) * this.tilesX + tx);
      }

      if (lz == 31 && tz < this.tilesZ - 1) {
         this.changes.incrementAndGet((tz + 1) * this.tilesX + tx);
      }

   }

   public int filled() {
      return this.filled.get();
   }

   public boolean complete() {
      return this.filled.get() >= this.width * this.depth;
   }

   public float progress() {
      return (float)this.filled.get() / (float)(this.width * this.depth);
   }

   private TextureAtlasSprite sprite(String texture) {
      return (TextureAtlasSprite)this.sprites.computeIfAbsent(texture, QuadMesh::sprite);
   }

   private @Nullable Column at(int x, int z) {
      if (x >= 0 && z >= 0 && x < this.width && z < this.depth) {
         Column c = (Column)this.cells.get(z * this.width + x);
         int rows = this.shelfRows;
         int level = this.shelfLevel;
         int left = this.depth - 1 - z;
         if (c != null && left < rows && c.ground() > level) {
            float t = Mth.clamp(((float)left + 0.5F) / (float)rows, 0.0F, 1.0F);
            t = t * t * (3.0F - 2.0F * t);
            int ground = Math.max(level, Math.round((float)level + (float)(c.ground() - level) * t));
            return ground == c.ground() ? c : c.withGround(ground);
         } else {
            return c;
         }
      } else {
         return null;
      }
   }

   private static String stratum(Column c, int below) {
      if (below <= 0) {
         return c.top();
      } else if (below <= 3) {
         return c.under();
      } else {
         return below <= 14 ? "stone" : "deepslate";
      }
   }

   private QuadMesh build(int tile) {
      QuadMesh.Builder b = QuadMesh.builder();
      int ox = -this.width / 2;
      int oz = -this.depth / 2;
      int x0 = tile % this.tilesX * 32;
      int z0 = tile / this.tilesX * 32;
      int x1 = Math.min(this.width, x0 + 32);
      int z1 = Math.min(this.depth, z0 + 32);

      for(int z = z0; z < z1; ++z) {
         for(int x = x0; x < x1; ++x) {
            Column c = this.at(x, z);
            if (c != null && c.ground() > 0) {
               int wx = ox + x;
               int wz = oz + z;
               if (!c.floats()) {
                  this.land(b, c, x, z, wx, wz, 0);
               } else {
                  this.land(b, c, x, z, wx, wz, c.floor());
                  Column low = c.beneath();
                  if (low == null || !low.solid(c.floor() - 1)) {
                     b.face(false, Direction.DOWN, wx, c.floor(), wz, this.sprite(stratum(c, c.ground() - 1 - c.floor())), 8, -1);
                  }

                  if (low != null && low.ground() > 0) {
                     this.land(b, low, x, z, wx, wz, 0);
                  }
               }
            }
         }
      }

      return b.build();
   }

   private void land(QuadMesh.Builder b, Column c, int x, int z, int wx, int wz, int from) {
      int g = Math.max(1, c.ground());
      b.face(false, Direction.UP, wx, g - 1, wz, this.sprite(c.top()), 8, c.topTint());
      int level = g + Math.max(0, c.water());
      if (c.water() > 0) {
         if (c.frozen()) {
            b.face(false, Direction.UP, wx, level - 1, wz, this.sprite("ice"), 8, -1);
         } else {
            b.face(true, Direction.UP, wx, level - 1, wz, this.sprite("water_still"), 8, c.waterTint() & 16777215 | -771751936);
         }
      }

      for(Direction d : Plane.HORIZONTAL) {
         if (!this.isHole(x + d.getStepX(), z + d.getStepZ())) {
            Column n = this.at(x + d.getStepX(), z + d.getStepZ());

            for(int y = from; y < g; ++y) {
               if (n == null || !n.solid(y)) {
                  int below = g - 1 - y;
                  b.face(false, d, wx, y, wz, this.sprite(stratum(c, below)), 8, below == 0 ? c.topTint() : -1);
               }
            }

            if (c.water() > 0) {
               for(int y = g; y < level; ++y) {
                  if (n == null || !n.solid(y) && !n.wet(y)) {
                     if (c.frozen() && y == level - 1) {
                        b.face(false, d, wx, y, wz, this.sprite("ice"), 8, -1);
                     } else {
                        b.face(true, d, wx, y, wz, this.sprite("water_still"), 8, c.waterTint() & 16777215 | -771751936);
                     }
                  }
               }
            }
         }
      }

      if (c.tree()) {
         TextureAtlasSprite bark = this.sprite(c.trunk());
         TextureAtlasSprite leaves = this.sprite(c.canopy());
         b.face(false, Direction.UP, wx, g + 1, wz, leaves, 8, c.canopyTint());

         for(Direction d : Plane.HORIZONTAL) {
            Column n = this.at(x + d.getStepX(), z + d.getStepZ());
            int ng = n != null && n.ground() > 0 ? Math.max(1, n.ground()) : 0;
            boolean beside = n != null && n.tree() && ng == g;
            if (!beside && ng <= g) {
               b.face(false, d, wx, g, wz, bark, 16, -1);
            }

            if (!beside && ng <= g + 1) {
               b.face(false, d, wx, g + 1, wz, leaves, 8, c.canopyTint());
            }
         }
      }

   }

   private void ensureMesh() {
      int gen = StageResources.generation();
      if (gen != this.meshResources) {
         this.sprites.clear();
         this.meshResources = gen;
         Arrays.fill(this.meshedChanges, -1);
         this.broken = false;
      }

      if (!this.broken) {
         long started = System.nanoTime();
         long now = Clock.nowMs();
         boolean all = this.complete();

         for(int t : this.order) {
            int seen = this.changes.get(t);
            if (seen != this.meshedChanges[t] && (this.meshes[t] == null || all || now - this.meshedMs[t] >= 280L)) {
               QuadMesh built;
               try {
                  built = this.build(t);
               } catch (Exception e) {
                  this.broken = true;
                  CFHGui.LOGGER.error("[CFHGui] stage: terrain could not be built", e);
                  return;
               }

               if (this.meshes[t] != null) {
                  this.meshes[t].close();
               }

               this.meshes[t] = built;
               this.meshedChanges[t] = seen;
               this.meshedMs[t] = now;
               if (!this.shown && built.quadCount() > 0) {
                  this.shown = true;
                  this.rise.snap(this.still ? 1.0F : 0.0F);
                  this.rise.set(1.0F);
               }

               if (!this.still && System.nanoTime() - started > 4000000L) {
                  return;
               }
            }
         }

      }
   }

   private void shade(StageRenderContext ctx) {
      Vector3f sun = ctx.lighting.sun();

      for(Direction d : Direction.values()) {
         this.normal.set((float)d.getStepX(), (float)d.getStepY(), (float)d.getStepZ());
         this.model.transformDirection(this.normal);
         if (this.normal.lengthSquared() > 1.0E-8F) {
            this.normal.normalize();
         }

         this.shades[d.ordinal()] = Mth.clamp(0.5F + 0.5F * Math.max(0.0F, this.normal.dot(sun)), 0.0F, 1.0F);
      }

   }

   private void drawLayer(StageRenderContext ctx, boolean translucent, float alpha) {
      float up = Math.max(0.02F, this.rise.get());
      boolean edge = ctx.soft && this.edgeRound > 0.0F;
      ctx.pose.pushPose();
      ctx.pose.scale(1.0F, up, 1.0F);
      if (edge) {
         StageSoft.footprint((float)(-(this.width / 2)) + (float)this.width / 2.0F, (float)(-(this.depth / 2)) + (float)this.depth / 2.0F, (float)this.width / 2.0F, (float)this.depth / 2.0F, this.edgeFrom, this.edgeRound);
      } else if (!ctx.soft) {
         this.shade(ctx);
      }

      try {
         for(QuadMesh mesh : this.meshes) {
            if (mesh != null && mesh.quadCount() != 0) {
               if (ctx.soft) {
                  mesh.drawSoft(translucent, ctx.pose.last().pose(), ctx.camera.projection(), alpha, translucent ? this.gloss : 0.0F);
               } else {
                  mesh.draw(translucent, ctx.pose.last().pose(), ctx.camera.projection(), this.shade, ctx.lighting.brightness(), alpha);
               }
            }
         }
      } finally {
         if (edge) {
            StageSoft.noFootprint();
         }

         ctx.pose.popPose();
      }

   }

   protected void draw(StageRenderContext ctx) {
      this.ensureMesh();
      if (!this.broken && this.shown && !(this.alpha() < 1.0F)) {
         this.drawLayer(ctx, false, 1.0F);
      }
   }

   public boolean hasTranslucentPass() {
      return true;
   }

   protected void drawTranslucent(StageRenderContext ctx) {
      if (!this.broken && this.shown) {
         float a = this.alpha();
         if (a < 1.0F) {
            this.drawLayer(ctx, false, a);
         }

         this.drawLayer(ctx, true, a);
      }
   }

   public void dispose() {
      for(int t = 0; t < this.meshes.length; ++t) {
         if (this.meshes[t] != null) {
            this.meshes[t].close();
            this.meshes[t] = null;
         }
      }

   }

   public static record Column(int ground, String top, int topTint, String under, int water, int waterTint, boolean frozen, @Nullable String trunk, @Nullable String canopy, int canopyTint, int floor, @Nullable Column beneath) {
      public Column(int ground, String top, int topTint, String under, int water, int waterTint, boolean frozen, @Nullable String trunk, @Nullable String canopy, int canopyTint) {
         this(ground, top, topTint, under, water, waterTint, frozen, trunk, canopy, canopyTint, 0, (Column)null);
      }

      public static Column none() {
         return new Column(0, "stone", -1, "stone", 0, -1, false, (String)null, (String)null, -1);
      }

      public Column floating(int floor, @Nullable Column beneath) {
         int lowest = beneath == null ? 1 : beneath.reach() + 1;
         return this.ground > 0 && floor >= lowest && floor < this.ground ? new Column(this.ground, this.top, this.topTint, this.under, this.water, this.waterTint, this.frozen, this.trunk, this.canopy, this.canopyTint, floor, beneath) : this;
      }

      boolean tree() {
         return this.trunk != null && this.canopy != null && this.water <= 0;
      }

      boolean floats() {
         return this.floor > 0 && this.floor < this.ground;
      }

      int reach() {
         return this.ground <= 0 ? 0 : Math.max(1, this.ground) + Math.max(0, this.water);
      }

      boolean solid(int y) {
         if (this.ground > 0 && y >= 0) {
            if (!this.floats()) {
               return y < Math.max(1, this.ground);
            } else {
               return y >= this.floor && y < this.ground || this.beneath != null && this.beneath.solid(y);
            }
         } else {
            return false;
         }
      }

      boolean wet(int y) {
         if (this.ground <= 0) {
            return false;
         } else {
            int g = Math.max(1, this.ground);
            if (this.water > 0 && y >= g && y < g + this.water) {
               return true;
            } else {
               return this.floats() && this.beneath != null && this.beneath.wet(y);
            }
         }
      }

      Column withGround(int g) {
         int f = this.floats() ? this.floor - (this.ground - g) : 0;
         Column plain = new Column(g, this.top, this.topTint, this.under, this.water, this.waterTint, this.frozen, this.trunk, this.canopy, this.canopyTint);
         return f > 0 ? plain.floating(f, this.beneath) : plain;
      }
   }
}
