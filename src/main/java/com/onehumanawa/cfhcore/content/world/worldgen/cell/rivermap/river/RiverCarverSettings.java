package com.onehumanawa.cfhcore.content.world.worldgen.cell.rivermap.river;

import java.util.Random;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.function.CurveFunction;
import com.onehumanawa.cfhcore.content.world.worldgen.noise.function.CurveFunctions;

public class RiverCarverSettings {
   public float valleySize = 275.0F;
   public float fadeIn = 0.7F;
   public boolean connecting = false;
   public CurveFunction valleyCurve;

   public RiverCarverSettings(Random random) {
      this.valleyCurve = getValleyType(random);
   }

   public static CurveFunction getValleyType(Random random) {
      int value = random.nextInt(100);
      if (value < 5) {
         return CurveFunctions.scurve(0.4F, 1.0F);
      } else if (value < 30) {
         return CurveFunctions.scurve(4.0F, 5.0F);
      } else {
         return value < 50 ? CurveFunctions.scurve(3.0F, 0.25F) : CurveFunctions.scurve(2.0F, -0.5F);
      }
   }

   public static enum RiverZone {
      Riverbed,
      Banks,
      ValleyFloor,
      ValleyFadeout,
      None;
   }
}
