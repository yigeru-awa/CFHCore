package com.onehumanawa.cfhcore.content.gui.core.compat.maps.mixin;

import com.onehumanawa.cfhcore.content.gui.core.compat.maps.MapsCompat;
import java.util.ArrayList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.MapTileSelection;

@Pseudo
@Mixin(
   targets = {"xaero.map.gui.GuiMap"},
   remap = false
)
public abstract class XaeroGuiMapMixin {
   @Shadow
   private int rightClickX;
   @Shadow
   private int rightClickY;
   @Shadow
   private int rightClickZ;
   @Shadow
   private ResourceKey<Level> rightClickDim;
   @Shadow
   private MapTileSelection mapTileSelection;

   @Inject(
      method = {"getRightClickOptions"},
      at = {@At("RETURN")},
      remap = false,
      require = 0
   )
   private void cfhgui$places(CallbackInfoReturnable<ArrayList<?>> cir) {
      ArrayList<?> options = (ArrayList)cir.getReturnValue();
      if (options != null) {
         int[] selection = null;

         try {
            MapTileSelection marked = this.mapTileSelection;
            if (marked != null) {
               selection = new int[]{marked.getLeft(), marked.getTop(), marked.getRight(), marked.getBottom()};
            }
         } catch (Throwable var5) {
         }

         MapsCompat.xaeroMapMenu(this, options, this.rightClickX, this.rightClickY, this.rightClickZ, this.rightClickDim, selection);
      }
   }
}
