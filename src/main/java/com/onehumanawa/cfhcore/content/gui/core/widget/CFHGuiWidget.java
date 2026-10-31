package com.onehumanawa.cfhcore.content.gui.core.widget;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.module.Features;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public abstract class CFHGuiWidget extends AbstractWidget {
   public static final int ENTER_SLIDE = 6;
   protected final Anim hoverAnim;
   protected final Anim pressAnim;
   protected final Anim focusAnim;
   protected final Anim enterAnim;
   private @Nullable List<Component> tip;
   private @Nullable String lockedBy;
   private long hoverSinceMs;
   private boolean pressed;
   private boolean silent;
   private long enterStartMs;

   protected CFHGuiWidget(int x, int y, int width, int height, Component message) {
      super(x, y, width, height, message);
      this.hoverAnim = new Anim(0.0F, 140, Ease.OUT_CUBIC);
      this.pressAnim = new Anim(0.0F, 90, Ease.OUT_CUBIC);
      this.focusAnim = new Anim(0.0F, 160, Ease.OUT_CUBIC);
      this.enterAnim = new Anim(1.0F, 220, Ease.OUT_CUBIC);
   }

   public CFHGuiWidget tip(Component tooltip) {
      this.tip = tooltip != null && !tooltip.getString().isEmpty() ? List.of(tooltip) : null;
      return this;
   }

   public CFHGuiWidget tip(List<Component> lines) {
      this.tip = lines != null && !lines.isEmpty() ? List.copyOf(lines) : null;
      return this;
   }

   public @Nullable List<Component> tipLines() {
      return this.tip;
   }

   public void playEntrance(int delayMs) {
      float motion = Theme.current().motion();
      if (motion <= 0.0F) {
         this.enterAnim.snap(1.0F);
         this.enterStartMs = 0L;
      } else {
         this.enterAnim.snap(0.0F);
         this.enterStartMs = Clock.nowMs() + (long)Math.max(0, Math.round((float)delayMs * motion));
      }
   }

   public CFHGuiWidget locked(@Nullable String moduleId) {
      this.lockedBy = moduleId;
      if (moduleId != null) {
         this.tip(Features.lockedTooltip(moduleId));
      }

      return this;
   }

   public boolean isLocked() {
      return this.lockedBy != null;
   }

   public @Nullable String lockedBy() {
      return this.lockedBy;
   }

   public CFHGuiWidget silent() {
      this.silent = true;
      return this;
   }

   public CFHGuiWidget enabled(boolean enabled) {
      this.active = enabled;
      return this;
   }

   public CFHGuiWidget shown(boolean visible) {
      this.visible = visible;
      return this;
   }

   public float hover() {
      return this.hoverAnim.get();
   }

   public float press() {
      return this.pressAnim.get();
   }

   public float focus() {
      return this.focusAnim.get();
   }

   protected float enterProgress() {
      if (this.enterStartMs != 0L) {
         if (Theme.current().motion() <= 0.0F) {
            this.enterStartMs = 0L;
            this.enterAnim.snap(1.0F);
            return 1.0F;
         }

         if (Clock.nowMs() < this.enterStartMs) {
            return 0.0F;
         }

         this.enterStartMs = 0L;
         this.enterAnim.set(1.0F, 220);
      }

      return Math.min(1.0F, Math.max(0.0F, this.enterAnim.get()));
   }

   public float effectiveAlpha() {
      return this.alpha * this.enterProgress();
   }

   protected int enterOffset() {
      return Math.round((1.0F - this.enterProgress()) * 6.0F);
   }

   protected boolean isPressed() {
      return this.pressed;
   }

   public boolean isEntering() {
      return this.enterStartMs != 0L || this.enterAnim.isAnimating();
   }

   protected final void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      boolean hovered = this.active && this.isHovered() && g.containsPointInScissor(mouseX, mouseY);
      this.hoverAnim.set(hovered);
      this.focusAnim.set(this.active && this.isFocused() && !hovered && CFHGuiDraw.focusShown());
      this.pressAnim.set(this.pressed && hovered);
      if (hovered) {
         if (this.hoverSinceMs == 0L) {
            this.hoverSinceMs = Clock.nowMs();
         }

         if (this.tip != null && Clock.nowMs() - this.hoverSinceMs > 350L) {
            CFHGuiTooltips.request(this.tip, this);
         }
      } else {
         this.hoverSinceMs = 0L;
      }

      if (Theme.current().isVanilla()) {
         this.renderVanilla(g, mouseX, mouseY, partialTick);
      } else {
         this.renderDark(g, mouseX, mouseY, partialTick);
      }

   }

   protected abstract void renderDark(GuiGraphics var1, int var2, int var3, float var4);

   protected abstract void renderVanilla(GuiGraphics var1, int var2, int var3, float var4);

   public void onClick(double mouseX, double mouseY) {
      if (!this.isLocked()) {
         this.pressed = true;
         this.pressAnim.snap(1.0F);
      }
   }

   public void onRelease(double mouseX, double mouseY) {
      this.pressed = false;
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.pressed = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public void playDownSound(SoundManager handler) {
      if (!this.silent && !this.isLocked()) {
         CFHGuiSounds.click(handler);
      }

   }

   protected void flashPress() {
      this.pressAnim.snap(1.0F);
      this.pressAnim.set(0.0F);
   }

   protected void updateWidgetNarration(NarrationElementOutput out) {
      this.defaultButtonNarrationText(out);
   }

   public boolean contains(double mx, double my) {
      return mx >= (double)this.getX() && mx < (double)(this.getX() + this.getWidth()) && my >= (double)this.getY() && my < (double)(this.getY() + this.getHeight());
   }
}
