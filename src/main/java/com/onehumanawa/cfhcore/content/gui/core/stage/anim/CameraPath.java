package com.onehumanawa.cfhcore.content.gui.core.stage.anim;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import java.util.Locale;

public interface CameraPath {
   void sample(float var1, CameraPose var2);

   float durationMs();

   Ease ease();

   default void begin(CameraPose current) {
   }

   static Ease parseEase(String name) {
      if (name == null) {
         return Ease.OUT_CUBIC;
      } else {
         Ease var10000;
         switch (name.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "")) {
            case "linear" -> var10000 = Ease.LINEAR;
            case "inoutcubic" -> var10000 = Ease.IN_OUT_CUBIC;
            case "outquint" -> var10000 = Ease.OUT_QUINT;
            case "outback" -> var10000 = Ease.OUT_BACK;
            case "outexpo" -> var10000 = Ease.OUT_EXPO;
            case "spring" -> var10000 = Ease.SPRING;
            default -> var10000 = Ease.OUT_CUBIC;
         }

         return var10000;
      }
   }
}
