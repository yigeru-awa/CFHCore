package com.onehumanawa.cfhcore.content.gui.menu.client.infohud;

final class Rects {
   static final int MAX = 24;
   final int[] v = new int[96];
   int n;

   void clear() {
      this.n = 0;
   }

   void add(int x0, int y0, int x1, int y1) {
      if (this.n < 24 && x1 > x0 && y1 > y0) {
         int i = this.n++ * 4;
         this.v[i] = x0;
         this.v[i + 1] = y0;
         this.v[i + 2] = x1;
         this.v[i + 3] = y1;
      }
   }

   int x0(int i) {
      return this.v[i * 4];
   }

   int y0(int i) {
      return this.v[i * 4 + 1];
   }

   int x1(int i) {
      return this.v[i * 4 + 2];
   }

   int y1(int i) {
      return this.v[i * 4 + 3];
   }

   boolean hits(int x, int y, int w, int h) {
      for(int i = 0; i < this.n; ++i) {
         if (x < this.x1(i) && x + w > this.x0(i) && y < this.y1(i) && y + h > this.y0(i)) {
            return true;
         }
      }

      return false;
   }
}
