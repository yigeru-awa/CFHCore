package com.onehumanawa.cfhcore.content.gui.core.client;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import net.minecraft.client.Options;
import org.jetbrains.annotations.Nullable;

public final class CompactAutoScale {
   public static int apply(int vanillaScale, @Nullable Options options) {
      if (vanillaScale >= 3 && options != null && (Integer)options.guiScale().get() == 0) {
         boolean compact = true;

         try {
            compact = CFHGui.config().compactAutoScale;
         } catch (RuntimeException var4) {
         }

         return compact ? vanillaScale - 1 : vanillaScale;
      } else {
         return vanillaScale;
      }
   }

   private CompactAutoScale() {
   }
}
