package com.onehumanawa.cfhcore.content.gui.menu.client.loading.scene;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.LongSupplier;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.system.MemoryUtil;

public final class IslandScene implements StartupScene {
   static final String FONT = "monocraft";
   private static final float DROP = 0.4F;
   private static final float FALL = 0.85F;
   private static final float YAW = 32.0F;
   private static final float TURN = 4.2F;
   private static final float PITCH = 21.0F;
   private static final float FOV = 30.0F;
   private static final int SKY_TOP = -9462286;
   private static final int SKY_LOW = -4467457;
   private static final int NIGHT_TOP = -16579057;
   private static final int NIGHT_LOW = -15327174;
   private static final int MOON = -10325336;
   private static final int NIGHT_CLOUD = -12827293;
   private static final int WHITE = -1;
   private static final int GREY = -5592406;
   private static final int RED = -43691;
   private static final float CLOUD = 2.2F;
   private static final float CLOUD_THICK = 0.8F;
   private static final float DRIFT = 0.45F;
   private static final float CLOUD_REACH = 44.0F;
   private static final float[] IDENTITY = Mat4.identity();
   private final LoadingScene.Look look;
   private final String title;
   private final boolean night;
   private final LongSupplier clock;
   private final ByteBuffer ttf;
   private final Sheet sheet = new Sheet();
   private final Island island;
   private final Gfx gfx;
   private final SceneGl gl;
   private final GlSave saved = new GlSave();
   private final SceneGl.Light light = new SceneGl.Light();
   private final Camera camera = new Camera();
   private final Mesh sky;
   private final Mesh clouds;
   private final Mesh land;
   private final Mesh water;
   private final Mesh moving;
   private final Mesh movingWater;
   private final Mesh bits;
   private boolean dirty = true;
   private int waterFrame = -1;
   private float cloudsFor = Float.NaN;
   private float cloudsYaw;
   private final float[] cloudShift = Mat4.identity();
   private final float[] cloudView = new float[16];
   private final boolean[] cloudMap;
   private final int cloudSide;
   private final Picture logo;
   private final Picture edition;
   private int logoTexture;
   private int editionTexture;
   private final GameFont game;
   private PixelFont text;
   private float bakedScale = -1.0F;
   private final long start;
   private long last;
   private float time;
   private int sent;
   private float owed;
   private float shown;
   private boolean seenOverall;
   private final List<Bit> flying = new ArrayList();
   private int seed = 4711;
   private final float[] corners;
   private final float[] target = new float[3];
   private final float[] clear = new float[3];

   public IslandScene(LoadingScene.Look look, byte[] font, LongSupplier clock, Assets assets, String title) {
      this.look = look;
      this.title = title == null ? "OVERHAUL" : title.trim().toUpperCase(Locale.ROOT);
      this.clock = clock;
      int bg = look.background();
      this.night = 0.299F * (float)(bg >>> 16 & 255) + 0.587F * (float)(bg >>> 8 & 255) + 0.114F * (float)(bg & 255) < 50.0F;
      this.island = new Island(assets, this.sheet);
      if (this.night) {
         this.island.light = -10325336;
      }

      Picture map = Picture.read(assets.read("minecraft", "textures/environment/clouds.png"));
      if (map != null && map.w() == map.h()) {
         this.cloudSide = map.w();
         this.cloudMap = new boolean[map.w() * map.h()];

         for(int i = 0; i < this.cloudMap.length; ++i) {
            this.cloudMap[i] = map.argb()[i] >>> 24 > 128;
         }
      } else {
         this.cloudSide = 0;
         this.cloudMap = new boolean[0];
      }

      this.logo = Picture.read(assets.read("minecraft", "textures/gui/title/minecraft.png"));
      this.edition = this.logo == null ? null : Picture.read(assets.read("minecraft", "textures/gui/title/edition.png"));
      this.game = GameFont.read(assets);
      this.corners = bounds(this.island);
      this.target[1] = (this.island.top + this.island.bottom) / 2.0F;
      this.gfx = new Gfx();
      this.gl = new SceneGl();
      this.sky = new Mesh(512);
      this.clouds = new Mesh(24000);
      this.land = new Mesh(60000);
      this.water = new Mesh(2048);
      this.moving = new Mesh(8000);
      this.movingWater = new Mesh(512);
      this.bits = new Mesh(4096);
      this.ttf = MemoryUtil.memAlloc(font.length).put(font).flip();
      this.start = clock.getAsLong();
      this.last = this.start;
   }

   private static float[] bounds(Island island) {
      int low = (int)island.bottom;
      int high = (int)island.top;
      float[] reach = new float[high - low + 1];

      for(Island.Block b : island.blocks) {
         if (b.kind.shape != Island.Shape.CROSS) {
            float x = (float)b.x + 0.5F;
            float z = (float)b.z + 0.5F;
            float r = (float)Math.sqrt((double)(x * x + z * z)) + 0.55F;
            reach[b.y - low] = Math.max(reach[b.y - low], r);
            reach[b.y + 1 - low] = Math.max(reach[b.y + 1 - low], r);
         }
      }

      int round = 16;
      float[] out = new float[reach.length * 16 * 3];

      for(int level = 0; level < reach.length; ++level) {
         for(int i = 0; i < 16; ++i) {
            double a = (Math.PI * 2D) * (double)i / (double)16.0F;
            int at = (level * 16 + i) * 3;
            out[at] = (float)Math.cos(a) * reach[level];
            out[at + 1] = (float)(low + level);
            out[at + 2] = (float)Math.sin(a) * reach[level];
         }
      }

      return out;
   }

   public int[] textures() {
      return (int[])this.sheet.pixels().clone();
   }

   public static int sky(boolean night) {
      return (night ? -15327174 : -4467457) & 16777215;
   }

   public void dispose() {
      if (this.text != null) {
         this.text.delete();
      }

      this.text = null;
      if (this.game != null) {
         this.game.delete();
      }

      if (this.logoTexture != 0) {
         GL32C.glDeleteTextures(this.logoTexture);
      }

      if (this.editionTexture != 0) {
         GL32C.glDeleteTextures(this.editionTexture);
      }

      this.logoTexture = this.editionTexture = 0;
      this.gfx.delete();
      this.gl.delete();

      for(Mesh m : new Mesh[]{this.sky, this.clouds, this.land, this.water, this.moving, this.movingWater, this.bits}) {
         m.free();
      }

      MemoryUtil.memFree(this.ttf);
   }

   public void draw(int pixelW, int pixelH, float scale, boolean flipped, float alpha, LoadingScene.Feed feed) {
      long now = this.clock.getAsLong();
      float dt = Math.max(0.0F, Math.min(0.1F, (float)(now - this.last) / 1.0E9F));
      this.last = now;
      this.time = (float)(now - this.start) / 1.0E9F;
      this.advance(feed, dt);
      this.saved.save();

      try {
         float w = (float)pixelW / scale;
         float h = (float)pixelH / scale;
         float[] logoAt = this.logoPlace(w, h);
         float barY = (float)(Math.round(h * 0.8325F) - 5);
         this.camera(w, h, logoAt[1] + 51.0F * logoAt[2] + 6.0F, barY - 22.0F);
         this.meshes(w / h);
         this.gl.begin(pixelW, pixelH, this.sheet);
         this.gl.load(this.sky, this.clouds, this.land, this.moving, this.bits, this.water, this.movingWater);
         rgb(this.night ? -15327174 : -4467457, this.clear);
         this.gl.picture(this.clear);
         this.gl.view(IDENTITY, this.camera.eye, this.light, IDENTITY, false);
         this.gl.draw(this.sky, 0);
         this.gl.camera(this.camera.viewProj);
         this.gl.draw(this.land, 0);
         this.gl.draw(this.moving, 0);
         this.gl.draw(this.bits, 0);
         Mat4.set(this.cloudShift, IDENTITY);
         Mat4.translate(this.cloudShift, Float.isNaN(this.cloudsFor) ? 0.0F : this.time * 0.45F - this.cloudsFor, 0.0F, 0.0F);
         Mat4.mul(this.cloudView, this.camera.viewProj, this.cloudShift);
         this.gl.camera(this.cloudView);
         this.gl.draw(this.clouds, 1);
         this.gl.camera(this.camera.viewProj);
         this.gl.draw(this.water, 1);
         this.gl.draw(this.movingWater, 1);
         int picture = this.gl.finish();
         if (this.logo != null && this.logoTexture == 0) {
            this.logoTexture = this.logo.texture();
            if (this.edition != null) {
               this.editionTexture = this.edition.texture();
            }
         }

         GL32C.glBindFramebuffer(36160, this.saved.canvas());
         GL32C.glViewport(0, 0, pixelW, pixelH);
         this.gfx.begin(pixelW, pixelH, scale, flipped, false);

         try {
            this.fonts(scale);
            float shownIn = alpha * ease(Math.min(1.0F, this.time / 0.45F));
            if (alpha >= 1.0F) {
               this.gfx.rect(0.0F, 0.0F, w, h, this.look.background() | -16777216);
            }

            this.gfx.alpha(shownIn);
            this.gfx.image(picture, 0.0F, 0.0F, w, h, 0.0F, 1.0F, 1.0F, 0.0F, -1);
            this.gfx.vgradient(0.0F, barY - 34.0F, w, h - barY + 34.0F, 0, 1711276032);
            if (this.logoTexture != 0) {
               this.logo(logoAt, scale);
            }

            this.words(feed, w, h, barY);
         } finally {
            this.gfx.end();
         }
      } finally {
         this.saved.restore();
      }

   }

   private void advance(LoadingScene.Feed feed, float dt) {
      List<Island.Block> blocks = this.island.blocks;
      float overall = feed.overall();
      float target = overall >= 0.0F ? Math.min(1.0F, overall) : 0.9F * (1.0F - (float)Math.exp((double)(-this.time / 30.0F)));
      if (!(overall >= 0.0F) || this.seenOverall && !(target < this.shown - 0.05F)) {
         this.shown = Math.max(0.0F, this.shown + (target - this.shown) * rate(dt, 6.0F));
      } else {
         this.shown = target;
      }

      this.seenOverall |= overall >= 0.0F;
      int want = Math.min(blocks.size(), (int)Math.floor((double)(target * (float)blocks.size()) + 1.0E-4));
      if (want + blocks.size() / 5 < this.sent) {
         this.island.clear();
         this.sent = 0;
         this.owed = 0.0F;
         this.flying.clear();
         this.dirty = true;
      }

      if (want > this.sent) {
         for(this.owed += dt * (9.0F + (float)(want - this.sent) * 3.5F); this.owed >= 1.0F && this.sent < want; --this.owed) {
            ((Island.Block)blocks.get(this.sent++)).sent = this.time;
         }
      } else {
         this.owed = 0.0F;
      }

      for(int i = 0; i < this.sent; ++i) {
         Island.Block b = (Island.Block)blocks.get(i);
         if (!b.standing && !(this.time - b.sent < 0.4F)) {
            this.island.stand(b);
            this.dirty = true;
            this.landed(b);
         }
      }

      for(Bit bit : this.flying) {
         bit.life += dt;
         bit.vy -= 14.0F * dt;
         bit.x += bit.vx * dt;
         bit.y += bit.vy * dt;
         bit.z += bit.vz * dt;
         bit.vx *= 1.0F - 1.2F * dt;
         bit.vz *= 1.0F - 1.2F * dt;
      }

      this.flying.removeIf((bx) -> bx.life >= bx.span);
   }

   private void landed(Island.Block b) {
      if (this.flying.size() <= 160 && b.kind.shape != Island.Shape.WATER) {
         int count = b.kind.shape == Island.Shape.CROSS ? 2 : 4;

         for(int i = 0; i < count; ++i) {
            Bit bit = new Bit();
            double a = (double)this.random() * Math.PI * (double)2.0F;
            float out = 0.6F + this.random() * 1.1F;
            bit.x = (float)b.x + 0.5F + (float)Math.cos(a) * 0.45F;
            bit.z = (float)b.z + 0.5F + (float)Math.sin(a) * 0.45F;
            bit.y = (float)b.y + 0.2F + this.random() * 0.7F;
            bit.vx = (float)Math.cos(a) * out;
            bit.vz = (float)Math.sin(a) * out;
            bit.vy = 1.6F + this.random() * 2.2F;
            bit.span = 0.45F + this.random() * 0.4F;
            bit.size = 0.07F + this.random() * 0.05F;
            bit.picture = this.island.crumb(b.kind, (int)(this.random() * 12.0F), (int)(this.random() * 12.0F));
            bit.tint = Island.crumbTint(b.kind);
            this.flying.add(bit);
         }

      }
   }

   private float random() {
      this.seed = this.seed * 1103515245 + 12345;
      return (float)(this.seed >>> 8 & '\uffff') / 65536.0F;
   }

   private static float rate(float dt, float speed) {
      return 1.0F - (float)Math.exp((double)(-dt * speed));
   }

   private static float ease(float t) {
      float u = 1.0F - t;
      return 1.0F - u * u * u;
   }

   private void camera(float w, float h, float top, float bottom) {
      float yTop = 1.0F - 2.0F * top / h;
      float yBottom = 1.0F - 2.0F * Math.max(top + 40.0F, bottom) / h;
      this.camera.fit(this.target, 32.0F + this.time * 4.2F, 21.0F, 30.0F, w / h, this.corners, new float[]{-0.8F, 0.8F, yBottom, yTop}, 0.5F);
   }

   private void meshes(float aspect) {
      int frame = (int)(this.time * 10.0F);
      if (this.dirty) {
         this.land.clear();
         this.island.standing(this.land);
      }

      if (this.dirty || frame != this.waterFrame) {
         this.water.clear();
         this.island.water(this.water, frame);
         this.waterFrame = frame;
      }

      this.dirty = false;
      this.moving.clear();
      this.movingWater.clear();

      for(int i = 0; i < this.sent; ++i) {
         Island.Block b = (Island.Block)this.island.blocks.get(i);
         if (!b.standing) {
            float t = Math.min(1.0F, (this.time - b.sent) / 0.4F);
            float drop = 0.85F * (1.0F - t) * (1.0F - t) * (1.0F - t);
            float s = 0.55F + 0.45F * back(t);
            this.island.falling(this.moving, this.movingWater, b, drop, s, frame);
         }
      }

      this.bits.clear();
      this.bits.reset();
      this.bits.kind(4);

      for(Bit bit : this.flying) {
         float fade = 1.0F - Math.max(0.0F, (bit.life / bit.span - 0.7F) / 0.3F);
         float s = bit.size * (0.4F + 0.6F * fade);
         float rx = this.camera.right[0] * s;
         float ry = this.camera.right[1] * s;
         float rz = this.camera.right[2] * s;
         float ux = this.camera.up[0] * s;
         float uy = this.camera.up[1] * s;
         float uz = this.camera.up[2] * s;
         Sheet.Region p = bit.picture;
         this.bits.colour(this.island.lit(bit.tint | -16777216));
         this.bits.rawQuad(bit.x - rx + ux, bit.y - ry + uy, bit.z - rz + uz, bit.x - rx - ux, bit.y - ry - uy, bit.z - rz - uz, bit.x + rx - ux, bit.y + ry - uy, bit.z + rz - uz, bit.x + rx + ux, bit.y + ry + uy, bit.z + rz + uz, 0.0F, 0.0F, 1.0F, p.u(0.1F), p.v(0.1F), p.u(0.1F), p.v(15.9F), p.u(15.9F), p.v(15.9F), p.u(15.9F), p.v(0.1F));
      }

      this.sky(aspect);
      this.clouds();
   }

   private static float back(float t) {
      float c = 1.5F;
      float u = t - 1.0F;
      return 1.0F + 2.5F * u * u * u + 1.5F * u * u;
   }

   private void sky(float aspect) {
      this.sky.clear();
      this.sky.reset();
      this.sky.kind(4);
      float u = Tile.WHITE.u(0.5F);
      float v = Tile.WHITE.v(0.5F);
      float z = 0.9999F;
      float[] xs = new float[]{-1.0F, -1.0F, 1.0F, -1.0F, 1.0F, 1.0F};
      float[] ys = new float[]{1.0F, -1.0F, -1.0F, 1.0F, -1.0F, 1.0F};

      for(int i = 0; i < xs.length; ++i) {
         this.sky.colour(ys[i] > 0.0F ? (this.night ? -16579057 : -9462286) : (this.night ? -15327174 : -4467457));
         this.sky.vertex(xs[i], ys[i], 0.9999F, 0.0F, 0.0F, 1.0F, u, v, 1.0F);
      }

      if (this.night) {
         for(int i = 0; i < 90; ++i) {
            float x = Atlas.hash(i, 1, 404) * 2.0F - 1.0F;
            float y = 1.0F - Atlas.hash(i, 2, 404) * Atlas.hash(i, 3, 404) * 1.3F;
            float twinkle = 0.55F + 0.45F * (float)Math.sin((double)(this.time * (0.8F + Atlas.hash(i, 4, 404) * 1.6F) + (float)i));
            float size = (0.0014F + 0.0022F * Atlas.hash(i, 5, 404)) * (0.7F + 0.3F * twinkle);
            int c = Math.round(255.0F * twinkle * (0.55F + 0.45F * (1.0F - (1.0F - y) / 1.3F)));
            this.sky.colour(-16777216 | c << 16 | c << 8 | Math.min(255, c + 18));
            this.sky.rawQuad(x - size, y + size * aspect, 0.9999F, x - size, y - size * aspect, 0.9999F, x + size, y - size * aspect, 0.9999F, x + size, y + size * aspect, 0.9999F, 0.0F, 0.0F, 1.0F, u, v, u, v, u, v, u, v);
         }

      }
   }

   private void clouds() {
      if (this.cloudSide != 0) {
         float drift = this.time * 0.45F;
         float whole = (float)Math.floor((double)(drift / 2.2F)) * 2.2F;
         float yaw = 32.0F + this.time * 4.2F;
         if (whole != this.cloudsFor || !(Math.abs(yaw - this.cloudsYaw) < 2.0F)) {
            this.cloudsFor = whole;
            this.cloudsYaw = yaw;
            this.clouds.clear();
            this.clouds.reset();
            this.clouds.kind(4);
            float y0 = this.island.bottom - 4.5F;
            float y1 = y0 + 0.8F;
            int first = (int)Math.floor((double)((-44.0F - whole) / 2.2F));
            int last = (int)Math.ceil((double)((44.0F - whole) / 2.2F));
            int rows = (int)Math.ceil((double)20.0F);
            float u = Tile.WHITE.u(0.5F);
            float v = Tile.WHITE.v(0.5F);
            float ex = this.camera.eye[0];
            float ey = this.camera.eye[1];
            float ez = this.camera.eye[2];

            for(int m = first; m <= last; ++m) {
               float x0 = (float)m * 2.2F + whole;
               float x1 = x0 + 2.2F;

               for(int n = -rows; n <= rows; ++n) {
                  if (this.cloud(m, n)) {
                     float z0 = (float)n * 2.2F;
                     float z1 = z0 + 2.2F;
                     float cx = (x0 + x1) / 2.0F;
                     float cz = (z0 + z1) / 2.0F;
                     float r = (float)Math.sqrt((double)(cx * cx + cz * cz));
                     float fade = Math.max(0.0F, Math.min(1.0F, (44.0F - r) / 19.8F));
                     if (!(fade <= 0.01F)) {
                        int a = Math.round(0.8F * fade * fade * (3.0F - 2.0F * fade) * 255.0F);
                        if (ey > y1) {
                           this.cloudFace(a, 1.0F, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, u, v);
                        }

                        if (ey < y0) {
                           this.cloudFace(a, 0.7F, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, u, v);
                        }

                        if (ez > z1 && !this.cloud(m, n + 1)) {
                           this.cloudFace(a, 0.8F, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1, u, v);
                        }

                        if (ez < z0 && !this.cloud(m, n - 1)) {
                           this.cloudFace(a, 0.8F, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0, u, v);
                        }

                        if (ex > x1 && !this.cloud(m + 1, n)) {
                           this.cloudFace(a, 0.9F, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0, u, v);
                        }

                        if (ex < x0 && !this.cloud(m - 1, n)) {
                           this.cloudFace(a, 0.9F, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1, u, v);
                        }
                     }
                  }
               }
            }

         }
      }
   }

   private boolean cloud(int m, int n) {
      int x = Math.floorMod(m + 101, this.cloudSide);
      int z = Math.floorMod(n + 23, this.cloudSide);
      return this.cloudMap[z * this.cloudSide + x];
   }

   private void cloudFace(int alpha, float shade, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, float u, float v) {
      int base = this.night ? -12827293 : -1;
      int r = Math.round((float)(base >>> 16 & 255) * shade);
      int g = Math.round((float)(base >>> 8 & 255) * shade);
      int b = Math.round((float)(base & 255) * shade);
      this.clouds.colour(alpha << 24 | r << 16 | g << 8 | b);
      this.clouds.rawQuad(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, 0.0F, 1.0F, 0.0F, u, v, u, v, u, v, u, v);
   }

   private static void rgb(int argb, float[] out) {
      out[0] = (float)(argb >>> 16 & 255) / 255.0F;
      out[1] = (float)(argb >>> 8 & 255) / 255.0F;
      out[2] = (float)(argb & 255) / 255.0F;
   }

   private float[] logoPlace(float w, float h) {
      float[] var10000;
      switch (this.title) {
         case "VANILLA":
            var10000 = new float[]{w / 2.0F, 30.0F, 1.0F};
            break;
         case "CUSTOM":
            var10000 = new float[]{w / 2.0F, h < 300.0F ? 4.0F : 20.0F, 1.0F};
            break;
         default:
            float s = Math.max(0.7F, Math.min(4.0F, Math.min(w * 0.51F / 256.0F, h * 0.22F / 51.0F)));
            int high = Math.round(51.0F * s);
            float middle = (float)Math.max(4, Math.round(h * 0.16F - (float)high / 2.0F)) + (float)high / 2.0F;
            var10000 = new float[]{w / 2.0F, middle - 25.5F * s, s};
      }

      return var10000;
   }

   private void logo(float[] at, float scale) {
      float s = at[2];
      float top = at[1];
      float x = (float)Math.round((at[0] - 128.0F * s) * scale) / scale;
      float y = (float)Math.round(top * scale) / scale;
      this.gfx.image(this.logoTexture, x, y, 256.0F * s, 44.0F * s, 0.0F, 0.0F, 1.0F, 0.6875F, -1);
      if (this.editionTexture != 0) {
         float ex = (float)Math.round((at[0] - 64.0F * s) * scale) / scale;
         this.gfx.image(this.editionTexture, ex, y + 37.0F * s, 128.0F * s, 14.0F * s, 0.0F, 0.0F, 1.0F, 0.875F, -1);
      }

   }

   private void fonts(float scale) {
      if (this.game == null && (scale != this.bakedScale || this.text == null)) {
         if (this.text != null) {
            this.text.delete();
         }

         this.text = PixelFont.bake(this.ttf, 9.0F, scale, 224, true);
         this.bakedScale = scale;
      }
   }

   private float width(String s) {
      return this.game != null ? this.game.width(s) : this.text.width(s);
   }

   private String fit(String s, float max) {
      return this.game != null ? this.game.fit(s, max) : this.text.fit(s, max);
   }

   private void write(String s, float x, float y, int argb) {
      int shadow = argb & -16777216 | (argb & 16579836) >> 2;
      if (this.game != null) {
         this.game.draw(this.gfx, s, x + 1.0F, y + 1.0F, shadow);
         this.game.draw(this.gfx, s, x, y, argb);
      } else {
         this.text.draw(this.gfx, s, x + 1.0F, y + 1.0F, shadow);
         this.text.draw(this.gfx, s, x, y, argb);
      }
   }

   private void words(LoadingScene.Feed feed, float w, float h, float barY) {
      float barW = (float)Math.round(Math.min(w * 0.75F, h));
      float barX = (float)Math.round((w - barW) / 2.0F);
      float fill = Math.min(1.0F, this.shown);
      this.gfx.rect(barX + 1.0F, barY, barW - 2.0F, 1.0F, -1);
      this.gfx.rect(barX + 1.0F, barY + 9.0F, barW - 2.0F, 1.0F, -1);
      this.gfx.rect(barX, barY, 1.0F, 10.0F, -1);
      this.gfx.rect(barX + barW - 1.0F, barY, 1.0F, 10.0F, -1);
      this.gfx.rect(barX + 2.0F, barY + 2.0F, (float)Math.round((barW - 4.0F) * fill), 6.0F, -1);
      String all = Math.round(fill * 100.0F) + "%";
      float allW = this.width(all);
      this.write(all, barX + barW - allW, barY - 12.0F, -1);
      String task = feed.task() != null && !feed.task().isBlank() ? feed.task() : "Loading";
      String count = feed.taskCount() == null ? "" : feed.taskCount();
      float countW = count.isEmpty() ? 0.0F : this.width(count) + 6.0F;
      String shownTask = this.fit(task, barW - allW - 10.0F - countW);
      this.write(shownTask, barX, barY - 12.0F, -1);
      if (!count.isEmpty()) {
         this.write(count, barX + this.width(shownTask) + 6.0F, barY - 12.0F, -5592406);
      }

      String detail = feed.detail();
      if (detail != null && !detail.isBlank() && !detail.equals(task)) {
         String line = this.fit(detail, barW);
         this.write(line, (float)Math.round((w - this.width(line)) / 2.0F), barY + 16.0F, -5592406);
      }

      boolean loader = this.look.loader() != null && !this.look.loader().isEmpty();
      this.write(this.look.version(), 2.0F, h - (float)(loader ? 20 : 10), -1);
      if (loader) {
         this.write(this.look.loader(), 2.0F, h - 10.0F, -1);
      }

      long max = feed.memoryMax();
      long used = feed.memoryUsed();
      if (max > 0L) {
         long pct = used * 100L / max;
         String memory = String.format(Locale.ROOT, "Mem: %d%% %d/%dMB", pct, used >> 20, max >> 20);
         this.write(memory, w - 2.0F - this.width(memory), h - 10.0F, pct >= 88L ? -43691 : -1);
      }

      String elapsed = feed.elapsed();
      if (elapsed != null) {
         this.write(elapsed, w - 2.0F - this.width(elapsed), h - 20.0F, -5592406);
      }

   }

   private static final class Bit {
      float x;
      float y;
      float z;
      float vx;
      float vy;
      float vz;
      float life;
      float span;
      float size;
      Sheet.Region picture;
      int tint;
   }
}
