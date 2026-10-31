package com.onehumanawa.cfhcore.content.gui.menu.client.loading.scene;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.system.MemoryUtil;

final class Mesh {
   static final int STRIDE = 32;
   static final int LIT = 0;
   static final int LAVA = 1;
   static final int GLOW = 2;
   static final int FLOOR = 3;
   static final int FLAT = 4;
   static final int TOP = 1;
   static final int BOTTOM = 2;
   static final int FRONT = 4;
   static final int BACK = 8;
   static final int RIGHT = 16;
   static final int LEFT = 32;
   static final int ALL = 63;
   static final int SIDES = 60;
   private ByteBuffer buffer;
   private int vertices;
   int first;
   private final float[] m = Mat4.identity();
   private final float[][] stack = new float[24][16];
   private int depth;
   private final float[] p = new float[4];
   private Tile side;
   private Tile top;
   private Tile bottom;
   private int colour;
   private int emissive;
   private int kind;
   private int gloss;
   private int faces;
   private float ground;
   private float texelU;
   private float texelV;
   private static final float[] IDENTITY = Mat4.identity();

   Mesh(int capacity) {
      this.side = Tile.WHITE;
      this.top = Tile.WHITE;
      this.bottom = Tile.WHITE;
      this.colour = -1;
      this.faces = 63;
      this.ground = 1.0F;
      this.buffer = MemoryUtil.memAlloc(capacity * 32).order(ByteOrder.nativeOrder());
   }

   void free() {
      MemoryUtil.memFree(this.buffer);
      this.buffer = null;
   }

   void clear() {
      this.buffer.clear();
      this.vertices = 0;
      this.depth = 0;
      Mat4.set(this.m, IDENTITY);
      this.reset();
   }

   void reset() {
      this.side = this.top = this.bottom = Tile.WHITE;
      this.colour = -1;
      this.emissive = 0;
      this.kind = 0;
      this.gloss = 0;
      this.faces = 63;
      this.ground = 1.0F;
      this.texelU = this.texelV = 0.0F;
   }

   int vertices() {
      return this.vertices;
   }

   void add(Mesh other) {
      ByteBuffer from = other.data();

      while(this.buffer.remaining() < from.remaining()) {
         this.grow();
      }

      this.buffer.put(from);
      this.vertices += other.vertices;
   }

   ByteBuffer data() {
      ByteBuffer view = this.buffer.duplicate().order(ByteOrder.nativeOrder());
      view.flip();
      return view;
   }

   void push() {
      Mat4.set(this.stack[this.depth++], this.m);
   }

   void pop() {
      Mat4.set(this.m, this.stack[--this.depth]);
   }

   void copyMatrix(Mesh from) {
      Mat4.set(this.m, from.m);
   }

   void translate(float x, float y, float z) {
      Mat4.translate(this.m, x, y, z);
   }

   void rotateX(float degrees) {
      Mat4.rotate(this.m, degrees, 1.0F, 0.0F, 0.0F);
   }

   void rotateY(float degrees) {
      Mat4.rotate(this.m, degrees, 0.0F, 1.0F, 0.0F);
   }

   void rotateZ(float degrees) {
      Mat4.rotate(this.m, degrees, 0.0F, 0.0F, 1.0F);
   }

   void scale(float s) {
      Mat4.scale(this.m, s, s, s);
   }

   void scale(float x, float y, float z) {
      Mat4.scale(this.m, x, y, z);
   }

   Mesh tile(Tile all) {
      this.side = this.top = this.bottom = all;
      return this;
   }

   Mesh tiles(Tile side, Tile top, Tile bottom) {
      this.side = side;
      this.top = top;
      this.bottom = bottom;
      return this;
   }

   Mesh colour(int argb) {
      this.colour = argb;
      return this;
   }

   Mesh emissive(float amount) {
      this.emissive = Math.round(clamp(amount) * 255.0F);
      return this;
   }

   Mesh kind(int kind) {
      this.kind = kind;
      return this;
   }

   Mesh gloss(float amount) {
      this.gloss = Math.round(clamp(amount) * 255.0F);
      return this;
   }

   Mesh faces(int mask) {
      this.faces = mask;
      return this;
   }

   Mesh ground(float light) {
      this.ground = light;
      return this;
   }

   Mesh texel(float u, float v) {
      this.texelU = u;
      this.texelV = v;
      return this;
   }

   private static float clamp(float v) {
      return Math.max(0.0F, Math.min(1.0F, v));
   }

   void box(float x0, float y0, float z0, float x1, float y1, float z1) {
      if ((this.faces & 1) != 0) {
         this.face(this.top, x0, y1, z0, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, x1 - x0, z1 - z0, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F);
      }

      if ((this.faces & 2) != 0) {
         this.face(this.bottom, x0, y0, z1, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, x1 - x0, z1 - z0, 0.0F, -1.0F, 0.0F, this.ground, this.ground);
      }

      if ((this.faces & 4) != 0) {
         this.face(this.side, x0, y1, z1, 1.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, x1 - x0, y1 - y0, 0.0F, 0.0F, 1.0F, 1.0F, this.ground);
      }

      if ((this.faces & 8) != 0) {
         this.face(this.side, x1, y1, z0, -1.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, x1 - x0, y1 - y0, 0.0F, 0.0F, -1.0F, 1.0F, this.ground);
      }

      if ((this.faces & 16) != 0) {
         this.face(this.side, x1, y1, z1, 0.0F, 0.0F, -1.0F, 0.0F, -1.0F, 0.0F, z1 - z0, y1 - y0, 1.0F, 0.0F, 0.0F, 1.0F, this.ground);
      }

      if ((this.faces & 32) != 0) {
         this.face(this.side, x0, y1, z0, 0.0F, 0.0F, 1.0F, 0.0F, -1.0F, 0.0F, z1 - z0, y1 - y0, -1.0F, 0.0F, 0.0F, 1.0F, this.ground);
      }

   }

   private void face(Tile tile, float ox, float oy, float oz, float ux, float uy, float uz, float vx, float vy, float vz, float w, float h, float nx, float ny, float nz, float lightTop, float lightBottom) {
      if (!(w <= 0.0F) && !(h <= 0.0F)) {
         float startU = this.texelU / 16.0F;
         float startV = this.texelV / 16.0F;

         float spanA;
         for(float a = 0.0F; a < w - 1.0E-5F; a += spanA) {
            float tu = into(a + startU);
            spanA = Math.min(w - a, 1.0F - tu);

            float b1;
            for(float b = 0.0F; b < h - 1.0E-5F; b = b1) {
               float tv = into(b + startV);
               float spanB = Math.min(h - b, 1.0F - tv);
               float a1 = a + spanA;
               b1 = b + spanB;
               float l0 = lightTop + (lightBottom - lightTop) * (b / h);
               float l1 = lightTop + (lightBottom - lightTop) * (b1 / h);
               float u0 = tile.u(tu);
               float u1 = tile.u(tu + spanA);
               float v0 = tile.v(tv);
               float v1 = tile.v(tv + spanB);
               this.vertex(ox + ux * a + vx * b, oy + uy * a + vy * b, oz + uz * a + vz * b, nx, ny, nz, u0, v0, l0);
               this.vertex(ox + ux * a + vx * b1, oy + uy * a + vy * b1, oz + uz * a + vz * b1, nx, ny, nz, u0, v1, l1);
               this.vertex(ox + ux * a1 + vx * b1, oy + uy * a1 + vy * b1, oz + uz * a1 + vz * b1, nx, ny, nz, u1, v1, l1);
               this.vertex(ox + ux * a + vx * b, oy + uy * a + vy * b, oz + uz * a + vz * b, nx, ny, nz, u0, v0, l0);
               this.vertex(ox + ux * a1 + vx * b1, oy + uy * a1 + vy * b1, oz + uz * a1 + vz * b1, nx, ny, nz, u1, v1, l1);
               this.vertex(ox + ux * a1 + vx * b, oy + uy * a1 + vy * b, oz + uz * a1 + vz * b, nx, ny, nz, u1, v0, l0);
            }
         }

      }
   }

   private static float into(float at) {
      float t = at - (float)Math.floor((double)at);
      return t > 0.9999F ? 0.0F : t;
   }

   void quad(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, float u0, float v0, float u1, float v1) {
      float e1x = bx - ax;
      float e1y = by - ay;
      float e1z = bz - az;
      float e2x = dx - ax;
      float e2y = dy - ay;
      float e2z = dz - az;
      float nx = e1y * e2z - e1z * e2y;
      float ny = e1z * e2x - e1x * e2z;
      float nz = e1x * e2y - e1y * e2x;
      float l = (float)Math.sqrt((double)(nx * nx + ny * ny + nz * nz));
      if (l > 0.0F) {
         nx /= l;
         ny /= l;
         nz /= l;
      }

      float tu0 = this.side.u(u0);
      float tu1 = this.side.u(u1);
      float tv0 = this.side.v(v0);
      float tv1 = this.side.v(v1);
      this.vertex(ax, ay, az, nx, ny, nz, tu0, tv0, 1.0F);
      this.vertex(bx, by, bz, nx, ny, nz, tu0, tv1, 1.0F);
      this.vertex(cx, cy, cz, nx, ny, nz, tu1, tv1, 1.0F);
      this.vertex(ax, ay, az, nx, ny, nz, tu0, tv0, 1.0F);
      this.vertex(cx, cy, cz, nx, ny, nz, tu1, tv1, 1.0F);
      this.vertex(dx, dy, dz, nx, ny, nz, tu1, tv0, 1.0F);
   }

   void rawQuad(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, float nx, float ny, float nz, float au, float av, float bu, float bv, float cu, float cv, float du, float dv) {
      this.vertex(ax, ay, az, nx, ny, nz, au, av, 1.0F);
      this.vertex(bx, by, bz, nx, ny, nz, bu, bv, 1.0F);
      this.vertex(cx, cy, cz, nx, ny, nz, cu, cv, 1.0F);
      this.vertex(ax, ay, az, nx, ny, nz, au, av, 1.0F);
      this.vertex(cx, cy, cz, nx, ny, nz, cu, cv, 1.0F);
      this.vertex(dx, dy, dz, nx, ny, nz, du, dv, 1.0F);
   }

   void cylinderZ(float cx, float cy, float z0, float z1, float radius, int segments) {
      float length = z1 - z0;

      for(int i = 0; i < segments; ++i) {
         double a0 = (Math.PI * 2D) * (double)i / (double)segments;
         double a1 = (Math.PI * 2D) * (double)(i + 1) / (double)segments;
         float c0 = (float)Math.cos(a0);
         float s0 = (float)Math.sin(a0);
         float c1 = (float)Math.cos(a1);
         float s1 = (float)Math.sin(a1);
         float x0 = cx + c0 * radius;
         float y0 = cy + s0 * radius;
         float x1 = cx + c1 * radius;
         float y1 = cy + s1 * radius;
         if ((this.faces & 60) != 0) {
            float v0 = this.side.v((float)i / (float)segments);
            float v1 = this.side.v((float)(i + 1) / (float)segments);
            float uEnd = this.side.u(Math.min(1.0F, length));
            float uStart = this.side.u(0.0F);
            this.vertex(x0, y0, z1, c0, s0, 0.0F, uStart, v0, 1.0F);
            this.vertex(x0, y0, z0, c0, s0, 0.0F, uEnd, v0, 1.0F);
            this.vertex(x1, y1, z0, c1, s1, 0.0F, uEnd, v1, 1.0F);
            this.vertex(x0, y0, z1, c0, s0, 0.0F, uStart, v0, 1.0F);
            this.vertex(x1, y1, z0, c1, s1, 0.0F, uEnd, v1, 1.0F);
            this.vertex(x1, y1, z1, c1, s1, 0.0F, uStart, v1, 1.0F);
         }

         if ((this.faces & 4) != 0) {
            this.vertex(cx, cy, z1, 0.0F, 0.0F, 1.0F, this.top.u(0.5F), this.top.v(0.5F), 1.0F);
            this.vertex(x0, y0, z1, 0.0F, 0.0F, 1.0F, this.top.u(0.5F + c0 * 0.5F), this.top.v(0.5F - s0 * 0.5F), 1.0F);
            this.vertex(x1, y1, z1, 0.0F, 0.0F, 1.0F, this.top.u(0.5F + c1 * 0.5F), this.top.v(0.5F - s1 * 0.5F), 1.0F);
         }

         if ((this.faces & 8) != 0) {
            this.vertex(cx, cy, z0, 0.0F, 0.0F, -1.0F, this.top.u(0.5F), this.top.v(0.5F), 1.0F);
            this.vertex(x1, y1, z0, 0.0F, 0.0F, -1.0F, this.top.u(0.5F - c1 * 0.5F), this.top.v(0.5F - s1 * 0.5F), 1.0F);
            this.vertex(x0, y0, z0, 0.0F, 0.0F, -1.0F, this.top.u(0.5F - c0 * 0.5F), this.top.v(0.5F - s0 * 0.5F), 1.0F);
         }
      }

   }

   void vertex(float x, float y, float z, float nx, float ny, float nz, float u, float v, float light) {
      if (this.buffer.remaining() < 32) {
         this.grow();
      }

      Mat4.transform(this.m, x, y, z, this.p);
      float wx = this.m[0] * nx + this.m[4] * ny + this.m[8] * nz;
      float wy = this.m[1] * nx + this.m[5] * ny + this.m[9] * nz;
      float wz = this.m[2] * nx + this.m[6] * ny + this.m[10] * nz;
      float l = (float)Math.sqrt((double)(wx * wx + wy * wy + wz * wz));
      if (l > 0.0F) {
         wx /= l;
         wy /= l;
         wz /= l;
      }

      this.buffer.putFloat(this.p[0]).putFloat(this.p[1]).putFloat(this.p[2]);
      this.buffer.put((byte)Math.round(wx * 127.0F)).put((byte)Math.round(wy * 127.0F)).put((byte)Math.round(wz * 127.0F)).put((byte)0);
      this.buffer.putFloat(u).putFloat(v);
      int r = this.colour >>> 16 & 255;
      int g = this.colour >>> 8 & 255;
      int b = this.colour & 255;
      int a = this.colour >>> 24;
      this.buffer.put((byte)Math.round((float)r * light)).put((byte)Math.round((float)g * light)).put((byte)Math.round((float)b * light)).put((byte)a);
      this.buffer.put((byte)this.emissive).put((byte)(this.kind * 32)).put((byte)this.gloss).put((byte)0);
      ++this.vertices;
   }

   private void grow() {
      ByteBuffer bigger = MemoryUtil.memAlloc(this.buffer.capacity() * 2).order(ByteOrder.nativeOrder());
      this.buffer.flip();
      bigger.put(this.buffer);
      MemoryUtil.memFree(this.buffer);
      this.buffer = bigger;
   }
}
