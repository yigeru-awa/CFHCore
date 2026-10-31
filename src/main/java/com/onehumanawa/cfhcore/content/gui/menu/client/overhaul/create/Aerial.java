package com.onehumanawa.cfhcore.content.gui.menu.client.overhaul.create;

import net.minecraft.util.Mth;

public final class Aerial {
   private static final double BELOW_HORIZON = (double)16.0F;

   public static View over(float halfX, float halfZ, float ground, Margins margins, double halfAcross, float aspect, double pitch) {
      double tanAcross = Math.tan(Math.toRadians(halfAcross));
      double tanUp = tanAcross / (double)Math.max(0.2F, aspect);
      double down = Math.toRadians(Math.max(pitch, Math.toDegrees(Math.atan(tanUp)) + (double)16.0F));
      double sin = Math.sin(down);
      double cos = Math.cos(down);
      double top = sin - tanUp * cos;
      double wide = tanAcross / top;
      double far = (cos + tanUp * sin) / top;
      double near = (cos - tanUp * sin) / (sin + tanUp * cos);
      double height = Math.max((double)0.5F, Math.min((double)(halfX - margins.side()) / wide, ((double)2.0F * (double)halfZ - (double)margins.far() - (double)margins.near()) / (far - near)));
      double eyeZ = height * far - (double)halfZ + (double)margins.far();
      return new View((float)((double)ground + height), (float)eyeZ, ground, (float)(eyeZ - height * cos / sin), (float)Math.toDegrees((double)2.0F * Math.atan(tanUp)));
   }

   public static float shelf(int rows, int shelf) {
      float t = Mth.clamp(((float)rows + 0.5F) / (float)Math.max(1, shelf), 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   private Aerial() {
   }

   public static record View(float eyeY, float eyeZ, float targetY, float targetZ, float fov) {
      public View back(float times) {
         return new View(this.targetY + (this.eyeY - this.targetY) * times, this.targetZ + (this.eyeZ - this.targetZ) * times, this.targetY, this.targetZ, this.fov);
      }
   }

   public static record Margins(float side, float far, float near) {
   }
}
