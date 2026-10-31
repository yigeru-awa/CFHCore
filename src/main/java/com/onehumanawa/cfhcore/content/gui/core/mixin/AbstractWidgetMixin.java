package com.onehumanawa.cfhcore.content.gui.core.mixin;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.screen.reskin.ReskinState;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({AbstractWidget.class})
public abstract class AbstractWidgetMixin implements ReskinState {
   @Unique
   private Anim cfhgui$hover;
   @Unique
   private Anim cfhgui$press;

   public Anim cfhgui$hoverAnim() {
      if (this.cfhgui$hover == null) {
         this.cfhgui$hover = new Anim(0.0F, 140, Ease.OUT_CUBIC);
      }

      return this.cfhgui$hover;
   }

   public Anim cfhgui$pressAnim() {
      if (this.cfhgui$press == null) {
         this.cfhgui$press = new Anim(0.0F, 90, Ease.OUT_CUBIC);
      }

      return this.cfhgui$press;
   }
}
