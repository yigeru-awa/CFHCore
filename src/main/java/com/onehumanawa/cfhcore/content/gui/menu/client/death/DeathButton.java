package com.onehumanawa.cfhcore.content.gui.menu.client.death;

import com.mojang.blaze3d.systems.RenderSystem;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Anim;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Ease;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.core.gfx.CFHGuiDraw;
import com.onehumanawa.cfhcore.content.gui.core.theme.Colors;
import com.onehumanawa.cfhcore.content.gui.core.theme.Palette;
import com.onehumanawa.cfhcore.content.gui.core.theme.Theme;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiSpinner;
import com.onehumanawa.cfhcore.content.gui.core.widget.CFHGuiButton.Variant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

final class DeathButton extends CFHGuiButton {
   private static final ResourceLocation DRAFT_REPORT = ResourceLocation.withDefaultNamespace("icon/draft_report");
   private static final Component BUSY = Component.translatable("menu.respawning");
   private static final int SPINNER = 10;
   private final DeathScreenBase screen;
   private final boolean respawn;
   private final Anim line;

   DeathButton(DeathScreenBase screen, int x, int y, int w, int h, Component label, Icon icon, boolean respawn, Runnable onPress) {
      super(x, y, w, h, label, onPress);
      this.line = new Anim(1.0F, 240, Ease.OUT_CUBIC);
      this.screen = screen;
      this.respawn = respawn;
      this.icon = icon;
      this.variant = respawn ? Variant.PRIMARY : Variant.GHOST;
      if (screen.armed()) {
         this.line.snap(0.0F);
      }

   }

   protected void renderDark(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Theme t = Theme.current();
      Palette p = t.palette();
      float arm = this.screen.arming();
      boolean busy = this.respawn && this.screen.respawning();
      float a = this.effectiveAlpha();
      int x = this.getX();
      int y = this.getY() + this.enterOffset();
      int w = this.getWidth();
      int h = this.getHeight();
      if (arm >= 1.0F && !busy) {
         super.renderDark(g, mouseX, mouseY, partialTick);
         this.line.set(0.0F);
         if (this.respawn) {
            line(g, x + 4, y + Math.round(this.press()) + h - 4, w - 8, 1.0F, p.accentText(), a * this.line.get());
         } else {
            this.mark(g, x, y, w, h, a);
         }

      } else if (!(a <= 0.02F)) {
         this.line.snap(1.0F);
         if (!this.respawn) {
            this.drawContent(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(p.textDim(), p.textMuted(), arm), a), false);
            this.mark(g, x, y, w, h, a);
         } else {
            int fill = busy ? p.surface() : Colors.lerp(p.surface(), p.accent(), 0.4F + 0.6F * arm);
            int border = busy ? p.border() : Colors.lerp(p.border(), Colors.brighten(p.accent(), 0.15F), arm);
            CFHGuiDraw.shadow(g, x, y, w, h, 0.35F * a);
            CFHGuiDraw.pixelRound(g, x, y, w, h, Colors.scaleAlpha(fill, a), t.radius());
            CFHGuiDraw.outline(g, x, y, w, h, Colors.scaleAlpha(border, a), t.radius());
            if (busy) {
               this.busy(g, x, y, w, h, Colors.scaleAlpha(p.textMuted(), a), Colors.scaleAlpha(p.accentInk(), a), false);
            } else {
               this.drawContent(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(p.textMuted(), p.accentText(), arm), a), false);
               line(g, x + 4, y + h - 4, w - 8, arm, p.accentText(), a);
            }

         }
      }
   }

   protected void renderVanilla(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      float arm = this.screen.arming();
      boolean busy = this.respawn && this.screen.respawning();
      float a = this.effectiveAlpha();
      int x = this.getX();
      int y = this.getY() + this.enterOffset();
      int w = this.getWidth();
      int h = this.getHeight();
      if (arm >= 1.0F && !busy) {
         super.renderVanilla(g, mouseX, mouseY, partialTick);
         this.line.set(0.0F);
         if (this.respawn) {
            line(g, x + 3, y + Math.round(this.press()) + h - 5, w - 6, 1.0F, -1, a * this.line.get());
         } else {
            this.mark(g, x, y, w, h, a);
         }

      } else if (!(a <= 0.02F)) {
         this.line.snap(1.0F);
         if (!this.respawn) {
            this.drawContent(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(-7303024, -2039584, arm), a), true);
            this.mark(g, x, y, w, h, a);
         } else {
            CFHGuiDraw.vanillaButton(g, x, y, w, h, 0.0F, !busy, a);
            if (busy) {
               this.busy(g, x, y, w, h, Colors.scaleAlpha(-6250336, a), Colors.scaleAlpha(-2039584, a), true);
            } else {
               if (this.variant() == Variant.PRIMARY) {
                  CFHGuiDraw.rect(g, x + 1, y + 1, w - 2, h - 2, Colors.scaleAlpha(Colors.withAlpha(Theme.current().palette().accent(), 90), a * arm));
               }

               this.drawContent(g, x, y, w, h, Colors.scaleAlpha(Colors.lerp(-5197648, -1, arm), a), true);
               line(g, x + 3, y + h - 5, w - 6, arm, -1, a);
            }

         }
      }
   }

   private static void line(GuiGraphics g, int x, int y, int width, float progress, int color, float alpha) {
      if (!(alpha <= 0.01F)) {
         CFHGuiDraw.rect(g, x, y, Math.round((float)width * Mth.clamp(progress, 0.0F, 1.0F)), 2, Colors.withAlpha(color, Math.round(200.0F * Mth.clamp(alpha, 0.0F, 1.0F))));
      }
   }

   private void busy(GuiGraphics g, int x, int y, int w, int h, int fg, int spinner, boolean shadow) {
      FormattedCharSequence words = CFHGuiDraw.truncate(BUSY, w - 12 - 10 - 5);
      int cx = x + (w - 10 - 5 - CFHGuiDraw.width(words)) / 2;
      CFHGuiSpinner.draw(g, cx, y + (h - 10) / 2, 10, spinner);
      if (fg >>> 24 >= 4) {
         g.drawString(CFHGuiDraw.font(), words, cx + 10 + 5, CFHGuiDraw.textY(y, h), fg, shadow);
      }

   }

   private void mark(GuiGraphics g, int x, int y, int w, int h, float alpha) {
      if (!(alpha <= 0.02F) && Minecraft.getInstance().getReportingContext().hasDraftReport()) {
         RenderSystem.enableBlend();
         g.setColor(1.0F, 1.0F, 1.0F, alpha);
         g.blitSprite(DRAFT_REPORT, x + w - 17, y + (h - 15) / 2 + 1, 15, 15);
         g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
      }
   }
}
