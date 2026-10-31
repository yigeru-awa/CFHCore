package com.onehumanawa.cfhcore.content.gui.core.layout.ui;

public record Rect(int x, int y, int w, int h) {
   public static Rect of(int x, int y, int w, int h) {
      return new Rect(x, y, w, h);
   }

   public int right() {
      return this.x + this.w;
   }

   public int bottom() {
      return this.y + this.h;
   }

   public int centerX() {
      return this.x + this.w / 2;
   }

   public int centerY() {
      return this.y + this.h / 2;
   }

   public boolean contains(double px, double py) {
      return px >= (double)this.x && px < (double)(this.x + this.w) && py >= (double)this.y && py < (double)(this.y + this.h);
   }

   public Rect inset(int all) {
      return new Rect(this.x + all, this.y + all, this.w - all * 2, this.h - all * 2);
   }

   public Rect inset(int horizontal, int vertical) {
      return new Rect(this.x + horizontal, this.y + vertical, this.w - horizontal * 2, this.h - vertical * 2);
   }

   public Rect withX(int nx) {
      return new Rect(nx, this.y, this.w, this.h);
   }

   public Rect withY(int ny) {
      return new Rect(this.x, ny, this.w, this.h);
   }

   public Rect withW(int nw) {
      return new Rect(this.x, this.y, nw, this.h);
   }

   public Rect withH(int nh) {
      return new Rect(this.x, this.y, this.w, nh);
   }

   public Rect move(int dx, int dy) {
      return new Rect(this.x + dx, this.y + dy, this.w, this.h);
   }

   public Rect[] splitTop(int px) {
      return new Rect[]{new Rect(this.x, this.y, this.w, px), new Rect(this.x, this.y + px, this.w, this.h - px)};
   }

   public Rect[] splitLeft(int px) {
      return new Rect[]{new Rect(this.x, this.y, px, this.h), new Rect(this.x + px, this.y, this.w - px, this.h)};
   }

   public Rect[] splitBottom(int px) {
      return new Rect[]{new Rect(this.x, this.y + this.h - px, this.w, px), new Rect(this.x, this.y, this.w, this.h - px)};
   }

   public Rect centered(int cw, int ch) {
      return new Rect(this.x + (this.w - cw) / 2, this.y + (this.h - ch) / 2, cw, ch);
   }

   public Rect intersect(Rect o) {
      int nx = Math.max(this.x, o.x);
      int ny = Math.max(this.y, o.y);
      int nr = Math.min(this.right(), o.right());
      int nb = Math.min(this.bottom(), o.bottom());
      return new Rect(nx, ny, Math.max(0, nr - nx), Math.max(0, nb - ny));
   }
}
