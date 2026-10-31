package com.onehumanawa.cfhcore.content.gui.core.module;

import com.onehumanawa.cfhcore.content.gui.core.screen.slot.Layout;
import com.onehumanawa.cfhcore.content.gui.core.screen.slot.MenuSlots;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiWidget;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class Features {
   public static boolean present(String moduleId) {
      return Modules.isLoaded(moduleId);
   }

   public static State state(String moduleId, Layout layout) {
      if (present(moduleId)) {
         return Features.State.AVAILABLE;
      } else {
         return layout == Layout.OVERHAUL ? Features.State.LOCKED : Features.State.HIDDEN;
      }
   }

   public static State state(String moduleId, String slotId) {
      return state(moduleId, MenuSlots.effective(slotId));
   }

   public static <T extends CFHGuiWidget> @Nullable T gate(String moduleId, Layout layout, Supplier<T> factory) {
      CFHGuiWidget var10000;
      switch (state(moduleId, layout).ordinal()) {
         case 0:
            var10000 = (CFHGuiWidget)factory.get();
            break;
         case 1:
            T w = (T)(factory.get());
            w.locked(moduleId);
            var10000 = w;
            break;
         case 2:
            var10000 = null;
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
      }

      return (T)var10000;
   }

   public static <T extends CFHGuiWidget> @Nullable T gate(String moduleId, String slotId, Supplier<T> factory) {
      return (T)gate(moduleId, MenuSlots.effective(slotId), factory);
   }

   public static Component lockedTooltip(String moduleId) {
      return Component.translatable("cfhgui.feature.locked", new Object[]{KnownModules.name(moduleId)});
   }

   private Features() {
   }

   public static enum State {
      AVAILABLE,
      LOCKED,
      HIDDEN;

      public boolean shown() {
         return this != HIDDEN;
      }
   }
}
