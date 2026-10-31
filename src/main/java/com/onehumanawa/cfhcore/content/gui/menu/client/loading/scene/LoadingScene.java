package com.onehumanawa.cfhcore.content.gui.menu.client.loading.scene;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.function.LongSupplier;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.system.MemoryUtil;

public final class LoadingScene implements StartupScene {
   static final float PRESS_DOWN = 0.11F;
   static final float PRESS_UP = 0.24F;
   private static final float EVERY = 0.95F;
   private static final float APART = 0.9F;
   private static final float STOCKED = 1.75F;
   private static final int MAX_WAITING = 5;
   private static final int MAX_ITEMS = 16;
   private static final int BG = -15329771;
   private static final int FLOOR = -11843519;
   private static final int TEXT = -1250588;
   private static final int MUTED = -6185065;
   private static final int DIM = -9540506;
   private static final int DANGER = -1750963;
   private final Look look;
   private final Face face;
   private final LongSupplier clock;
   private final ByteBuffer ttf;
   private final Sheet sheet = new Sheet();
   private final Factory factory;
   private final RuntimeException createFailure;
   private final Gfx gfx = new Gfx();
   private final SceneGl gl = new SceneGl();
   private final GlSave saved = new GlSave();
   private final Frame frame = new Frame();
   private final Mesh title = new Mesh(8000);
   private boolean titleBuilt;
   private Picture logo;
   private Picture edition;
   private int logoTexture;
   private int editionTexture;
   private final float[] mirrored = new float[16];
   private final SceneGl.Light light = new SceneGl.Light();
   private final SceneGl.Light titleLight = new SceneGl.Light();
   private PixelFont text;
   private float bakedScale = -1.0F;
   private final long start;
   private long last;
   private final Motion motion = new Motion();
   private float taskAge;
   private boolean seenOverall;
   private long seenDone = -1L;
   private int waiting;
   private float next;
   private boolean stocked;
   private boolean struck;
   private int seed = 12345;
   private int made;
   private final Camera camera = new Camera();
   private final Camera titleCamera = new Camera();
   private final float[] lightViewProj = new float[16];
   private final float[] tmp = new float[4];
   private static final float TITLE_W;
   private static final float TITLE_H = 5.0F;
   private static final float[] TITLE;

   public LoadingScene(Look look, byte[] font, LongSupplier clock, Assets assets) {
      this.look = look;
      this.face = LoadingScene.Face.of(look.font());
      this.clock = clock;
      this.ttf = MemoryUtil.memAlloc(font.length).put(font).flip();
      this.start = clock.getAsLong();
      this.last = this.start;
      if (assets != null) {
         this.logo = Picture.read(assets.read("minecraft", "textures/gui/title/minecraft.png"));
         this.edition = this.logo == null ? null : Picture.read(assets.read("minecraft", "textures/gui/title/edition.png"));
      }

      Factory create = CreateFactory.make(assets, this.sheet);
      this.factory = (Factory)(create != null ? create : new OwnFactory());
      this.createFailure = create != null ? null : CreateFactory.failure;
   }

   public boolean logo() {
      return this.logo != null;
   }

   public boolean create() {
      return this.factory instanceof CreateFactory;
   }

   public RuntimeException createFailure() {
      return this.createFailure;
   }

   public static String fontFile(String key) {
      return LoadingScene.Face.of(key).file();
   }

   public int[] textures() {
      return (int[])this.sheet.pixels().clone();
   }

   public void dispose() {
      if (this.text != null) {
         this.text.delete();
      }

      this.text = null;
      if (this.logoTexture != 0) {
         GL32C.glDeleteTextures(this.logoTexture);
      }

      if (this.editionTexture != 0) {
         GL32C.glDeleteTextures(this.editionTexture);
      }

      this.logoTexture = this.editionTexture = 0;
      this.gfx.delete();
      this.gl.delete();
      this.frame.free();
      this.title.free();
      this.factory.free();
      MemoryUtil.memFree(this.ttf);
   }

   public void draw(int pixelW, int pixelH, float scale, boolean flipped, float alpha, Feed feed) {
      long now = this.clock.getAsLong();
      float dt = Math.max(0.0F, Math.min(0.1F, (float)(now - this.last) / 1.0E9F));
      this.last = now;
      this.motion.time = (float)(now - this.start) / 1.0E9F;
      this.advance(feed, dt);
      this.saved.save();

      try {
         this.cameras(pixelW, pixelH);
         this.lights();
         this.frame.clear();
         this.frame.camera = this.camera;
         this.frame.ground.kind(3);
         this.frame.ground.rawQuad(-60.0F, 0.0F, -40.0F, -60.0F, 0.0F, 40.0F, 60.0F, 0.0F, 40.0F, 60.0F, 0.0F, -40.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F);
         this.factory.build(this.motion, this.frame, this.look.accent());
         if (!this.titleBuilt) {
            this.titleBuilt = true;
            this.title.clear();
            Letters.build(this.title);
         }

         this.gl.begin(pixelW, pixelH, this.sheet);
         this.gl.load(this.frame.ground, this.frame.stage, this.frame.polish, this.frame.solid, this.frame.glass, this.frame.glow, this.title);
         this.gl.shadows(this.frame.solid, this.lightViewProj);
         this.gl.picture(this.light.background);
         this.gl.view(this.camera.viewProj, this.camera.eye, this.light, this.lightViewProj, true);
         this.gl.draw(this.frame.ground, 0);
         this.gl.camera(this.mirrored);
         this.gl.draw(this.frame.solid, 3);
         this.gl.camera(this.camera.viewProj);
         this.gl.draw(this.frame.stage, 0);
         this.gl.draw(this.frame.polish, 1);
         this.gl.draw(this.frame.solid, 0);
         this.gl.draw(this.frame.glass, 1);
         this.gl.draw(this.frame.glow, 2);
         if (this.logo == null) {
            this.gl.inFront();
            this.gl.view(this.titleCamera.viewProj, this.titleCamera.eye, this.titleLight, this.lightViewProj, false);
            this.gl.draw(this.title, 0);
         }

         int picture = this.gl.finish();
         if (this.logo != null && this.logoTexture == 0) {
            this.logoTexture = this.logo.texture();
            if (this.edition != null) {
               this.editionTexture = this.edition.texture();
            }
         }

         GL32C.glBindFramebuffer(36160, this.saved.canvas());
         GL32C.glViewport(0, 0, pixelW, pixelH);
         float fine = (float)Math.max(1, Math.round(scale * 0.7F));
         this.gfx.begin(pixelW, pixelH, fine, flipped, false);

         try {
            this.fonts(fine);
            float w = (float)pixelW / fine;
            float h = (float)pixelH / fine;
            float shown = alpha * ease(Math.min(1.0F, this.motion.time / 0.45F));
            if (alpha >= 1.0F) {
               this.gfx.rect(0.0F, 0.0F, w, h, this.background());
            }

            this.gfx.alpha(shown);
            this.gfx.image(picture, 0.0F, 0.0F, w, h, 0.0F, 1.0F, 1.0F, 0.0F, -1);
            this.gfx.vignette(w, h, this.look.vanilla() ? 805306368 : 1711276032);
            if (this.logoTexture != 0) {
               this.logo((float)pixelW / scale, (float)pixelH / scale, scale / fine);
            }

            this.words(feed, w, h);
         } finally {
            this.gfx.end();
         }
      } finally {
         this.saved.restore();
      }

   }

   private int background() {
      return this.look.vanilla() ? this.look.background() | -16777216 : -15329771;
   }

   private void fonts(float scale) {
      if (scale != this.bakedScale || this.text == null) {
         if (this.text != null) {
            this.text.delete();
         }

         this.text = PixelFont.bake(this.ttf, this.face.text(), scale, 224, this.face.crisp());
         this.bakedScale = scale;
      }
   }

   private void advance(Feed feed, float dt) {
      Motion m = this.motion;
      float overall = feed.overall();
      if (overall >= 0.0F) {
         float to = Math.min(1.0F, overall);
         if (this.seenOverall && !(to < m.overall - 0.05F)) {
            m.overall += (to - m.overall) * rate(dt, 5.0F);
         } else {
            m.overall = to;
         }

         this.seenOverall = true;
      }

      long max = feed.memoryMax();
      float memory = max > 0L ? Math.max(0.0F, Math.min(1.0F, (float)feed.memoryUsed() / (float)max)) : 0.0F;
      m.memory = m.memory < 0.0F ? memory : m.memory + (memory - m.memory) * rate(dt, 1.6F);
      long done = feed.tasksDone();
      if (this.seenDone < 0L) {
         this.seenDone = done;
      }

      if (done > this.seenDone) {
         this.waiting = (int)Math.min(5L, (long)this.waiting + (done - this.seenDone));
         this.seenDone = done;
         this.taskAge = 0.0F;
         m.lump = 0.0F;
      }

      this.taskAge += dt;
      float progress = feed.taskProgress();
      float target = progress >= 0.0F ? Math.min(1.0F, progress) : 0.9F * (1.0F - (float)Math.exp((double)(-this.taskAge / 5.0F)));
      m.lump += (Math.max(m.lump, target) - m.lump) * rate(dt, 6.0F);
      m.needle += (m.lump - m.needle) * rate(dt, 10.0F);
      m.beltA += 1.9F * dt;
      m.beltB += 1.9F * dt;
      float first = this.factory.lumpX1 - this.factory.lumpX0;
      if (!this.stocked) {
         this.stocked = true;

         for(float s = this.factory.way.length() - 0.6F; s > -first + 0.4F; --s) {
            Motion.Item item = new Motion.Item();
            item.s = s;
            item.pressed = s > this.factory.pressAt;
            item.id = ++this.made;
            m.items.add(item);
         }

         this.next = 0.38F;
      }

      this.next -= dt * (1.0F + 1.2F * (float)this.waiting);
      Motion.Item newest = m.items.isEmpty() ? null : (Motion.Item)m.items.get(m.items.size() - 1);
      if (this.next <= 0.0F && m.items.size() < 16 && (newest == null || newest.s >= -first + 0.9F)) {
         Motion.Item item = new Motion.Item();
         item.s = -first;
         item.id = ++this.made;
         m.items.add(item);
         this.next = 0.95F;
         if (this.waiting > 0) {
            --this.waiting;
         }
      }

      float end = this.factory.way.length();
      Motion.Item ahead = null;

      for(int i = 0; i < m.items.size(); ++i) {
         Motion.Item it = (Motion.Item)m.items.get(i);
         if (it.leaving >= 0.0F) {
            it.leaving += dt;
         } else {
            float to = it.s + 1.9F * dt;
            if (!it.pressed && to >= this.factory.pressAt) {
               to = this.factory.pressAt;
               if (m.stroke < 0.0F) {
                  m.stroke = 0.0F;
                  this.struck = false;
               }
            }

            if (ahead != null && ahead.leaving < 0.0F) {
               to = Math.min(to, ahead.s - 0.62F);
            }

            it.s = Math.max(it.s, to);
            if (it.s >= end) {
               it.leaving = 0.0F;
            }

            ahead = it;
         }
      }

      float gone = this.factory.leaving(m);
      m.items.removeIf((itx) -> {
         if (itx.leaving < gone) {
            return false;
         } else {
            m.landed = 0.0F;
            return true;
         }
      });
      m.landed = Math.min(1.0F, m.landed + dt * 4.0F);
      if (m.stroke >= 0.0F) {
         m.stroke += dt;
         if (m.stroke < 0.11F) {
            float t = m.stroke / 0.11F;
            m.pressDown = t * t;
         } else {
            if (!this.struck) {
               this.struck = true;
               this.strike();
            }

            float t = Math.min(1.0F, (m.stroke - 0.11F) / 0.24F);
            m.pressDown = 1.0F - ease(t);
            if (t >= 1.0F) {
               m.stroke = -1.0F;
            }
         }
      } else {
         m.pressDown = 0.0F;
      }

      for(Motion.Spark s : m.sparks) {
         s.life += dt;
         s.x += s.vx * dt;
         s.y += s.vy * dt;
         s.z += s.vz * dt;
         if (s.smoke) {
            s.vy += 0.5F * dt;
            s.vx *= 1.0F - 1.5F * dt;
            s.vz *= 1.0F - 1.5F * dt;
         } else {
            s.vy -= 9.0F * dt;
            if (s.y < this.factory.beltY + 0.02F && s.vy < 0.0F) {
               s.y = this.factory.beltY + 0.02F;
               s.vy *= -0.35F;
               s.vx *= 0.6F;
            }
         }
      }

      m.sparks.removeIf((sx) -> sx.life >= sx.span);
   }

   private void strike() {
      Motion m = this.motion;

      for(Motion.Item it : m.items) {
         if (!it.pressed && it.leaving < 0.0F && Math.abs(it.s - this.factory.pressAt) < 0.05F) {
            it.pressed = true;
            break;
         }
      }

      int hot = Atlas.mix(this.look.accent(), -5712, 0.55F);

      for(int i = 0; i < 12; ++i) {
         Motion.Spark s = new Motion.Spark();
         double a = (double)this.random() * Math.PI * (double)2.0F;
         float out = 1.4F + this.random() * 2.2F;
         s.x = this.factory.pressX + (float)Math.cos(a) * 0.3F;
         s.z = (float)Math.sin(a) * 0.3F;
         s.y = this.factory.beltY + 0.19F;
         s.vx = (float)Math.cos(a) * out;
         s.vz = (float)Math.sin(a) * out;
         s.vy = 1.2F + this.random() * 2.2F;
         s.span = 0.35F + this.random() * 0.3F;
         s.size = 0.05F + this.random() * 0.05F;
         s.colour = hot;
         m.sparks.add(s);
      }

      for(int i = 0; i < 3; ++i) {
         Motion.Spark s = new Motion.Spark();
         s.smoke = true;
         s.x = this.factory.pressX + (this.random() - 0.5F) * 0.5F;
         s.z = 0.2F + this.random() * 0.3F;
         s.y = this.factory.beltY + 0.25F;
         s.vx = (this.random() - 0.5F) * 0.6F;
         s.vz = this.random() * 0.3F;
         s.vy = 0.5F + this.random() * 0.4F;
         s.span = 0.9F + this.random() * 0.5F;
         s.size = 0.32F + this.random() * 0.2F;
         s.colour = -1514278;
         m.sparks.add(s);
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

   private void cameras(int w, int h) {
      float aspect = (float)w / (float)h;
      this.camera.fit(this.factory.target, 13.0F, this.factory.pitch, 24.0F, aspect, this.factory.bounds, new float[]{-0.96F, 0.96F, -0.94F, 0.16F}, 0.0F);
      this.titleCamera.fit(new float[]{0.0F, 2.5F, 0.0F}, 0.0F, -13.0F, 18.0F, aspect, TITLE, new float[]{-0.56F, 0.56F, 0.34F, 0.8F}, 0.5F);
      float kx = this.light.keyDir[0];
      float ky = this.light.keyDir[1];
      float kz = this.light.keyDir[2];
      float cx = this.factory.target[0];
      float[] v = Mat4.lookAt(cx - kx * 30.0F, 1.5F - ky * 30.0F, -kz * 30.0F, cx, 1.5F, 0.0F, 0.0F, 1.0F, 0.0F);
      float l = Float.MAX_VALUE;
      float r = -Float.MAX_VALUE;
      float b = Float.MAX_VALUE;
      float t = -Float.MAX_VALUE;
      float n = Float.MAX_VALUE;
      float f = -Float.MAX_VALUE;
      float[] bounds = this.factory.bounds;

      for(int i = 0; i + 2 < bounds.length; i += 3) {
         Mat4.transform(v, bounds[i], bounds[i + 1], bounds[i + 2], this.tmp);
         l = Math.min(l, this.tmp[0]);
         r = Math.max(r, this.tmp[0]);
         b = Math.min(b, this.tmp[1]);
         t = Math.max(t, this.tmp[1]);
         n = Math.min(n, -this.tmp[2]);
         f = Math.max(f, -this.tmp[2]);
      }

      Mat4.mul(this.lightViewProj, Mat4.ortho(l - 0.5F, r + 0.5F, b - 0.5F, t + 0.5F, n - 2.0F, f + 6.0F), v);
      float[] flip = Mat4.identity();
      Mat4.translate(flip, 0.0F, 0.375F, 0.0F);
      Mat4.scale(flip, 1.0F, -1.0F, 1.0F);
      Mat4.translate(flip, 0.0F, -0.375F, 0.0F);
      Mat4.mul(this.mirrored, this.camera.viewProj, flip);
   }

   private void lights() {
      int bg = this.background();
      rgb(bg, this.light.background);
      rgb(this.look.vanilla() ? bg : -11843519, this.light.floorLit);
      if (this.look.vanilla()) {
         this.light.floorLit[0] = Math.min(1.0F, this.light.floorLit[0] * 1.18F + 0.04F);
         this.light.floorLit[1] = Math.min(1.0F, this.light.floorLit[1] * 1.18F + 0.04F);
         this.light.floorLit[2] = Math.min(1.0F, this.light.floorLit[2] * 1.18F + 0.04F);
      }

      this.light.pool[0] = this.factory.stage[0];
      this.light.pool[1] = this.factory.stage[1] + 0.2F;
      this.light.pool[2] = this.factory.stage[2] + 5.25F;
      this.light.pool[3] = this.factory.stage[3] + 4.75F;
      System.arraycopy(this.factory.stage, 0, this.light.stage, 0, 4);
      this.light.time = this.motion.time;
      normalize(this.light.keyDir);
      normalize(this.light.fillDir);
      this.factory.lamps(this.motion, this.light, this.look.accent());
      float along = this.motion.time % 7.0F / 1.6F;
      float run = along < 1.0F ? (float)Math.sin((double)along * Math.PI) : 0.0F;
      Factory.lamp(this.titleLight, 0, 0.0F, 2.5F, 9.0F, 30.0F, 0.16F, 0.15F, 0.13F);
      Factory.lamp(this.titleLight, 1, (along - 0.5F) * (TITLE_W + 12.0F), 2.5F, 3.5F, 9.0F, 0.5F * run, 0.48F * run, 0.42F * run);
      System.arraycopy(this.light.keyDir, 0, this.titleLight.keyDir, 0, 3);
      System.arraycopy(this.light.background, 0, this.titleLight.background, 0, 3);
      this.titleLight.keyColour[0] = 0.98F;
      this.titleLight.keyColour[1] = 0.93F;
      this.titleLight.keyColour[2] = 0.84F;
      this.titleLight.ambient[0] = this.titleLight.ambient[1] = 0.36F;
      this.titleLight.ambient[2] = 0.39F;
      this.titleLight.wrap = 0.5F;
      this.titleLight.time = this.motion.time;
   }

   private static void rgb(int argb, float[] out) {
      out[0] = (float)(argb >>> 16 & 255) / 255.0F;
      out[1] = (float)(argb >>> 8 & 255) / 255.0F;
      out[2] = (float)(argb & 255) / 255.0F;
   }

   private static void normalize(float[] v) {
      float l = (float)Math.sqrt((double)(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]));
      v[0] /= l;
      v[1] /= l;
      v[2] /= l;
   }

   private void logo(float w, float h, float unit) {
      float s = Math.max(0.7F, Math.min(4.0F, Math.min(w * 0.51F / 256.0F, h * 0.22F / 51.0F)));
      int high = Math.round(51.0F * s);
      float middle = (float)Math.max(4, Math.round(h * 0.16F - (float)high / 2.0F)) + (float)high / 2.0F;
      float top = middle - 25.5F * s;
      this.gfx.image(this.logoTexture, (w / 2.0F - 128.0F * s) * unit, top * unit, 256.0F * s * unit, 44.0F * s * unit, 0.0F, 0.0F, 1.0F, 0.6875F, -1);
      if (this.editionTexture != 0) {
         this.gfx.image(this.editionTexture, (w / 2.0F - 64.0F * s) * unit, (top + 37.0F * s) * unit, 128.0F * s * unit, 14.0F * s * unit, 0.0F, 0.0F, 1.0F, 0.875F, -1);
      }

   }

   private void words(Feed feed, float w, float h) {
      int strong = this.look.vanilla() ? -1 : -1250588;
      int soft = this.look.vanilla() ? -637534209 : -6185065;
      int faint = this.look.vanilla() ? -1711276033 : -9540506;
      float[] at = new float[2];
      this.text.draw(this.gfx, this.look.version(), 10.0F, 10.0F, soft);
      boolean loader = this.look.loader() != null && !this.look.loader().isEmpty();
      if (loader) {
         this.text.draw(this.gfx, this.look.loader(), 10.0F, 21.0F, faint);
      }

      String elapsed = feed.elapsed();
      if (elapsed != null) {
         this.text.draw(this.gfx, elapsed, 10.0F, loader ? 32.0F : 21.0F, faint);
      }

      long max = feed.memoryMax();
      long used = feed.memoryUsed();
      String memory = String.format(Locale.ROOT, "%,d / %,d MB", used >> 20, max >> 20);
      this.project(this.factory.memoryAt, w, h, at);
      float mw = this.text.width(memory);
      float mx = Math.max(8.0F, (float)Math.round(at[0] - mw / 2.0F));
      float my = (float)Math.round(at[1] - 26.0F);
      this.text.draw(this.gfx, "RAM", mx, my, faint);
      this.text.draw(this.gfx, memory, mx, my + 10.0F, max > 0L && (double)used / (double)max > 0.88 ? (this.look.vanilla() ? -1 : -1750963) : strong);
      this.project(this.factory.taskFrom, w, h, at);
      float tx = Math.max(mx + mw + 14.0F, (float)Math.round(at[0]));
      this.project(this.factory.taskTo, w, h, at);
      float room = Math.max(60.0F, at[0] - 8.0F - tx);
      float clear = h;

      for(float[] point : this.factory.over) {
         this.project(point, w, h, at);
         clear = Math.min(clear, at[1]);
      }

      float ty = (float)Math.round(clear - 8.0F - 25.0F);
      String task = feed.task() != null && !feed.task().isBlank() ? feed.task() : "Loading";
      String count = feed.taskCount() == null ? "" : feed.taskCount();
      float cw = this.text.width(count);
      String shown = this.text.fit(task, room - (cw > 0.0F ? cw + 8.0F : 0.0F));
      this.text.draw(this.gfx, shown, tx, ty, strong);
      if (cw > 0.0F) {
         this.text.draw(this.gfx, count, tx + room - cw, ty, soft);
      }

      float capW = Math.max(16.0F, Math.min(this.text.width(shown), 48.0F));
      this.gfx.hgradient(tx, ty + 10.0F, capW, 2.0F, this.look.accent(), this.look.accent() & 16777215 | 805306368);
      String detail = feed.detail();
      if (detail != null && !detail.isBlank() && !detail.equals(task)) {
         this.text.draw(this.gfx, this.text.fit(detail, room), tx, ty + 17.0F, soft);
      }

      if (feed.overall() >= 0.0F) {
         float var10000 = Math.min(1.0F, this.motion.overall);
         String all = Math.round(var10000 * 100.0F) + "%";
         this.project(this.factory.totalAt, w, h, at);
         float ax = Math.min(w - 8.0F - this.text.width(all), (float)Math.round(at[0] - this.text.width(all) / 2.0F));
         this.text.draw(this.gfx, all, ax, (float)Math.round(at[1] - 9.0F), strong);
      }

   }

   private void project(float[] point, float w, float h, float[] out) {
      this.camera.project(point[0], point[1], point[2], w, h, out);
   }

   static {
      TITLE_W = Letters.WIDTH;
      TITLE = Camera.box(-TITLE_W / 2.0F, 0.0F, -0.6F, TITLE_W / 2.0F, 5.0F, 0.6F);
   }

   public interface Feed {
      float overall();

      String task();

      float taskProgress();

      String taskCount();

      long tasksDone();

      String detail();

      long memoryUsed();

      long memoryMax();

      default String elapsed() {
         return null;
      }
   }

   public static record Look(int accent, boolean vanilla, int background, String font, String version, String loader) {
   }

   private static record Face(String file, float text, boolean crisp) {
      static Face of(String key) {
         Face var10000;
         switch (key == null ? "" : key) {
            case "monocraft" -> var10000 = new Face("monocraft.ttf", 9.0F, true);
            case "pixelify" -> var10000 = new Face("pixelify.ttf", 10.0F, false);
            default -> var10000 = new Face("pixeloidsans.ttf", 9.0F, true);
         }

         return var10000;
      }
   }
}
