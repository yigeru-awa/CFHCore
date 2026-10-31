package com.onehumanawa.cfhcore.content.gui.core.stage.node;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageBlend;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageRenderContext;
import com.onehumanawa.cfhcore.content.gui.core.stage.StageResources;
import com.onehumanawa.cfhcore.content.gui.core.stage.mesh.QuadMesh;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class CubePlanetNode extends StageNode {
   private static final Mat[] MATS = CubePlanetNode.Mat.values();
   public static final int SIDE = 24;
   private static final int RELIEF = 3;
   private static final int GRID = 32;
   private static final int TEXELS = 4;
   private static final float SURFACE = 11.5F;
   private static final float CLOUD_LIFT = 4.7F;
   private static final float BELT_SQUARE = 5.0F;
   private final long seed;
   private final Kind kind;
   private final VoxelNode.Noise elevation;
   private final VoxelNode.Noise moisture;
   private final VoxelNode.Noise detail;
   private final int grass;
   private final int foliage;
   private final int water;
   private final List<Cloud> clouds = new ArrayList();
   private @Nullable QuadMesh mesh;
   private int meshResources = -1;
   private boolean broken;
   private boolean showClouds = true;
   private float cloudDegPerSec = -7.0F;
   private float cloudAngle;
   private int ringColor;
   private boolean ring;
   private float ringAngle;
   private final Vector3f normal = new Vector3f();
   private final Vector3f corner = new Vector3f();
   private final Matrix4f rotated = new Matrix4f();
   private final float[] shades = new float[6];
   private final Matrix4f local = new Matrix4f();
   private Vector3f sun = (new Vector3f(0.35F, 1.0F, 0.55F)).normalize();

   private CubePlanetNode(long seed, Kind kind) {
      this.seed = seed;
      this.kind = kind;
      this.elevation = new VoxelNode.Noise(mix(seed));
      this.moisture = new VoxelNode.Noise(mix(seed + 101L));
      this.detail = new VoxelNode.Noise(mix(seed + 977L));
      switch (kind.ordinal()) {
         case 1:
            this.grass = -7226023;
            this.foliage = -8934609;
            this.water = -12331538;
            break;
         case 2:
            this.grass = -10892996;
            this.foliage = -13583605;
            this.water = -12618012;
            break;
         case 3:
            this.grass = -4212907;
            this.foliage = -5331926;
            this.water = -13458024;
            break;
         case 4:
            this.grass = -8342377;
            this.foliage = -10444421;
            this.water = -13027127;
            break;
         default:
            this.grass = -7226023;
            this.foliage = -8934609;
            this.water = -12618012;
      }

      float reach = 16.0F;
      this.bounds(-16.0F, -16.0F, -16.0F, 16.0F, 16.0F, 16.0F);
      this.hoverFeel(0.0F, 1.0F);
      this.buildClouds();
      this.named("planet");
   }

   public static CubePlanetNode create(long seed) {
      return new CubePlanetNode(seed, CubePlanetNode.Kind.of(seed));
   }

   public static CubePlanetNode create(long seed, Kind kind) {
      return new CubePlanetNode(seed, kind);
   }

   public static CubePlanetNode server(long seed) {
      CubePlanetNode p = new CubePlanetNode(seed, CubePlanetNode.Kind.of(seed));
      p.ring(-7698304);
      return p;
   }

   public static long seedOf(String name) {
      long h = 1125899906842597L;

      for(int i = 0; i < name.length(); ++i) {
         h = 31L * h + (long)name.charAt(i);
      }

      return h;
   }

   private static long mix(long z) {
      z = (z ^ z >>> 30) * -4658895280553007687L;
      z = (z ^ z >>> 27) * -7723592293110705685L;
      return z ^ z >>> 31;
   }

   public Kind kind() {
      return this.kind;
   }

   public CubePlanetNode size(float blocks) {
      this.scale(blocks / 24.0F);
      return this;
   }

   public CubePlanetNode clouds(boolean on) {
      this.showClouds = on;
      return this;
   }

   public CubePlanetNode cloudSpeed(float degPerSec) {
      this.cloudDegPerSec = degPerSec;
      return this;
   }

   public CubePlanetNode ring(int argb) {
      this.ring = true;
      this.ringColor = argb;
      return this;
   }

   public CubePlanetNode noRing() {
      this.ring = false;
      return this;
   }

   private Terrain terrain(float dx, float dy, float dz) {
      float e = this.elevation.fbm(dx * 1.9F, dy * 1.9F, dz * 1.9F) * 0.66F + this.detail.fbm(dx * 4.4F + 7.0F, dy * 4.4F, dz * 4.4F - 3.0F) * 0.34F;
      float m = this.moisture.fbm(dx * 2.6F + 13.0F, dy * 2.6F - 5.0F, dz * 2.6F);
      float lat = Math.abs(dy);
      float fringe = this.detail.value(dx * 6.0F, dy * 6.0F + 20.0F, dz * 6.0F) * 0.07F;
      Terrain var10000;
      switch (this.kind.ordinal()) {
         case 3 -> var10000 = this.desert(e, m, lat + fringe);
         case 4 -> var10000 = this.ice(e, m, lat + fringe);
         case 5 -> var10000 = this.nether(e, m);
         case 6 -> var10000 = this.end(e, m);
         default -> var10000 = this.temperate(e, m, lat + fringe);
      }

      return var10000;
   }

   private Terrain temperate(float e, float m, float lat) {
      float sea = this.kind == CubePlanetNode.Kind.OCEAN ? 0.2F : (this.kind == CubePlanetNode.Kind.FOREST ? -0.1F : 0.0F);
      boolean polar = lat > 0.9F;
      float h = e - sea;
      if (h < 0.0F) {
         Mat bed = h < -0.26F ? CubePlanetNode.Mat.GRAVEL : (h < -0.12F ? CubePlanetNode.Mat.CLAY : CubePlanetNode.Mat.SAND);
         return polar ? new Terrain(CubePlanetNode.Mat.ICE, CubePlanetNode.Mat.ICE, 0, (Mat)null, false, bed) : new Terrain(CubePlanetNode.Mat.WATER, bed, 0, (Mat)null, true, bed);
      } else {
         int height = h < 0.13F ? 0 : (h < 0.29F ? 1 : (h < 0.43F ? 2 : 3));
         if (polar) {
            return new Terrain(CubePlanetNode.Mat.SNOW, height >= 2 ? CubePlanetNode.Mat.STONE : CubePlanetNode.Mat.DIRT, height, (Mat)null, false, CubePlanetNode.Mat.STONE);
         } else if (h < 0.045F) {
            return new Terrain(CubePlanetNode.Mat.SAND, CubePlanetNode.Mat.SAND, 0, (Mat)null, false, CubePlanetNode.Mat.SAND);
         } else if (height == 3) {
            return new Terrain(CubePlanetNode.Mat.SNOW, CubePlanetNode.Mat.STONE, 3, (Mat)null, false, CubePlanetNode.Mat.STONE);
         } else if (height == 2) {
            return new Terrain(CubePlanetNode.Mat.STONE, CubePlanetNode.Mat.STONE, 2, (Mat)null, false, CubePlanetNode.Mat.STONE);
         } else {
            float wet = m + (this.kind == CubePlanetNode.Kind.FOREST ? 0.32F : 0.0F);
            if (lat < 0.34F && wet < -0.3F) {
               return new Terrain(CubePlanetNode.Mat.SAND, CubePlanetNode.Mat.SAND, height, (Mat)null, false, CubePlanetNode.Mat.SAND);
            } else if (lat > 0.74F) {
               return new Terrain(wet > 0.05F ? CubePlanetNode.Mat.PODZOL : CubePlanetNode.Mat.GRASS, CubePlanetNode.Mat.DIRT, height, wet > -0.05F ? CubePlanetNode.Mat.SPRUCE : null, false, CubePlanetNode.Mat.DIRT);
            } else if (!(wet > 0.16F)) {
               return new Terrain(CubePlanetNode.Mat.GRASS, CubePlanetNode.Mat.DIRT, height, (Mat)null, false, CubePlanetNode.Mat.DIRT);
            } else {
               Mat leaves = this.kind == CubePlanetNode.Kind.FOREST && lat < 0.3F ? CubePlanetNode.Mat.JUNGLE : (wet > 0.42F ? CubePlanetNode.Mat.BIRCH : CubePlanetNode.Mat.LEAVES);
               return new Terrain(CubePlanetNode.Mat.GRASS, CubePlanetNode.Mat.DIRT, height, leaves, false, CubePlanetNode.Mat.DIRT);
            }
         }
      }
   }

   private Terrain desert(float e, float m, float lat) {
      float h = e + 0.3F;
      if (h < 0.0F) {
         return new Terrain(CubePlanetNode.Mat.WATER, CubePlanetNode.Mat.SAND, 0, (Mat)null, true, CubePlanetNode.Mat.SAND);
      } else if (lat > 0.93F) {
         return new Terrain(CubePlanetNode.Mat.SNOW, CubePlanetNode.Mat.STONE, h > 0.5F ? 1 : 0, (Mat)null, false, CubePlanetNode.Mat.STONE);
      } else {
         int height = h < 0.42F ? 0 : (h < 0.58F ? 1 : (h < 0.72F ? 2 : 3));
         if (h < 0.07F) {
            return new Terrain(CubePlanetNode.Mat.GRASS, CubePlanetNode.Mat.DIRT, 0, m > 0.0F ? CubePlanetNode.Mat.LEAVES : null, false, CubePlanetNode.Mat.DIRT);
         } else if (height >= 1) {
            Mat var10000;
            switch (height) {
               case 1 -> var10000 = CubePlanetNode.Mat.ORANGE;
               case 2 -> var10000 = CubePlanetNode.Mat.TERRACOTTA;
               default -> var10000 = CubePlanetNode.Mat.WHITE;
            }

            Mat band = var10000;
            return new Terrain(m > 0.25F ? CubePlanetNode.Mat.RED : band, band, height, (Mat)null, false, CubePlanetNode.Mat.TERRACOTTA);
         } else {
            return new Terrain(m > 0.3F ? CubePlanetNode.Mat.RED_SAND : CubePlanetNode.Mat.SAND, CubePlanetNode.Mat.SAND, 0, (Mat)null, false, CubePlanetNode.Mat.SAND);
         }
      }
   }

   private Terrain ice(float e, float m, float lat) {
      float h = e + 0.04F;
      if (h < 0.0F) {
         boolean open = lat < 0.3F && m > 0.05F;
         return open ? new Terrain(CubePlanetNode.Mat.WATER, CubePlanetNode.Mat.GRAVEL, 0, (Mat)null, true, CubePlanetNode.Mat.GRAVEL) : new Terrain(CubePlanetNode.Mat.ICE, CubePlanetNode.Mat.ICE, 0, (Mat)null, false, CubePlanetNode.Mat.GRAVEL);
      } else {
         int height = h < 0.14F ? 0 : (h < 0.3F ? 1 : (h < 0.44F ? 2 : 3));
         return height == 2 ? new Terrain(CubePlanetNode.Mat.STONE, CubePlanetNode.Mat.STONE, 2, (Mat)null, false, CubePlanetNode.Mat.STONE) : new Terrain(CubePlanetNode.Mat.SNOW, height == 3 ? CubePlanetNode.Mat.STONE : CubePlanetNode.Mat.DIRT, height, m > 0.22F && height < 2 ? CubePlanetNode.Mat.SPRUCE : null, false, CubePlanetNode.Mat.STONE);
      }
   }

   private Terrain nether(float e, float m) {
      if (e < -0.08F) {
         return new Terrain(CubePlanetNode.Mat.LAVA, CubePlanetNode.Mat.MAGMA, 0, (Mat)null, false, CubePlanetNode.Mat.MAGMA);
      } else {
         int height = e < 0.1F ? 0 : (e < 0.26F ? 1 : (e < 0.4F ? 2 : 3));
         if (e < -0.03F) {
            return new Terrain(CubePlanetNode.Mat.MAGMA, CubePlanetNode.Mat.NETHERRACK, 0, (Mat)null, false, CubePlanetNode.Mat.NETHERRACK);
         } else if (height >= 2) {
            return new Terrain(height == 3 ? CubePlanetNode.Mat.BLACKSTONE : CubePlanetNode.Mat.BASALT, CubePlanetNode.Mat.BASALT, height, (Mat)null, false, CubePlanetNode.Mat.BASALT);
         } else if (m > 0.2F) {
            return new Terrain(CubePlanetNode.Mat.CRIMSON, CubePlanetNode.Mat.NETHERRACK, height, m > 0.34F ? CubePlanetNode.Mat.WART : null, false, CubePlanetNode.Mat.NETHERRACK);
         } else {
            return m < -0.22F ? new Terrain(CubePlanetNode.Mat.WARPED, CubePlanetNode.Mat.NETHERRACK, height, m < -0.36F ? CubePlanetNode.Mat.WARPED_WART : null, false, CubePlanetNode.Mat.NETHERRACK) : new Terrain(CubePlanetNode.Mat.NETHERRACK, CubePlanetNode.Mat.NETHERRACK, height, (Mat)null, false, CubePlanetNode.Mat.NETHERRACK);
         }
      }
   }

   private Terrain end(float e, float m) {
      int height = e < 0.18F ? 0 : (e < 0.34F ? 1 : (e < 0.46F ? 2 : 3));
      return height == 3 ? new Terrain(CubePlanetNode.Mat.OBSIDIAN, CubePlanetNode.Mat.OBSIDIAN, 3, (Mat)null, false, CubePlanetNode.Mat.END_STONE) : new Terrain(CubePlanetNode.Mat.END_STONE, CubePlanetNode.Mat.END_STONE, height, m > 0.36F && height < 2 ? CubePlanetNode.Mat.PURPUR : null, false, CubePlanetNode.Mat.END_STONE);
   }

   private static int index(int x, int y, int z) {
      return (x * 32 + y) * 32 + z;
   }

   private byte[] voxels() {
      byte[] cells = new byte['耀'];
      Map<Integer, Terrain> cache = new HashMap();
      float c = 15.5F;
      Random rnd = new Random(mix(this.seed + 31L));

      for(int x = 0; x < 32; ++x) {
         for(int y = 0; y < 32; ++y) {
            for(int z = 0; z < 32; ++z) {
               float cx = (float)x - 15.5F;
               float cy = (float)y - 15.5F;
               float cz = (float)z - 15.5F;
               float r = Math.max(Math.abs(cx), Math.max(Math.abs(cy), Math.abs(cz)));
               int shell = Math.round(r - 11.5F);
               if (shell < -1) {
                  cells[index(x, y, z)] = (byte)(CubePlanetNode.Mat.STONE.ordinal() + 1);
               } else {
                  float bx = base(cx, r);
                  float by = base(cy, r);
                  float bz = base(cz, r);
                  int key = index(Math.round(bx + 15.5F), Math.round(by + 15.5F), Math.round(bz + 15.5F));
                  Terrain t = (Terrain)cache.get(key);
                  if (t == null) {
                     float len = (float)Math.sqrt((double)(bx * bx + by * by + bz * bz));
                     t = this.terrain(bx / len, by / len, bz / len);
                     if (t.canopy() != null && rnd.nextFloat() > 0.62F) {
                        t = new Terrain(t.top(), t.under(), t.height(), (Mat)null, t.sea(), t.bed());
                     }

                     cache.put(key, t);
                  }

                  Mat mat;
                  if (shell == -1) {
                     mat = t.sea() ? t.bed() : t.under();
                  } else if (shell == 0) {
                     mat = t.height() == 0 ? t.top() : t.under();
                  } else if (shell <= t.height()) {
                     mat = shell == t.height() ? t.top() : t.under();
                  } else if (shell == t.height() + 1 && t.canopy() != null) {
                     mat = t.canopy();
                  } else {
                     mat = null;
                  }

                  if (mat != null) {
                     cells[index(x, y, z)] = (byte)(mat.ordinal() + 1);
                  }
               }
            }
         }
      }

      return cells;
   }

   private static float base(float v, float r) {
      float a = Math.abs(v);
      return !(a >= r - 0.01F) && !(a > 11.5F) ? v : Math.copySign(11.5F, v);
   }

   private QuadMesh build() {
      byte[] cells = this.voxels();
      TextureAtlasSprite[] sprites = new TextureAtlasSprite[MATS.length];
      QuadMesh.Builder b = QuadMesh.builder();
      int half = 16;

      for(int x = 0; x < 32; ++x) {
         for(int y = 0; y < 32; ++y) {
            for(int z = 0; z < 32; ++z) {
               int id = cells[index(x, y, z)];
               if (id != 0) {
                  Mat mat = MATS[id - 1];

                  for(Direction d : Direction.values()) {
                     int nx = x + d.getStepX();
                     int ny = y + d.getStepY();
                     int nz = z + d.getStepZ();
                     boolean outside = nx < 0 || ny < 0 || nz < 0 || nx >= 32 || ny >= 32 || nz >= 32;
                     int nid = outside ? 0 : cells[index(nx, ny, nz)];
                     if (nid != 0) {
                        Mat other = MATS[nid - 1];
                        if (!other.seeThrough() || other == mat || mat.gaps && !other.translucent) {
                           continue;
                        }
                     }

                     if (sprites[id - 1] == null) {
                        sprites[id - 1] = QuadMesh.sprite(mat.texture);
                     }

                     b.face(mat.translucent, d, x - 16, y - 16, z - 16, sprites[id - 1], 4, this.colour(mat));
                  }
               }
            }
         }
      }

      return b.build();
   }

   private int colour(Mat mat) {
      int var10000;
      switch (mat.tint.ordinal()) {
         case 1 -> var10000 = this.grass;
         case 2 -> var10000 = this.foliage;
         case 3 -> var10000 = this.water & 16777215 | -671088640;
         default -> var10000 = mat.fixed;
      }

      return var10000;
   }

   private void ensureMesh() {
      int gen = StageResources.generation();
      if (this.mesh == null || gen != this.meshResources) {
         if (!this.broken || gen != this.meshResources) {
            if (this.mesh != null) {
               this.mesh.close();
               this.mesh = null;
            }

            this.meshResources = gen;

            try {
               this.mesh = this.build();
               this.broken = false;
            } catch (Exception e) {
               this.broken = true;
               CFHGui.LOGGER.error("[CFHGui] stage: planet {} could not be built", this.seed, e);
            }

         }
      }
   }

   private void buildClouds() {
      Random rnd = new Random(mix(this.seed + 7L));
      float out = 16.7F;
      int count = 7 + rnd.nextInt(3);

      for(int i = 0; i < count; ++i) {
         float angle = ((float)i + rnd.nextFloat() * 0.7F) / (float)count * ((float)Math.PI * 2F);
         float y = (rnd.nextFloat() - 0.5F) * 18.0F;
         float w = 1.5F + rnd.nextFloat() * 1.7F;
         float h = 0.8F + rnd.nextFloat() * 0.9F;
         this.clouds.add(new Cloud(true, 16.7F + rnd.nextFloat() * 0.8F, y, angle, w, h, 0.42F));
         if (rnd.nextInt(3) != 0) {
            this.clouds.add(new Cloud(true, 17.2F + rnd.nextFloat() * 0.6F, y + (rnd.nextBoolean() ? 0.9F : -0.9F) * h, angle + (rnd.nextBoolean() ? 0.09F : -0.09F), w * 0.6F, h * 0.62F, 0.38F));
         }
      }

      int caps = 3 + rnd.nextInt(2);

      for(int i = 0; i < caps; ++i) {
         float angle = ((float)i + rnd.nextFloat() * 0.6F) / (float)caps * ((float)Math.PI * 2F);
         float r = 3.0F + rnd.nextFloat() * 6.5F;
         float w = 1.5F + rnd.nextFloat() * 1.5F;
         float d = 0.9F + rnd.nextFloat() * 0.8F;
         this.clouds.add(new Cloud(false, r, 16.7F + rnd.nextFloat() * 0.8F, angle, w, 0.4F, d));
         if (rnd.nextBoolean()) {
            this.clouds.add(new Cloud(false, r + d * 0.9F, 17.150002F + rnd.nextFloat() * 0.4F, angle + 0.12F, w * 0.6F, 0.36F, d * 0.65F));
         }
      }

   }

   private static float beltRadius(float radius, float angle) {
      double c = Math.abs(Math.cos((double)angle));
      double s = Math.abs(Math.sin((double)angle));
      return (float)((double)radius / Math.pow(Math.pow(c, (double)5.0F) + Math.pow(s, (double)5.0F), 0.2));
   }

   public void update(StageRenderContext ctx) {
      if (!(Theme.current().motion() <= 0.0F)) {
         this.cloudAngle = (this.cloudAngle + this.cloudDegPerSec * ctx.deltaMs / 1000.0F) % 360.0F;
         this.ringAngle = (this.ringAngle + 11.0F * ctx.deltaMs / 1000.0F) % 360.0F;
      }
   }

   private void shade(StageRenderContext ctx, Matrix4f local) {
      Vector3f sun = ctx.lighting.sun();
      float ambient = 0.5F;

      for(Direction d : Direction.values()) {
         this.normal.set((float)d.getStepX(), (float)d.getStepY(), (float)d.getStepZ());
         local.transformDirection(this.normal);
         if (this.normal.lengthSquared() > 1.0E-8F) {
            this.normal.normalize();
         }

         float lit = Math.max(0.0F, this.normal.dot(sun));
         this.shades[d.ordinal()] = Mth.clamp(0.5F + 0.5F * lit, 0.0F, 1.0F);
      }

   }

   protected void draw(StageRenderContext ctx) {
      this.ensureMesh();
      if (this.mesh != null) {
         this.shade(ctx, this.model);
         float a = this.alpha();
         if (!(a < 1.0F)) {
            if (ctx.soft) {
               this.mesh.drawSoft(false, ctx.pose.last().pose(), ctx.camera.projection(), 1.0F);
            } else {
               this.mesh.draw(false, ctx.pose.last().pose(), ctx.camera.projection(), (d) -> this.shades[d.ordinal()], ctx.lighting.brightness(), 1.0F);
            }

         }
      }
   }

   public boolean hasTranslucentPass() {
      return true;
   }

   protected void drawTranslucent(StageRenderContext ctx) {
      if (this.mesh != null) {
         float a = this.alpha();
         Matrix4f pose = ctx.pose.last().pose();
         this.shade(ctx, this.model);
         if (ctx.soft) {
            if (a < 1.0F) {
               this.mesh.drawSoft(false, pose, ctx.camera.projection(), a);
            }

            this.mesh.drawSoft(true, pose, ctx.camera.projection(), a);
         } else {
            if (a < 1.0F) {
               RenderSystem.depthMask(true);
               this.mesh.draw(false, pose, ctx.camera.projection(), (d) -> this.shades[d.ordinal()], ctx.lighting.brightness(), a);
               RenderSystem.depthMask(false);
            }

            this.mesh.draw(true, pose, ctx.camera.projection(), (d) -> this.shades[d.ordinal()], ctx.lighting.brightness(), a);
         }

         boolean wantClouds = this.showClouds && !this.clouds.isEmpty();
         if (wantClouds || this.ring) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableBlend();
            StageBlend.over();
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            if (this.ring) {
               RenderSystem.depthMask(true);
               BufferBuilder bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
               this.ring(bb, pose, a);
               MeshData data = bb.build();
               if (data != null) {
                  BufferUploader.drawWithShader(data);
               }
            }

            if (wantClouds) {
               RenderSystem.depthMask(false);
               BufferBuilder bb = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

               for(Cloud c : this.clouds) {
                  this.cloud(bb, pose, c, a);
               }

               MeshData data = bb.build();
               if (data != null) {
                  BufferUploader.drawWithShader(data);
               }
            }

            RenderSystem.depthMask(true);
         }
      }
   }

   private void cloud(BufferBuilder bb, Matrix4f pose, Cloud c, float alpha) {
      float angle = c.angle() + (float)Math.toRadians((double)this.cloudAngle);
      float turn;
      if (c.belt()) {
         float r = beltRadius(c.radius(), angle);
         float x = Mth.sin(angle) * r;
         float z = Mth.cos(angle) * r;
         float ahead = angle + 0.02F;
         float ra = beltRadius(c.radius(), ahead);
         float behind = angle - 0.02F;
         float rb = beltRadius(c.radius(), behind);
         float tx = Mth.sin(ahead) * ra - Mth.sin(behind) * rb;
         float tz = Mth.cos(ahead) * ra - Mth.cos(behind) * rb;
         turn = (float)Math.atan2((double)(-tz), (double)tx);
         this.rotated.set(pose).translate(x, c.y(), z).rotateY(turn);
      } else {
         turn = angle;
         this.rotated.set(pose).rotateY(angle).translate(0.0F, c.y(), c.radius());
      }

      this.local.set(this.model).rotateY(turn);
      this.box(bb, this.rotated, this.local, c.halfW(), c.halfH(), c.halfD(), 255, 255, 255, Math.round(alpha * 222.0F), 0.66F);
   }

   private void ring(BufferBuilder bb, Matrix4f pose, float alpha) {
      float radius = 26.81714F;
      int beads = 30;
      int r = this.ringColor >> 16 & 255;
      int g = this.ringColor >> 8 & 255;
      int b = this.ringColor & 255;
      int a = Math.round((float)(this.ringColor >>> 24 & 255) * alpha);

      for(int i = 0; i < 30; ++i) {
         float angle = (float)i / 30.0F * ((float)Math.PI * 2F) + (float)Math.toRadians((double)this.ringAngle);
         this.rotated.set(pose).rotateZ(0.2F).rotateX(0.12F).rotateY(angle).translate(0.0F, 0.0F, 26.81714F);
         this.local.set(this.model).rotateZ(0.2F).rotateX(0.12F).rotateY(angle);
         float s = i % 3 == 0 ? 0.78F : 0.52F;
         this.box(bb, this.rotated, this.local, s, s, s, r, g, b, a, 0.78F);
      }

   }

   private void box(BufferBuilder bb, Matrix4f at, Matrix4f turned, float hx, float hy, float hz, int r, int g, int b, int a, float ambient) {
      Vector3f sun = this.sunOr();

      for(Direction d : Direction.values()) {
         this.normal.set((float)d.getStepX(), (float)d.getStepY(), (float)d.getStepZ());
         turned.transformDirection(this.normal);
         if (this.normal.lengthSquared() > 1.0E-8F) {
            this.normal.normalize();
         }

         float s = Mth.clamp(ambient + (1.0F - ambient) * Math.max(0.0F, this.normal.dot(sun)), 0.0F, 1.0F);
         int cr = Math.round((float)r * s);
         int cg = Math.round((float)g * s);
         int cb = Math.round((float)b * s);
         float x0 = -hx;
         float y0 = -hy;
         float z0 = -hz;
         switch (d) {
            case UP:
               this.quad(bb, at, x0, hy, z0, x0, hy, hz, hx, hy, hz, hx, hy, z0, cr, cg, cb, a);
               break;
            case DOWN:
               this.quad(bb, at, x0, y0, hz, x0, y0, z0, hx, y0, z0, hx, y0, hz, cr, cg, cb, a);
               break;
            case NORTH:
               this.quad(bb, at, x0, y0, z0, x0, hy, z0, hx, hy, z0, hx, y0, z0, cr, cg, cb, a);
               break;
            case SOUTH:
               this.quad(bb, at, hx, y0, hz, hx, hy, hz, x0, hy, hz, x0, y0, hz, cr, cg, cb, a);
               break;
            case WEST:
               this.quad(bb, at, x0, y0, hz, x0, hy, hz, x0, hy, z0, x0, y0, z0, cr, cg, cb, a);
               break;
            default:
               this.quad(bb, at, hx, y0, z0, hx, hy, z0, hx, hy, hz, hx, y0, hz, cr, cg, cb, a);
         }
      }

   }

   private Vector3f sunOr() {
      return this.sun;
   }

   public void prepare(StageRenderContext ctx) {
      super.prepare(ctx);
      this.sun = ctx.lighting.sun();
   }

   private void quad(BufferBuilder bb, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, int r, int g, int b, int a) {
      bb.addVertex(m, ax, ay, az).setColor(r, g, b, a);
      bb.addVertex(m, bx, by, bz).setColor(r, g, b, a);
      bb.addVertex(m, cx, cy, cz).setColor(r, g, b, a);
      bb.addVertex(m, dx, dy, dz).setColor(r, g, b, a);
   }

   public void dispose() {
      if (this.mesh != null) {
         this.mesh.close();
         this.mesh = null;
      }

   }

   public static enum Kind {
      TEMPERATE(10),
      OCEAN(3),
      FOREST(3),
      DESERT(2),
      ICE(2),
      NETHER(0),
      END(0);

      private final int weight;

      private Kind(final int weight) {
         this.weight = weight;
      }

      public static Kind of(long seed) {
         int total = 0;

         for(Kind k : values()) {
            total += k.weight;
         }

         int pick = (int)Math.floorMod(CubePlanetNode.mix(seed ^ -7046029254386353131L), (long)total);

         for(Kind k : values()) {
            if (pick < k.weight) {
               return k;
            }

            pick -= k.weight;
         }

         return TEMPERATE;
      }
   }

   private static enum Tint {
      NONE,
      GRASS,
      FOLIAGE,
      WATER;
   }

   private static enum Mat {
      WATER("water_still", CubePlanetNode.Tint.WATER, true),
      LAVA("lava_still"),
      SAND("sand"),
      RED_SAND("red_sand"),
      GRAVEL("gravel"),
      STONE("stone"),
      DEEPSLATE("deepslate"),
      DIRT("dirt"),
      CLAY("clay"),
      GRASS("grass_block_top", CubePlanetNode.Tint.GRASS, false),
      PODZOL("podzol_top"),
      MYCELIUM("mycelium_top"),
      MOSS("moss_block"),
      SNOW("snow"),
      ICE("packed_ice"),
      LEAVES("oak_leaves", CubePlanetNode.Tint.FOLIAGE, false),
      SPRUCE("spruce_leaves", -10380959),
      BIRCH("birch_leaves", -8345771),
      JUNGLE("jungle_leaves", CubePlanetNode.Tint.FOLIAGE, false),
      CHERRY("cherry_leaves"),
      TERRACOTTA("terracotta"),
      ORANGE("orange_terracotta"),
      YELLOW("yellow_terracotta"),
      RED("red_terracotta"),
      WHITE("white_terracotta"),
      NETHERRACK("netherrack"),
      CRIMSON("crimson_nylium"),
      WARPED("warped_nylium"),
      WART("nether_wart_block"),
      WARPED_WART("warped_wart_block"),
      BASALT("smooth_basalt"),
      BLACKSTONE("blackstone"),
      MAGMA("magma"),
      GLOWSTONE("glowstone"),
      END_STONE("end_stone"),
      OBSIDIAN("obsidian"),
      PURPUR("purpur_block");

      final String texture;
      final Tint tint;
      final int fixed;
      final boolean translucent;
      final boolean gaps;

      private Mat(final String texture) {
         this(texture, CubePlanetNode.Tint.NONE, false, -1);
      }

      private Mat(final String texture, final int fixed) {
         this(texture, CubePlanetNode.Tint.NONE, false, fixed);
      }

      private Mat(final String texture, final Tint tint, final boolean translucent) {
         this(texture, tint, translucent, -1);
      }

      private Mat(final String texture, final Tint tint, final boolean translucent, final int fixed) {
         this.texture = texture;
         this.tint = tint;
         this.translucent = translucent;
         this.fixed = fixed;
         this.gaps = texture.endsWith("_leaves");
      }

      boolean seeThrough() {
         return this.translucent || this.gaps;
      }
   }

   private static record Terrain(Mat top, Mat under, int height, @Nullable Mat canopy, boolean sea, Mat bed) {
   }

   private static record Cloud(boolean belt, float radius, float y, float angle, float halfW, float halfH, float halfD) {
   }
}
