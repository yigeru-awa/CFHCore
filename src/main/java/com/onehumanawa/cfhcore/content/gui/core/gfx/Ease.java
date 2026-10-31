package com.onehumanawa.cfhcore.content.gui.core.gfx;

public enum Ease {
   LINEAR,
   OUT_CUBIC,
   IN_OUT_CUBIC,
   OUT_QUINT,
   OUT_BACK,
   OUT_EXPO,
   SPRING;

   public float apply(float t) {
      float x = t <= 0.0F ? 0.0F : Math.min(1.0F, t);
      float var10000;
      switch (this.ordinal()) {
         case 0:
            var10000 = x;
            break;
         case 1:
            var10000 = 1.0F - (1.0F - x) * (1.0F - x) * (1.0F - x);
            break;
         case 2:
            var10000 = x < 0.5F ? 4.0F * x * x * x : 1.0F - (float)Math.pow((double)(-2.0F * x + 2.0F), (double)3.0F) / 2.0F;
            break;
         case 3:
            var10000 = 1.0F - (float)Math.pow((double)(1.0F - x), (double)5.0F);
            break;
         case 4:
            float c1 = 1.70158F;
            float c3 = 2.70158F;
            var10000 = 1.0F + 2.70158F * (float)Math.pow((double)(x - 1.0F), (double)3.0F) + 1.70158F * (float)Math.pow((double)(x - 1.0F), (double)2.0F);
            break;
         case 5:
            var10000 = x >= 1.0F ? 1.0F : 1.0F - (float)Math.pow((double)2.0F, (double)(-10.0F * x));
            break;
         case 6:
            float d = 1.0F - x;
            var10000 = 1.0F - d * d * (float)Math.cos((double)x * Math.PI * (double)1.5F);
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }
}
