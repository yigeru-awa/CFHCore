package com.onehumanawa.cfhcore.content.gui.core.layout.ui;

import net.minecraft.client.gui.components.AbstractWidget;

public final class Flow {
   private final boolean vertical;
   private final int gap;
   private int x;
   private int y;
   private int maxX;
   private int maxY;

   private Flow(int x, int y, int gap, boolean vertical) {
      this.x = x;
      this.y = y;
      this.gap = gap;
      this.vertical = vertical;
      this.maxX = x;
      this.maxY = y;
   }

   public static Flow column(int x, int y, int gap) {
      return new Flow(x, y, gap, true);
   }

   public static Flow row(int x, int y, int gap) {
      return new Flow(x, y, gap, false);
   }

   public <T extends AbstractWidget> T place(T w) {
      w.setX(this.x);
      w.setY(this.y);
      this.advance(w.getWidth(), w.getHeight());
      return w;
   }

   public Flow skip(int px) {
      if (this.vertical) {
         this.y += px;
      } else {
         this.x += px;
      }

      return this;
   }

   private void advance(int w, int h) {
      this.maxX = Math.max(this.maxX, this.x + w);
      this.maxY = Math.max(this.maxY, this.y + h);
      if (this.vertical) {
         this.y += h + this.gap;
      } else {
         this.x += w + this.gap;
      }

   }

   public int x() {
      return this.x;
   }

   public int y() {
      return this.y;
   }

   public int maxX() {
      return this.maxX;
   }

   public int maxY() {
      return this.maxY;
   }
}
