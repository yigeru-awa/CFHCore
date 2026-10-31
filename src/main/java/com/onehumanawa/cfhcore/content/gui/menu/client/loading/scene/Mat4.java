package com.onehumanawa.cfhcore.content.gui.menu.client.loading.scene;

final class Mat4 {
   static float[] identity() {
      float[] m = new float[16];
      m[0] = m[5] = m[10] = m[15] = 1.0F;
      return m;
   }

   static void set(float[] to, float[] from) {
      System.arraycopy(from, 0, to, 0, 16);
   }

   static void mul(float[] out, float[] a, float[] b) {
      float[] r = new float[16];

      for(int c = 0; c < 4; ++c) {
         for(int row = 0; row < 4; ++row) {
            r[c * 4 + row] = a[row] * b[c * 4] + a[4 + row] * b[c * 4 + 1] + a[8 + row] * b[c * 4 + 2] + a[12 + row] * b[c * 4 + 3];
         }
      }

      System.arraycopy(r, 0, out, 0, 16);
   }

   static float[] perspective(float fovY, float aspect, float near, float far, float shiftX, float shiftY) {
      float f = (float)((double)1.0F / Math.tan(Math.toRadians((double)fovY) / (double)2.0F));
      float[] m = new float[16];
      m[0] = f / aspect;
      m[5] = f;
      m[8] = -shiftX;
      m[9] = -shiftY;
      m[10] = (far + near) / (near - far);
      m[11] = -1.0F;
      m[14] = 2.0F * far * near / (near - far);
      return m;
   }

   static float[] ortho(float l, float r, float b, float t, float n, float f) {
      float[] m = new float[16];
      m[0] = 2.0F / (r - l);
      m[5] = 2.0F / (t - b);
      m[10] = -2.0F / (f - n);
      m[12] = -(r + l) / (r - l);
      m[13] = -(t + b) / (t - b);
      m[14] = -(f + n) / (f - n);
      m[15] = 1.0F;
      return m;
   }

   static float[] lookAt(float ex, float ey, float ez, float tx, float ty, float tz, float ux, float uy, float uz) {
      float fx = tx - ex;
      float fy = ty - ey;
      float fz = tz - ez;
      float fl = (float)Math.sqrt((double)(fx * fx + fy * fy + fz * fz));
      fx /= fl;
      fy /= fl;
      fz /= fl;
      float sx = fy * uz - fz * uy;
      float sy = fz * ux - fx * uz;
      float sz = fx * uy - fy * ux;
      float sl = (float)Math.sqrt((double)(sx * sx + sy * sy + sz * sz));
      sx /= sl;
      sy /= sl;
      sz /= sl;
      float vx = sy * fz - sz * fy;
      float vy = sz * fx - sx * fz;
      float vz = sx * fy - sy * fx;
      float[] m = new float[16];
      m[0] = sx;
      m[4] = sy;
      m[8] = sz;
      m[1] = vx;
      m[5] = vy;
      m[9] = vz;
      m[2] = -fx;
      m[6] = -fy;
      m[10] = -fz;
      m[12] = -(sx * ex + sy * ey + sz * ez);
      m[13] = -(vx * ex + vy * ey + vz * ez);
      m[14] = fx * ex + fy * ey + fz * ez;
      m[15] = 1.0F;
      return m;
   }

   static void translate(float[] m, float x, float y, float z) {
      m[12] += m[0] * x + m[4] * y + m[8] * z;
      m[13] += m[1] * x + m[5] * y + m[9] * z;
      m[14] += m[2] * x + m[6] * y + m[10] * z;
      m[15] += m[3] * x + m[7] * y + m[11] * z;
   }

   static void scale(float[] m, float x, float y, float z) {
      for(int i = 0; i < 4; ++i) {
         m[i] *= x;
         m[4 + i] *= y;
         m[8 + i] *= z;
      }

   }

   static void rotate(float[] m, float degrees, float ax, float ay, float az) {
      double rad = Math.toRadians((double)degrees);
      float c = (float)Math.cos(rad);
      float s = (float)Math.sin(rad);
      float t = 1.0F - c;
      float[] r = identity();
      r[0] = t * ax * ax + c;
      r[1] = t * ax * ay + s * az;
      r[2] = t * ax * az - s * ay;
      r[4] = t * ax * ay - s * az;
      r[5] = t * ay * ay + c;
      r[6] = t * ay * az + s * ax;
      r[8] = t * ax * az + s * ay;
      r[9] = t * ay * az - s * ax;
      r[10] = t * az * az + c;
      mul(m, m, r);
   }

   static void transform(float[] m, float x, float y, float z, float[] out) {
      out[0] = m[0] * x + m[4] * y + m[8] * z + m[12];
      out[1] = m[1] * x + m[5] * y + m[9] * z + m[13];
      out[2] = m[2] * x + m[6] * y + m[10] * z + m[14];
      out[3] = m[3] * x + m[7] * y + m[11] * z + m[15];
   }

   private Mat4() {
   }
}
