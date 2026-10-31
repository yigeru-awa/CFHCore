package com.onehumanawa.cfhcore.content.gui.menu.client.death;

import com.onehumanawa.cfhcore.content.gui.core.gfx.Clock;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiModal;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

final class LeaveConfirm extends CFHGuiModal {
   private static final int ARM_MS = 1000;
   private static final int OUT_MS = 200;
   private final long shownMs = Clock.nowMs();
   private final Rule rule = new Rule(this);

   private LeaveConfirm() {
      super(Component.translatable("deathScreen.titleScreen"), Component.translatable("deathScreen.quit.confirm"), Icon.QUESTION);
   }

   static void show(Runnable leave, Runnable respawn) {
      LeaveConfirm dialog = new LeaveConfirm();
      dialog.extra(dialog.rule);
      dialog.button(Component.translatable("deathScreen.titleScreen"), Variant.SECONDARY, leave);
      dialog.button(Component.translatable("deathScreen.respawn"), Variant.PRIMARY, respawn);
      dialog.show();
   }

   private long shownFor() {
      return Clock.nowMs() - this.shownMs;
   }

   private boolean armed() {
      return this.shownFor() >= 1000L;
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      boolean armed = this.armed();
      super.render(g, armed ? mouseX : -9999, armed ? mouseY : -9999, partialTick);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return !this.armed() || super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.armed()) {
         return keyCode != 256;
      } else {
         boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
         if (keyCode == 258 && this.rule.isFocused()) {
            super.keyPressed(keyCode, scanCode, modifiers);
         }

         return handled;
      }
   }

   private static final class Rule extends AbstractWidget {
      private final LeaveConfirm dialog;

      Rule(LeaveConfirm dialog) {
         super(0, 0, 10, 1, Component.empty());
         this.dialog = dialog;
         this.active = false;
      }

      protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         Theme t = Theme.current();
         long shown = this.dialog.shownFor();
         CFHGuiDraw.hline(g, this.getX(), this.getY(), this.getWidth(), Colors.scaleAlpha(t.isVanilla() ? 1090519039 : t.palette().border(), this.alpha));
         float out = 1.0F - Mth.clamp((float)(shown - 1000L) / 200.0F, 0.0F, 1.0F);
         if (!(out <= 0.01F)) {
            int fill = Math.round((float)this.getWidth() * Mth.clamp((float)shown / 1000.0F, 0.0F, 1.0F));
            CFHGuiDraw.hline(g, this.getX(), this.getY(), fill, Colors.scaleAlpha(t.isVanilla() ? -1 : t.palette().accentInk(), this.alpha * out));
         }
      }

      protected void updateWidgetNarration(NarrationElementOutput out) {
      }

      public void playDownSound(SoundManager sounds) {
      }
   }
}
